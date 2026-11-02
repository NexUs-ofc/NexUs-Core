package com.example.nexuscore.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

/**
 * Rotas que o painel do administrador consome. Todas exigem o header {@code X-Admin-Key}.
 */
@Tag(name = "Administração")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "AdminKeyAuth")
public class AdminController {

    private final AdminMetricsService metricsService;
    private final String servico;

    public AdminController(AdminMetricsService metricsService, @Value("${spring.application.name}") String servico) {
        this.metricsService = metricsService;
        this.servico = servico;
    }

    /** Valida a chave no login do painel: 200 com a chave certa, 401 sem ela. */
    @Operation(summary = "Verificar acesso administrativo",
            description = "Verifica a chave X-Admin-Key; não aceita JWT de usuário como substituto.")
    @GetMapping("/session")
    public AdminSession session() {
        return new AdminSession(servico, true, Instant.now().toString());
    }

    /** As doze visoes da pagina do servico, na janela pedida (padrao: ultima hora). */
    @Operation(summary = "Consultar métricas administrativas",
            description = "Retorna métricas dentro da janela configurada de retenção. Exige X-Admin-Key.")
    @GetMapping("/metrics")
    public AdminDashboard metrics(@RequestParam(defaultValue = "60") int minutos) {
        return metricsService.montar(minutos);
    }

    public record AdminSession(String servico, boolean autenticado, String verificadoEm) {
    }
}
