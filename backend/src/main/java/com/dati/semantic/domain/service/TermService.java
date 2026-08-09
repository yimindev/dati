package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.StringUtils;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.TermRelationType;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.dao.TermRelationDAO;
import com.dati.semantic.repository.mapper.TermMapper;
import com.dati.semantic.repository.mapper.TermRelationMapper;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.TermPO;
import com.dati.semantic.repository.po.TermRelationPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TermService {

    private final TermDAO termDAO;
    private final TermRelationDAO termRelationDAO;
    private final SubjectTableDAO subjectTableDAO;
    private final TableInfoDAO tableInfoDAO;
    private final SemanticIndexService semanticIndexService;

    private final SubjectDAO subjectDAO;

    public TermService(TermDAO termDAO, TermRelationDAO termRelationDAO,
                       SubjectTableDAO subjectTableDAO, TableInfoDAO tableInfoDAO,
                       SemanticIndexService semanticIndexService, SubjectDAO subjectDAO) {
        this.termDAO = termDAO;
        this.termRelationDAO = termRelationDAO;
        this.subjectTableDAO = subjectTableDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.semanticIndexService = semanticIndexService;
        this.subjectDAO = subjectDAO;
    }

    @Transactional
    public Term createTerm(Term term) {
        TermPO termPO = TermMapper.toPO(term);
        termDAO.save(termPO);

        String id = termPO.getId();
        String name = term.getName();
        String description = term.getDescription();
        List<String> aliases = term.getAliases();

        List<String> keywords = new java.util.ArrayList<>();
        keywords.add(name);
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("term:" + id)
                .type(SemanticEntityType.TERM)
                .keywords(keywords.stream().distinct().toList())
                .description(description)
                .entity(EntityReference.builder().subjectId(term.getSubjectId()).build())
                .build();
        semanticIndexService.save(doc);

        return TermMapper.toTerm(termPO);
    }

    @Transactional
    public Term updateTerm(String id, Term term) {
        TermPO termPO = termDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_TERM_NOT_FOUND, id));

        if (term.getName() != null) {
            termPO.setName(term.getName());
        }
        if (term.getDescription() != null) {
            termPO.setDescription(term.getDescription());
        }
        if (term.getAliases() != null) {
            termPO.setAliases(term.getAliases());
        }
        if (term.getUpdatedBy() != null) {
            termPO.setUpdatedBy(term.getUpdatedBy());
        }
        termDAO.save(termPO);

        List<String> keywords = new java.util.ArrayList<>();
        keywords.add(termPO.getName());
        List<String> aliases = termPO.getAliases();
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("term:" + id)
                .type(SemanticEntityType.TERM)
                .keywords(keywords.stream().distinct().toList())
                .description(termPO.getDescription())
                .entity(EntityReference.builder().subjectId(termPO.getSubjectId()).build())
                .build();
        semanticIndexService.save(doc);

        return TermMapper.toTerm(termPO);
    }

    @Transactional
    public void deleteTerm(String id) {
        if (!termDAO.existsById(id)) {
            throw new DatiException(ErrorCode.SM_TERM_NOT_FOUND, id);
        }
        termRelationDAO.deleteByTermId(id);
        termDAO.deleteById(id);
        semanticIndexService.deleteById("term:" + id);
    }

    @Transactional
    public void linkEntity(String termId, TermRelationType entityType, String tableId, String fieldName) {
        TermPO termPO = termDAO.findById(termId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_TERM_NOT_FOUND, termId));

        if (entityType == TermRelationType.FIELD && fieldName == null) {
            throw new DatiException(ErrorCode.INVALID_PARAMETER, "fieldName is required");
        }

        if (!subjectTableDAO.existsBySubjectIdAndTableId(termPO.getSubjectId(), tableId)) {
            throw new DatiException(ErrorCode.SM_TABLE_NOT_IN_SUBJECT, tableId, termPO.getSubjectId());
        }

        TermRelationPO relationPO = TermRelationMapper.toPO(termId, entityType, tableId, fieldName);
        termRelationDAO.save(relationPO);
    }

    @Transactional
    public void unlinkEntity(String termId, String tableId, String fieldName) {
        if (fieldName == null) {
            termRelationDAO.deleteByTermIdAndTableId(termId, tableId);
        } else {
            termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName)
                    .ifPresent(termRelationDAO::delete);
        }
    }

    @Transactional(readOnly = true)
    public Page<Term> getTermsBySubject(String subjectId, @Nullable String keyword, Pageable pageable) {
        Page<TermPO> pos = StringUtils.isEmpty(keyword)
                ? termDAO.findBySubjectId(subjectId, pageable)
                : termDAO.findBySubjectIdAndKeyword(subjectId, keyword, pageable);
        return pos.map(TermMapper::toTerm);
    }

    @Transactional(readOnly = true)
    public List<TermRelation> getTermRelations(String termId) {
        List<TermRelationPO> relationPOList = termRelationDAO.findByTermId(termId);
        Map<String, TableInfoPO> tableInfoMap = getTableInfoMap(termId, relationPOList);
        return relationPOList.stream()
                .map(relation -> toTermRelation(relation, tableInfoMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Term getTermById(String id) {
        TermPO termPO = termDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_TERM_NOT_FOUND, id));
        return TermMapper.toTerm(termPO);
    }

    @Transactional(readOnly = true)
    public Term getTermByIdWithRelations(String id) {
        Term term = getTermById(id);
        List<TermRelation> relations = getTermRelations(id);
        term.setRelations(relations);
        return term;
    }

    private Map<String, TableInfoPO> getTableInfoMap(String termId, List<TermRelationPO> relations) {
        Set<String> tableIds = relations.stream()
                .map(TermRelationPO::getTableId)
                .filter(tableId -> tableId != null && !tableId.isBlank())
                .collect(Collectors.toSet());
        if (tableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return tableInfoDAO.findAllById(tableIds).stream()
                    .collect(Collectors.toMap(TableInfoPO::getId, table -> table));
        } catch (Exception e) {
            log.warn("Failed to load table metadata for termId={}, tableIds={}", termId, tableIds, e);
            return Collections.emptyMap();
        }
    }

    /** Lightweight term info with subject name for SEARCH_METADATA. */
    public record TermInfo(String name, String description, String subjectName) {}

    /** Batch query terms with resolved subject names. */
    public List<TermInfo> getTermsWithSubject(Set<String> termIds) {
        if (termIds == null || termIds.isEmpty()) return List.of();

        List<TermPO> terms = termDAO.findAllById(termIds);
        Set<String> subjectIds = terms.stream()
                .map(TermPO::getSubjectId).collect(Collectors.toSet());
        Map<String, String> subjectNames = subjectDAO.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(
                        com.dati.base.pojo.BaseResourcePO::getId,
                        com.dati.base.pojo.BaseResourcePO::getName));

        return terms.stream()
                .map(t -> new TermInfo(t.getName(), t.getDescription(),
                        subjectNames.getOrDefault(t.getSubjectId(), "")))
                .toList();
    }

    private TermRelation toTermRelation(TermRelationPO po, Map<String, TableInfoPO> tableInfoMap) {
        String tableName = null;
        String schema = null;
        TableInfoPO tableInfo = tableInfoMap.get(po.getTableId());
        if (tableInfo != null) {
            tableName = tableInfo.getName();
            schema = tableInfo.getSchema();
        }

        return TermRelation.builder()
                .id(po.getId())
                .termId(po.getTermId())
                .entityType(po.getEntityType())
                .tableId(po.getTableId())
                .fieldName(po.getFieldName())
                .tableName(tableName)
                .schema(schema)
                .build();
    }

}
