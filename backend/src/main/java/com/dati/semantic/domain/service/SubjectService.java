package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.StringUtils;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.TableMapper;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.mapper.SubjectMapper;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import com.dati.semantic.server.pojo.vo.SubjectAvailableTableVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubjectService {

    private final SubjectDAO subjectDAO;
    private final SubjectTableDAO subjectTableDAO;
    private final TableInfoDAO tableInfoDAO;
    private final SemanticIndexService semanticIndexService;

    public SubjectService(SubjectDAO subjectDAO, SubjectTableDAO subjectTableDAO,
                          TableInfoDAO tableInfoDAO, SemanticIndexService semanticIndexService) {
        this.subjectDAO = subjectDAO;
        this.subjectTableDAO = subjectTableDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.semanticIndexService = semanticIndexService;
    }

    @Transactional
    public Subject createSubject(String name, String description, String datasourceId, List<String> aliases) {
        SubjectPO subjectPO = new SubjectPO();
        subjectPO.setName(name);
        subjectPO.setDescription(description);
        subjectPO.setDatasourceId(datasourceId);
        subjectPO.setAliases(aliases != null ? aliases : new ArrayList<>());
        subjectDAO.save(subjectPO);

        String id = subjectPO.getId();

        List<String> keywords = new ArrayList<>();
        keywords.add(name);
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("subject:" + id)
                .type(SemanticEntityType.SUBJECT)
                .keywords(keywords.stream().distinct().toList())
                .description(description)
                .entity(EntityReference.builder().subjectId(id).build())
                .build();
        semanticIndexService.save(doc);

        return SubjectMapper.toSubject(subjectPO);
    }

    @Transactional
    public Subject updateSubject(String id, String name, String description, List<String> aliases) {
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, id));

        subjectPO.setName(name);
        subjectPO.setDescription(description);
        subjectPO.setAliases(aliases != null ? aliases : new ArrayList<>());
        subjectDAO.save(subjectPO);

        List<String> keywords = new ArrayList<>();
        keywords.add(name);
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("subject:" + id)
                .type(SemanticEntityType.SUBJECT)
                .keywords(keywords.stream().distinct().toList())
                .description(description)
                .entity(EntityReference.builder().subjectId(id).build())
                .build();
        semanticIndexService.save(doc);

        return SubjectMapper.toSubject(subjectPO);
    }

    @Transactional
    public void deleteSubject(String id) {
        if (!subjectDAO.existsById(id)) {
            throw new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, id);
        }
        subjectDAO.deleteById(id);
        semanticIndexService.deleteByEntity_SubjectId(id);
    }

    @Transactional
    public void addTableToSubject(String subjectId, String tableId) {
        SubjectPO subjectPO = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, subjectId));

        TableInfoPO tableInfoPO = tableInfoDAO.findById(tableId)
                .orElseThrow(() -> new DatiException(ErrorCode.DS_NOT_FOUND, tableId));

        if (!subjectPO.getDatasourceId().equals(tableInfoPO.getDataSourceId())) {
            throw new DatiException(ErrorCode.SM_TABLE_NOT_IN_SUBJECT, tableInfoPO.getName(), subjectId);
        }

        if (subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)) {
            throw new DatiException(ErrorCode.SM_TABLE_ALREADY_ASSOCIATED, tableInfoPO.getName(), subjectId);
        }

        SubjectTablePO subjectTablePO = new SubjectTablePO();
        subjectTablePO.setSubjectId(subjectId);
        subjectTablePO.setTableId(tableId);
        subjectTableDAO.save(subjectTablePO);
    }

    @Transactional
    public void removeTableFromSubject(String subjectId, String tableId) {
        subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_ASSOCIATION_NOT_FOUND, subjectId, tableId));

        subjectTableDAO.deleteBySubjectIdAndTableId(subjectId, tableId);
    }

    /**
     * 查询 Subject 列表。
     * keyword 非空时同时按 ID 前缀匹配（区分大小写）或名称模糊匹配（忽略大小写）。
     */
    @Transactional(readOnly = true)
    public Page<Subject> getSubjects(@Nullable String keyword, Pageable pageable) {
        Page<SubjectPO> pos = StringUtils.isEmpty(keyword)
                ? subjectDAO.findAll(pageable)
                : subjectDAO.findByKeyword(keyword, pageable);
        return pos.map(SubjectMapper::toSubject);
    }

    @Transactional(readOnly = true)
    public List<SubjectAvailableTableVO> getAvailableTables(String subjectId, String schema) {
        SubjectPO subjectPO = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, subjectId));

        List<SubjectTablePO> linkedTables = subjectTableDAO.findBySubjectId(subjectId);
        List<String> linkedTableIds = linkedTables.stream()
                .map(SubjectTablePO::getTableId)
                .toList();

        List<TableInfoPO> allTables = tableInfoDAO.findByDataSourceIdAndSchema(subjectPO.getDatasourceId(), schema);

        return allTables.stream()
                .filter(table -> !linkedTableIds.contains(table.getId()))
                .map(table -> {
                    SubjectAvailableTableVO vo = new SubjectAvailableTableVO();
                    vo.setTableId(table.getId());
                    vo.setTableName(table.getName());
                    vo.setSchema(table.getSchema());
                    vo.setDescription(table.getDescription());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TableInfo> getTablesBySubjectId(
            String subjectId, @Nullable String keyword, Pageable pageable) {
        if (!subjectDAO.existsById(subjectId)) {
            throw new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, subjectId);
        }

        return (StringUtils.isEmpty(keyword)
                ? subjectTableDAO.findTablesBySubjectId(subjectId, pageable)
                : subjectTableDAO.findTablesBySubjectIdAndNameContaining(subjectId, keyword, pageable))
                .map(TableMapper::toTableInfo);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return subjectDAO.findAllById(ids).stream()
                .map(SubjectMapper::toSubject)
                .toList();
    }

    @Transactional(readOnly = true)
    public Subject getSubjectById(String id) {
        List<Subject> subjects = getSubjectsByIds(List.of(id));
        if (subjects.isEmpty()) {
            throw new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, id);
        }
        return subjects.getFirst();
    }

}
