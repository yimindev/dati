package com.dati.semantic.domain.service;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.domain.service.TableMetadataService.TableMeta;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SemanticSearchService 单元测试")
class SemanticSearchServiceTest {

    @InjectMocks
    private SemanticSearchService service;

    // ── helpers ──

    private static SemanticSearchDocument valueDoc(String tableId, String field, String value) {
        return SemanticSearchDocument.builder()
                .type(SemanticEntityType.FIELD_VALUE)
                .entity(EntityReference.builder().tableId(tableId).field(field).build())
                .keywords(List.of(value))
                .build();
    }

    private static ColumnDef col(String name, List<String> sampleValues) {
        return new ColumnDef(name, "varchar", null, List.of(), sampleValues);
    }

    private static TableMeta meta(String tableId, String tableName, List<ColumnDef> columns) {
        return new TableMeta(tableId, "public", tableName, null, List.of(), "ds-1", columns);
    }

    @Test
    @DisplayName("匹配值 < 5 条时放在首位，随机值补齐到 5")
    void matchedValuesLeadAndRandomFill() {
        List<SemanticSearchDocument> docs = List.of(
                valueDoc("t1", "name", "Alice"),
                valueDoc("t1", "name", "Bob"));
        List<TableMeta> metas = List.of(
                meta("t1", "users", List.of(
                        col("id", List.of("1", "2", "3", "4", "5")),
                        col("name", List.of("Charlie", "Eve", "David", "Frank", "Grace")))));

        List<TableMeta> result = service.mergeWithMatches(metas, docs);

        assertThat(result).hasSize(1);
        ColumnDef nameCol = result.getFirst().columns().get(1);
        assertThat(nameCol.sampleValues())
                .containsExactly("Alice", "Bob", "Charlie", "Eve", "David");
        // id 列不变
        ColumnDef idCol = result.getFirst().columns().getFirst();
        assertThat(idCol.sampleValues()).containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    @DisplayName("匹配值 >= 5 条时丢弃随机值，仅保留匹配值")
    void matchedValuesOverFiveDiscardRandom() {
        List<SemanticSearchDocument> docs = List.of(
                valueDoc("t1", "status", "A"), valueDoc("t1", "status", "B"),
                valueDoc("t1", "status", "C"), valueDoc("t1", "status", "D"),
                valueDoc("t1", "status", "E"), valueDoc("t1", "status", "F"));
        List<TableMeta> metas = List.of(
                meta("t1", "orders", List.of(col("status", List.of("X", "Y", "Z")))));

        List<TableMeta> result = service.mergeWithMatches(metas, docs);

        assertThat(result.getFirst().columns().getFirst().sampleValues())
                .containsExactly("A", "B", "C", "D", "E", "F");
    }

    @Test
    @DisplayName("无匹配值时保持原样")
    void noMatchesUnchanged() {
        List<TableMeta> metas = List.of(
                meta("t1", "users", List.of(col("id", List.of("1", "2", "3", "4", "5")))));

        List<TableMeta> result = service.mergeWithMatches(metas, List.of());

        assertThat(result.getFirst().columns().getFirst().sampleValues())
                .containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    @DisplayName("多张表的匹配值各自合并")
    void multipleTablesEachOwnMatches() {
        List<SemanticSearchDocument> docs = List.of(
                valueDoc("t1", "name", "Alice"),
                valueDoc("t2", "type", "VIP"));
        List<TableMeta> metas = List.of(
                meta("t1", "users", List.of(col("name", List.of("X", "Y", "Z", "W", "Q")))),
                meta("t2", "orders", List.of(col("type", List.of("A", "B", "C", "D", "E")))));

        List<TableMeta> result = service.mergeWithMatches(metas, docs);

        assertThat(result.get(0).columns().getFirst().sampleValues())
                .startsWith("Alice");
        assertThat(result.get(1).columns().getFirst().sampleValues())
                .startsWith("VIP");
    }
}
