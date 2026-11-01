package com.example.nexuscore.service;

import com.example.nexuscore.categorization.GpcCategoryResolver;
import com.example.nexuscore.dto.stock.FoodRegistrationRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.UnitOfMeasure;
import com.example.nexuscore.repository.CategoryRepository;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.PantryProductSettingRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FoodBatchRegistrationTest {

    private static final String FIRST_GTIN = "7891234567890";
    private static final String SECOND_GTIN = "7891234567891";
    private static final LocalDate EXPIRY = LocalDate.of(2026, 12, 10);

    private final FoodRepository foods = mock(FoodRepository.class);
    private final PantryItemRepository items = mock(PantryItemRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final GpcCategoryResolver resolver = mock(GpcCategoryResolver.class);
    private final ProductRankingService ranking = mock(ProductRankingService.class);
    private final ActivityRecordService activity = mock(ActivityRecordService.class);
    private final Profile profile = mock(Profile.class);
    private final Address address = mock(Address.class);
    private final Map<String, Food> catalog = new HashMap<>();
    private final TestTransactionManager transactions = new TestTransactionManager();

    private StockService service;

    @BeforeEach
    void setUp() {
        StockService target = new StockService(items, mock(PantryProductSettingRepository.class),
                foods, categories, profiles, resolver, ranking, activity);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(interceptor);
        service = (StockService) proxy.getProxy();

        when(profiles.getReferenceById(12)).thenReturn(profile);
        when(profile.getAddress()).thenReturn(address);
        when(resolver.isKnownFoodBrick("10000001")).thenReturn(true);
        when(resolver.resolve("10000001")).thenReturn("Cereais");
        when(categories.findByCategoryName("Cereais")).thenReturn(Optional.of(new Category("Cereais")));
        when(foods.findByGtin(anyString())).thenAnswer(invocation -> Optional.ofNullable(catalog.get(
                invocation.getArgument(0, String.class))));

        AtomicInteger nextFoodId = new AtomicInteger(20);
        when(foods.save(any(Food.class))).thenAnswer(invocation -> {
            Food food = invocation.getArgument(0);
            ReflectionTestUtils.setField(food, "id", nextFoodId.getAndIncrement());
            catalog.put(food.getGtin(), food);
            return food;
        });
        AtomicInteger nextItemId = new AtomicInteger(100);
        when(items.save(any(PantryItem.class))).thenAnswer(invocation -> {
            PantryItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", nextItemId.getAndIncrement());
            return item;
        });
    }

    @Test
    void reusesExistingFoodCreatesMissingFoodAndPreservesRequestOrder() {
        Food existing = new Food("Arroz original", new Category("Cereais"), "Marca original",
                BigDecimal.ONE, UnitOfMeasure.kg, FIRST_GTIN, "https://example.com/original.jpg");
        ReflectionTestUtils.setField(existing, "id", 7);
        catalog.put(FIRST_GTIN, existing);
        FoodRegistrationRequest first = new FoodRegistrationRequest(FIRST_GTIN, "Outro nome", "Outra marca",
                "https://example.com/new.jpg", BigDecimal.TEN, UnitOfMeasure.g, null, 2, EXPIRY);

        List<StockItemResponse> responses = service.registerFoods(12, List.of(first, request(SECOND_GTIN, "Feijao")));

        assertEquals(List.of(100, 101), responses.stream().map(StockItemResponse::id).toList());
        assertEquals(List.of(7, 20), responses.stream().map(StockItemResponse::foodId).toList());
        assertEquals(List.of("Arroz original", "Feijao"),
                responses.stream().map(StockItemResponse::foodName).toList());
        assertEquals("https://example.com/original.jpg", responses.get(0).foodImageUrl());
        assertEquals("kg", responses.get(0).unitOfMeasure());
        assertEquals(EXPIRY, responses.get(1).expiryDate());
        ArgumentCaptor<Food> created = ArgumentCaptor.forClass(Food.class);
        verify(foods).save(created.capture());
        assertEquals(SECOND_GTIN, created.getValue().getGtin());
        ArgumentCaptor<PantryItem> saved = ArgumentCaptor.forClass(PantryItem.class);
        verify(items, times(2)).save(saved.capture());
        saved.getAllValues().forEach(item -> {
            assertSame(profile, item.getProfile());
            assertEquals(2, item.getQuantity());
        });
        verify(profiles).getReferenceById(12);
        assertTrue(transactions.committed);
        assertFalse(transactions.readOnly);
        verify(ranking).recordRegistered(100, 7, address);
        verify(ranking).recordRegistered(101, 20, address);
        verify(activity).record(12, "stock_item_added", "Adicionou 2 unidade(s) de Arroz original ao estoque");
        verify(activity).record(12, "stock_item_added", "Adicionou 2 unidade(s) de Feijao ao estoque");
    }

    @Test
    void keepsSeparateStockItemsForRepeatedGtinWithDifferentExpiryDates() {
        FoodRegistrationRequest second = new FoodRegistrationRequest(FIRST_GTIN, "Arroz", "Marca",
                "https://example.com/arroz.jpg", BigDecimal.ONE, UnitOfMeasure.kg, "10000001", 3,
                EXPIRY.plusMonths(1));

        List<StockItemResponse> responses = service.registerFoods(12, List.of(request(FIRST_GTIN, "Arroz"), second));

        verify(foods).save(any(Food.class));
        verify(items, times(2)).save(any(PantryItem.class));
        assertEquals(responses.get(0).foodId(), responses.get(1).foodId());
        assertEquals(List.of(2, 3), responses.stream().map(StockItemResponse::quantity).toList());
        assertEquals(List.of(EXPIRY, EXPIRY.plusMonths(1)),
                responses.stream().map(StockItemResponse::expiryDate).toList());
        assertEquals("https://example.com/arroz.jpg", responses.get(1).foodImageUrl());
    }

    @Test
    void rollsBackBatchAndSkipsExternalRecordsWhenLaterProductHasInvalidGpc() {
        FoodRegistrationRequest invalid = new FoodRegistrationRequest(SECOND_GTIN, "Feijao", "Marca", null,
                BigDecimal.ONE, UnitOfMeasure.kg, "invalid", 2, EXPIRY);

        assertThrows(IllegalArgumentException.class,
                () -> service.registerFoods(12, List.of(request(FIRST_GTIN, "Arroz"), invalid)));

        assertTrue(transactions.rolledBack);
        assertFalse(transactions.committed);
        verify(items).save(any(PantryItem.class));
        verifyNoInteractions(ranking, activity);
    }

    @Test
    void rollsBackBatchAndSkipsExternalRecordsWhenSavingAnItemFails() {
        when(items.save(any(PantryItem.class))).thenAnswer(invocation -> {
            PantryItem item = invocation.getArgument(0);
            if (SECOND_GTIN.equals(item.getFood().getGtin())) {
                throw new DataIntegrityViolationException("Invalid stock item");
            }
            return item;
        });

        assertThrows(DataIntegrityViolationException.class, () -> service.registerFoods(12,
                List.of(request(FIRST_GTIN, "Arroz"), request(SECOND_GTIN, "Feijao"))));

        assertTrue(transactions.rolledBack);
        assertFalse(transactions.committed);
        verifyNoInteractions(ranking, activity);
    }

    @Test
    void skipsExternalRecordsWhenCommitFails() {
        transactions.failCommit = true;

        assertThrows(DataIntegrityViolationException.class,
                () -> service.registerFoods(12, List.of(request(FIRST_GTIN, "Arroz"))));

        assertFalse(transactions.committed);
        verifyNoInteractions(ranking, activity);
    }

    private FoodRegistrationRequest request(String gtin, String name) {
        return new FoodRegistrationRequest(gtin, name, "Marca", null, BigDecimal.ONE,
                UnitOfMeasure.kg, "10000001", 2, EXPIRY);
    }

    private class TestTransactionManager extends AbstractPlatformTransactionManager {

        private boolean committed;
        private boolean rolledBack;
        private boolean readOnly;
        private boolean failCommit;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            readOnly = definition.isReadOnly();
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            verifyNoInteractions(ranking, activity);
            if (failCommit) {
                throw new DataIntegrityViolationException("Commit failed");
            }
            committed = true;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rolledBack = true;
        }
    }
}
