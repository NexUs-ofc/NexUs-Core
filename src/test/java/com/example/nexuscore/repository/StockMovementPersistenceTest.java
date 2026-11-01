package com.example.nexuscore.repository;

import com.example.nexuscore.dto.stock.StockMovementRequest;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.StockMovement;
import com.example.nexuscore.model.StockMovementType;
import com.example.nexuscore.service.StockMovementService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockMovementPersistenceTest {

    private static LocalContainerEntityManagerFactoryBean factory;
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transactions;
    private static JpaTransactionManager transactionManager;
    private static PantryItemRepository items;
    private static StockMovementRepository movements;
    private static StockMovementService service;
    private static Connection schemaConnection;

    @BeforeAll
    static void startDatabase() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:stock_movements;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                        + "DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        schemaConnection = source.getConnection();
        new ResourceDatabasePopulator(new ClassPathResource("stock-movements-schema.sql")).populate(schemaConnection);
        jdbc = new JdbcTemplate(source);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(source);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(
                PantryItem.class.getName(), StockMovement.class.getName(), Profile.class.getName(),
                Address.class.getName(), Food.class.getName(), Category.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        var entityManagerFactory = factory.getObject();
        transactionManager = new JpaTransactionManager(entityManagerFactory);
        transactions = new TransactionTemplate(transactionManager);
        var repositoryFactory = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory));
        items = repositoryFactory.getRepository(PantryItemRepository.class);
        movements = repositoryFactory.getRepository(StockMovementRepository.class);
        service = transactionalService(movements);
    }

    @AfterAll
    static void closeDatabase() throws Exception {
        if (factory != null) {
            factory.destroy();
        }
        if (schemaConnection != null) {
            schemaConnection.close();
        }
    }

    @BeforeEach
    void resetStock() {
        jdbc.update("delete from bi_hist.registro_estoque");
        jdbc.update("delete from pantry_item");
        jdbc.update("delete from food");
        jdbc.update("delete from profile");
        jdbc.update("insert into profile (id, email, name, type, status) "
                + "values (12, 'user@example.com', 'User', 'HOUSEHOLD', 'ACTIVE')");
        jdbc.update("insert into food (id, name, category_id, package_quantity, unit_of_measure) "
                + "values (1, 'Arroz', 1, 1, 'kg')");
        jdbc.update("insert into pantry_item (id, food_id, profile_id, quantity, expiry_date) "
                + "values (7, 1, 12, 5, current_date - 1)");
    }

    @Test
    void commitsMovementAndHidesEmptyItemFromStockAndExpiredQueries() {
        transactions.executeWithoutResult(status -> {
            assertThat(items.findByProfileIdOrderByExpiryDateAscIdDesc(12)).hasSize(1);
            assertThat(items.findByProfileIdAndExpiryDateBefore(12, LocalDate.now())).hasSize(1);
            assertThat(items.findByExpiryDateBefore(LocalDate.now(), PageRequest.of(0, 10))).hasSize(1);
            assertThat(items.findExpiredByStatus(LocalDate.now(), ProfileStatus.ACTIVE, PageRequest.of(0, 10)))
                    .hasSize(1);
        });
        service.register(12, 7, new StockMovementRequest(StockMovementType.CONSUMED, 5));

        assertThat(balance()).isZero();
        assertThat(movementCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select quantity from bi_hist.registro_estoque", Integer.class)).isEqualTo(5);
        transactions.executeWithoutResult(status -> {
            assertThat(items.findByProfileIdOrderByExpiryDateAscIdDesc(12)).isEmpty();
            assertThat(items.findByProfileIdAndExpiryDateBefore(12, LocalDate.now())).isEmpty();
            assertThat(items.findByExpiryDateBefore(LocalDate.now(), PageRequest.of(0, 10))).isEmpty();
            assertThat(items.findExpiredByStatus(LocalDate.now(), ProfileStatus.ACTIVE, PageRequest.of(0, 10)))
                    .isEmpty();
            assertThat(items.findById(7)).isPresent();
        });
    }

    @Test
    void rollsBackBalanceAndMovementTogetherWhenSavingFails() {
        StockMovementRepository failing = mock(StockMovementRepository.class);
        when(failing.save(any(StockMovement.class))).thenAnswer(invocation -> {
            movements.saveAndFlush(invocation.getArgument(0));
            throw new DataIntegrityViolationException("Falha ao registrar movimento");
        });

        assertThatThrownBy(() -> transactionalService(failing).register(
                12, 7, new StockMovementRequest(StockMovementType.DISCARDED, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(balance()).isEqualTo(5);
        assertThat(movementCount()).isZero();
    }

    @Test
    void serializesConcurrentMovementsWithoutOverspendingStock() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> moveAfterStart(ready, start));
            var second = executor.submit(() -> moveAfterStart(ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successes = (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).isEqualTo(1);
            assertThat(balance()).isEqualTo(1);
            assertThat(movementCount()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private boolean moveAfterStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            service.register(12, 7, new StockMovementRequest(StockMovementType.CONSUMED, 4));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int balance() {
        return jdbc.queryForObject("select quantity from pantry_item where id = 7", Integer.class);
    }

    private int movementCount() {
        return jdbc.queryForObject("select count(*) from bi_hist.registro_estoque", Integer.class);
    }

    private static StockMovementService transactionalService(StockMovementRepository repository) {
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(new StockMovementService(items, repository));
        proxy.addAdvice(interceptor);
        return (StockMovementService) proxy.getProxy();
    }
}
