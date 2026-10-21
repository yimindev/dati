package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.param.ExecuteSqlArgs;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.mcp.domain.model.param.SearchMetadataArgs;
import com.dati.mcp.domain.model.param.UpdateTableInfoArgs;
import com.dati.mcp.domain.model.param.UpsertTermArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ToolParameterBinder tests")
class ToolParameterBinderTest {

    private final ToolParameterBinder binder = new ToolParameterBinder();

    @Test
    @DisplayName("valid arguments bind to the record")
    void bindsValidArguments() {
        ExecuteSqlArgs args = (ExecuteSqlArgs) binder.bind(ExecuteSqlArgs.class,
            Map.of("data_source_id", "ds-1", "sql", "SELECT 1"));

        assertThat(args.dataSourceId()).isEqualTo("ds-1");
        assertThat(args.sql()).isEqualTo("SELECT 1");
    }

    @Test
    @DisplayName("snake_case keys map to record components automatically")
    void bindsSnakeCaseKeys() {
        GetTableInfoArgs args = (GetTableInfoArgs) binder.bind(GetTableInfoArgs.class,
            Map.of("tables", List.of(Map.of(
                "data_source_id", "ds-1", "table", "orders", "fields", List.of("id")))));

        assertThat(args.tables()).hasSize(1);
        GetTableInfoArgs.TableRef ref = args.tables().getFirst();
        assertThat(ref.dataSourceId()).isEqualTo("ds-1");
        assertThat(ref.table()).isEqualTo("orders");
        assertThat(ref.fields()).containsExactly("id");
    }

    @Test
    @DisplayName("missing required field → PARAM_INVALID with violation message")
    void missingRequiredFails() {
        assertThatThrownBy(() -> binder.bind(ExecuteSqlArgs.class, Map.of("data_source_id", "ds-1")))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory()).isEqualTo("PARAM_ERROR"))
            .hasMessageContaining("sql: is required");
    }

    @Test
    @DisplayName("blank required field → PARAM_INVALID")
    void blankRequiredFails() {
        assertThatThrownBy(() -> binder.bind(ExecuteSqlArgs.class,
            Map.of("data_source_id", " ", "sql", "SELECT 1")))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("over-limit tables list → PARAM_INVALID")
    void overLimitFails() {
        List<Map<String, String>> tooMany = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add(Map.of("data_source_id", "ds-1", "table", "t" + i));
        }
        assertThatThrownBy(() -> binder.bind(GetTableInfoArgs.class, Map.of("tables", tooMany)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("tables: must contain between 1 and 20 items");
    }

    @Test
    @DisplayName("empty keywords reports the field name and a readable message")
    void emptyKeywordsReportsFieldName() {
        assertThatThrownBy(() -> binder.bind(SearchMetadataArgs.class, Map.of("keywords", List.of())))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("keywords: must not be empty");
    }

    @Test
    @DisplayName("over-limit description reports nested field path and limit")
    void overLimitDescriptionReportsNestedPath() {
        List<Map<String, Object>> tables = List.of(Map.of(
            "data_source_id", "ds-1",
            "table", "orders",
            "description", "x".repeat(501)));

        assertThatThrownBy(() -> binder.bind(UpdateTableInfoArgs.class, Map.of("tables", tables)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("tables[0].description: must be at most 500 characters");
    }

    @Test
    @DisplayName("blank nested field reports the nested path")
    void blankNestedFieldReportsPath() {
        List<Map<String, String>> tables = List.of(Map.of(
            "data_source_id", "ds-1",
            "table", " "));

        assertThatThrownBy(() -> binder.bind(UpdateTableInfoArgs.class, Map.of("tables", tables)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("tables[0].table: must not be blank");
    }

    @Test
    @DisplayName("type error (number where array expected) → PARAM_INVALID, not ClassCastException")
    void typeErrorFails() {
        assertThatThrownBy(() -> binder.bind(SearchMetadataArgs.class, Map.of("keywords", 123)))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("Parse JSON failed");
    }

    @Test
    @DisplayName("null arguments treated as empty map → PARAM_INVALID")
    void nullArgumentsFails() {
        assertThatThrownBy(() -> binder.bind(ExecuteSqlArgs.class, null))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("data_source_id: is required");
    }

    @Test
    @DisplayName("multiple violations are aggregated with '; ' separator")
    void aggregatesViolations() {
        assertThatThrownBy(() -> binder.bind(ExecuteSqlArgs.class, Map.of()))
            .isInstanceOf(ToolExecuteException.class)
            .hasMessageContaining("is required", "; ");
    }

    @Test
    @DisplayName("null parameter type (dynamic tools) passes the raw map through")
    void nullTypePassesThrough() {
        assertThat(binder.bind(null, Map.of("x", 1))).isEqualTo(Map.of("x", 1));
        assertThat(binder.bind(null, null)).isEqualTo(Map.of());
    }

    @Test
    @DisplayName("UPDATE_TABLE_INFO missing required field → PARAM_INVALID")
    void updateTableMissingRequired() {
        assertThatThrownBy(() -> binder.bind(UpdateTableInfoArgs.class,
            Map.of("tables", List.of(Map.of("data_source_id", "ds-1")))))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PARAM_ERROR"));
    }

    @Test
    @DisplayName("UPDATE_TABLE_INFO list over 20 items → PARAM_INVALID")
    void updateTableTooManyItems() {
        List<Map<String, String>> tooMany = IntStream.range(0, 21)
            .mapToObj(i -> Map.of("data_source_id", "ds-1", "table", "t" + i))
            .toList();
        assertThatThrownBy(() -> binder.bind(UpdateTableInfoArgs.class, Map.of("tables", tooMany)))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PARAM_ERROR"));
    }

    @Test
    @DisplayName("UPDATE_TABLE_INFO description over 500 chars → PARAM_INVALID")
    void updateTableDescriptionTooLong() {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("data_source_id", "ds-1");
        item.put("table", "orders");
        item.put("description", "x".repeat(501));
        assertThatThrownBy(() -> binder.bind(UpdateTableInfoArgs.class, Map.of("tables", List.of(item))))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PARAM_ERROR"));
    }

    @Test
    @DisplayName("UPSERT_TERM alias over 100 chars → PARAM_INVALID")
    void upsertTermAliasTooLong() {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("subject_name", "销售");
        item.put("name", "退货单");
        item.put("aliases", List.of("x".repeat(101)));
        assertThatThrownBy(() -> binder.bind(UpsertTermArgs.class, Map.of("terms", List.of(item))))
            .isInstanceOf(ToolExecuteException.class)
            .satisfies(e -> assertThat(((ToolExecuteException) e).getErrorCategory())
                .isEqualTo("PARAM_ERROR"));
    }
}
