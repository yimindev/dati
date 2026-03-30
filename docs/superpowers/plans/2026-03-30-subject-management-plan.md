# 主题管理模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现主题管理模块，支持主题创建、表关联、主题私有术语管理，所有语义实体同步到 ES 索引

**Architecture:** 遵循现有 DDD 架构（domain/repository/server 三层），SQL 存储主体 + ES 索引搜索文档，TermRelation 支持 TABLE/FIELD 两种关联类型

**Tech Stack:** Java, Spring Boot, JPA, Spring Data Elasticsearch, JUnit 5, Mockito

---

## 文件结构规划

### Phase 1: 主题基础管理

```
backend/src/main/java/com/dati/
├── semantic/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Subject.java                    # 主题聚合根
│   │   │   └── SubjectTable.java               # 主题-表关联
│   │   └── service/
│   │       ├── SubjectService.java              # 主题业务逻辑
│   │       └── TermService.java                 # 术语业务逻辑
│   ├── repository/
│   │   ├── dao/
│   │   │   ├── SubjectDAO.java                 # JPA Repository
│   │   │   ├── SubjectTableDAO.java
│   │   │   ├── TermDAO.java
│   │   │   └── TermRelationDAO.java
│   │   ├── po/
│   │   │   ├── SubjectPO.java                  # 主题持久化对象
│   │   │   ├── SubjectTablePO.java
│   │   │   ├── TermPO.java
│   │   │   └── TermRelationPO.java
│   │   └── mapper/
│   │       ├── SubjectMapper.java
│   │       ├── TermMapper.java
│   │       └── TermRelationMapper.java
│   └── server/
│       ├── controller/
│       │   ├── SubjectController.java           # 主题 REST API
│       │   └── TermController.java              # 术语 REST API
│       ├── pojo/
│       │   ├── request/
│       │   │   ├── CreateSubjectRequest.java
│       │   │   ├── UpdateSubjectRequest.java
│       │   │   ├── AddTableToSubjectRequest.java
│       │   │   ├── CreateTermRequest.java
│       │   │   └── LinkTermRelationRequest.java
│       │   └── vo/
│       │       ├── SubjectVO.java
│       │       ├── SubjectDetailVO.java
│       │       └── TermVO.java
│       └── assembler/
│           ├── SubjectAssembler.java
│           └── TermAssembler.java
```

---

## Task 1: Subject PO + Mapper + DAO

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/repository/po/SubjectPO.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/po/SubjectTablePO.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/mapper/SubjectMapper.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/dao/SubjectDAO.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/dao/SubjectTableDAO.java`
- Test: `backend/src/test/java/com/dati/semantic/repository/SubjectRepositoryTest.java`

- [ ] **Step 1: Write failing test for SubjectDAO**

```java
// backend/src/test/java/com/dati/semantic/repository/SubjectRepositoryTest.java
package com.dati.semantic.repository;

