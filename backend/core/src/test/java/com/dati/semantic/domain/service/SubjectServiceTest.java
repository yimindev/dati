package com.dati.semantic.domain.service;

import com.dati.TestFixtures;
import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.UserGroupService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.model.Subject;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import com.dati.permission.domain.model.PrincipalType;
import com.dati.permission.domain.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dati.datasource.repository.dao.DataSourceDAO;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectService unit tests")
class SubjectServiceTest {

    @Mock
    private SubjectDAO subjectDAO;

    @Mock
    private SubjectTableDAO subjectTableDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private DataSourceDAO dataSourceDAO;

    @Mock
    private SemanticIndexService semanticIndexService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserGroupService userGroupService;

    @InjectMocks
    private SubjectService subjectService;

    private SubjectPO sampleSubjectPO;
    private TableInfoPO sampleTableInfoPO;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(TestFixtures.TEST_USER_ID);
        user.setName(TestFixtures.TEST_USER_ID);
        RequestContext.setUser(user);
        lenient().when(permissionService.isAdmin(anyString())).thenReturn(false);
        lenient().when(userGroupService.groupIdsOf(TestFixtures.TEST_USER_ID))
                .thenReturn(Set.of(PrincipalType.ALL_USERS));
        lenient().doAnswer(inv -> {
            String ownerId = inv.getArgument(3);
            if (!TestFixtures.TEST_USER_ID.equals(ownerId)) {
                throw new DatiException(ErrorCode.PERMISSION_DENIED);
            }
            return null;
        }).when(permissionService).requireCurrentUser(any(), anyString(), any(), anyString());
        lenient().when(dataSourceDAO.existsById(anyString())).thenReturn(true);
        sampleSubjectPO = new SubjectPO();
        sampleSubjectPO.setId("subject-001");
        sampleSubjectPO.setName("Test Subject");
        sampleSubjectPO.setDescription("Test Description");
        sampleSubjectPO.setCreatedBy(TestFixtures.TEST_USER_ID);
        sampleSubjectPO.setDatasourceId("datasource-001");
        sampleSubjectPO.setCreatedAt(Instant.now());
        sampleSubjectPO.setUpdatedAt(Instant.now());

