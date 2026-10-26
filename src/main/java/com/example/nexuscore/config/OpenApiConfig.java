package com.example.nexuscore.config;

import com.example.nexuscore.admin.AdminKeyFilter;
import com.example.nexuscore.security.NotifyTokenFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/")))
                .info(new Info()
                        .title("NexUs Core API")
                        .version("1.0.0")
                        .description("API principal do NexUs. Em Authorize, informe o JWT emitido pelo Auth, "
                                + "sem o prefixo Bearer. Rotas internas e administrativas usam suas próprias chaves."))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT de acesso emitido pelo Auth, sem o prefixo Bearer."))
                        .addSecuritySchemes("NotifyTokenAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(NotifyTokenFilter.HEADER_NAME)
                                .description("Valor de NOTIFY_CORE_TOKEN, somente para /internal/notifications/**."))
                        .addSecuritySchemes("AdminKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(AdminKeyFilter.HEADER)
                                .description("Valor de ADMIN_API_KEY, somente para as rotas administrativas.")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
