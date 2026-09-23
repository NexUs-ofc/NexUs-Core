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

    public StockService(PantryItemRepository pantryItemRepository,
                         PantryProductSettingRepository settingRepository,
                         FoodRepository foodRepository,
                         CategoryRepository categoryRepository,
                         ProfileRepository profileRepository,
                         GpcCategoryResolver gpcCategoryResolver) {
        this.pantryItemRepository = pantryItemRepository;
        this.settingRepository = settingRepository;
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.gpcCategoryResolver = gpcCategoryResolver;
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
        return settingRepository.findByProfileId(profileId).stream()
                .filter(setting -> {
                    Integer current = pantryItemRepository
                            .sumQuantityByProfileIdAndFoodId(profileId, setting.getFood().getId());
                    return current < setting.getMinimumQuantity();
                })
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
        return StockMapper.toResponse(pantryItemRepository.save(item));
    }

    @Transactional
    public StockItemResponse registerFood(Integer profileId, FoodRegistrationRequest request) {
        Food food = foodRepository.findByGtin(request.gtin())
                .orElseGet(() -> createFoodFromRegistration(request));
        Profile profile = profileRepository.getReferenceById(profileId);
        PantryItem item = new PantryItem(food, profile, request.quantity(), request.expiryDate());
        return StockMapper.toResponse(pantryItemRepository.save(item));
    }

    private Food createFoodFromRegistration(FoodRegistrationRequest request) {
        String categoryName = gpcCategoryResolver.resolve(request.gpcCode());
        Category category = categoryRepository.findByCategoryName(categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(categoryName)));

        Food food = new Food(request.name(), category, request.productBrand(),
                request.packageQuantity(), request.unitOfMeasure(), request.gtin());
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
        }
        return StockMapper.toResponse(pantryItemRepository.save(item));
    }

    @Transactional
    public void remove(Integer profileId, Integer itemId) {
        PantryItem item = findOwnedItem(profileId, itemId);
        pantryItemRepository.delete(item);
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
