package com.dati.permission.repository.dao;

import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.repository.po.ResourceAclPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResourceAclDAO extends JpaRepository<ResourceAclPO, String> {

    Optional<ResourceAclPO> findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
            ResourceType resourceType, String resourceId, PrincipalType principalType, String principalId);

    List<ResourceAclPO> findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalIdIn(
            ResourceType resourceType, String resourceId, PrincipalType principalType, Collection<String> principalIds);

    List<ResourceAclPO> findByResourceTypeAndResourceId(ResourceType resourceType, String resourceId);

    void deleteByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
            ResourceType resourceType, String resourceId, PrincipalType principalType, String principalId);

    void deleteByResourceTypeAndResourceId(ResourceType resourceType, String resourceId);
}
