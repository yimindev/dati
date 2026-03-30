package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.model.SubjectDetailVO;
import com.dati.semantic.domain.model.SubjectTable;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        SubjectPO subjectPO = new SubjectPO();
        subjectPO.setId(id);
        subjectPO.setName(name);
        subjectPO.setDescription(description);
        subjectPO.setDatasourceId(datasourceId);
        subjectPO.setCreatedAt(now.toInstant(ZoneOffset.UTC));
        subjectPO.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
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
    public Subject updateSubject(String id, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subject name cannot be null or empty");
        }
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException("Subject not found: " + id));

        subjectPO.setName(name);
        subjectPO.setDescription(description);
        subjectPO.setUpdatedAt(Instant.now());
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

        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        SubjectTablePO subjectTablePO = new SubjectTablePO();
        subjectTablePO.setId(id);
        subjectTablePO.setSubjectId(subjectId);
        subjectTablePO.setTableId(tableId);
        subjectTablePO.setCreatedAt(now.toInstant(ZoneOffset.UTC));
        subjectTableDAO.save(subjectTablePO);

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("subject_table:" + subjectId + ":" + tableId)
                .type(SemanticEntityType.SUBJECT)
                .keywords(List.of(tableInfoPO.getName(), tableInfoPO.getDisplayName()))
                .entity(EntityReference.builder()
                        .subjectId(subjectId)
                        .tableId(tableId)
                        .tableName(tableInfoPO.getName())
                        .build())
                .build();
        semanticIndexService.save(doc);
    }

    @Transactional
    public void removeTableFromSubject(String subjectId, String tableId) {
        SubjectTablePO subjectTablePO = subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId)
                .orElseThrow(() -> new DatiException("Association not found"));
        subjectTableDAO.deleteBySubjectIdAndTableId(subjectId, tableId);
        semanticIndexService.deleteById("subject_table:" + subjectId + ":" + tableId);
    }

    @Transactional(readOnly = true)
    public SubjectDetailVO getSubjectWithTables(String id) {
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException("Subject not found: " + id));

        List<SubjectTablePO> subjectTables = subjectTableDAO.findBySubjectId(id);

        List<String> tableIds = subjectTables.stream()
                .map(SubjectTablePO::getTableId)
                .collect(Collectors.toList());

        Map<String, TableInfoPO> tableInfoMap = tableInfoDAO.findAllById(tableIds).stream()
                .collect(Collectors.toMap(TableInfoPO::getId, Function.identity()));

        List<SubjectTable> tables = subjectTables.stream()
                .map(st -> {
                    TableInfoPO tableInfo = tableInfoMap.get(st.getTableId());
                    return SubjectTable.builder()
                            .id(st.getId())
                            .subjectId(st.getSubjectId())
                            .tableId(st.getTableId())
                            .tableName(tableInfo != null ? tableInfo.getName() : null)
                            .displayName(tableInfo != null ? tableInfo.getDisplayName() : null)
                            .createdAt(tableInfo != null ? LocalDateTime.ofInstant(st.getCreatedAt(), ZoneOffset.UTC) : null)
                            .build();
                })
                .collect(Collectors.toList());

        return SubjectDetailVO.builder()
                .subject(toSubject(subjectPO))
                .tables(tables)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByDatasource(String datasourceId) {
        return subjectDAO.findByDatasourceId(datasourceId).stream()
                .map(this::toSubject)
                .collect(Collectors.toList());
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
