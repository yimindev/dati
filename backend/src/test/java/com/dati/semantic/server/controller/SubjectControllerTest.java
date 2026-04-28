package com.dati.semantic.server.controller;

import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.TableInfoVO;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.server.assembler.SubjectAssembler;
import com.dati.semantic.server.pojo.request.AddTableToSubjectRequest;
import com.dati.semantic.server.pojo.request.CreateSubjectRequest;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private SubjectAssembler subjectAssembler;

    @MockitoBean
    private TableAssembler tableAssembler;

    @Test
    @DisplayName("创建 Subject - 参数校验失败返回 400")
    void createSubject_withInvalidRequest_shouldReturn400() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest();
        request.setName("");
        request.setDatasourceId("");

        mockMvc.perform(post("/v1/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("创建 Subject - 成功返回 201")
    void createSubject_shouldReturn201() throws Exception {
        CreateSubjectRequest request = new CreateSubjectRequest();
        request.setName("Test Subject");
        request.setDescription("Test Description");
        request.setDatasourceId("datasource-001");

        Subject subject = new Subject();
        subject.setId("subject-001");
        subject.setName("Test Subject");
        subject.setDescription("Test Description");
        subject.setDatasourceId("datasource-001");
        subject.setCreatedAt(Instant.now());
        subject.setUpdatedAt(Instant.now());

        SubjectVO subjectVO = new SubjectVO();
        subjectVO.setId("subject-001");
        subjectVO.setName("Test Subject");
        subjectVO.setDescription("Test Description");
        subjectVO.setDatasourceId("datasource-001");
        subjectVO.setCreatedAt(Instant.now());
        subjectVO.setUpdatedAt(Instant.now());

        when(subjectService.createSubject(anyString(), anyString(), anyString(), any())).thenReturn(subject);

        mockMvc.perform(post("/v1/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("subject-001"));
    }

    @Test
    @DisplayName("获取 Subject Tables - 成功返回 200")
    void getSubjectTables_shouldReturn200() throws Exception {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setId("table-001");
        tableInfo.setName("users");
        tableInfo.setDescription("Users table description");
        tableInfo.setDatasourceId("datasource-001");
        tableInfo.setSchema("public");
        tableInfo.setCreatedAt(Instant.now());
        tableInfo.setUpdatedAt(Instant.now());

        TableInfoVO tableVO = new TableInfoVO();
        tableVO.setId("table-001");
        tableVO.setName("users");
        tableVO.setSchema("public");
        tableVO.setDescription("Users table description");

        when(subjectService.getTablesBySubjectId("subject-001")).thenReturn(List.of(tableInfo));
        when(tableAssembler.toTableInfoVO(any(TableInfo.class))).thenReturn(tableVO);

        mockMvc.perform(get("/v1/subjects/subject-001/tables"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("table-001"))
            .andExpect(jsonPath("$[0].name").value("users"));
    }

    @Test
    @DisplayName("删除 Subject - 成功返回 200")
    void deleteSubject_shouldReturn200() throws Exception {
        doNothing().when(subjectService).deleteSubject("subject-001");

        mockMvc.perform(delete("/v1/subjects/subject-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("subject-001"));
    }

    @Test
    @DisplayName("添加 Table 到 Subject - 成功返回 200")
    void addTableToSubject_shouldReturn200() throws Exception {
        AddTableToSubjectRequest request = new AddTableToSubjectRequest();
        request.setTableId("table-001");

        doNothing().when(subjectService).addTableToSubject("subject-001", "table-001");

        mockMvc.perform(post("/v1/subjects/subject-001/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("table-001"));
    }

    @Test
    @DisplayName("删除 Subject 的 Table - 成功返回 200")
    void removeTableFromSubject_shouldReturn200() throws Exception {
        doNothing().when(subjectService).removeTableFromSubject("subject-001", "table-001");

        mockMvc.perform(delete("/v1/subjects/subject-001/tables/table-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("table-001"));
    }
}
