package com.dati.permission.repository.po;

import com.dati.base.pojo.BasePO;
import com.dati.permission.domain.model.Permission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resource_acl", uniqueConstraints = @UniqueConstraint(
        name = "uk_resource_acl",
        columnNames = {"resource_type", "resource_id", "principal_type", "principal_id"}))
public class ResourceAclPO extends BasePO {

    @Column(name = "resource_type", length = 32, nullable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 36, nullable = false)
    private String resourceId;

    @Column(name = "principal_type", length = 16, nullable = false)
    private String principalType;

    @Column(name = "principal_id", length = 64, nullable = false)
    private String principalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 16, nullable = false)
    private Permission permission;
}
