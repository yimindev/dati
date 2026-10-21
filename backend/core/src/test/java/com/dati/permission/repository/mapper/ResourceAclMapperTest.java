package com.dati.permission.repository.mapper;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceAcl;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.po.ResourceAclPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceAclMapper unit tests")
class ResourceAclMapperTest {

    @Test
    @DisplayName("toModel copies audit fields from the PO")
    void toModel_shouldCopyAuditFields() {
        Instant created = Instant.now().minusSeconds(60);
        Instant updated = Instant.now();

        ResourceAclPO po = new ResourceAclPO();
        po.setId("acl-1");
        po.setResourceType(ResourceType.DATA_SOURCE);
        po.setResourceId("ds-1");
        po.setPrincipalType(PrincipalType.USER);
        po.setPrincipalId("u2");
        po.setPermission(Permission.VIEW);
        po.setCreatedBy("creator-1");
        po.setCreatedAt(created);
        po.setUpdatedBy("editor-1");
        po.setUpdatedAt(updated);

        ResourceAcl acl = ResourceAclMapper.toModel(po);

        assertThat(acl.getCreatedBy()).isEqualTo("creator-1");
        assertThat(acl.getCreatedAt()).isEqualTo(created);
        assertThat(acl.getUpdatedBy()).isEqualTo("editor-1");
        assertThat(acl.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    @DisplayName("toPO copies audit fields from the model")
    void toPO_shouldCopyAuditFields() {
        Instant created = Instant.now().minusSeconds(60);
        Instant updated = Instant.now();

        ResourceAcl acl = new ResourceAcl();
        acl.setId("acl-1");
        acl.setResourceType(ResourceType.SUBJECT);
        acl.setResourceId("subject-1");
        acl.setPrincipalType(PrincipalType.USER);
        acl.setPrincipalId("u2");
        acl.setPermission(Permission.EDIT);
        acl.setCreatedBy("creator-1");
        acl.setCreatedAt(created);
        acl.setUpdatedBy("editor-1");
        acl.setUpdatedAt(updated);

        ResourceAclPO po = ResourceAclMapper.toPO(acl);

        assertThat(po.getCreatedBy()).isEqualTo("creator-1");
        assertThat(po.getCreatedAt()).isEqualTo(created);
        assertThat(po.getUpdatedBy()).isEqualTo("editor-1");
        assertThat(po.getUpdatedAt()).isEqualTo(updated);
    }
}
