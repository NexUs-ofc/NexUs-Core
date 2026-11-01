package com.example.nexuscore.repository;

import com.example.nexuscore.model.Plan;
import com.example.nexuscore.service.PlanService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlanCatalogPersistenceTest {

    private static LocalContainerEntityManagerFactoryBean factory;
    private static PlanRepository plans;

    @BeforeAll
    static void startDatabase() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:plan_catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(source);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(Plan.class.getName()));
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
        var repositoryFactory = new JpaRepositoryFactory(
                SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        plans = repositoryFactory.getRepository(PlanRepository.class);
        var jdbc = new JdbcTemplate(source);
        jdbc.update("insert into plan (id, plan_name, plan_price, description, is_active) "
                + "values (47, 'Plano inicial', 79.90, 'Descrição inicial do banco', true), "
                + "(11, 'Plano completo', 149.90, 'Descrição completa do banco', true), "
                + "(99, 'Plano antigo', 0.00, 'Não deve ser oferecido', false)");
    }

    @AfterAll
    static void closeDatabase() {
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    void listsOnlyActivePlansWithTheDatabaseIdsPricesAndDescriptions() {
        var catalog = new PlanService(plans).list();

        assertThat(catalog).extracting(plan -> plan.id()).containsExactly(47, 11);
        assertThat(catalog.get(0).planName()).isEqualTo("Plano inicial");
        assertThat(catalog.get(0).planPrice()).isEqualByComparingTo(new BigDecimal("79.90"));
        assertThat(catalog.get(0).description()).isEqualTo("Descrição inicial do banco");
    }

    @Test
    void rejectsInactiveAndMissingPlansWhenResolvingAChange() {
        assertThat(plans.findByIdAndActiveTrue(99)).isEmpty();
        assertThat(plans.findByIdAndActiveTrue(999)).isEmpty();
        assertThat(plans.findByIdAndActiveTrue(47)).isPresent();
    }
}
