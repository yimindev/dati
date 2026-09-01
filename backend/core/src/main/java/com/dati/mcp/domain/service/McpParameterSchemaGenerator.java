package com.dati.mcp.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates JSON Schema (tools/list inputSchema) from parameter records.
 * Schema and validation read the same record annotations — structural drift is impossible.
 */
@Component
public class McpParameterSchemaGenerator {

    private final Map<Class<?>, Map<String, Object>> cache = new ConcurrentHashMap<>();
    private final SchemaGenerator schemaGenerator;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpParameterSchemaGenerator() {
        SchemaGeneratorConfigBuilder builder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
            .with(new JacksonModule())
            .with(new JakartaValidationModule(JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED))
            .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
            .with(Option.INLINE_ALL_SCHEMAS);
        this.schemaGenerator = new SchemaGenerator(builder.build());
    }

    /** Returns the JSON Schema for a parameter record, generated once per type. */
    public Map<String, Object> generate(Class<?> parameterType) {
        return cache.computeIfAbsent(parameterType, type -> {
            JsonNode schema = schemaGenerator.generateSchema(type);
            return mapper.convertValue(schema, new TypeReference<>() {
            });
        });
    }
}
