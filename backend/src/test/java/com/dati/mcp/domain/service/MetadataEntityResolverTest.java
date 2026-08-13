package com.dati.mcp.domain.service;

import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetadataEntityResolver tests")
class MetadataEntityResolverTest {

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    private MetadataEntityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MetadataEntityResolver(tableInfoDAO, columnInfoDAO);
    }

    private static TableInfoPO table(String id, String schema) {
        TableInfoPO po = new TableInfoPO();
        po.setId(id);
        po.setDataSourceId("ds-1");
        po.setSchema(schema);
        po.setName("orders");
        po.setDescription("desc " + "orders");
        po.setAliases(List.of("a1"));
        return po;
    }

    private static ColumnInfoPO column(String id, String name) {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setId(id);
        po.setName(name);
        po.setDescription("col desc");
        po.setAliases(List.of("c1"));
        return po;
    }

    @Test
    @DisplayName("schema omitted matches any table with that name in the data source")
    void resolveTableByNameOnly() {
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(table("t1", "public"), table("t2", "billing")));

        Optional<MetadataEntityResolver.TableTarget> target =
            resolver.resolveTable("ds-1", null, "orders");

        assertThat(target).isPresent();
        assertThat(target.get().tableId()).isEqualTo("t1");
        assertThat(target.get().description()).isEqualTo("desc orders");
        assertThat(target.get().aliases()).containsExactly("a1");
    }

    @Test
    @DisplayName("schema provided narrows the match")
    void resolveTableWithSchema() {
        when(tableInfoDAO.findByDataSourceId("ds-1"))
            .thenReturn(List.of(table("t1", "public"), table("t2", "billing")));

        Optional<MetadataEntityResolver.TableTarget> target =
            resolver.resolveTable("ds-1", "billing", "orders");

        assertThat(target).isPresent();
        assertThat(target.get().tableId()).isEqualTo("t2");
    }

    @Test
    @DisplayName("unknown table resolves empty")
    void resolveTableNotFound() {
        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of());

        assertThat(resolver.resolveTable("ds-1", null, "ghost")).isEmpty();
    }

    @Test
    @DisplayName("column resolves by table + column name")
    void resolveColumn() {
        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of(table("t1", "public")));
        when(columnInfoDAO.findByTableId("t1"))
            .thenReturn(List.of(column("c1", "amount"), column("c2", "status")));

        Optional<MetadataEntityResolver.ColumnTarget> target =
            resolver.resolveColumn("ds-1", "public", "orders", "status");

        assertThat(target).isPresent();
        assertThat(target.get().columnId()).isEqualTo("c2");
        assertThat(target.get().tableId()).isEqualTo("t1");
        assertThat(target.get().description()).isEqualTo("col desc");
    }

    @Test
    @DisplayName("unknown column resolves empty")
    void resolveColumnNotFound() {
        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of(table("t1", "public")));
        when(columnInfoDAO.findByTableId("t1")).thenReturn(List.of(column("c1", "amount")));

        assertThat(resolver.resolveColumn("ds-1", "public", "orders", "ghost")).isEmpty();
    }
}
