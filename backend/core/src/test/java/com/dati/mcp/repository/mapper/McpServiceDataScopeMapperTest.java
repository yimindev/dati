package com.dati.mcp.repository.mapper;

import com.dati.mcp.domain.model.McpDataScopeAccessMode;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.repository.po.McpServiceDataScopePO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpServiceDataScopeMapper tests")
class McpServiceDataScopeMapperTest {

    @Test
    @DisplayName("toPO copies access mode and allowed schemas")
    void toPO_copiesAccessConfig() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId("svc-1");
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId("ds-1");
        scope.setAccessMode(McpDataScopeAccessMode.SCHEMA_SCOPE);
        scope.setAllowedSchemas(List.of("sandbox", "public"));

        McpServiceDataScopePO po = McpServiceDataScopeMapper.toPO(scope);

        assertThat(po.getAccessMode()).isEqualTo(McpDataScopeAccessMode.SCHEMA_SCOPE);
        assertThat(po.getAllowedSchemas()).containsExactly("sandbox", "public");
    }

    @Test
    @DisplayName("toModel defaults null access mode to IMPORTED_TABLES")
    void toModel_defaultsNullAccessMode() {
        McpServiceDataScopePO po = new McpServiceDataScopePO();
        po.setServiceId("svc-1");
        po.setScopeType(McpDataScopeType.DATA_SOURCE);
        po.setReferenceId("ds-1");
        po.setAccessMode(null);
        po.setAllowedSchemas(null);

        McpServiceDataScope scope = McpServiceDataScopeMapper.toModel(po);

        assertThat(scope.getAccessMode()).isEqualTo(McpDataScopeAccessMode.IMPORTED_TABLES);
        assertThat(scope.getAllowedSchemas()).isEmpty();
    }
}