        sampleTableInfoPO = new TableInfoPO();
        sampleTableInfoPO.setId("table-001");
        sampleTableInfoPO.setName("test_table");
        sampleTableInfoPO.setDataSourceId("datasource-001");
    }

    @AfterEach
    void tearDown() {
        RequestContext.getContext().clear();
    }

    @Test
    @DisplayName("createSubject - nonexistent datasource throws DS_NOT_FOUND")
    void createSubject_nonexistentDatasource_throwsException() {
        org.mockito.Mockito.doThrow(new DatiException(ErrorCode.DS_NOT_FOUND))
                .when(permissionService).requireDataSource("bad-ds-id", com.dati.permission.domain.model.Permission.VIEW);

        Subject model = new Subject();
        model.setName("Sub");
        model.setDatasourceId("bad-ds-id");

        assertThatThrownBy(() -> subjectService.createSubject(model))
                .isInstanceOf(DatiException.class)
                .satisfies(e -> assertThat(((DatiException) e).getCode()).isEqualTo(ErrorCode.DS_NOT_FOUND));
    }

    @Test
    @DisplayName("createSubject - saves SubjectPO and indexes ES document")
    void createSubject_shouldSaveAndIndex() {
        String name = "New Subject";
        String description = "New Description";
        String datasourceId = "datasource-001";

        when(subjectDAO.save(any(SubjectPO.class))).thenAnswer(invocation -> {
            SubjectPO po = invocation.getArgument(0);
            if (po.getId() == null) {
                po.setId("generated-subject-id");
            }
            return po;
        });

        Subject model = new Subject();
        model.setName(name);
        model.setDescription(description);
        model.setDatasourceId(datasourceId);
        model.setCreatedBy(TestFixtures.TEST_USER_ID);
        model.setUpdatedBy(TestFixtures.TEST_USER_ID);
        Subject result = subjectService.createSubject(model);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getDatasourceId()).isEqualTo(datasourceId);

        verify(subjectDAO).save(any(SubjectPO.class));

        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());

        SemanticSearchDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getType()).isEqualTo(SemanticEntityType.SUBJECT);
        assertThat(savedDoc.getKeywords()).contains(name);
        assertThat(savedDoc.getDescription()).isEqualTo(description);
        assertThat(savedDoc.getEntity().getSubjectId()).isNotNull();
    }

    @Test
    @DisplayName("updateSubject - updates SubjectPO and re-indexes ES document")
    void updateSubject_shouldUpdateAndReindex() {
        String id = "subject-001";
        String newName = "Updated Subject";
        String newDescription = "Updated Description";

        when(subjectDAO.findById(id)).thenReturn(Optional.of(sampleSubjectPO));
        when(subjectDAO.save(any(SubjectPO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject model = new Subject();
        model.setName(newName);
        model.setDescription(newDescription);
        model.setUpdatedBy(TestFixtures.TEST_USER_ID);
        Subject result = subjectService.updateSubject(id, model);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(newName);
        assertThat(result.getDescription()).isEqualTo(newDescription);

        verify(subjectDAO).save(any(SubjectPO.class));

        ArgumentCaptor<SemanticSearchDocument> docCaptor = ArgumentCaptor.forClass(SemanticSearchDocument.class);
        verify(semanticIndexService).save(docCaptor.capture());

        SemanticSearchDocument savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getId()).isEqualTo("subject:" + id);
        assertThat(savedDoc.getKeywords()).contains(newName);
    }

    @Test
    @DisplayName("updateSubject - throws when Subject not found")
    void updateSubject_shouldThrowWhenNotFound() {
        String id = "non-existent";
        when(subjectDAO.findById(id)).thenReturn(Optional.empty());

        Subject model = new Subject();
        model.setName("name");
        model.setDescription("desc");
        assertThatThrownBy(() -> subjectService.updateSubject(id, model))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Subject not found");
    }

    @Test
    @DisplayName("deleteSubject - deletes SubjectPO and ES document")
    void deleteSubject_shouldDeleteSubjectAndEsDocs() {
        String id = "subject-001";
        when(subjectDAO.findById(id)).thenReturn(Optional.of(sampleSubjectPO));

        subjectService.deleteSubject(id);

        verify(subjectDAO).deleteById(id);
        verify(semanticIndexService).deleteByEntity_SubjectId(id);
    }

    @Test
    @DisplayName("getSubjectById - denies without VIEW permission")
    void getSubjectById_requiresViewPermission() {
        SubjectPO other = new SubjectPO();
        other.setId("subject-002");
        other.setCreatedBy("other-user");
        when(subjectDAO.findById("subject-002")).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> subjectService.getSubjectById("subject-002"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("updateSubject - denies without EDIT permission")
    void updateSubject_requiresEditPermission() {
        SubjectPO other = new SubjectPO();
        other.setId("subject-002");
        other.setCreatedBy("other-user");
        when(subjectDAO.findById("subject-002")).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> subjectService.updateSubject("subject-002", new Subject()));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("deleteSubject - denies without EDIT permission")
    void deleteSubject_requiresEditPermission() {
        SubjectPO other = new SubjectPO();
        other.setId("subject-002");
        other.setCreatedBy("other-user");
        when(subjectDAO.findById("subject-002")).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> subjectService.deleteSubject("subject-002"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("addTableToSubject - denies without EDIT permission")
    void addTableToSubject_requiresEditPermission() {
        SubjectPO other = new SubjectPO();
        other.setId("subject-002");
        other.setCreatedBy("other-user");
        when(subjectDAO.findById("subject-002")).thenReturn(Optional.of(other));

        DatiException ex = assertThrows(DatiException.class,
                () -> subjectService.addTableToSubject("subject-002", "table-001"));
        assertThat(ex.getCode()).isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    @DisplayName("deleteSubject - throws when Subject not found")
    void deleteSubject_shouldThrowWhenNotFound() {
        String id = "non-existent";
        when(subjectDAO.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.deleteSubject(id))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Subject not found");
    }

    @Test
    @DisplayName("addTableToSubject - validates table belongs to same datasource")
    void addTableToSubject_shouldValidateDatasource() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        TableInfoPO otherDatasourceTable = new TableInfoPO();
        otherDatasourceTable.setId(tableId);
        otherDatasourceTable.setDataSourceId("other-datasource");

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.of(otherDatasourceTable));

        assertThatThrownBy(() -> subjectService.addTableToSubject(subjectId, tableId))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("does not belong to subject");

        verify(semanticIndexService, never()).save(any());
    }

    @Test
    @DisplayName("addTableToSubject - throws when relation already exists")
    void addTableToSubject_shouldThrowWhenAlreadyAssociated() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.of(sampleTableInfoPO));
        when(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)).thenReturn(true);

        assertThatThrownBy(() -> subjectService.addTableToSubject(subjectId, tableId))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("already associated");

        verify(semanticIndexService, never()).save(any());
    }

    @Test
    @DisplayName("addTableToSubject - saves relation on valid add")
    void addTableToSubject_shouldSaveAssociation() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(tableInfoDAO.findById(tableId)).thenReturn(Optional.of(sampleTableInfoPO));
        when(subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)).thenReturn(false);
        when(subjectTableDAO.save(any(SubjectTablePO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        subjectService.addTableToSubject(subjectId, tableId);

        verify(subjectTableDAO).save(any(SubjectTablePO.class));
        verify(semanticIndexService, never()).save(any());
    }

    @Test
    @DisplayName("removeTableFromSubject - deletes the relation")
    void removeTableFromSubject_shouldDeleteAssociation() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        SubjectTablePO subjectTablePO = new SubjectTablePO();
        subjectTablePO.setId("st-001");
        subjectTablePO.setSubjectId(subjectId);
        subjectTablePO.setTableId(tableId);

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId))
                .thenReturn(Optional.of(subjectTablePO));

        subjectService.removeTableFromSubject(subjectId, tableId);

        verify(subjectTableDAO).deleteBySubjectIdAndTableId(subjectId, tableId);
    }

    @Test
    @DisplayName("removeTableFromSubject - throws when relation not found")
    void removeTableFromSubject_shouldThrowWhenNotFound() {
        String subjectId = "subject-001";
        String tableId = "table-001";

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.removeTableFromSubject(subjectId, tableId))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Association between subject");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("getSubjects - null or empty keyword queries all")
    void getSubjects_withNullOrEmptyKeyword_shouldReturnAll(String keyword) {
        Pageable pageable = PageRequest.of(0, 10);

        Page<SubjectPO> subjectPOPage = new org.springframework.data.domain.PageImpl<>(List.of(sampleSubjectPO), pageable, 1);
        when(subjectDAO.findAllAccessible(TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(subjectPOPage);

        Page<Subject> result = subjectService.getSubjects(keyword, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Test Subject");
        verify(subjectDAO, never()).findByKeywordAndAccessible(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getSubjects - non-empty keyword matches by ID prefix or name")
    void getSubjects_withKeyword_shouldReturnMatchingSubjects() {
        String keyword = "Test";
        Pageable pageable = PageRequest.of(0, 10);

        Page<SubjectPO> subjectPOPage = new org.springframework.data.domain.PageImpl<>(List.of(sampleSubjectPO), pageable, 1);
        when(subjectDAO.findByKeywordAndAccessible(keyword, TestFixtures.TEST_USER_ID,
                Set.of(PrincipalType.ALL_USERS), pageable)).thenReturn(subjectPOPage);

        Page<Subject> result = subjectService.getSubjects(keyword, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Test Subject");
    }

    @Test
    @DisplayName("getTablesBySubjectId (paged) - empty keyword calls findTablesBySubjectId")
    void getTablesBySubjectId_paginated_withoutKeyword_shouldReturnAll() {
        String subjectId = "subject-001";
        Pageable pageable = PageRequest.of(0, 10);

        TableInfoPO ti1 = new TableInfoPO();
        ti1.setId("table-001");
        ti1.setName("orders");
        ti1.setDataSourceId("datasource-001");
        ti1.setUpdatedAt(Instant.now());

        TableInfoPO ti2 = new TableInfoPO();
        ti2.setId("table-002");
        ti2.setName("users");
        ti2.setDataSourceId("datasource-001");
        ti2.setUpdatedAt(Instant.now().minusSeconds(100));

        Page<TableInfoPO> poPage = new org.springframework.data.domain.PageImpl<>(List.of(ti1, ti2), pageable, 2);

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(subjectTableDAO.findTablesBySubjectId(subjectId, pageable)).thenReturn(poPage);

        Page<TableInfo> result = subjectService.getTablesBySubjectId(subjectId, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("orders");
        assertThat(result.getContent().get(1).getName()).isEqualTo("users");
    }

    @Test
    @DisplayName("getTablesBySubjectId (paged) - non-empty keyword filters by name")
    void getTablesBySubjectId_paginated_withKeyword_shouldFilterByName() {
        String subjectId = "subject-001";
        String keyword = "user";
        Pageable pageable = PageRequest.of(0, 10);

        TableInfoPO ti1 = new TableInfoPO();
        ti1.setId("table-002");
        ti1.setName("users");
        ti1.setDataSourceId("datasource-001");
        ti1.setUpdatedAt(Instant.now());

        Page<TableInfoPO> poPage = new org.springframework.data.domain.PageImpl<>(List.of(ti1), pageable, 1);

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.of(sampleSubjectPO));
        when(subjectTableDAO.findTablesBySubjectIdAndNameContaining(subjectId, keyword, pageable)).thenReturn(poPage);

        Page<TableInfo> result = subjectService.getTablesBySubjectId(subjectId, keyword, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("users");
    }

    @Test
    @DisplayName("getTablesBySubjectId (paged) - throws when Subject not found")
    void getTablesBySubjectId_paginated_shouldThrowWhenSubjectNotFound() {
        String subjectId = "non-existent";
        Pageable pageable = PageRequest.of(0, 10);

        when(subjectDAO.findById(subjectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.getTablesBySubjectId(subjectId, null, pageable))
                .isInstanceOf(DatiException.class)
                .hasMessageContaining("Subject not found");
    }
}
