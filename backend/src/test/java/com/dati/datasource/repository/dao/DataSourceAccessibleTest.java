package com.dati.datasource.repository.dao;

import com.dati.datasource.repository.po.DataSourcePO;
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
@DisplayName("DataSource accessible query tests")
class DataSourceAccessibleTest {

    @Autowired
    private DataSourceDAO dataSourceDAO;

    @Autowired
    private ResourceAclDAO aclDAO;

    /** 不手动设 id（Hibernate 6 对手动 id + save 走 merge，无行时报 StaleObjectState），由数据库生成 UUID。 */
    private DataSourcePO ds(String createdBy, String name) {
        DataSourcePO po = new DataSourcePO();
        po.setCreatedBy(createdBy);
        po.setName(name);
        return po;
    }

    private void acl(String resourceId, Permission permission) {
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("DATA_SOURCE");
        po.setResourceId(resourceId);
        po.setPrincipalType("USER");
        po.setPrincipalId("u1");
        po.setPermission(permission);
        aclDAO.save(po);
    }

    @Test
    void returnsOwnedAndGrantedOnly() {
        String mine = dataSourceDAO.save(ds("u1", "mine")).getId();
        dataSourceDAO.save(ds("u2", "theirs"));
        String granted = dataSourceDAO.save(ds("u3", "granted")).getId();
        acl(granted, Permission.VIEW);

        Page<DataSourcePO> page = dataSourceDAO.findAllAccessible("u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(DataSourcePO::getId)
                .containsExactlyInAnyOrder(mine, granted);
    }

    @Test
    void searchRespectsAccessibleFilter() {
        String mine = dataSourceDAO.save(ds("u1", "sales")).getId();
        String granted = dataSourceDAO.save(ds("u2", "sales")).getId();
        dataSourceDAO.save(ds("u2", "other"));
        acl(granted, Permission.EDIT);

        Page<DataSourcePO> page = dataSourceDAO.findByNameContainingOrIdAndAccessible(
                "sales", "u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(DataSourcePO::getId)
                .containsExactlyInAnyOrder(mine, granted);
    }

    @Test
    void publicResourceVisibleToAnyone() {
        dataSourceDAO.save(ds("u2", "public-ds"));
        String granted = dataSourceDAO.save(ds("u3", "another")).getId();
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("DATA_SOURCE");
        po.setResourceId(granted);
        po.setPrincipalType("GROUP");
        po.setPrincipalId("ALL_USERS");
        po.setPermission(Permission.VIEW);
        aclDAO.save(po);

        Page<DataSourcePO> page = dataSourceDAO.findAllAccessible("stranger", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(DataSourcePO::getId)
                .containsExactlyInAnyOrder(granted);
    }
}
