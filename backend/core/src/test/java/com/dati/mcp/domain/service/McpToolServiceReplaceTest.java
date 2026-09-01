package com.dati.mcp.domain.service;

import com.dati.TestFixtures;
import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.SqlRenderer;
import com.dati.common.template.SqlValidator;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.dao.McpCustomToolDAO;
import com.dati.mcp.repository.dao.McpPrebuiltToolConfigDAO;
import com.dati.mcp.repository.dao.McpServiceDAO;
import com.dati.mcp.repository.mapper.McpCustomToolMapper;
import com.dati.mcp.repository.po.McpCustomToolPO;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for draft restore (rollback) semantics with real DAOs.
 * Guards against the delete-then-insert flush ordering bug: when restored content
 * contains a tool/prompt whose name matches an existing draft row, the DELETE must
 * hit the database before the INSERT of the restored row.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("McpToolService replace integration tests")
class McpToolServiceReplaceTest {

    @Autowired
    private McpPrebuiltToolConfigDAO prebuiltDAO;

    @Autowired
    private McpCustomToolDAO customToolDAO;

    @Autowired
    private McpServiceDAO mcpServiceDAO;

    private final PermissionService permissionService = Mockito.mock(PermissionService.class);

    private McpToolService service() {
        return new McpToolService(prebuiltDAO, customToolDAO, new HandlebarsStyleParser(), new SqlRenderer(), new SqlValidator(), permissionService);
    }

    private McpServicePO saveService() {
        McpServicePO svc = new McpServicePO();
        svc.setCode("svc-" + System.nanoTime());
        svc.setName("replace-it-svc");
        svc.setStatus(McpServiceStatus.DRAFT);
        return mcpServiceDAO.save(svc);
    }

    private void saveDraftTool(String serviceId) {
        McpCustomTool tool = TestFixtures.createTestCustomTool();
        tool.setId(null); // let DB generate id (Hibernate 6: manual id + save goes merge)
        tool.setServiceId(serviceId);
        tool.setName("list_genres");
        customToolDAO.save(McpCustomToolMapper.toPO(tool));
    }

    @Test
    @DisplayName("replaceCustomTools restores content with colliding names without unique violation")
    void replaceCustomTools_replacesWithCollidingNames() {
        McpServicePO svc = saveService();

        // existing draft tool with name list_genres (same as restored content)
        saveDraftTool(svc.getId());

        McpCustomTool restored = TestFixtures.createTestCustomTool();
        restored.setId(null);
        restored.setServiceId(svc.getId());
        restored.setName("list_genres");
        restored.setDescription("restored description");

        assertThatCode(() -> service().replaceCustomTools(svc.getId(), List.of(restored)))
            .doesNotThrowAnyException();

        List<McpCustomToolPO> remaining = customToolDAO.findAllByServiceIdOrderByCreatedAtDesc(svc.getId());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getName()).isEqualTo("list_genres");
        assertThat(remaining.getFirst().getDescription()).isEqualTo("restored description");
    }

    @Test
    @DisplayName("replaceCustomTools with empty list clears all draft tools")
    void replaceCustomTools_clearsAll() {
        McpServicePO svc = saveService();
        saveDraftTool(svc.getId());

        service().replaceCustomTools(svc.getId(), List.of());

        assertThat(customToolDAO.findAllByServiceIdOrderByCreatedAtDesc(svc.getId())).isEmpty();
    }
}
