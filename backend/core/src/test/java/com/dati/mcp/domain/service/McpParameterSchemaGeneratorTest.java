package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.mcp.domain.model.param.SearchMetadataArgs;
import com.dati.mcp.domain.model.param.UpdateColumnInfoArgs;
import com.dati.mcp.domain.model.param.UpdateTableInfoArgs;
import com.dati.mcp.domain.model.param.UpsertTermArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpParameterSchemaGenerator tests")
class McpParameterSchemaGeneratorTest {

    private final McpParameterSchemaGenerator generator = new McpParameterSchemaGenerator();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    @Test
    @DisplayName("SEARCH_METADATA schema matches record contract")
    void searchMetadataSchema() {
        Map<String, Object> schema = generator.generate(SearchMetadataArgs.class);

        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(map(schema.get("properties")).keySet()).containsExactly("keywords");
        Map<String, Object> keywords = map(map(schema.get("properties")).get("keywords"));
        assertThat(keywords.get("type")).isEqualTo("array");
        assertThat(keywords.get("minItems")).isEqualTo(1);
        assertThat(keywords.get("description")).isEqualTo("Search keywords or business terms");
        Map<String, Object> items = map(keywords.get("items"));
        assertThat(items.get("type")).isEqualTo("string");
        assertThat(items.get("minLength")).isEqualTo(1);
        assertThat(schema.get("required")).isEqualTo(List.of("keywords"));
    }

    @Test
    @DisplayName("GET_TABLE_INFO schema is tables[] shape with data_source_id per item")
    void getTableInfoSchema() {
        Map<String, Object> schema = generator.generate(GetTableInfoArgs.class);

        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(schema.get("required")).isEqualTo(List.of("tables"));
        Map<String, Object> tables = map(map(schema.get("properties")).get("tables"));
        assertThat(tables.get("type")).isEqualTo("array");
        assertThat(tables.get("minItems")).isEqualTo(1);
        assertThat(tables.get("maxItems")).isEqualTo(20);
        Map<String, Object> item = map(tables.get("items"));
        assertThat(item.get("type")).isEqualTo("object");
        assertThat(item.get("additionalProperties")).isEqualTo(false);
        assertThat(item.get("required")).isEqualTo(List.of("data_source_id", "table"));
        Map<String, Object> props = map(item.get("properties"));
        // victools emits properties in alphabetical order (deterministic); order is not part of the contract
        assertThat(props.keySet()).containsExactlyInAnyOrder("data_source_id", "schema", "table", "fields");
        assertThat(map(props.get("data_source_id")).get("type")).isEqualTo("string");
        assertThat(map(props.get("schema")).get("type")).isEqualTo("string");
        assertThat(map(props.get("table")).get("type")).isEqualTo("string");
        Map<String, Object> fields = map(props.get("fields"));
        assertThat(fields.get("type")).isEqualTo("array");
        assertThat(fields.get("maxItems")).isEqualTo(100);
        assertThat(map(fields.get("items")).get("maxLength")).isEqualTo(100);
    }

    @Test
    @DisplayName("EXECUTE_SQL schema matches record contract")
    void executeSqlSchema() {
        Map<String, Object> schema = generator.generate(ExecuteSqlArgs.class);

        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(schema.get("required")).isEqualTo(List.of("data_source_id", "sql"));
        assertThat(map(schema.get("properties")).keySet()).containsExactly("data_source_id", "sql");
        assertThat(map(map(schema.get("properties")).get("data_source_id")).get("type")).isEqualTo("string");
        assertThat(map(map(schema.get("properties")).get("sql")).get("type")).isEqualTo("string");
    }

    @Test
    @DisplayName("UPDATE_TABLE_INFO schema matches record contract")
    void updateTableInfoSchema() {
        Map<String, Object> schema = generator.generate(UpdateTableInfoArgs.class);

        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(schema.get("required")).isEqualTo(List.of("tables"));
        Map<String, Object> tables = map(map(schema.get("properties")).get("tables"));
        assertThat(tables.get("type")).isEqualTo("array");
        assertThat(tables.get("minItems")).isEqualTo(1);
        assertThat(tables.get("maxItems")).isEqualTo(20);
        Map<String, Object> item = map(tables.get("items"));
        assertThat(item.get("additionalProperties")).isEqualTo(false);
        assertThat(item.get("required")).isEqualTo(List.of("data_source_id", "table"));
        Map<String, Object> props = map(item.get("properties"));
        // victools emits properties in alphabetical order (deterministic); order is not part of the contract
        assertThat(props.keySet()).containsExactlyInAnyOrder(
            "aliases", "data_source_id", "description", "schema", "table");
        assertThat(map(props.get("data_source_id")).get("type")).isEqualTo("string");
        assertThat(map(props.get("description")).get("maxLength")).isEqualTo(500);
        Map<String, Object> aliases = map(props.get("aliases"));
        assertThat(aliases.get("type")).isEqualTo("array");
        assertThat(aliases.get("maxItems")).isEqualTo(20);
        assertThat(map(aliases.get("items")).get("maxLength")).isEqualTo(100);
    }

    @Test
    @DisplayName("UPDATE_COLUMN_INFO schema matches record contract")
    void updateColumnInfoSchema() {
        Map<String, Object> schema = generator.generate(UpdateColumnInfoArgs.class);

        assertThat(schema.get("required")).isEqualTo(List.of("columns"));
        Map<String, Object> columns = map(map(schema.get("properties")).get("columns"));
        assertThat(columns.get("minItems")).isEqualTo(1);
        assertThat(columns.get("maxItems")).isEqualTo(20);
        Map<String, Object> item = map(columns.get("items"));
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) item.get("required");
        assertThat(required).containsExactlyInAnyOrder("data_source_id", "table", "column");
        assertThat(map(item.get("properties")).keySet()).containsExactlyInAnyOrder(
            "aliases", "column", "data_source_id", "description", "schema", "table");
    }

    @Test
    @DisplayName("UPSERT_TERM schema matches record contract")
    void upsertTermSchema() {
        Map<String, Object> schema = generator.generate(UpsertTermArgs.class);

        assertThat(schema.get("required")).isEqualTo(List.of("terms"));
        Map<String, Object> terms = map(map(schema.get("properties")).get("terms"));
        assertThat(terms.get("minItems")).isEqualTo(1);
        assertThat(terms.get("maxItems")).isEqualTo(20);
        Map<String, Object> item = map(terms.get("items"));
        assertThat(item.get("required")).isEqualTo(List.of("name", "subject_name"));
        Map<String, Object> props = map(item.get("properties"));
        assertThat(props.keySet()).containsExactlyInAnyOrder(
            "aliases", "description", "name", "subject_name");
        assertThat(map(props.get("subject_name")).get("maxLength")).isEqualTo(200);
        assertThat(map(props.get("name")).get("maxLength")).isEqualTo(200);
    }

    @Test
    @DisplayName("generation is cached per type")
    void cachesPerType() {
        assertThat(generator.generate(SearchMetadataArgs.class))
            .isSameAs(generator.generate(SearchMetadataArgs.class));
    }
}
