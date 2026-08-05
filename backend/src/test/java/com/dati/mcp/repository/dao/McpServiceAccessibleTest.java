package com.dati.mcp.repository.dao;

import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.po.ResourceAclPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("McpService accessible query tests")
class McpServiceAccessibleTest {

    @Autowired
    private McpServiceDAO mcpServiceDAO;

    @Autowired
    private ResourceAclDAO aclDAO;

    /** 不手动设 id（Hibernate 6 对手动 id + save 走 merge，无行时报 StaleObjectState），由数据库生成 UUID。 */
    private McpServicePO svc(String createdBy, String name, McpServiceStatus status) {
        McpServicePO po = new McpServicePO();
        po.setCreatedBy(createdBy);
        po.setName(name);
        po.setCode("code-" + System.nanoTime());
        po.setStatus(status);
        return po;
    }

    private void acl(String resourceId, Permission permission) {
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("MCP_SERVICE");
        po.setResourceId(resourceId);
        po.setPrincipalType("USER");
        po.setPrincipalId("u1");
        po.setPermission(permission);
        aclDAO.save(po);
    }

    @Test
    void returnsOwnedAndGrantedOnly() {
        String mine = mcpServiceDAO.save(svc("u1", "mine", McpServiceStatus.DRAFT)).getId();
        mcpServiceDAO.save(svc("u2", "theirs", McpServiceStatus.DRAFT));
        String granted = mcpServiceDAO.save(svc("u3", "granted", McpServiceStatus.DRAFT)).getId();
        acl(granted, Permission.VIEW);

        Page<McpServicePO> page = mcpServiceDAO.findAllAccessible("u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(McpServicePO::getId)
                .containsExactlyInAnyOrder(mine, granted);
    }

    @Test
    void keywordAndStatusSearchRespectsAccessibleFilter() {
        String mine = mcpServiceDAO.save(svc("u1", "sales", McpServiceStatus.PUBLISHED)).getId();
        String granted = mcpServiceDAO.save(svc("u2", "sales", McpServiceStatus.PUBLISHED)).getId();
        mcpServiceDAO.save(svc("u3", "sales", McpServiceStatus.DRAFT));
        acl(granted, Permission.EDIT);

        Page<McpServicePO> page = mcpServiceDAO.searchByKeywordAndStatusAndAccessible(
                "sales", McpServiceStatus.PUBLISHED, "u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(McpServicePO::getId)
                .containsExactlyInAnyOrder(mine, granted);
    }

    @Test
    void publicResourceVisibleToAnyone() {
        mcpServiceDAO.save(svc("u2", "public-svc", McpServiceStatus.DRAFT));
        String granted = mcpServiceDAO.save(svc("u3", "another", McpServiceStatus.DRAFT)).getId();
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("MCP_SERVICE");
        po.setResourceId(granted);
        po.setPrincipalType("GROUP");
        po.setPrincipalId("ALL_USERS");
        po.setPermission(Permission.VIEW);
        aclDAO.save(po);

        Page<McpServicePO> page = mcpServiceDAO.findAllAccessible("stranger", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(McpServicePO::getId)
                .containsExactlyInAnyOrder(granted);
    }
}
