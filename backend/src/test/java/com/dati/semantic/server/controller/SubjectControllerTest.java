package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.model.SubjectTable;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.server.assembler.SubjectAssembler;
import com.dati.semantic.server.pojo.request.AddTableToSubjectRequest;
import com.dati.semantic.server.pojo.request.CreateSubjectRequest;
import com.dati.semantic.server.pojo.vo.SubjectDetailVO;
import com.dati.semantic.server.pojo.vo.SubjectTableVO;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubjectController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SubjectController 集成测试")
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubjectService subjectService;

    @MockBean
    private SubjectAssembler subjectAssembler;

    @Test
    @DisplayName("创建 Subject - 成功返回 201")
    void createSubject_shouldReturn201() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest();
        request.setName("Test Subject");
        request.setDescription("Test Description");
        request.setDatasourceId("datasource-001");

        Subject subject = Subject.builder()
                .id("subject-001")
                .name("Test Subject")
                .description("Test Description")
                .datasourceId("datasource-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SubjectVO subjectVO = new SubjectVO();
        subjectVO.setId("subject-001");
        subjectVO.setName("Test Subject");
        subjectVO.setDescription("Test Description");
        subjectVO.setDatasourceId("datasource-001");
        subjectVO.setCreatedAt(LocalDateTime.now());
        subjectVO.setUpdatedAt(LocalDateTime.now());

        when(subjectService.createSubject(anyString(), anyString(), anyString())).thenReturn(subject);
        when(subjectAssembler.toVO(any(Subject.class))).thenReturn(subjectVO);

        mockMvc.perform(post("/v1/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("subject-001"))
            .andExpect(jsonPath("$.name").value("Test Subject"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.datasource_id").value("datasource-001"));
    }

    @Test
    @DisplayName("获取 Subject - 成功返回 200 和详情(含tables)")
    void getSubject_shouldReturn200WithDetail() throws Exception {
        Subject subject = Subject.builder()
                .id("subject-001")
                .name("Test Subject")
                .description("Test Description")
                .datasourceId("datasource-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SubjectTable subjectTable = SubjectTable.builder()
                .id("st-001")
                .subjectId("subject-001")
                .tableId("table-001")
                .tableName("users")
                .displayName("Users Table")
                .createdAt(LocalDateTime.now())
                .build();

        com.dati.semantic.domain.model.SubjectDetailVO domainDetail = 
                new com.dati.semantic.domain.model.SubjectDetailVO(subject, List.of(subjectTable));

        SubjectDetailVO serverDetail = new SubjectDetailVO();
        serverDetail.setId("subject-001");
        serverDetail.setName("Test Subject");
        serverDetail.setDescription("Test Description");
        serverDetail.setDatasourceId("datasource-001");
        serverDetail.setCreatedAt(subject.getCreatedAt());
        serverDetail.setUpdatedAt(subject.getUpdatedAt());
        SubjectTableVO tableVO = new SubjectTableVO();
        tableVO.setId("st-001");
        tableVO.setSubjectId("subject-001");
        tableVO.setTableId("table-001");
        tableVO.setCreatedAt(subjectTable.getCreatedAt());
        serverDetail.setTables(List.of(tableVO));

        when(subjectService.getSubjectWithTables("subject-001")).thenReturn(domainDetail);
        when(subjectAssembler.toDetailVO(any(com.dati.semantic.domain.model.SubjectDetailVO.class))).thenReturn(serverDetail);

        mockMvc.perform(get("/v1/subjects/subject-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("subject-001"))
            .andExpect(jsonPath("$.name").value("Test Subject"))
            .andExpect(jsonPath("$.tables[0].id").value("st-001"))
            .andExpect(jsonPath("$.tables[0].table_id").value("table-001"));
    }

    @Test
    @DisplayName("删除 Subject - 成功返回 204")
    void deleteSubject_shouldReturn204() throws Exception {
        doNothing().when(subjectService).deleteSubject("subject-001");

        mockMvc.perform(delete("/v1/subjects/subject-001"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("添加 Table 到 Subject - 成功返回 201")
    void addTableToSubject_shouldReturn201() throws Exception {
        AddTableToSubjectRequest request = new AddTableToSubjectRequest();
        request.setTableId("table-001");

        doNothing().when(subjectService).addTableToSubject("subject-001", "table-001");

        mockMvc.perform(post("/v1/subjects/subject-001/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("删除 Subject 的 Table - 成功返回 204")
    void removeTableFromSubject_shouldReturn204() throws Exception {
        doNothing().when(subjectService).removeTableFromSubject("subject-001", "table-001");

        mockMvc.perform(delete("/v1/subjects/subject-001/tables/table-001"))
            .andExpect(status().isNoContent());
    }
}
