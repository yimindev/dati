package com.dati.permission.repository.dao;

import com.dati.permission.repository.po.ResourceAclPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceAclDAO extends JpaRepository<ResourceAclPO, String> {

    Optional<ResourceAclPO> findByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
            String resourceType, String resourceId, String principalType, String principalId);

    List<ResourceAclPO> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    void deleteByResourceTypeAndResourceIdAndPrincipalTypeAndPrincipalId(
            String resourceType, String resourceId, String principalType, String principalId);

    void deleteByResourceTypeAndResourceId(String resourceType, String resourceId);
}
