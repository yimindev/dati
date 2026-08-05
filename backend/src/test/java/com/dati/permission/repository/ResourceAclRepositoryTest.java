package com.dati.permission.repository;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.po.ResourceAclPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ResourceAcl repository tests")
class ResourceAclRepositoryTest {

    @Autowired
    private ResourceAclDAO aclDAO;

    private ResourceAclPO acl(String principalId, Permission permission) {
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("DATA_SOURCE");
        po.setResourceId("ds-1");
        po.setPrincipalType("USER");
        po.setPrincipalId(principalId);
        po.setPermission(permission);
        return po;
    }

    @Test
    void saveAndFindByResourceAndPrincipal() {
        aclDAO.save(acl("user-b", Permission.EDIT));
        var found = aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "user-b");
        assertThat(found).isPresent();
        assertThat(found.get().getPermission()).isEqualTo(Permission.EDIT);
    }

    @Test
    void sameResourceAndPrincipalIsUnique() {
        aclDAO.save(acl("user-b", Permission.VIEW));
        assertThrows(DataIntegrityViolationException.class,
                () -> aclDAO.saveAndFlush(acl("user-b", Permission.EDIT)));
    }

    @Test
    void findListByResource() {
        aclDAO.save(acl("user-b", Permission.VIEW));
        aclDAO.save(acl("user-c", Permission.EDIT));
        List<ResourceAclPO> list = aclDAO.findByResourceTypeAndResourceId("DATA_SOURCE", "ds-1");
        assertThat(list).hasSize(2);
    }

    @Test
    void deleteByResourceAndPrincipal() {
        aclDAO.save(acl("user-b", Permission.VIEW));
        aclDAO.deleteByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "user-b");
        assertThat(aclDAO.findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
                "DATA_SOURCE", "ds-1", "USER", "user-b")).isEmpty();
    }

    @Test
    void deleteAllByResource() {
        aclDAO.save(acl("user-b", Permission.VIEW));
        aclDAO.save(acl("user-c", Permission.VIEW));
        aclDAO.deleteByResourceTypeAndResourceId("DATA_SOURCE", "ds-1");
        assertThat(aclDAO.findByResourceTypeAndResourceId("DATA_SOURCE", "ds-1")).isEmpty();
    }
}
