package com.dati.datasource.domain.service;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableMetadataService 单元测试")
class TableMetadataServiceTest {

    private static final String DS_ID = "ds-001";
    private static final String TABLE_ID = "tbl-001";

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @InjectMocks
    private TableMetadataService service;

    // ── helpers ──

    private static TableInfoPO table() {
        TableInfoPO po = new TableInfoPO();
        po.setId(TableMetadataServiceTest.TABLE_ID);
        po.setDataSourceId(TableMetadataServiceTest.DS_ID);
        po.setSchema("public");
        po.setName("users");
        po.setDescription("desc-" + "users");
        po.setAliases(List.of("users" + "_alias"));
        return po;
    }

    private static ColumnInfoPO column(String name, String type, String comment) {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setName(name);
        po.setColumnType(type);
        po.setDescription(comment);
        po.setAliases(List.of(name + "_alias"));
        return po;
    }

    private static SemanticSearchDocument valueDoc(String field, List<String> keywords) {
        return SemanticSearchDocument.builder()
                .entity(EntityReference.builder().tableId(TABLE_ID).field(field).build())
                .keywords(keywords)
                .build();
    }

    // ── tests ──

    @Test
    @DisplayName("表存在且有列和维度值时返回完整元数据")
    void tableFoundWithColumnsAndValues() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));
        when(columnInfoDAO.findByTableId(TABLE_ID))
                .thenReturn(List.of(column("id", "int4", "主键"), column("name", "varchar", "用户名")));
        when(semanticIndexService.findByTableIdAndType(TABLE_ID, SemanticEntityType.FIELD_VALUE, 5000))
                .thenReturn(List.of(
                        valueDoc("id", List.of("1")),
                        valueDoc("id", List.of("2")),
                        valueDoc("id", List.of("3")),
                        valueDoc("name", List.of("Alice")),
                        valueDoc("name", List.of("Bob"))));

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "public", "users");

        assertThat(result).isPresent();
        TableMetadataService.TableMeta meta = result.get();
        assertThat(meta.schema()).isEqualTo("public");
        assertThat(meta.tableName()).isEqualTo("users");
        assertThat(meta.description()).isEqualTo("desc-users");
        assertThat(meta.aliases()).contains("users_alias");
        assertThat(meta.tableId()).isEqualTo(TABLE_ID);
        assertThat(meta.dataSourceId()).isEqualTo(DS_ID);
        assertThat(meta.columns()).hasSize(2);

        ColumnDef col1 = meta.columns().getFirst();
        assertThat(col1.name()).isEqualTo("id");
        assertThat(col1.type()).isEqualTo("int4");
        assertThat(col1.comment()).isEqualTo("主键");
        assertThat(col1.aliases()).contains("id_alias");
        assertThat(col1.sampleValues()).containsExactly("1", "2", "3");

        ColumnDef col2 = meta.columns().get(1);
        assertThat(col2.sampleValues()).containsExactly("Alice", "Bob");
    }

    @Test
    @DisplayName("schema=null 匹配任意 schema 的表")
    void nullSchemaMatchesAny() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));
        when(columnInfoDAO.findByTableId(TABLE_ID))
                .thenReturn(List.of());
        when(semanticIndexService.findByTableIdAndType(TABLE_ID, SemanticEntityType.FIELD_VALUE, 5000))
                .thenReturn(List.of());

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, null, "users");

        assertThat(result).isPresent();
        assertThat(result.get().schema()).isEqualTo("public");
    }

    @Test
    @DisplayName("表存在但无维度值时列仍正常返回")
    void tableFoundWithoutValues() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));
        when(columnInfoDAO.findByTableId(TABLE_ID))
                .thenReturn(List.of(column("id", "int4", "主键")));
        when(semanticIndexService.findByTableIdAndType(TABLE_ID, SemanticEntityType.FIELD_VALUE, 5000))
                .thenReturn(List.of());

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "public", "users");

        assertThat(result).isPresent();
        assertThat(result.get().columns()).hasSize(1);
        assertThat(result.get().columns().getFirst().sampleValues()).isEmpty();
    }

    @Test
    @DisplayName("维度值超过 5 条时截断为 5")
    void valuesTruncatedToFive() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));
        when(columnInfoDAO.findByTableId(TABLE_ID))
                .thenReturn(List.of(column("name", "varchar", "用户名")));
        when(semanticIndexService.findByTableIdAndType(TABLE_ID, SemanticEntityType.FIELD_VALUE, 5000))
                .thenReturn(List.of(
                        valueDoc("name", List.of("v1")),
                        valueDoc("name", List.of("v2")),
                        valueDoc("name", List.of("v3")),
                        valueDoc("name", List.of("v4")),
                        valueDoc("name", List.of("v5")),
                        valueDoc("name", List.of("v6")),
                        valueDoc("name", List.of("v7"))));

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "public", "users");

        assertThat(result).isPresent();
        assertThat(result.get().columns().getFirst().sampleValues())
                .hasSize(5)
                .containsExactly("v1", "v2", "v3", "v4", "v5");
    }

    @Test
    @DisplayName("keyword 为空或 null 的文档被过滤")
    void emptyKeywordsFiltered() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));
        when(columnInfoDAO.findByTableId(TABLE_ID))
                .thenReturn(List.of(column("name", "varchar", "用户名")));
        when(semanticIndexService.findByTableIdAndType(TABLE_ID, SemanticEntityType.FIELD_VALUE, 5000))
                .thenReturn(List.of(
                        valueDoc("name", null),
                        valueDoc("name", List.of()),
                        valueDoc("name", List.of("Alice"))));

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "public", "users");

        assertThat(result).isPresent();
        assertThat(result.get().columns().getFirst().sampleValues())
                .containsExactly("Alice");
    }

    @Test
    @DisplayName("表不存在时返回空")
    void tableNotFound() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of());

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "public", "nonexist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("schema 不匹配时返回空")
    void schemaMismatchReturnsEmpty() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "audit", "users");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTableMetasByIds - 批量查表含别名和列")
    void getTableMetasByIds_withAliasesAndColumns() {
        when(tableInfoDAO.findAllById(java.util.Set.of(TABLE_ID)))
                .thenReturn(List.of(table()));
        when(columnInfoDAO.findByTableId(TABLE_ID))
                .thenReturn(List.of(column("id", "int4", "主键"), column("name", "varchar", "用户名")));
        when(semanticIndexService.findByTableIdAndType(TABLE_ID, SemanticEntityType.FIELD_VALUE, 5000))
                .thenReturn(List.of());

        java.util.List<TableMetadataService.TableMeta> result =
                service.getTableMetasByIds(java.util.Set.of(TABLE_ID));

        assertThat(result).hasSize(1);
        TableMetadataService.TableMeta meta = result.getFirst();
        assertThat(meta.tableName()).isEqualTo("users");
        assertThat(meta.tableId()).isEqualTo(TABLE_ID);
        assertThat(meta.dataSourceId()).isEqualTo(DS_ID);
        assertThat(meta.aliases()).contains("users_alias");
        assertThat(meta.columns()).hasSize(2);
    }
}
