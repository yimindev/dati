package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import com.dati.semantic.server.pojo.vo.SubjectAvailableTableVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    public Subject createSubject(String name, String description, String datasourceId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subject name cannot be null or empty");
        }

        SubjectPO subjectPO = new SubjectPO();
        subjectPO.setName(name);
        subjectPO.setDescription(description);
        subjectPO.setDatasourceId(datasourceId);
        subjectDAO.save(subjectPO);

        String id = subjectPO.getId();

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("subject:" + id)
                .type(SemanticEntityType.SUBJECT)
                .keywords(List.of(name))
                .description(description)
                .entity(EntityReference.builder().subjectId(id).build())
                .build();
        semanticIndexService.save(doc);

        return toSubject(subjectPO);
    }

    @Transactional
    public Subject updateSubject(String id, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subject name cannot be null or empty");
        }
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException("Subject not found: " + id));

        subjectPO.setName(name);
        subjectPO.setDescription(description);
        subjectDAO.save(subjectPO);

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("subject:" + id)
                .type(SemanticEntityType.SUBJECT)
                .keywords(List.of(name))
                .description(description)
                .entity(EntityReference.builder().subjectId(id).build())
                .build();
        semanticIndexService.save(doc);

        return toSubject(subjectPO);
    }

    @Transactional
    public void deleteSubject(String id) {
        if (!subjectDAO.existsById(id)) {
            throw new DatiException("Subject not found: " + id);
        }
        subjectDAO.deleteById(id);
        semanticIndexService.deleteByEntity_SubjectId(id);
    }

    @Transactional
    public void addTableToSubject(String subjectId, String tableId) {
        SubjectPO subjectPO = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException("Subject not found: " + subjectId));

        TableInfoPO tableInfoPO = tableInfoDAO.findById(tableId)
                .orElseThrow(() -> new DatiException("Table not found: " + tableId));

        if (!subjectPO.getDatasourceId().equals(tableInfoPO.getDataSourceId())) {
            throw new IllegalStateException("Table does not belong to the subject's datasource");
        }

        if (subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)) {
            throw new IllegalStateException("Table is already associated with this subject");
        }

        SubjectTablePO subjectTablePO = new SubjectTablePO();
        subjectTablePO.setSubjectId(subjectId);
        subjectTablePO.setTableId(tableId);
        subjectTableDAO.save(subjectTablePO);
    }

    @Transactional
    public void removeTableFromSubject(String subjectId, String tableId) {
        subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId)
                .orElseThrow(() -> new DatiException("Association not found"));
        subjectTableDAO.deleteBySubjectIdAndTableId(subjectId, tableId);
    }

    @Transactional(readOnly = true)
    public Page<Subject> getSubjectsByDatasource(String datasourceId, Pageable pageable) {
        Page<SubjectPO> pos = (datasourceId == null || datasourceId.isBlank())
                ? subjectDAO.findAll(pageable)
                : subjectDAO.findByDatasourceId(datasourceId, pageable);
        return pos.map(this::toSubject);
    }

    @Transactional(readOnly = true)
    public List<SubjectAvailableTableVO> getAvailableTables(String subjectId, String schema) {
        SubjectPO subjectPO = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException("Subject not found: " + subjectId));

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
    public List<com.dati.datasource.domain.model.TableInfo> getTablesBySubjectId(String subjectId) {
        if (!subjectDAO.existsById(subjectId)) {
            throw new DatiException("Subject not found: " + subjectId);
        }

        List<SubjectTablePO> subjectTables = subjectTableDAO.findBySubjectId(subjectId);
        List<String> tableIds = subjectTables.stream()
                .map(SubjectTablePO::getTableId)
                .collect(Collectors.toList());

        return tableInfoDAO.findAllById(tableIds).stream()
                .map(tableInfo -> {
                    com.dati.datasource.domain.model.TableInfo ti = new com.dati.datasource.domain.model.TableInfo();
                    ti.setId(tableInfo.getId());
                    ti.setName(tableInfo.getName());
                    ti.setDescription(tableInfo.getDescription());
                    ti.setDatasourceId(tableInfo.getDataSourceId());
                    ti.setSchema(tableInfo.getSchema());
                    ti.setAliases(tableInfo.getAliases());
                    ti.setCreatedAt(tableInfo.getCreatedAt());
                    ti.setUpdatedAt(tableInfo.getUpdatedAt());
                    return ti;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Subject getSubjectById(String id) {
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException("Subject not found: " + id));
        return toSubject(subjectPO);
    }

    private Subject toSubject(SubjectPO po) {
        return Subject.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .datasourceId(po.getDatasourceId())
                .createdAt(po.getCreatedAt() != null ? LocalDateTime.ofInstant(po.getCreatedAt(), ZoneOffset.UTC) : null)
                .updatedAt(po.getUpdatedAt() != null ? LocalDateTime.ofInstant(po.getUpdatedAt(), ZoneOffset.UTC) : null)
                .build();
    }
}
