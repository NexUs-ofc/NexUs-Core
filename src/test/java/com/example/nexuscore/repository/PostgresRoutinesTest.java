package com.example.nexuscore.repository;

import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.PantryProductSetting;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.service.BillingJobs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "postgresql.test.url", matches = ".+")
class PostgresRoutinesTest {

    private static JdbcTemplate admin;
    private static JdbcTemplate jdbc;
    private static String schema;
    private static LocalContainerEntityManagerFactoryBean factory;
    private static PantryProductSettingRepository settings;
    private static PantryItemRepository items;

    @BeforeAll
    static void connect() throws Exception {
        String url = System.getProperty("postgresql.test.url");
        String username = System.getProperty("postgresql.test.username", "nexus_test");
        String password = System.getProperty("postgresql.test.password", "nexus_test");
        admin = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        schema = "nexus_routines_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("create schema " + schema);
        var source = new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, username, password);
        jdbc = new JdbcTemplate(source);
        jdbc.execute(new ClassPathResource("postgres-routines-schema.sql").getContentAsString(StandardCharsets.UTF_8));
        jdbc.execute(new ClassPathResource("postgres-routines.sql").getContentAsString(StandardCharsets.UTF_8));
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(source);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(
                PantryProductSetting.class.getName(), PantryItem.class.getName(), Profile.class.getName(),
                Address.class.getName(), Food.class.getName(), Category.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        var repositories = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        settings = repositories.getRepository(PantryProductSettingRepository.class);
        items = repositories.getRepository(PantryItemRepository.class);
    }

    @AfterAll
    static void disconnect() {
        try {
            if (factory != null) {
                factory.destroy();
            }
        } finally {
            if (admin != null && schema != null) {
                admin.execute("drop schema " + schema + " cascade");
            }
        }
    }

    @BeforeEach
    void seed() {
        jdbc.execute("truncate payment, store, company, plan, pantry_product_setting, pantry_item, food, profile cascade");
        jdbc.update("insert into profile (id, name, type, status) values "
                + "(12, 'Casa', 'HOUSEHOLD', 'ACTIVE'), (13, 'Outra casa', 'HOUSEHOLD', 'ACTIVE'), "
                + "(30, 'Empresa', 'COMPANY', 'ACTIVE'), (31, 'Inativa', 'COMPANY', 'INACTIVE'), "
                + "(40, 'Loja', 'STORE', 'ACTIVE')");
        jdbc.update("insert into food (id, name, food_image_url) values "
                + "(1, 'Arroz', 'https://example.com/arroz.jpg'), (2, 'Feijao', null), (3, 'Leite', null)");
        jdbc.update("insert into plan values (1, 49.90)");
        jdbc.update("insert into company values (3, 30, 1), (4, 31, 1)");
        jdbc.update("insert into store values (4, 3, 40)");
    }

    @Test
    void findsMissingStockWithoutCountingExpiredLotsOrOtherProfiles() {
        jdbc.update("insert into pantry_product_setting (profile_id, food_id, minimum_quantity) values "
                + "(12, 1, 5), (12, 2, 3), (12, 3, 2), (13, 1, 10)");
        jdbc.update("insert into pantry_item (food_id, profile_id, quantity, expiry_date) values "
                + "(1, 12, 10, current_date - 1), (1, 12, 1, current_date), "
                + "(1, 13, 20, null), (3, 12, 2, null)");

        var missing = settings.findMissingByProfileId(12);

        assertThat(missing).extracting(row -> row.getFoodId()).containsExactly(1, 2);
        assertThat(missing.get(0).getFoodName()).isEqualTo("Arroz");
        assertThat(missing.get(0).getFoodImageUrl()).isEqualTo("https://example.com/arroz.jpg");
        assertThat(missing.get(0).getMinimumQuantity()).isEqualTo(5);
        assertThat(settings.findMissingByProfileId(13)).isEmpty();
    }

    @Test
    void callsTheExpiringProductsFunctionThroughTheJpaRepository() {
        jdbc.update("insert into pantry_item (id, food_id, profile_id, quantity, expiry_date) values "
                + "(1, 1, 12, 1, current_date), (2, 2, 12, 0, current_date), "
                + "(3, 1, 12, 1, current_date + 8)");

        var expiring = items.findExpiringProducts(LocalDate.now(), LocalDate.now().plusDays(7), PageRequest.of(0, 10));

        assertThat(expiring.getContent()).hasSize(1);
        assertThat(expiring.getContent().get(0).getItemId()).isEqualTo(1);
        assertThat(expiring.getContent().get(0).getFoodName()).isEqualTo("Arroz");
        assertThat(expiring.hasNext()).isFalse();
    }

    @Test
    void generatesMonthlyPaymentsOnlyOnceAndSkipsInactiveCompanies() {
        BillingJobs jobs = new BillingJobs(jdbc, 10, 0, "America/Sao_Paulo");

        jobs.generateMonthlyPayments();
        jobs.generateMonthlyPayments();

        assertThat(jdbc.queryForObject("select count(*) from payment", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select company_id from payment", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select payment_status from payment", String.class)).isEqualTo("PENDING");
    }

    @Test
    void updatesOverduePaymentsAndRestoresAccessAfterSettlement() {
        jdbc.update("insert into payment (due_date, payment_status, company_id, plan_id, amount) "
                + "values (current_date - 10, 'PENDING', 3, 1, 49.90)");
        BillingJobs jobs = new BillingJobs(jdbc, 10, 0, "America/Sao_Paulo");

        jobs.updateOverduePayments();

        assertThat(jdbc.queryForObject("select payment_status from payment", String.class)).isEqualTo("OVERDUE");
        assertThat(jdbc.queryForList("select status from profile where id in (30, 40)", String.class))
                .containsOnly("BLOCKED");
        jdbc.update("update payment set payment_status = 'PAID'");

        jobs.updateOverduePayments();

        assertThat(jdbc.queryForList("select status from profile where id in (30, 40)", String.class))
                .containsOnly("ACTIVE");
        assertThat(jdbc.queryForObject("select status from profile where id = 31", String.class)).isEqualTo("INACTIVE");
    }
}
