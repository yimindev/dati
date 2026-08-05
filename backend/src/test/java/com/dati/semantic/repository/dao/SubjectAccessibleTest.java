package com.dati.semantic.repository.dao;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.repository.dao.ResourceAclDAO;
import com.dati.permission.repository.po.ResourceAclPO;
import com.dati.semantic.repository.po.SubjectPO;
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
@DisplayName("Subject accessible query tests")
class SubjectAccessibleTest {

    @Autowired
    private SubjectDAO subjectDAO;

    @Autowired
    private ResourceAclDAO aclDAO;

    /** 不手动设 id（Hibernate 6 对手动 id + save 走 merge，无行时报 StaleObjectState），由数据库生成 UUID。 */
    private SubjectPO subject(String createdBy, String name) {
        SubjectPO po = new SubjectPO();
        po.setCreatedBy(createdBy);
        po.setName(name);
        po.setDatasourceId("ds-dummy");
        return po;
    }

    private void acl(String resourceId, Permission permission) {
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("SUBJECT");
        po.setResourceId(resourceId);
        po.setPrincipalType("USER");
        po.setPrincipalId("u1");
        po.setPermission(permission);
        aclDAO.save(po);
    }

    @Test
    void returnsOwnedAndGrantedOnly() {
        String mine = subjectDAO.save(subject("u1", "mine")).getId();
        subjectDAO.save(subject("u2", "theirs"));
        String granted = subjectDAO.save(subject("u3", "granted")).getId();
        acl(granted, Permission.VIEW);

        Page<SubjectPO> page = subjectDAO.findAllAccessible("u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(SubjectPO::getId)
                .containsExactlyInAnyOrder(mine, granted);
    }

    @Test
    void keywordSearchRespectsAccessibleFilter() {
        String mine = subjectDAO.save(subject("u1", "sales")).getId();
        String granted = subjectDAO.save(subject("u2", "sales")).getId();
        acl(granted, Permission.EDIT);

        Page<SubjectPO> page = subjectDAO.findByKeywordAndAccessible(
                "sales", "u1", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(SubjectPO::getId)
                .containsExactlyInAnyOrder(mine, granted);
    }

    @Test
    void publicResourceVisibleToAnyone() {
        subjectDAO.save(subject("u2", "public-subject"));
        String granted = subjectDAO.save(subject("u3", "another")).getId();
        ResourceAclPO po = new ResourceAclPO();
        po.setResourceType("SUBJECT");
        po.setResourceId(granted);
        po.setPrincipalType("GROUP");
        po.setPrincipalId("ALL_USERS");
        po.setPermission(Permission.VIEW);
        aclDAO.save(po);

        Page<SubjectPO> page = subjectDAO.findAllAccessible("stranger", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(SubjectPO::getId)
                .containsExactlyInAnyOrder(granted);
    }
}
