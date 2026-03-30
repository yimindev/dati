package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.server.assembler.TermAssembler;
import com.dati.semantic.server.pojo.request.CreateTermRequest;
import com.dati.semantic.server.pojo.vo.TermVO;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TermController 集成测试")
class TermControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TermService termService;

    @MockBean
    private TermAssembler termAssembler;

    @Test
    @DisplayName("创建 Term - 成功返回 201")
    void createTerm_shouldReturn201() throws Exception {
        CreateTermRequest request = new CreateTermRequest();
        request.setName("Test Term");
        request.setDescription("Test Description");

        Term term = new Term();
        term.setId("term-001");
        term.setSubjectId("subject-001");
        term.setName("Test Term");
        term.setDescription("Test Description");
        term.setCreatedAt(java.time.Instant.now());
        term.setUpdatedAt(java.time.Instant.now());

        TermVO termVO = new TermVO();
        termVO.setId("term-001");
        termVO.setSubjectId("subject-001");
        termVO.setName("Test Term");
        termVO.setDescription("Test Description");
        termVO.setCreatedAt(java.time.Instant.now());
        termVO.setUpdatedAt(java.time.Instant.now());

        when(termService.createTerm(anyString(), anyString(), anyString())).thenReturn(term);
        when(termAssembler.toVO(any(Term.class))).thenReturn(termVO);

        mockMvc.perform(post("/v1/subjects/subject-001/terms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("term-001"))
            .andExpect(jsonPath("$.name").value("Test Term"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.subject_id").value("subject-001"));
    }

    @Test
    @DisplayName("获取 Term - 成功返回 200")
    void getTerm_shouldReturn200() throws Exception {
        Term term = new Term();
        term.setId("term-001");
        term.setSubjectId("subject-001");
        term.setName("Test Term");
        term.setDescription("Test Description");
        term.setCreatedAt(java.time.Instant.now());
        term.setUpdatedAt(java.time.Instant.now());

        TermVO termVO = new TermVO();
        termVO.setId("term-001");
        termVO.setSubjectId("subject-001");
        termVO.setName("Test Term");
        termVO.setDescription("Test Description");

        when(termService.getTermById("term-001")).thenReturn(term);
        when(termAssembler.toVO(term)).thenReturn(termVO);

        mockMvc.perform(get("/v1/terms/term-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("term-001"))
            .andExpect(jsonPath("$.name").value("Test Term"));
    }

    @Test
    @DisplayName("删除 Term - 成功返回 204")
    void deleteTerm_shouldReturn204() throws Exception {
        doNothing().when(termService).deleteTerm("term-001");

        mockMvc.perform(delete("/v1/terms/term-001"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("更新 Term - 成功返回 200")
    void updateTerm_shouldReturn200() throws Exception {
        com.dati.semantic.server.pojo.request.UpdateTermRequest request = new com.dati.semantic.server.pojo.request.UpdateTermRequest();
        request.setName("Updated Term");
        request.setDescription("Updated Description");

        Term term = new Term();
        term.setId("term-001");
        term.setSubjectId("subject-001");
        term.setName("Updated Term");
        term.setDescription("Updated Description");
        term.setCreatedAt(java.time.Instant.now());
        term.setUpdatedAt(java.time.Instant.now());

        TermVO termVO = new TermVO();
        termVO.setId("term-001");
        termVO.setSubjectId("subject-001");
        termVO.setName("Updated Term");
        termVO.setDescription("Updated Description");
        termVO.setCreatedAt(java.time.Instant.now());
        termVO.setUpdatedAt(java.time.Instant.now());

        when(termService.updateTerm(anyString(), anyString(), anyString())).thenReturn(term);
        when(termAssembler.toVO(any(Term.class))).thenReturn(termVO);

        mockMvc.perform(put("/v1/terms/term-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("term-001"))
            .andExpect(jsonPath("$.name").value("Updated Term"));
    }

    @Test
    @DisplayName("关联 Term 关系 - 成功返回 201")
    void linkTermRelation_shouldReturn201() throws Exception {
        com.dati.semantic.server.pojo.request.LinkTermRelationRequest request = new com.dati.semantic.server.pojo.request.LinkTermRelationRequest();
        request.setEntityType(com.dati.semantic.domain.SemanticEntityType.FIELD);
        request.setTableId("table-001");
        request.setFieldName("field-001");

        doNothing().when(termService).linkEntity(anyString(), any(), anyString(), anyString());

        mockMvc.perform(post("/v1/terms/term-001/relations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("取消关联 Term 关系 - 成功返回 204")
    void unlinkTermRelation_shouldReturn204() throws Exception {
        doNothing().when(termService).unlinkEntity(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/v1/terms/term-001/relations/table-001/field-001"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("获取 Subject 下的 Terms - 成功返回 200")
    void getTermsBySubject_shouldReturn200() throws Exception {
        Term term1 = new Term();
        term1.setId("term-001");
        term1.setSubjectId("subject-001");
        term1.setName("Term 1");
        term1.setDescription("Description 1");
        term1.setCreatedAt(java.time.Instant.now());
        term1.setUpdatedAt(java.time.Instant.now());

        Term term2 = new Term();
        term2.setId("term-002");
        term2.setSubjectId("subject-001");
        term2.setName("Term 2");
        term2.setDescription("Description 2");
        term2.setCreatedAt(java.time.Instant.now());
        term2.setUpdatedAt(java.time.Instant.now());

        TermVO termVO1 = new TermVO();
        termVO1.setId("term-001");
        termVO1.setSubjectId("subject-001");
        termVO1.setName("Term 1");
        termVO1.setDescription("Description 1");

        TermVO termVO2 = new TermVO();
        termVO2.setId("term-002");
        termVO2.setSubjectId("subject-001");
        termVO2.setName("Term 2");
        termVO2.setDescription("Description 2");

        when(termService.getTermsBySubject("subject-001")).thenReturn(java.util.List.of(term1, term2));
        when(termAssembler.toVO(term1)).thenReturn(termVO1);
        when(termAssembler.toVO(term2)).thenReturn(termVO2);

        mockMvc.perform(get("/v1/subjects/subject-001/terms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("term-001"))
            .andExpect(jsonPath("$[1].id").value("term-002"));
    }
}
