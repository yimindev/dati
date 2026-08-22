package com.dati.semantic.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserGroupService;
import com.dati.base.RequestContext;
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
import com.dati.permission.domain.service.PermissionService;
import com.dati.permission.domain.model.Permission;
import com.dati.permission.domain.model.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dati.datasource.repository.dao.DataSourceDAO;
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
    private final DataSourceDAO dataSourceDAO;
    private final SemanticIndexService semanticIndexService;
    private final PermissionService permissionService;
    private final UserGroupService userGroupService;

    public SubjectService(SubjectDAO subjectDAO, SubjectTableDAO subjectTableDAO,
                          TableInfoDAO tableInfoDAO, DataSourceDAO dataSourceDAO,
                          SemanticIndexService semanticIndexService,
                          PermissionService permissionService, UserGroupService userGroupService) {
        this.subjectDAO = subjectDAO;
        this.subjectTableDAO = subjectTableDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.dataSourceDAO = dataSourceDAO;
        this.semanticIndexService = semanticIndexService;
        this.permissionService = permissionService;
        this.userGroupService = userGroupService;
    }

    @Transactional
    public Subject createSubject(Subject subject) {
        if (subject.getDatasourceId() != null && !dataSourceDAO.existsById(subject.getDatasourceId())) {
            throw new DatiException(ErrorCode.DS_NOT_FOUND, subject.getDatasourceId());
        }
        SubjectPO subjectPO = SubjectMapper.toPO(subject);
        subjectDAO.save(subjectPO);

        String id = subjectPO.getId();
        String name = subject.getName();
        String description = subject.getDescription();
        List<String> aliases = subject.getAliases();

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
    public Subject updateSubject(String id, Subject subject) {
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, id));
        permissionService.requireCurrentUser(ResourceType.SUBJECT, id, Permission.EDIT, subjectPO.getCreatedBy());

        if (subject.getName() != null) {
            subjectPO.setName(subject.getName());
        }
        if (subject.getDescription() != null) {
            subjectPO.setDescription(subject.getDescription());
        }
        if (subject.getAliases() != null) {
            subjectPO.setAliases(subject.getAliases());
        }
        if (subject.getUpdatedBy() != null) {
            subjectPO.setUpdatedBy(subject.getUpdatedBy());
        }
        subjectDAO.save(subjectPO);

        List<String> keywords = new ArrayList<>();
        keywords.add(subjectPO.getName());
        List<String> aliases = subjectPO.getAliases();
        if (aliases != null) {
            keywords.addAll(aliases);
        }

        SemanticSearchDocument doc = SemanticSearchDocument.builder()
                .id("subject:" + id)
                .type(SemanticEntityType.SUBJECT)
                .keywords(keywords.stream().distinct().toList())
                .description(subjectPO.getDescription())
                .entity(EntityReference.builder().subjectId(id).build())
                .build();
        semanticIndexService.save(doc);

        return SubjectMapper.toSubject(subjectPO);
    }

    @Transactional
    public void deleteSubject(String id) {
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, id));
        permissionService.requireCurrentUser(ResourceType.SUBJECT, id, Permission.EDIT, subjectPO.getCreatedBy());
        subjectDAO.deleteById(id);
        semanticIndexService.deleteByEntity_SubjectId(id);
    }

    @Transactional
    public void addTableToSubject(String subjectId, String tableId) {
        SubjectPO subjectPO = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, subjectId));
        permissionService.requireCurrentUser(ResourceType.SUBJECT, subjectId, Permission.EDIT, subjectPO.getCreatedBy());

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
        SubjectPO subjectPO = subjectDAO.findById(subjectId)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, subjectId));
        permissionService.requireCurrentUser(ResourceType.SUBJECT, subjectId, Permission.EDIT, subjectPO.getCreatedBy());
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
        User user = RequestContext.getUser();
        Page<SubjectPO> pos;
        if (permissionService.isAdmin(user.getName())) {
            pos = StringUtils.isEmpty(keyword)
                    ? subjectDAO.findAll(pageable)
                    : subjectDAO.findByKeyword(keyword, pageable);
        } else {
            var groupIds = userGroupService.groupIdsOf(user.getId());
            pos = StringUtils.isEmpty(keyword)
                    ? subjectDAO.findAllAccessible(user.getId(), groupIds, pageable)
                    : subjectDAO.findByKeywordAndAccessible(keyword, user.getId(), groupIds, pageable);
        }
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
        SubjectPO subjectPO = subjectDAO.findById(id)
                .orElseThrow(() -> new DatiException(ErrorCode.SM_SUBJECT_NOT_FOUND, id));
        permissionService.requireCurrentUser(ResourceType.SUBJECT, id, Permission.VIEW, subjectPO.getCreatedBy());
        return SubjectMapper.toSubject(subjectPO);
    }

}
