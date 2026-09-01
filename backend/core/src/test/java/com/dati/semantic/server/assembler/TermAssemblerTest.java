package com.dati.semantic.server.assembler;

import com.dati.auth.domain.service.UserService;
import com.dati.semantic.domain.TermRelationType;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.server.pojo.vo.TermRelationVO;
import com.dati.semantic.server.pojo.vo.TermVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("TermAssembler unit tests")
class TermAssemblerTest {

    @Mock
    private UserService userService;

    private TermAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new TermAssembler();
        assembler.setUserService(userService);
        org.mockito.Mockito.lenient().when(userService.getUserMap(any())).thenReturn(Map.of());
    }

    private Term buildTerm(TermRelation relation) {
        Term term = new Term();
        term.setId("term-001");
        term.setSubjectId("subject-001");
        term.setName("Term " + "term-001");
        term.setDescription("Desc");
        term.setCreatedBy("user-1");
        term.setUpdatedBy("user-1");
        term.setAliases(List.of("alias"));
        if (relation != null) {
            term.setRelations(List.of(relation));
        }
        return term;
    }

    private TermRelation buildRelation() {
        return TermRelation.builder()
                .id("rel-001")
                .termId("term-001")
                .entityType(TermRelationType.FIELD)
                .tableId("table-001")
                .tableName("orders")
                .schema("sales")
                .fieldName("amount")
                .build();
    }

    @Test
    @DisplayName("toPageResponse - fills relations into each list VO")
    void toPageResponse_shouldFillRelations() {
        TermRelation relation = buildRelation();
        Term term = buildTerm(relation);
        Page<Term> page = new PageImpl<>(List.of(term), PageRequest.of(0, 10), 1);

        var response = assembler.toPageResponse(page);

        TermVO vo = response.getData().getFirst();
        assertThat(vo.getRelations()).hasSize(1);
        TermRelationVO relVO = vo.getRelations().getFirst();
        assertThat(relVO.getEntityType()).isEqualTo("FIELD");
        assertThat(relVO.getTableName()).isEqualTo("orders");
        assertThat(relVO.getSchema()).isEqualTo("sales");
        assertThat(relVO.getFieldName()).isEqualTo("amount");
    }

    @Test
    @DisplayName("toPageResponse - term without relations gets empty list, not null")
    void toPageResponse_withoutRelations_shouldReturnEmptyList() {
        Term term = buildTerm(null);
        Page<Term> page = new PageImpl<>(List.of(term), PageRequest.of(0, 10), 1);

        var response = assembler.toPageResponse(page);

        assertThat(response.getData().getFirst().getRelations()).isEmpty();
    }

    @Test
    @DisplayName("toVO(term, relations) - fills relations for detail view")
    void toVO_withRelations_shouldFillRelations() {
        Term term = buildTerm(null);

        TermVO vo = assembler.toVO(term, List.of(buildRelation()));

        assertThat(vo.getRelations()).hasSize(1);
        assertThat(vo.getRelations().getFirst().getTableName()).isEqualTo("orders");
    }

    @Test
    @DisplayName("toRelationVO - handles null relation")
    void toRelationVO_null_shouldReturnNull() {
        assertThat(assembler.toRelationVO(null)).isNull();
    }
}
