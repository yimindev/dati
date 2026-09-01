package com.dati.permission.server.controller;

import com.dati.base.pojo.IdResponse;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceAcl;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.domain.service.AclService;
import com.dati.permission.server.pojo.AclEntryVO;
import com.dati.permission.server.pojo.GrantRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/acls")
public class AclController {

    private final AclService aclService;

    public AclController(AclService aclService) {
        this.aclService = aclService;
    }

    @PostMapping("/{type}/{resourceId}")
    public IdResponse grant(@PathVariable ResourceType type,
                            @PathVariable String resourceId,
                            @Valid @RequestBody GrantRequest request) {
        String id = aclService.grant(type, resourceId, request.getPrincipalType(),
                request.getPrincipalId(), request.getPermission());
        return new IdResponse(id);
    }

    @DeleteMapping("/{type}/{resourceId}/{principalType}/{principalId}")
    public IdResponse revoke(@PathVariable ResourceType type,
                             @PathVariable String resourceId,
                             @PathVariable PrincipalType principalType,
                             @PathVariable String principalId) {
        aclService.revoke(type, resourceId, principalType, principalId);
        return new IdResponse(resourceId);
    }

    @GetMapping("/{type}/{resourceId}")
    public List<AclEntryVO> list(@PathVariable ResourceType type,
                                 @PathVariable String resourceId) {
        return aclService.list(type, resourceId).stream().map(this::toVO).toList();
    }

    private AclEntryVO toVO(ResourceAcl acl) {
        AclEntryVO vo = new AclEntryVO();
        vo.setId(acl.getId());
        vo.setPrincipalType(acl.getPrincipalType().name());
        vo.setPrincipalId(acl.getPrincipalId());
        vo.setPrincipalName(acl.getPrincipalName());
        vo.setPermission(acl.getPermission().name());
        vo.setCreatedBy(acl.getCreatedBy());
        return vo;
    }

}