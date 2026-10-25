package com.example.nexuscore.service;

import com.example.nexuscore.categorization.GpcCategoryResolver;
import com.example.nexuscore.dto.stock.FoodRegistrationRequest;
import com.example.nexuscore.dto.stock.FoodResponse;
import com.example.nexuscore.dto.stock.MinimumQuantityRequest;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.dto.stock.StockItemRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.dto.stock.StockItemUpdateRequest;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.StockMapper;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.PantryProductSetting;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.CategoryRepository;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.PantryProductSettingRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockService {

    private final PantryItemRepository pantryItemRepository;
    private final PantryProductSettingRepository settingRepository;
    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;
    private final GpcCategoryResolver gpcCategoryResolver;
    private final ProductRankingService rankingService;
    private final ActivityRecordService activityRecordService;

    public StockService(PantryItemRepository pantryItemRepository,
                         PantryProductSettingRepository settingRepository,
                         FoodRepository foodRepository,
                         CategoryRepository categoryRepository,
                         ProfileRepository profileRepository,
                         GpcCategoryResolver gpcCategoryResolver,
                         ProductRankingService rankingService,
                         ActivityRecordService activityRecordService) {
        this.pantryItemRepository = pantryItemRepository;
        this.settingRepository = settingRepository;
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.gpcCategoryResolver = gpcCategoryResolver;
        this.rankingService = rankingService;
        this.activityRecordService = activityRecordService;
    }

    public List<StockItemResponse> list(Integer profileId) {
        return pantryItemRepository.findByProfileIdOrderByExpiryDateAscIdDesc(profileId).stream()
                .map(StockMapper::toResponse)
                .toList();
    }

    public List<StockItemResponse> expired(Integer profileId) {
        return pantryItemRepository.findByProfileIdAndExpiryDateBefore(profileId, LocalDate.now()).stream()
                .map(StockMapper::toResponse)
                .toList();
    }

    public List<PantryProductSettingResponse> missing(Integer profileId) {
        return settingRepository.findMissingByProfileId(profileId).stream()
                .map(StockMapper::toResponse)
                .toList();
    }

    public List<FoodResponse> searchFoods(String search) {
        List<Food> foods = search == null || search.isBlank()
                ? foodRepository.findAll()
                : foodRepository.findByNameContainingIgnoreCase(search);
        return foods.stream().map(StockMapper::toResponse).toList();
    }

    public List<PantryProductSettingResponse> listSettings(Integer profileId) {
        return settingRepository.findByProfileId(profileId).stream()
                .map(StockMapper::toResponse)
                .toList();
    }

    @Transactional
    public StockItemResponse add(Integer profileId, StockItemRequest request) {
        Profile profile = profileRepository.getReferenceById(profileId);
        Food food = foodRepository.findById(request.foodId())
                .orElseThrow(() -> new NotFoundException("Alimento nao encontrado: " + request.foodId()));
        PantryItem item = new PantryItem(food, profile, request.quantity(), request.expiryDate());
        PantryItem saved = pantryItemRepository.save(item);
        rankingService.recordRegistered(saved.getId(), food.getId(), profile.getAddress());
        activityRecordService.record(profileId, "stock_item_added",
                "Adicionou " + request.quantity() + " unidade(s) de " + food.getName() + " ao estoque");
        return StockMapper.toResponse(saved);
    }

    @Transactional
    public StockItemResponse registerFood(Integer profileId, FoodRegistrationRequest request) {
        Profile profile = profileRepository.getReferenceById(profileId);
        StockItemResponse response = StockMapper.toResponse(saveFoodRegistration(profile, request));
        recordFoodRegistration(profileId, profile.getAddress(), response);
        return response;
    }

    @Transactional
    public List<StockItemResponse> registerFoods(Integer profileId, List<FoodRegistrationRequest> requests) {
        Profile profile = profileRepository.getReferenceById(profileId);
        List<StockItemResponse> responses = requests.stream()
                .map(request -> StockMapper.toResponse(saveFoodRegistration(profile, request)))
                .toList();
        Address address = profile.getAddress();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                responses.forEach(response -> recordFoodRegistration(profileId, address, response));
            }
        });
        return responses;
    }

    private PantryItem saveFoodRegistration(Profile profile, FoodRegistrationRequest request) {
        Food food = foodRepository.findByGtin(request.gtin())
                .orElseGet(() -> createFoodFromRegistration(request));
        if (food.getFoodImageUrl() == null && request.foodImageUrl() != null) {
            food.setFoodImageUrl(request.foodImageUrl());
        }
        PantryItem item = new PantryItem(food, profile, request.quantity(), request.expiryDate());
        return pantryItemRepository.save(item);
    }

    private void recordFoodRegistration(Integer profileId, Address address, StockItemResponse response) {
        rankingService.recordRegistered(response.id(), response.foodId(), address);
        activityRecordService.record(profileId, "stock_item_added",
                "Adicionou " + response.quantity() + " unidade(s) de " + response.foodName() + " ao estoque");
    }

    private Food createFoodFromRegistration(FoodRegistrationRequest request) {
        if (!gpcCategoryResolver.isKnownFoodBrick(request.gpcCode())) {
            throw new IllegalArgumentException(
                    "Informe um código GPC alimentar conhecido para cadastrar um novo produto");
        }
        String categoryName = gpcCategoryResolver.resolve(request.gpcCode());
        Category category = categoryRepository.findByCategoryName(categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(categoryName)));

        Food food = new Food(request.name(), category, request.productBrand(),
                request.packageQuantity(), request.unitOfMeasure(), request.gtin(), request.foodImageUrl());
        return foodRepository.save(food);
    }

    @Transactional
    public StockItemResponse update(Integer profileId, Integer itemId, StockItemUpdateRequest request) {
        PantryItem item = findOwnedItem(profileId, itemId);
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.expiryDate() != null) {
            item.setExpiryDate(request.expiryDate());
            if (request.expiryDate().isBefore(LocalDate.now())) {
                rankingService.recordExpired(item.getId(), item.getFood().getId(),
                        item.getProfile().getAddress());
            }
        }
        PantryItem saved = pantryItemRepository.save(item);
        activityRecordService.record(profileId, "stock_item_updated",
                "Atualizou o estoque de " + item.getFood().getName());
        return StockMapper.toResponse(saved);
    }

    @Transactional
    public void remove(Integer profileId, Integer itemId) {
        PantryItem item = findOwnedItem(profileId, itemId);
        pantryItemRepository.delete(item);
        activityRecordService.record(profileId, "stock_item_removed",
                "Removeu " + item.getFood().getName() + " do estoque");
    }

    @Transactional
    public PantryProductSettingResponse setMinimumQuantity(Integer profileId, Integer foodId,
                                                              MinimumQuantityRequest request) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new NotFoundException("Alimento nao encontrado: " + foodId));
        PantryProductSetting setting = settingRepository.findByProfileIdAndFoodId(profileId, foodId)
                .orElseGet(() -> new PantryProductSetting(food,
                        profileRepository.getReferenceById(profileId), request.minimumQuantity()));
        setting.setMinimumQuantity(request.minimumQuantity());
        PantryProductSetting saved = settingRepository.save(setting);
        activityRecordService.record(profileId, "minimum_stock_quantity_changed",
                "Definiu o estoque mínimo de " + food.getName() + " para " + request.minimumQuantity());
        return StockMapper.toResponse(saved);
    }

    private PantryItem findOwnedItem(Integer profileId, Integer itemId) {
        PantryItem item = pantryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item de estoque nao encontrado: " + itemId));
        if (!item.getProfile().getId().equals(profileId)) {
            throw new ForbiddenException("Item de estoque nao pertence ao perfil autenticado");
        }
        return item;
    }

}
