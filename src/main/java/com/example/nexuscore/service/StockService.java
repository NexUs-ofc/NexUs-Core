package com.example.nexuscore.service;

import com.example.nexuscore.dto.stock.FoodResponse;
import com.example.nexuscore.dto.stock.MinimumQuantityRequest;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.dto.stock.StockItemRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.dto.stock.StockItemUpdateRequest;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
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
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {

    private final PantryItemRepository pantryItemRepository;
    private final PantryProductSettingRepository settingRepository;
    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;

    public StockService(PantryItemRepository pantryItemRepository,
                         PantryProductSettingRepository settingRepository,
                         FoodRepository foodRepository,
                         CategoryRepository categoryRepository,
                         ProfileRepository profileRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.settingRepository = settingRepository;
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
    }

    public List<StockItemResponse> list(Integer profileId) {
        return pantryItemRepository.findByProfileIdOrderByExpiryDateAscIdDesc(profileId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StockItemResponse> expired(Integer profileId) {
        return pantryItemRepository.findByProfileIdAndExpiryDateBefore(profileId, LocalDate.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PantryProductSettingResponse> missing(Integer profileId) {
        return settingRepository.findByProfileId(profileId).stream()
                .filter(setting -> {
                    Integer current = pantryItemRepository.sumQuantityByProfileIdAndFoodId(profileId, setting.getFood().getId());
                    return current < setting.getMinimumQuantity();
                })
                .map(setting -> new PantryProductSettingResponse(
                        setting.getFood().getId(), setting.getFood().getName(), setting.getMinimumQuantity()))
                .toList();
    }

    public List<FoodResponse> searchFoods(String search) {
        List<Food> foods = search == null || search.isBlank()
                ? foodRepository.findAll()
                : foodRepository.findByNameContainingIgnoreCase(search);
        return foods.stream().map(this::toResponse).toList();
    }

    public List<PantryProductSettingResponse> listSettings(Integer profileId) {
        return settingRepository.findByProfileId(profileId).stream()
                .map(setting -> new PantryProductSettingResponse(
                        setting.getFood().getId(), setting.getFood().getName(), setting.getMinimumQuantity()))
                .toList();
    }

    @Transactional
    public StockItemResponse add(Integer profileId, StockItemRequest request) {
        Profile profile = profileRepository.getReferenceById(profileId);
        Food food = foodRepository.findById(request.foodId())
                .orElseThrow(() -> new NotFoundException("Alimento nao encontrado: " + request.foodId()));
        PantryItem item = new PantryItem(food, profile, request.quantity(), request.expiryDate());
        return toResponse(pantryItemRepository.save(item));
    }

    @Transactional
    public StockItemResponse update(Integer profileId, Integer itemId, StockItemUpdateRequest request) {
        PantryItem item = findOwnedItem(profileId, itemId);
        if (request.quantity() != null) item.setQuantity(request.quantity());
        if (request.expiryDate() != null) item.setExpiryDate(request.expiryDate());
        return toResponse(item);
    }

    @Transactional
    public void remove(Integer profileId, Integer itemId) {
        PantryItem item = findOwnedItem(profileId, itemId);
        pantryItemRepository.delete(item);
    }

    @Transactional
    public PantryProductSettingResponse setMinimumQuantity(Integer profileId, Integer foodId, MinimumQuantityRequest request) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new NotFoundException("Alimento nao encontrado: " + foodId));
        PantryProductSetting setting = settingRepository.findByProfileIdAndFoodId(profileId, foodId)
                .orElseGet(() -> new PantryProductSetting(food, profileRepository.getReferenceById(profileId), request.minimumQuantity()));
        setting.setMinimumQuantity(request.minimumQuantity());
        PantryProductSetting saved = settingRepository.save(setting);
        return new PantryProductSettingResponse(saved.getFood().getId(), saved.getFood().getName(), saved.getMinimumQuantity());
    }

    private PantryItem findOwnedItem(Integer profileId, Integer itemId) {
        PantryItem item = pantryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item de estoque nao encontrado: " + itemId));
        if (!item.getProfile().getId().equals(profileId)) {
            throw new ForbiddenException("Item de estoque nao pertence ao perfil autenticado");
        }
        return item;
    }

    private StockItemResponse toResponse(PantryItem item) {
        return new StockItemResponse(
                item.getId(),
                item.getFood().getId(),
                item.getFood().getName(),
                item.getFood().getProductBrand(),
                item.getFood().getUnitOfMeasure().name(),
                item.getQuantity(),
                item.getExpiryDate());
    }

    private FoodResponse toResponse(Food food) {
        Category category = food.getCategory();
        return new FoodResponse(
                food.getId(),
                food.getName(),
                category != null ? category.getCategoryName() : null,
                food.getProductBrand(),
                food.getPackageQuantity(),
                food.getUnitOfMeasure().name());
    }
}
