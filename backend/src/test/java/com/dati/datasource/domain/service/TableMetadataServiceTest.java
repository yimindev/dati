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
@DisplayName("TableMetadataService unit tests")
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
    @DisplayName("table with columns and dimension values returns full metadata")
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
    @DisplayName("schema=null matches tables of any schema")
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
    @DisplayName("table without dimension values still returns columns")
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
    @DisplayName("dimension values truncated to 5 when exceeding 5")
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
    @DisplayName("documents with empty or null keyword are filtered")
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
    @DisplayName("returns empty when table not found")
    void tableNotFound() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of());

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "public", "nonexist");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns empty when schema does not match")
    void schemaMismatchReturnsEmpty() {
        when(tableInfoDAO.findByDataSourceId(DS_ID))
                .thenReturn(List.of(table()));

        Optional<TableMetadataService.TableMeta> result =
                service.getTableMeta(DS_ID, "audit", "users");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTableMetasByIds - batch query includes aliases and columns")
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
