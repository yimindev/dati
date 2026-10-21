package com.dati.semantic.repository.dao;

import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import com.dati.semantic.repository.po.TermPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static com.dati.permission.domain.model.PrincipalType.ALL_USERS;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Semantic layer keyword search tests")
class SemanticKeywordSearchTest {

    private static final PageRequest PAGE = PageRequest.of(0, 10);

    @Autowired
    private SubjectDAO subjectDAO;

    @Autowired
    private TermDAO termDAO;

    @Autowired
    private SubjectTableDAO subjectTableDAO;

    @Autowired
    private TableInfoDAO tableInfoDAO;

    private SubjectPO subject(String name, List<String> aliases) {
        SubjectPO po = new SubjectPO();
        po.setDatasourceId("ds-1");
        po.setName(name);
        po.setAliases(aliases);
        po.setCreatedBy("u1");
        return po;
    }

    private TermPO term(String subjectId, String name, List<String> aliases) {
        TermPO po = new TermPO();
        po.setSubjectId(subjectId);
        po.setName(name);
        po.setAliases(aliases);
        po.setCreatedBy("u1");
        return po;
    }

    @Test
    @DisplayName("subject keyword matches aliases")
    void subjectKeyword_matchesAliases() {
        SubjectPO saved = subjectDAO.save(subject("销售主题", List.of("sales-domain", "revenue")));
        subjectDAO.save(subject("其他主题", List.of()));

        assertThat(subjectDAO.findByKeyword("sales-domain", PAGE).getContent())
                .extracting(SubjectPO::getId).containsExactly(saved.getId());
        assertThat(subjectDAO.findByKeywordAndAccessible(
                "revenue", "u1", Set.of(ALL_USERS), PAGE).getContent())
                .extracting(SubjectPO::getId).containsExactly(saved.getId());
    }

    @Test
    @DisplayName("subject keyword treats % and _ literally")
    void subjectKeyword_treatsWildcardsLiterally() {
        SubjectPO underscore = subjectDAO.save(subject("sales_report", List.of()));
        subjectDAO.save(subject("salesreport", List.of()));

        assertThat(subjectDAO.findByKeyword("%", PAGE).getContent()).isEmpty();
        assertThat(subjectDAO.findByKeyword("_", PAGE).getContent())
                .extracting(SubjectPO::getId).containsExactly(underscore.getId());
        assertThat(subjectDAO.findByKeywordAndAccessible(
                "%", "u1", Set.of(ALL_USERS), PAGE).getContent()).isEmpty();
        assertThat(subjectDAO.findByKeywordAndAccessible(
                "_", "u1", Set.of(ALL_USERS), PAGE).getContent())
                .extracting(SubjectPO::getId).containsExactly(underscore.getId());
    }

    @Test
    @DisplayName("term keyword matches name and aliases")
    void termKeyword_matchesNameAndAliases() {
        String subjectId = "subject-1";
        TermPO customer = termDAO.save(term(subjectId, "客户", List.of("customer", "buyer")));
        termDAO.save(term(subjectId, "订单", List.of()));

        assertThat(termDAO.findBySubjectIdAndKeyword(subjectId, "客户", PAGE).getContent())
                .extracting(TermPO::getId).containsExactly(customer.getId());
        assertThat(termDAO.findBySubjectIdAndKeyword(subjectId, "customer", PAGE).getContent())
                .extracting(TermPO::getId).containsExactly(customer.getId());
    }

    @Test
    @DisplayName("term keyword treats % and _ literally")
    void termKeyword_treatsWildcardsLiterally() {
        String subjectId = "subject-1";
        TermPO underscore = termDAO.save(term(subjectId, "vip_customer", List.of()));
        termDAO.save(term(subjectId, "vipcustomer", List.of()));

        assertThat(termDAO.findBySubjectIdAndKeyword(subjectId, "%", PAGE).getContent()).isEmpty();
        assertThat(termDAO.findBySubjectIdAndKeyword(subjectId, "_", PAGE).getContent())
                .extracting(TermPO::getId).containsExactly(underscore.getId());
    }

    @Test
    @DisplayName("subject table keyword matches table aliases and treats wildcards literally")
    void subjectTableKeyword_matchesTableAliases() {
        String subjectId = "subject-1";
        TableInfoPO genre = tableInfoDAO.save(table("genre", List.of("音乐流派", "流派")));
        tableInfoDAO.save(table("album", List.of()));

        SubjectTablePO link = new SubjectTablePO();
        link.setSubjectId(subjectId);
        link.setTableId(genre.getId());
        subjectTableDAO.save(link);

        assertThat(subjectTableDAO.findTablesBySubjectIdAndNameContaining(
                subjectId, "流派", PAGE).getContent())
                .extracting(TableInfoPO::getId).containsExactly(genre.getId());
        assertThat(subjectTableDAO.findTablesBySubjectIdAndNameContaining(
                subjectId, "%", PAGE).getContent()).isEmpty();
        assertThat(subjectTableDAO.findTablesBySubjectIdAndNameContaining(
                subjectId, "_", PAGE).getContent()).isEmpty();
    }

    private TableInfoPO table(String name, List<String> aliases) {
        TableInfoPO po = new TableInfoPO();
        po.setDataSourceId("ds-1");
        po.setSchema("public");
        po.setName(name);
        po.setAliases(aliases);
        po.setCreatedBy("u1");
        return po;
    }
}
