package com.example.nexuscore.config;

import com.example.nexuscore.geo.GeocodingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@MockitoBean(types = GeocodingService.class)
class OpenApiContractTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void describesEveryApplicationOperationAndItsParameters() throws Exception {
        JsonNode document = document();
        document.path("paths").properties().forEach(path -> path.getValue().properties().forEach(method -> {
            JsonNode operation = method.getValue();
            assertThat(operation.path("summary").asText()).as(path.getKey()).isNotBlank();
            assertThat(operation.path("description").asText()).as(path.getKey()).isNotBlank();
            operation.path("parameters").forEach(parameter ->
                    assertThat(parameter.path("description").asText()).as(parameter.path("name").asText()).isNotBlank());
        }));
    }

    @Test
    void documentsCrudStatusesErrorsAndPublicAccessAccurately() throws Exception {
        JsonNode document = document();
        JsonNode create = document.at("/paths/~1api~1companies~1me~1stores/post/responses");
        assertThat(create.has("201")).isTrue();
        assertThat(create.has("400")).isTrue();
        assertThat(create.has("401")).isTrue();
        assertThat(create.has("409")).isTrue();
        assertThat(create.has("429")).isTrue();
        assertThat(create.has("500")).isTrue();
        assertThat(create.at("/409/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ApiError");
        assertThat(document.at("/components/schemas/ApiError/properties/message").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1companies~1me~1stores~1{id}/delete/responses").has("204")).isTrue();
        assertThat(document.at("/paths/~1api~1plans/get/responses").has("401")).isFalse();
        assertThat(document.at("/paths/~1api~1plans/get/security").isEmpty()).isTrue();
    }

    private JsonNode document() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readTree(body);
    }
}
