package com.dati.mcp.server.assembler;

import com.dati.datasource.domain.service.DataSourceService;
import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.server.pojo.DataScopeItemVO;
import com.dati.mcp.server.pojo.DataScopeResponse;
import com.dati.semantic.domain.service.SubjectService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpDataScopeAssembler tests")
class McpDataScopeAssemblerTest {

    @Mock
    private SubjectService subjectService;

    @Mock
    private DataSourceService dataSourceService;

    private McpDataScopeAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new McpDataScopeAssembler(subjectService, dataSourceService);
    }

    @Test
    @DisplayName("toDataScopeResponse copies access mode and allowed schemas")
    void toDataScopeResponse_copiesAccessConfig() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setId("scope-1");
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        scope.setAccessMode(McpDataScopeAccessMode.MANAGED_SCHEMAS);
        scope.setAllowedSchemas(List.of("sandbox"));

        when(dataSourceService.getDataSourceNameMap(any())).thenReturn(Map.of("ds-1", "DS"));
        when(subjectService.getSubjectsByIds(List.of())).thenReturn(List.of());

        DataScopeResponse response = assembler.toDataScopeResponse(List.of(scope), Set.of("ds-1"));

        DataScopeItemVO item = response.getItems().getFirst();
        assertThat(item.getAccessMode()).isEqualTo(McpDataScopeAccessMode.MANAGED_SCHEMAS);
        assertThat(item.getAllowedSchemas()).containsExactly("sandbox");
    }

    @Test
    @DisplayName("toDataScopeResponse defaults null access mode to IMPORTED_TABLES")
    void toDataScopeResponse_defaultsNullAccessMode() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setId("scope-1");
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        scope.setAccessMode(null);
        scope.setAllowedSchemas(null);

        when(dataSourceService.getDataSourceNameMap(any())).thenReturn(Map.of("ds-1", "DS"));
        when(subjectService.getSubjectsByIds(List.of())).thenReturn(List.of());

        DataScopeResponse response = assembler.toDataScopeResponse(List.of(scope), Set.of());

        DataScopeItemVO item = response.getItems().getFirst();
        assertThat(item.getAccessMode()).isEqualTo(McpDataScopeAccessMode.IMPORTED_TABLES);
        assertThat(item.getAllowedSchemas()).isEmpty();
    }
}
