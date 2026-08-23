package com.dati.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI datiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DatI API")
                        .version("0.3.0"));
    }

    /**
     * Converts all schema property names from camelCase to snake_case, but only when
     * spring.jackson.property-naming-strategy is explicitly set to SNAKE_CASE.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.jackson.property-naming-strategy", havingValue = "SNAKE_CASE")
    public OpenApiCustomizer snakeCaseSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return;
            openApi.getComponents().getSchemas().forEach((schemaName, schema) -> renameProperties(schema));
        };
    }

    private void renameProperties(Schema<?> schema) {
        if (schema == null) return;

        if (schema.getProperties() != null) {
            Map<String, Schema> newProps = new LinkedHashMap<>();
            schema.getProperties().forEach((propName, propSchema) -> {
                String snakeName = camelToSnake(propName);
                renameProperties(propSchema);
                newProps.put(snakeName, propSchema);
            });
            schema.setProperties(newProps);
        }

        if (schema.getRequired() != null) {
            List<String> newRequired = schema.getRequired().stream()
                    .map(SpringDocConfig::camelToSnake)
                    .toList();
            schema.setRequired(newRequired);
        }
    }

    private static String camelToSnake(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
