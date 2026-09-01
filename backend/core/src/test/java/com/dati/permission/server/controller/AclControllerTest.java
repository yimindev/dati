package com.dati.permission.server.controller;

import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.model.ResourceAcl;
import com.dati.permission.domain.model.ResourceType;
import com.dati.permission.domain.service.AclService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AclController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AclController integration tests")
class AclControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AclService aclService;

    @Test
    void grantReturnsId() throws Exception {
        when(aclService.grant(eq(ResourceType.DATA_SOURCE), eq("ds-1"), eq(PrincipalType.USER),
                eq("u2"), eq(Permission.VIEW))).thenReturn("acl-1");
        mockMvc.perform(post("/v1/acls/DATA_SOURCE/ds-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principal_type":"USER","principal_id":"u2","permission":"VIEW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("acl-1"));
    }

    @Test
    void revokeReturnsId() throws Exception {
        doNothing().when(aclService).revoke(
                eq(ResourceType.DATA_SOURCE), eq("ds-1"), eq(PrincipalType.USER), eq("u2"));
        mockMvc.perform(delete("/v1/acls/DATA_SOURCE/ds-1/USER/u2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ds-1"));
    }

    @Test
    void revokeRejectsUnknownPrincipalType() throws Exception {
        mockMvc.perform(delete("/v1/acls/DATA_SOURCE/ds-1/TEAM/t1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantRejectsUnknownResourceType() throws Exception {
        mockMvc.perform(post("/v1/acls/TEAM/ds-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principal_type":"USER","principal_id":"u2","permission":"VIEW"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsEntries() throws Exception {
        ResourceAcl acl = new ResourceAcl();
        acl.setId("acl-1");
        acl.setPrincipalType(PrincipalType.USER);
        acl.setPrincipalId("u2");
        acl.setPermission(Permission.VIEW);
        when(aclService.list(ResourceType.DATA_SOURCE, "ds-1")).thenReturn(List.of(acl));
        mockMvc.perform(get("/v1/acls/DATA_SOURCE/ds-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].principal_id").value("u2"))
                .andExpect(jsonPath("$[0].permission").value("VIEW"));
    }

    @Test
    void grantAllowsPublicGroupPrincipal() throws Exception {
        when(aclService.grant(eq(ResourceType.DATA_SOURCE), eq("ds-1"), eq(PrincipalType.GROUP),
                eq("ALL_USERS"), eq(Permission.VIEW))).thenReturn("acl-9");
        mockMvc.perform(post("/v1/acls/DATA_SOURCE/ds-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principal_type":"GROUP","principal_id":"ALL_USERS","permission":"VIEW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("acl-9"));
    }

    @Test
    void grantRejectsUnknownPrincipalType() throws Exception {
        mockMvc.perform(post("/v1/acls/DATA_SOURCE/ds-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principal_type":"TEAM","principal_id":"t1","permission":"VIEW"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
