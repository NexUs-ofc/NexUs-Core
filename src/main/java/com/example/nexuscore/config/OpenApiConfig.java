package com.example.nexuscore.config;

import com.example.nexuscore.admin.AdminKeyFilter;
import com.example.nexuscore.exception.ApiError;
import com.example.nexuscore.security.NotifyTokenFilter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Components components = new Components();
        ModelConverters.getInstance().read(ApiError.class).forEach(components::addSchemas);
        return new OpenAPI()
                .servers(List.of(new Server().url("/")))
                .info(new Info()
                        .title("NexUs Core API")
                        .version("1.0.0")
                        .description("API principal do NexUs. Em Authorize, informe o JWT emitido pelo Auth, "
                                + "sem o prefixo Bearer. Rotas internas e administrativas usam suas próprias chaves. "
                                + "O Core valida o JWT localmente até sua expiração; logout e bloqueio de perfil "
                                + "não invalidam imediatamente um token já emitido."))
                .components(components
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

    @Bean
    public OperationCustomizer documentResponsesAndParameters() {
        Map<String, String> descriptions = Map.ofEntries(
                Map.entry("id", "Identificador do recurso. O acesso respeita o perfil autenticado."),
                Map.entry("itemId", "Identificador do item de estoque ou da lista indicada na rota."),
                Map.entry("foodId", "Identificador do alimento no catálogo."),
                Map.entry("from", "Início inclusivo do intervalo, no formato yyyy-MM-dd."),
                Map.entry("to", "Fim inclusivo do intervalo, no formato yyyy-MM-dd. Janela máxima de 31 dias."),
                Map.entry("page", "Índice da página, começando em zero."),
                Map.entry("size", "Quantidade de resultados por página, entre 1 e 500."),
                Map.entry("latitude", "Latitude entre -90 e 90; informe também a longitude."),
                Map.entry("longitude", "Longitude entre -180 e 180; informe também a latitude."),
                Map.entry("radiusKm", "Raio em quilômetros, maior que zero e menor ou igual a 100."),
                Map.entry("search", "Trecho do nome do alimento, sem diferenciar maiúsculas e minúsculas."),
                Map.entry("status", "Filtra pagamentos pelo estado informado."),
                Map.entry("eventType", "Tipo do ranking: registered ou expired."),
                Map.entry("state", "UF com duas letras, obrigatória para os recortes por localização."),
                Map.entry("city", "Cidade do recorte de ranking, acompanhada da UF."),
                Map.entry("date", "Data do ranking diário, no formato yyyy-MM-dd."),
                Map.entry("limit", "Número máximo de resultados, entre 1 e 100."),
                Map.entry("file", "Imagem JPEG, PNG ou WebP de até 5 MB."),
                Map.entry("minutos", "Janela em minutos, limitada à retenção configurada (720 por padrão)."));
        return (operation, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("com.example.nexuscore")) {
                return operation;
            }
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(parameter -> {
                    if (parameter.getDescription() == null) {
                        parameter.setDescription(descriptions.get(parameter.getName()));
                    }
                });
            }
            if (handler.getMethodParameters().length > 0) {
                operation.getResponses().addApiResponse("400",
                        apiError("Entrada inválida ou regra de negócio violada"));
            }
            boolean publicEndpoint = handler.hasMethodAnnotation(
                    io.swagger.v3.oas.annotations.security.SecurityRequirements.class);
            if (!publicEndpoint) {
                operation.getResponses().addApiResponse("401", new ApiResponse()
                        .description("Credencial ausente, inválida ou expirada; o corpo pode estar vazio"));
                operation.getResponses().addApiResponse("403", new ApiResponse()
                        .description("Perfil sem permissão ou recurso de outro proprietário"));
                operation.getResponses().addApiResponse("404", apiError("Recurso ou perfil não encontrado"));
            }
            boolean writes = handler.hasMethodAnnotation(PostMapping.class)
                    || handler.hasMethodAnnotation(PutMapping.class)
                    || handler.hasMethodAnnotation(PatchMapping.class)
                    || handler.hasMethodAnnotation(DeleteMapping.class);
            if (writes) {
                operation.getResponses().addApiResponse("409", apiError("Duplicidade ou conflito de integridade"));
                operation.getResponses().addApiResponse("415", apiError("Formato de requisição não suportado"));
            }
            if (Arrays.stream(handler.getMethodParameters())
                    .anyMatch(parameter -> MultipartFile.class.isAssignableFrom(parameter.getParameterType()))) {
                operation.getResponses().addApiResponse("413", apiError("A imagem excede o limite de 5 MB"));
                operation.getResponses().addApiResponse("502", apiError("Falha na comunicação com o Cloudinary"));
                operation.getResponses().addApiResponse("503", apiError("Serviço de imagens não configurado"));
            }
            if (handler.getBeanType().getPackageName().endsWith(".admin")) {
                operation.getResponses().addApiResponse("503",
                        new ApiResponse().description("Chave administrativa não configurada"));
            }
            if (handler.getBeanType().getPackageName().endsWith(".controller")) {
                operation.getResponses().addApiResponse("429", new ApiResponse()
                        .description("Limite de requisições atingido; Retry-After informa a espera em segundos"));
            }
            operation.getResponses().addApiResponse("500", apiError("Falha interna ao acessar os dados"));
            return operation;
        };
    }

    private ApiResponse apiError(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
    }
}