import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.po.SubjectPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SubjectRepositoryTest {
    @Autowired
    private SubjectDAO subjectDAO;

    @Test
    void save_shouldPersistSubject() {
        SubjectPO subject = SubjectPO.builder()
            .name("测试主题")
            .description("描述")
            .datasourceId("ds-001")
            .build();
        
        SubjectPO saved = subjectDAO.save(subject);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("测试主题");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=SubjectRepositoryTest test -q`
Expected: FAIL - compilation error (classes don't exist)

- [ ] **Step 3: Create SubjectPO**

```java
// backend/src/main/java/com/dati/semantic/repository/po/SubjectPO.java
package com.dati.semantic.repository.po;

import lombok.*;
import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subject")
public class SubjectPO {
    @Id
    private String id;
    
    @Column(name = "name", nullable = false, length = 128)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "datasource_id", nullable = false, length = 64)
    private String datasourceId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create SubjectTablePO**

```java
// backend/src/main/java/com/dati/semantic/repository/po/SubjectTablePO.java
package com.dati.semantic.repository.po;

import lombok.*;
import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subject_table")
public class SubjectTablePO {
    @Id
    private String id;
    
    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;
    
    @Column(name = "table_id", nullable = false, length = 64)
    private String tableId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Create SubjectMapper**

```java
// backend/src/main/java/com/dati/semantic/repository/mapper/SubjectMapper.java
package com.dati.semantic.repository.mapper;

import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;

public class SubjectMapper {
    public static SubjectPO toPO(String name, String description, String datasourceId) {
        return SubjectPO.builder()
            .name(name)
            .description(description)
            .datasourceId(datasourceId)
            .build();
    }
    
    public static SubjectTablePO toSubjectTablePO(String subjectId, String tableId) {
        return SubjectTablePO.builder()
            .subjectId(subjectId)
            .tableId(tableId)
            .build();
    }
}
```

- [ ] **Step 6: Create SubjectDAO**

```java
// backend/src/main/java/com/dati/semantic/repository/dao/SubjectDAO.java
package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectDAO extends JpaRepository<SubjectPO, String> {
    List<SubjectPO> findByDatasourceId(String datasourceId);
}
```

- [ ] **Step 7: Create SubjectTableDAO**

```java
// backend/src/main/java/com/dati/semantic/repository/dao/SubjectTableDAO.java
package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.SubjectTablePO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectTableDAO extends JpaRepository<SubjectTablePO, String> {
    List<SubjectTablePO> findBySubjectId(String subjectId);
    Optional<SubjectTablePO> findBySubjectIdAndTableId(String subjectId, String tableId);
    void deleteBySubjectIdAndTableId(String subjectId, String tableId);
    boolean existsBySubjectIdAndTableId(String subjectId, String tableId);
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `cd backend && mvn -Dtest=SubjectRepositoryTest test -q`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/repository/po/SubjectPO.java \
  backend/src/main/java/com/dati/semantic/repository/po/SubjectTablePO.java \
  backend/src/main/java/com/dati/semantic/repository/mapper/SubjectMapper.java \
  backend/src/main/java/com/dati/semantic/repository/dao/SubjectDAO.java \
  backend/src/main/java/com/dati/semantic/repository/dao/SubjectTableDAO.java \
  backend/src/test/java/com/dati/semantic/repository/SubjectRepositoryTest.java
git commit -m "feat(semantic): add Subject and SubjectTable PO/DAO/Mapper"
```

---

## Task 2: Term PO + Mapper + DAO

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/repository/po/TermPO.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/po/TermRelationPO.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/mapper/TermMapper.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/mapper/TermRelationMapper.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/dao/TermDAO.java`
- Create: `backend/src/main/java/com/dati/semantic/repository/dao/TermRelationDAO.java`
- Test: `backend/src/test/java/com/dati/semantic/repository/TermRepositoryTest.java`

- [ ] **Step 1: Write failing test for TermDAO**

```java
// backend/src/test/java/com/dati/semantic/repository/TermRepositoryTest.java
package com.dati.semantic.repository;

import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.po.TermPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TermRepositoryTest {
    @Autowired
    private TermDAO termDAO;

    @Test
    void save_shouldPersistTerm() {
        TermPO term = TermPO.builder()
            .name("订单")
            .description("订单信息")
            .subjectId("subject-001")
            .build();
        
        TermPO saved = termDAO.save(term);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("订单");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=TermRepositoryTest test -q`
Expected: FAIL - compilation error (classes don't exist)

- [ ] **Step 3: Create TermPO**

```java
// backend/src/main/java/com/dati/semantic/repository/po/TermPO.java
package com.dati.semantic.repository.po;

import lombok.*;
import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "term")
public class TermPO {
    @Id
    private String id;
    
    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;
    
    @Column(name = "name", nullable = false, length = 128)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create TermRelationPO**

```java
// backend/src/main/java/com/dati/semantic/repository/po/TermRelationPO.java
package com.dati.semantic.repository.po;

import lombok.*;
import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "term_relation")
public class TermRelationPO {
    @Id
    private String id;
    
    @Column(name = "term_id", nullable = false, length = 64)
    private String termId;
    
    @Column(name = "entity_type", nullable = false, length = 16)
    private String entityType;  // TABLE or FIELD
    
    @Column(name = "table_id", nullable = false, length = 64)
    private String tableId;
    
    @Column(name = "field_name", length = 128)
    private String fieldName;
}
```

- [ ] **Step 5: Create TermMapper and TermRelationMapper**

```java
// backend/src/main/java/com/dati/semantic/repository/mapper/TermMapper.java
package com.dati.semantic.repository.mapper;

import com.dati.semantic.repository.po.TermPO;

public class TermMapper {
    public static TermPO toPO(String subjectId, String name, String description) {
        return TermPO.builder()
            .subjectId(subjectId)
            .name(name)
            .description(description)
            .build();
    }
}
```

```java
// backend/src/main/java/com/dati/semantic/repository/mapper/TermRelationMapper.java
package com.dati.semantic.repository.mapper;

import com.dati.semantic.repository.po.TermRelationPO;

public class TermRelationMapper {
    public static TermRelationPO toPO(String termId, String entityType, String tableId, String fieldName) {
        return TermRelationPO.builder()
            .termId(termId)
            .entityType(entityType)
            .tableId(tableId)
            .fieldName(fieldName)
            .build();
    }
}
```

- [ ] **Step 6: Create TermDAO**

```java
// backend/src/main/java/com/dati/semantic/repository/dao/TermDAO.java
package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.TermPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TermDAO extends JpaRepository<TermPO, String> {
    List<TermPO> findBySubjectId(String subjectId);
}
```

- [ ] **Step 7: Create TermRelationDAO**

```java
// backend/src/main/java/com/dati/semantic/repository/dao/TermRelationDAO.java
package com.dati.semantic.repository.dao;

import com.dati.semantic.repository.po.TermRelationPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TermRelationDAO extends JpaRepository<TermRelationPO, String> {
    List<TermRelationPO> findByTermId(String termId);
    Optional<TermRelationPO> findByTermIdAndTableIdAndFieldName(String termId, String tableId, String fieldName);
    void deleteByTermId(String termId);
    void deleteByTermIdAndTableId(String termId, String tableId);
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `cd backend && mvn -Dtest=TermRepositoryTest test -q`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/repository/po/TermPO.java \
  backend/src/main/java/com/dati/semantic/repository/po/TermRelationPO.java \
  backend/src/main/java/com/dati/semantic/repository/mapper/TermMapper.java \
  backend/src/main/java/com/dati/semantic/repository/mapper/TermRelationMapper.java \
  backend/src/main/java/com/dati/semantic/repository/dao/TermDAO.java \
  backend/src/main/java/com/dati/semantic/repository/dao/TermRelationDAO.java \
  backend/src/test/java/com/dati/semantic/repository/TermRepositoryTest.java
git commit -m "feat(semantic): add Term and TermRelation PO/DAO/Mapper"
```

---

## Task 3: SubjectService + ES 索引同步

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/domain/model/Subject.java`
- Create: `backend/src/main/java/com/dati/semantic/domain/model/SubjectTable.java`
- Create: `backend/src/main/java/com/dati/semantic/domain/service/SubjectService.java`
- Test: `backend/src/test/java/com/dati/semantic/domain/service/SubjectServiceTest.java`

- [ ] **Step 1: Write failing test for SubjectService**

```java
// backend/src/test/java/com/dati/semantic/domain/service/SubjectServiceTest.java
package com.dati.semantic.domain.service;

import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.repository.dao.SubjectDAO;
import com.dati.semantic.repository.dao.SubjectTableDAO;
import com.dati.semantic.repository.dao.TableInfoDAO;
import com.dati.semantic.repository.po.SubjectPO;
import com.dati.semantic.repository.po.SubjectTablePO;
import com.dati.semantic.repository.po.TableInfoPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {
    @Mock private SubjectDAO subjectDAO;
    @Mock private SubjectTableDAO subjectTableDAO;
    @Mock private TableInfoDAO tableInfoDAO;
    @Mock private SemanticIndexService indexService;
    @InjectMocks private SubjectService subjectService;

    private SubjectPO testSubject;

    @BeforeEach
    void setUp() {
        testSubject = SubjectPO.builder()
            .id("subject-001")
            .name("测试主题")
            .description("描述")
            .datasourceId("ds-001")
            .build();
    }

    @Test
    void createSubject_shouldSaveAndIndex() {
        when(subjectDAO.save(any())).thenReturn(testSubject);
        
        Subject result = subjectService.createSubject("测试主题", "描述", "ds-001");
        
        assertThat(result.getName()).isEqualTo("测试主题");
        verify(indexService).save(any());
    }

    @Test
    void addTableToSubject_shouldVerifyTableBelongsToDatasource() {
        when(subjectDAO.findById("subject-001")).thenReturn(Optional.of(testSubject));
        when(tableInfoDAO.findById("table-001")).thenReturn(Optional.of(
            TableInfoPO.builder().id("table-001").dataSourceId("ds-001").name("orders").build()
        ));
        
        subjectService.addTableToSubject("subject-001", "table-001");
        
        verify(subjectTableDAO).save(any());
        verify(indexService).save(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=SubjectServiceTest test -q`
Expected: FAIL - classes don't exist

- [ ] **Step 3: Create Subject domain model**

```java
// backend/src/main/java/com/dati/semantic/domain/model/Subject.java
package com.dati.semantic.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Subject {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create SubjectTable domain model**

```java
// backend/src/main/java/com/dati/semantic/domain/model/SubjectTable.java
package com.dati.semantic.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectTable {
    private String id;
    private String subjectId;
    private String tableId;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Create SubjectService with ES indexing**

```java
// backend/src/main/java/com/dati/semantic/domain/service/SubjectService.java
package com.dati.semantic.domain.service;

import com.dati.semantic.domain.model.*;
import com.dati.semantic.repository.*;
import com.dati.semantic.repository.dao.*;
import com.dati.semantic.repository.po.*;
import com.dati.semantic.repository.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectDAO subjectDAO;
    private final SubjectTableDAO subjectTableDAO;
    private final TableInfoDAO tableInfoDAO;
    private final SemanticIndexService indexService;

    @Transactional
    public Subject createSubject(String name, String description, String datasourceId) {
        SubjectPO po = SubjectMapper.toPO(name, description, datasourceId);
        po.setId("subject:" + UUID.randomUUID().toString());
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        SubjectPO saved = subjectDAO.save(po);
        
        indexService.save(buildSubjectDocument(saved));
        return toSubject(saved);
    }

    @Transactional
    public Subject updateSubject(String id, String name, String description) {
        SubjectPO po = subjectDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        po.setName(name);
        po.setDescription(description);
        po.setUpdatedAt(LocalDateTime.now());
        SubjectPO saved = subjectDAO.save(po);
        
        indexService.save(buildSubjectDocument(saved));
        return toSubject(saved);
    }

    @Transactional
    public void deleteSubject(String id) {
        SubjectPO po = subjectDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        subjectDAO.delete(po);
        indexService.deleteByCondition(id);
    }

    @Transactional
    public SubjectTable addTableToSubject(String subjectId, String tableId) {
        SubjectPO subject = subjectDAO.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        
        TableInfoPO table = tableInfoDAO.findById(tableId)
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        
        if (!table.getDataSourceId().equals(subject.getDatasourceId())) {
            throw new IllegalArgumentException("Table does not belong to subject's datasource");
        }
        
        if (subjectTableDAO.existsBySubjectIdAndTableId(subjectId, tableId)) {
            throw new IllegalArgumentException("Table already added to subject");
        }
        
        SubjectTablePO po = SubjectMapper.toSubjectTablePO(subjectId, tableId);
        po.setId(UUID.randomUUID().toString());
        po.setCreatedAt(LocalDateTime.now());
        SubjectTablePO saved = subjectTableDAO.save(po);
        
        indexService.save(buildSubjectTableDocument(subjectId, table));
        return toSubjectTable(saved);
    }

    @Transactional
    public void removeTableFromSubject(String subjectId, String tableId) {
        subjectTableDAO.findBySubjectIdAndTableId(subjectId, tableId)
            .orElseThrow(() -> new IllegalArgumentException("Subject-table relation not found"));
        subjectTableDAO.deleteBySubjectIdAndTableId(subjectId, tableId);
        indexService.delete("subject_table:" + subjectId + ":" + tableId);
    }

    public SubjectDetailVO getSubjectWithTables(String subjectId) {
        SubjectPO subject = subjectDAO.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        List<SubjectTablePO> tables = subjectTableDAO.findBySubjectId(subjectId);
        return SubjectDetailVO.builder()
            .subject(toSubject(subject))
            .tables(tables.stream().map(this::toSubjectTable).collect(Collectors.toList()))
            .build();
    }

    public List<Subject> getSubjectsByDatasource(String datasourceId) {
        return subjectDAO.findByDatasourceId(datasourceId).stream()
            .map(this::toSubject)
            .collect(Collectors.toList());
    }

    private SemanticSearchDocument buildSubjectDocument(SubjectPO po) {
        return SemanticSearchDocument.builder()
            .id(po.getId())
            .type(SemanticEntityType.SUBJECT)
            .keywords(Collections.singletonList(po.getName()))
            .description(po.getDescription())
            .entity(EntityReference.builder().subjectId(po.getId()).build())
            .createdTime(po.getCreatedAt())
            .updatedTime(po.getUpdatedAt())
            .build();
    }

    private SemanticSearchDocument buildSubjectTableDocument(String subjectId, TableInfoPO table) {
        return SemanticSearchDocument.builder()
            .id("subject_table:" + subjectId + ":" + table.getId())
            .type(SemanticEntityType.SUBJECT_TABLE)
            .keywords(Arrays.asList(table.getName(), table.getDisplayName()))
            .description(table.getDescription())
            .entity(EntityReference.builder()
                .subjectId(subjectId)
                .tableId(table.getId())
                .tableName(table.getName())
                .build())
            .createdTime(LocalDateTime.now())
            .updatedTime(LocalDateTime.now())
            .build();
    }

    private Subject toSubject(SubjectPO po) {
        return Subject.builder()
            .id(po.getId())
            .name(po.getName())
            .description(po.getDescription())
            .datasourceId(po.getDatasourceId())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private SubjectTable toSubjectTable(SubjectTablePO po) {
        return SubjectTable.builder()
            .id(po.getId())
            .subjectId(po.getSubjectId())
            .tableId(po.getTableId())
            .createdAt(po.getCreatedAt())
            .build();
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd backend && mvn -Dtest=SubjectServiceTest test -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/domain/model/Subject.java \
  backend/src/main/java/com/dati/semantic/domain/model/SubjectTable.java \
  backend/src/main/java/com/dati/semantic/domain/service/SubjectService.java \
  backend/src/test/java/com/dati/semantic/domain/service/SubjectServiceTest.java
git commit -m "feat(semantic): add SubjectService with ES indexing"
```

---

## Task 4: TermService + ES 索引同步

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/domain/model/Term.java`
- Create: `backend/src/main/java/com/dati/semantic/domain/model/TermRelation.java`
- Create: `backend/src/main/java/com/dati/semantic/domain/service/TermService.java`
- Create: `backend/src/main/java/com/dati/semantic/domain/model/SubjectDetailVO.java`
- Test: `backend/src/test/java/com/dati/semantic/domain/service/TermServiceTest.java`

- [ ] **Step 1: Write failing test for TermService**

```java
// backend/src/test/java/com/dati/semantic/domain/service/TermServiceTest.java
package com.dati.semantic.domain.service;

import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.repository.dao.*;
import com.dati.semantic.repository.po.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TermServiceTest {
    @Mock private TermDAO termDAO;
    @Mock private TermRelationDAO termRelationDAO;
    @Mock private SubjectTableDAO subjectTableDAO;
    @Mock private SemanticIndexService indexService;
    @InjectMocks private TermService termService;

    private TermPO testTerm;

    @BeforeEach
    void setUp() {
        testTerm = TermPO.builder()
            .id("term-001")
            .name("订单")
            .description("订单信息")
            .subjectId("subject-001")
            .build();
    }

    @Test
    void createTerm_shouldSaveAndIndex() {
        when(termDAO.save(any())).thenReturn(testTerm);
        
        Term result = termService.createTerm("subject-001", "订单", "订单信息");
        
        assertThat(result.getName()).isEqualTo("订单");
        verify(indexService).save(any());
    }

    @Test
    void linkEntity_shouldVerifyTableBelongsToSubject() {
        when(termDAO.findById("term-001")).thenReturn(Optional.of(testTerm));
        when(subjectTableDAO.existsBySubjectIdAndTableId("subject-001", "table-001")).thenReturn(true);
        when(termRelationDAO.save(any())).thenAnswer(i -> i.getArgument(0));
        
        TermRelation result = termService.linkEntity("term-001", "TABLE", "table-001", null);
        
        assertThat(result.getEntityType()).isEqualTo("TABLE");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=TermServiceTest test -q`
Expected: FAIL - classes don't exist

- [ ] **Step 3: Create Term domain model**

```java
// backend/src/main/java/com/dati/semantic/domain/model/Term.java
package com.dati.semantic.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Term {
    private String id;
    private String subjectId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create TermRelation domain model**

```java
// backend/src/main/java/com/dati/semantic/domain/model/TermRelation.java
package com.dati.semantic.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermRelation {
    private String id;
    private String termId;
    private String entityType;  // TABLE or FIELD
    private String tableId;
    private String fieldName;
}
```

- [ ] **Step 5: Create SubjectDetailVO**

```java
// backend/src/main/java/com/dati/semantic/domain/model/SubjectDetailVO.java
package com.dati.semantic.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDetailVO {
    private Subject subject;
    private java.util.List<SubjectTable> tables;
}
```

- [ ] **Step 6: Create TermService with ES indexing**

```java
// backend/src/main/java/com/dati/semantic/domain/service/TermService.java
package com.dati.semantic.domain.service;

import com.dati.semantic.domain.model.*;
import com.dati.semantic.repository.dao.*;
import com.dati.semantic.repository.po.*;
import com.dati.semantic.repository.mapper.TermMapper;
import com.dati.semantic.repository.mapper.TermRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermService {
    private final TermDAO termDAO;
    private final TermRelationDAO termRelationDAO;
    private final SubjectTableDAO subjectTableDAO;
    private final SemanticIndexService indexService;

    @Transactional
    public Term createTerm(String subjectId, String name, String description) {
        TermPO po = TermMapper.toPO(subjectId, name, description);
        po.setId("term:" + UUID.randomUUID().toString());
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        TermPO saved = termDAO.save(po);
        
        indexService.save(buildTermDocument(saved));
        return toTerm(saved);
    }

    @Transactional
    public Term updateTerm(String id, String name, String description) {
        TermPO po = termDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Term not found: " + id));
        po.setName(name);
        po.setDescription(description);
        po.setUpdatedAt(LocalDateTime.now());
        TermPO saved = termDAO.save(po);
        
        indexService.save(buildTermDocument(saved));
        return toTerm(saved);
    }

    @Transactional
    public void deleteTerm(String id) {
        TermPO po = termDAO.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Term not found: " + id));
        termRelationDAO.deleteByTermId(id);
        termDAO.delete(po);
        indexService.delete(id);
    }

    @Transactional
    public TermRelation linkEntity(String termId, String entityType, String tableId, String fieldName) {
        TermPO term = termDAO.findById(termId)
            .orElseThrow(() -> new IllegalArgumentException("Term not found: " + termId));
        
        if (!subjectTableDAO.existsBySubjectIdAndTableId(term.getSubjectId(), tableId)) {
            throw new IllegalArgumentException("Table not associated with subject");
        }
        
        if ("FIELD".equals(entityType) && fieldName == null) {
            throw new IllegalArgumentException("Field name required for FIELD entity type");
        }
        
        TermRelationPO po = TermRelationMapper.toPO(termId, entityType, tableId, fieldName);
        po.setId(UUID.randomUUID().toString());
        TermRelationPO saved = termRelationDAO.save(po);
        return toTermRelation(saved);
    }

    @Transactional
    public void unlinkEntity(String termId, String tableId, String fieldName) {
        Optional<TermRelationPO> opt = "FIELD".equals(fieldName) != null
            ? termRelationDAO.findByTermIdAndTableIdAndFieldName(termId, tableId, fieldName)
            : termRelationDAO.findByTermIdAndTableId(termId, tableId);
        opt.orElseThrow(() -> new IllegalArgumentException("Term relation not found"));
        termRelationDAO.delete(opt.get());
    }

    public List<Term> getTermsBySubject(String subjectId) {
        return termDAO.findBySubjectId(subjectId).stream()
            .map(this::toTerm)
            .collect(Collectors.toList());
    }

    public List<TermRelation> getTermRelations(String termId) {
        return termRelationDAO.findByTermId(termId).stream()
            .map(this::toTermRelation)
            .collect(Collectors.toList());
    }

    private SemanticSearchDocument buildTermDocument(TermPO po) {
        return SemanticSearchDocument.builder()
            .id(po.getId())
            .type(SemanticEntityType.TERM)
            .keywords(Collections.singletonList(po.getName()))
            .description(po.getDescription())
            .entity(EntityReference.builder().subjectId(po.getSubjectId()).build())
            .createdTime(po.getCreatedAt())
            .updatedTime(po.getUpdatedAt())
            .build();
    }

    private Term toTerm(TermPO po) {
        return Term.builder()
            .id(po.getId())
            .subjectId(po.getSubjectId())
            .name(po.getName())
            .description(po.getDescription())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private TermRelation toTermRelation(TermRelationPO po) {
        return TermRelation.builder()
            .id(po.getId())
            .termId(po.getTermId())
            .entityType(po.getEntityType())
            .tableId(po.getTableId())
            .fieldName(po.getFieldName())
            .build();
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `cd backend && mvn -Dtest=TermServiceTest test -q`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/domain/model/Term.java \
  backend/src/main/java/com/dati/semantic/domain/model/TermRelation.java \
  backend/src/main/java/com/dati/semantic/domain/model/SubjectDetailVO.java \
  backend/src/main/java/com/dati/semantic/domain/service/TermService.java \
  backend/src/test/java/com/dati/semantic/domain/service/TermServiceTest.java
git commit -m "feat(semantic): add TermService with ES indexing"
```

---

## Task 5: SubjectController REST API

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/server/controller/SubjectController.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/request/CreateSubjectRequest.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/request/UpdateSubjectRequest.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/request/AddTableToSubjectRequest.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/vo/SubjectVO.java`
- Create: `backend/src/main/java/com/dati/semantic/server/assembler/SubjectAssembler.java`
- Test: `backend/src/test/java/com/dati/semantic/server/controller/SubjectControllerTest.java`

- [ ] **Step 1: Write failing test for SubjectController**

```java
// backend/src/test/java/com/dati/semantic/server/controller/SubjectControllerTest.java
package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.Subject;
import com.dati.semantic.domain.model.SubjectTable;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.server.assembler.SubjectAssembler;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import com.dati.semantic.server.pojo.vo.SubjectDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubjectController.class)
class SubjectControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private SubjectService subjectService;
    @MockBean
    private SubjectAssembler subjectAssembler;

    @Test
    void createSubject_shouldReturn201() throws Exception {
        Subject subject = Subject.builder()
            .id("subject-001")
            .name("测试主题")
            .description("描述")
            .datasourceId("ds-001")
            .build();
        when(subjectService.createSubject(any(), any(), any())).thenReturn(subject);
        when(subjectAssembler.toVO(any())).thenReturn(SubjectVO.builder()
            .id("subject-001")
            .name("测试主题")
            .description("描述")
            .datasourceId("ds-001")
            .build());

        mockMvc.perform(post("/v1/subjects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试主题\",\"description\":\"描述\",\"datasourceId\":\"ds-001\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("subject-001"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=SubjectControllerTest test -q`
Expected: FAIL - classes don't exist

- [ ] **Step 3: Create Request/VO classes**

```java
// backend/src/main/java/com/dati/semantic/server/pojo/request/CreateSubjectRequest.java
package com.dati.semantic.server.pojo.request;

import lombok.Data;

@Data
public class CreateSubjectRequest {
    private String name;
    private String description;
    private String datasourceId;
}
```

```java
// backend/src/main/java/com/dati/semantic/server/pojo/request/UpdateSubjectRequest.java
package com.dati.semantic.server.pojo.request;

import lombok.Data;

@Data
public class UpdateSubjectRequest {
    private String name;
    private String description;
}
```

```java
// backend/src/main/java/com/dati/semantic/server/pojo/request/AddTableToSubjectRequest.java
package com.dati.semantic.server.pojo.request;

import lombok.Data;

@Data
public class AddTableToSubjectRequest {
    private String tableId;
}
```

```java
// backend/src/main/java/com/dati/semantic/server/pojo/vo/SubjectVO.java
package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubjectVO {
    private String id;
    private String name;
    private String description;
    private String datasourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create SubjectAssembler**

```java
// backend/src/main/java/com/dati/semantic/server/assembler/SubjectAssembler.java
package com.dati.semantic.server.assembler;

import com.dati.semantic.domain.model.*;
import com.dati.semantic.server.pojo.vo.SubjectVO;

public class SubjectAssembler {
    public static SubjectVO toVO(Subject subject) {
        return SubjectVO.builder()
            .id(subject.getId())
            .name(subject.getName())
            .description(subject.getDescription())
            .datasourceId(subject.getDatasourceId())
            .createdAt(subject.getCreatedAt())
            .updatedAt(subject.getUpdatedAt())
            .build();
    }
}
```

- [ ] **Step 5: Create SubjectController**

```java
// backend/src/main/java/com/dati/semantic/server/controller/SubjectController.java
package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.*;
import com.dati.semantic.domain.service.SubjectService;
import com.dati.semantic.server.assembler.SubjectAssembler;
import com.dati.semantic.server.pojo.request.*;
import com.dati.semantic.server.pojo.vo.SubjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectVO createSubject(@RequestBody CreateSubjectRequest request) {
        Subject subject = subjectService.createSubject(
            request.getName(),
            request.getDescription(),
            request.getDatasourceId()
        );
        return SubjectAssembler.toVO(subject);
    }

    @PutMapping("/{id}")
    public SubjectVO updateSubject(@PathVariable String id, @RequestBody UpdateSubjectRequest request) {
        Subject subject = subjectService.updateSubject(id, request.getName(), request.getDescription());
        return SubjectAssembler.toVO(subject);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable String id) {
        subjectService.deleteSubject(id);
    }

    @GetMapping("/{id}")
    public SubjectVO getSubject(@PathVariable String id) {
        SubjectDetailVO detail = subjectService.getSubjectWithTables(id);
        return SubjectAssembler.toVO(detail.getSubject());
    }

    @GetMapping("/{id}/tables")
    public List<SubjectVO> getSubjectTables(@PathVariable String id) {
        SubjectDetailVO detail = subjectService.getSubjectWithTables(id);
        return detail.getTables().stream().map(SubjectAssembler::toSubjectTableVO).collect(Collectors.toList());
    }

    @PostMapping("/{id}/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public void addTableToSubject(@PathVariable String id, @RequestBody AddTableToSubjectRequest request) {
        subjectService.addTableToSubject(id, request.getTableId());
    }

    @DeleteMapping("/{id}/tables/{tableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTableFromSubject(@PathVariable String id, @PathVariable String tableId) {
        subjectService.removeTableFromSubject(id, tableId);
    }

    @GetMapping
    public List<SubjectVO> getSubjects(@RequestParam(required = false) String datasourceId) {
        List<Subject> subjects = datasourceId != null
            ? subjectService.getSubjectsByDatasource(datasourceId)
            : subjectService.getAllSubjects();
        return subjects.stream().map(SubjectAssembler::toVO).collect(Collectors.toList());
    }
}
```

Note: Need to add `getAllSubjects()` method to SubjectService if not exists.

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd backend && mvn -Dtest=SubjectControllerTest test -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/server/controller/SubjectController.java \
  backend/src/main/java/com/dati/semantic/server/pojo/request/*.java \
  backend/src/main/java/com/dati/semantic/server/pojo/vo/SubjectVO.java \
  backend/src/main/java/com/dati/semantic/server/assembler/SubjectAssembler.java \
  backend/src/test/java/com/dati/semantic/server/controller/SubjectControllerTest.java
git commit -m "feat(semantic): add SubjectController REST API"
```

---

## Task 6: TermController REST API

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/server/controller/TermController.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/request/CreateTermRequest.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/request/UpdateTermRequest.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/request/LinkTermRelationRequest.java`
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/vo/TermVO.java`
- Create: `backend/src/main/java/com/dati/semantic/server/assembler/TermAssembler.java`
- Test: `backend/src/test/java/com/dati/semantic/server/controller/TermControllerTest.java`

- [ ] **Step 1: Write failing test for TermController**

```java
// backend/src/test/java/com/dati/semantic/server/controller/TermControllerTest.java
package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.model.TermRelation;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.server.assembler.TermAssembler;
import com.dati.semantic.server.pojo.vo.TermVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TermController.class)
class TermControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TermService termService;
    @MockBean
    private TermAssembler termAssembler;

    @Test
    void createTerm_shouldReturn201() throws Exception {
        Term term = Term.builder()
            .id("term-001")
            .name("订单")
            .description("订单信息")
            .subjectId("subject-001")
            .build();
        when(termService.createTerm(eq("subject-001"), any(), any())).thenReturn(term);
        when(termAssembler.toVO(any())).thenReturn(TermVO.builder()
            .id("term-001")
            .name("订单")
            .description("订单信息")
            .subjectId("subject-001")
            .build());

        mockMvc.perform(post("/v1/subjects/subject-001/terms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"订单\",\"description\":\"订单信息\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("term-001"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=TermControllerTest test -q`
Expected: FAIL - classes don't exist

- [ ] **Step 3: Create Request/VO classes**

```java
// backend/src/main/java/com/dati/semantic/server/pojo/request/CreateTermRequest.java
package com.dati.semantic.server.pojo.request;

import lombok.Data;

@Data
public class CreateTermRequest {
    private String name;
    private String description;
}
```

```java
// backend/src/main/java/com/dati/semantic/server/pojo/request/UpdateTermRequest.java
package com.dati.semantic.server.pojo.request;

import lombok.Data;

@Data
public class UpdateTermRequest {
    private String name;
    private String description;
}
```

```java
// backend/src/main/java/com/dati/semantic/server/pojo/request/LinkTermRelationRequest.java
package com.dati.semantic.server.pojo.request;

import lombok.Data;

@Data
public class LinkTermRelationRequest {
    private String entityType;  // TABLE or FIELD
    private String tableId;
    private String fieldName;
}
```

```java
// backend/src/main/java/com/dati/semantic/server/pojo/vo/TermVO.java
package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TermVO {
    private String id;
    private String subjectId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Create TermAssembler**

```java
// backend/src/main/java/com/dati/semantic/server/assembler/TermAssembler.java
package com.dati.semantic.server.assembler;

import com.dati.semantic.domain.model.*;
import com.dati.semantic.server.pojo.vo.TermVO;

public class TermAssembler {
    public static TermVO toVO(Term term) {
        return TermVO.builder()
            .id(term.getId())
            .subjectId(term.getSubjectId())
            .name(term.getName())
            .description(term.getDescription())
            .createdAt(term.getCreatedAt())
            .updatedAt(term.getUpdatedAt())
            .build();
    }
}
```

- [ ] **Step 5: Create TermController**

```java
// backend/src/main/java/com/dati/semantic/server/controller/TermController.java
package com.dati.semantic.server.controller;

import com.dati.semantic.domain.model.*;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.server.assembler.TermAssembler;
import com.dati.semantic.server.pojo.request.*;
import com.dati.semantic.server.pojo.vo.TermVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TermController {
    private final TermService termService;

    @PostMapping("/subjects/{subjectId}/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public TermVO createTerm(@PathVariable String subjectId, @RequestBody CreateTermRequest request) {
        Term term = termService.createTerm(subjectId, request.getName(), request.getDescription());
        return TermAssembler.toVO(term);
    }

    @PutMapping("/terms/{id}")
    public TermVO updateTerm(@PathVariable String id, @RequestBody UpdateTermRequest request) {
        Term term = termService.updateTerm(id, request.getName(), request.getDescription());
        return TermAssembler.toVO(term);
    }

    @DeleteMapping("/terms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTerm(@PathVariable String id) {
        termService.deleteTerm(id);
    }

    @GetMapping("/terms/{id}")
    public TermVO getTerm(@PathVariable String id) {
        Term term = termService.getTermById(id);
        return TermAssembler.toVO(term);
    }

    @GetMapping("/subjects/{subjectId}/terms")
    public List<TermVO> getTermsBySubject(@PathVariable String subjectId) {
        return termService.getTermsBySubject(subjectId).stream()
            .map(TermAssembler::toVO)
            .collect(Collectors.toList());
    }

    @PostMapping("/terms/{id}/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkEntity(@PathVariable String id, @RequestBody LinkTermRelationRequest request) {
        termService.linkEntity(id, request.getEntityType(), request.getTableId(), request.getFieldName());
    }

    @DeleteMapping("/terms/{id}/relations/{tableId}/{fieldName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkEntity(@PathVariable String id, @PathVariable String tableId, @PathVariable String fieldName) {
        termService.unlinkEntity(id, tableId, fieldName);
    }
}
```

Note: Need to add `getTermById()` method to TermService.

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd backend && mvn -Dtest=TermControllerTest test -q`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/server/controller/TermController.java \
  backend/src/main/java/com/dati/semantic/server/pojo/request/*.java \
  backend/src/main/java/com/dati/semantic/server/pojo/vo/TermVO.java \
  backend/src/main/java/com/dati/semantic/server/assembler/TermAssembler.java \
  backend/src/test/java/com/dati/semantic/server/controller/TermControllerTest.java
git commit -m "feat(semantic): add TermController REST API"
```

---

## Task 7: SemanticIndexService 增强 - deleteByCondition 支持

**Files:**
- Modify: `backend/src/main/java/com/dati/semantic/domain/service/SemanticIndexService.java`
- Test: `backend/src/test/java/com/dati/semantic/domain/service/SemanticIndexServiceTest.java`

- [ ] **Step 1: Write failing test for deleteByCondition**

```java
@Test
void deleteByCondition_shouldDeleteBySubjectId() {
    indexService.deleteByCondition("subject-001");
    verify(semanticSearchDAO).deleteByEntity_SubjectId("subject-001");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -Dtest=SemanticIndexServiceTest#deleteByCondition_shouldDeleteBySubjectId test -q`
Expected: FAIL - method doesn't exist

- [ ] **Step 3: Add deleteByCondition method to SemanticSearchDAO**

```java
// In SemanticSearchDAO
void deleteByEntity_SubjectId(String subjectId);
```

- [ ] **Step 4: Add deleteByCondition implementation to SemanticIndexService**

```java
public void deleteByCondition(String subjectId) {
    semanticSearchDAO.deleteByEntity_SubjectId(subjectId);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && mvn -Dtest=SemanticIndexServiceTest#deleteByCondition_shouldDeleteBySubjectId test -q`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/dati/semantic/repository/dao/SemanticSearchDAO.java \
  backend/src/main/java/com/dati/semantic/domain/service/SemanticIndexService.java
git commit -m "feat(semantic): add deleteByCondition for subject cleanup"
```

---

## Task 8: 数据库表创建 SQL

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_subject_tables.sql`

- [ ] **Step 1: Create Flyway migration script**

```sql
-- V1__create_subject_tables.sql
CREATE TABLE subject (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    datasource_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (datasource_id)
);

CREATE TABLE subject_table (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_subject_table (subject_id, table_id),
    INDEX idx_subject (subject_id)
);

CREATE TABLE term (
    id VARCHAR(64) PRIMARY KEY,
    subject_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subject (subject_id)
);

CREATE TABLE term_relation (
    id VARCHAR(64) PRIMARY KEY,
    term_id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(16) NOT NULL COMMENT 'TABLE or FIELD',
    table_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(128),
    UNIQUE INDEX uk_term_relation (term_id, table_id, field_name),
    INDEX idx_term (term_id)
);
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V1__create_subject_tables.sql
git commit -m "db: add subject, term and relation tables"
```

---

## 实施顺序

1. Task 1: Subject PO + Mapper + DAO
2. Task 2: Term PO + Mapper + DAO
3. Task 3: SubjectService + ES 索引同步
4. Task 4: TermService + ES 索引同步
5. Task 5: SubjectController REST API
6. Task 6: TermController REST API
7. Task 7: SemanticIndexService 增强
8. Task 8: 数据库迁移脚本

---

## 验证命令

```bash
cd backend
mvn test -Dtest="*Subject*,*Term*,*SemanticIndex*"  # 运行所有相关测试
mvn spring-boot:run                                   # 启动服务验证
```

---

**Spec 覆盖检查：**
- [x] Subject 实体、CRUD API - Task 1, 3, 5
- [x] SubjectTable 关联管理 - Task 1, 3, 5
- [x] Term 实体、CRUD API - Task 2, 4, 6
- [x] TermRelation 关联管理（TABLE/FIELD）- Task 2, 4, 6
- [x] ES 索引同步（SUBJECT, SUBJECT_TABLE, TERM）- Task 3, 4, 7
- [x] 数据库表设计 - Task 8
- [x] 数据源约束校验（表必须属于主题的数据源）- Task 3, 4
- [x] 主题删除级联（删除关联的 SubjectTable, Term, TermRelation）- Task 3

**类型一致性检查：**
- Subject.id = "subject:" + uuid
- Term.id = "term:" + uuid
- SubjectTable.id = uuid
- TermRelation.id = uuid
- ES document id 格式与上述一致

---

Plan complete and saved to `docs/superpowers/plans/2026-03-30-subject-management-plan.md`.

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?