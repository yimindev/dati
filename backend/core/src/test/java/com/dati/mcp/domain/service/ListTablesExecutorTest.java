package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.param.ListTablesArgs;
import com.dati.mcp.server.pojo.TableListData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListTablesExecutor tests")
class ListTablesExecutorTest {

    @Mock
    private McpServiceDataScopeService dataScopeService;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private DataSourceService dataSourceService;

    private ListTablesExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ListTablesExecutor(dataScopeService, tableInfoDAO, dataSourceService);
    }

    private ToolExecutionContext ctx(List<McpServiceDataScope> scopes) {
        return new ToolExecutionContext("svc-1", McpToolType.LIST_TABLES,
            new ToolConfig.ListTablesConfig(), new ListTablesArgs(), scopes);
    }

    @Test
    @DisplayName("empty scope returns empty result without DB queries")
    void emptyScopeReturnsEmpty() {
        TableListData data = (TableListData) executor.execute(ctx(List.of()));

        assertThat(data.dataSources()).isEmpty();
        verifyNoInteractions(tableInfoDAO, dataSourceService);
    }

    @Test
    @DisplayName("lists table-level metadata grouped by data source, no columns")
    void listsTablesGroupedByDataSource() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        when(dataScopeService.getResolvedDataSourceIds("svc-1")).thenReturn(Set.of("ds-1"));

        TableInfoPO genre = new TableInfoPO();
        genre.setId("t-1");
        genre.setDataSourceId("ds-1");
        genre.setSchema("public");
        genre.setName("genre");
        genre.setDescription("音乐流派");
        genre.setAliases(List.of("流派"));

        TableInfoPO artist = new TableInfoPO();
        artist.setId("t-2");
        artist.setDataSourceId("ds-1");
        artist.setSchema("public");
        artist.setName("artist");
        artist.setAliases(null);

        when(tableInfoDAO.findByDataSourceId("ds-1")).thenReturn(List.of(genre, artist));
        when(dataSourceService.getDataSourceBriefs(Set.of("ds-1")))
            .thenReturn(Map.of("ds-1", new DataSourceService.DsBrief("音乐库", DbType.POSTGRESQL, "public", "音乐数据")));

        TableListData data = (TableListData) executor.execute(ctx(List.of(scope)));

        assertThat(data.dataSources()).hasSize(1);
        DataSourceDef ds = data.dataSources().getFirst();
        assertThat(ds.id()).isEqualTo("ds-1");
        assertThat(ds.name()).isEqualTo("音乐库");
        assertThat(ds.dbType()).isEqualTo(DbType.POSTGRESQL.name());
        assertThat(ds.defaultSchema()).isEqualTo("public");
        assertThat(ds.description()).isEqualTo("音乐数据");

        assertThat(ds.tables()).hasSize(2);
        TableDef first = ds.tables().getFirst();
        assertThat(first.table()).isEqualTo("genre");
        assertThat(first.schema()).isEqualTo("public");
        assertThat(first.description()).isEqualTo("音乐流派");
        assertThat(first.aliases()).containsExactly("流派");
        assertThat(first.columns()).isNull();

        // Schema-less table still listed with null alias list
        TableDef second = ds.tables().get(1);
        assertThat(second.table()).isEqualTo("artist");
        assertThat(second.aliases()).isNull();
        assertThat(second.columns()).isNull();
    }
}
