package com.dati.auth.server.controller;

import com.dati.auth.domain.service.UserService;
import com.dati.auth.server.pojo.UserBriefVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController integration tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void searchReturnsUserBriefs() throws Exception {
        UserBriefVO vo = new UserBriefVO();
        vo.setId("u-1");
        vo.setName("alice");
        vo.setDisplayName("Alice");
        when(userService.searchUsers(eq("ali"))).thenReturn(List.of(vo));

        mockMvc.perform(get("/v1/users/search").param("keyword", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("u-1"))
                .andExpect(jsonPath("$[0].name").value("alice"))
                .andExpect(jsonPath("$[0].display_name").value("Alice"));
    }

    @Test
    void searchWithoutKeywordReturnsEmptyList() throws Exception {
        when(userService.searchUsers(eq(""))).thenReturn(List.of());
        mockMvc.perform(get("/v1/users/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
