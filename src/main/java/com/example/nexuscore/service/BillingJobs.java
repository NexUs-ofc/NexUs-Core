package com.example.nexuscore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class BillingJobs {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingJobs.class);

    private final JdbcTemplate jdbcTemplate;
    private final int dueDay;
    private final int graceDays;
    private final ZoneId zoneId;

    public BillingJobs(JdbcTemplate jdbcTemplate,
                       @Value("${app.billing.due-day:10}") int dueDay,
                       @Value("${app.billing.grace-days:0}") int graceDays,
                       @Value("${app.scheduling.zone:America/Sao_Paulo}") String zone) {
        if (dueDay < 1 || dueDay > 28) {
            throw new IllegalArgumentException("O dia de vencimento deve estar entre 1 e 28");
        }
        if (graceDays < 0) {
            throw new IllegalArgumentException("A tolerancia de pagamento nao pode ser negativa");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.dueDay = dueDay;
        this.graceDays = graceDays;
        this.zoneId = ZoneId.of(zone);
    }

    @Scheduled(cron = "${app.billing.monthly-payments-cron:0 10 0 1 * *}",
            zone = "${app.scheduling.zone:America/Sao_Paulo}")
    public void generateMonthlyPayments() {
        try {
            int created = jdbcTemplate.query("CALL sp_generate_monthly_payments(?, ?, ?)", statement -> {
                statement.setDate(1, Date.valueOf(LocalDate.now(zoneId)));
                statement.setInt(2, dueDay);
                statement.setNull(3, Types.INTEGER);
            }, resultSet -> resultSet.next() ? resultSet.getInt(1) : 0);
            LOGGER.info("Geracao mensal de pagamentos concluida; criados={}", created);
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao gerar pagamentos mensais", exception);
        }
    }

    @Scheduled(cron = "${app.billing.overdue-payments-cron:0 15 0 * * *}",
            zone = "${app.scheduling.zone:America/Sao_Paulo}")
    public void updateOverduePayments() {
        try {
            int[] counts = jdbcTemplate.query("CALL sp_update_overdue_payments(?, ?, ?, ?)", statement -> {
                statement.setInt(1, graceDays);
                statement.setNull(2, Types.INTEGER);
                statement.setNull(3, Types.INTEGER);
                statement.setNull(4, Types.INTEGER);
            }, resultSet -> resultSet.next()
                    ? new int[] {resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3)}
                    : new int[] {0, 0, 0});
            LOGGER.info("Atualizacao de inadimplencia concluida; vencidos={}, bloqueadas={}, reativadas={}",
                    counts[0], counts[1], counts[2]);
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao atualizar pagamentos vencidos e status de empresas", exception);
        }
    }
}
