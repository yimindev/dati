# 列值抽取功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为列提供值抽取功能，支持 NL2SQL 场景下匹配列的实际值

**Architecture:**
- 后端新增 `ColumnValueService` 处理值抽取和 ES 写入，复用现有 `SemanticIndexService`
- ColumnInfo 模型新增 `extractValueEnabled` 和 `valueSampleLimit` 字段
- 系统配置通过 `@ConfigurationProperties` 注入，支持 YAML 配置
- 前端在列管理页新增值配置区和值列表弹窗

**Tech Stack:** Spring Boot, JPA, Elasticsearch, Vue 3, Element Plus

---

## Task 1: 系统配置

**Files:**
- Create: `backend/src/main/java/com/dati/config/ColumnValueConfig.java`
- Modify: `backend/src/main/resources/application.yaml`

- [ ] **Step 1: 创建 ColumnValueConfig 配置类**

```java
package com.dati.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "system")
public class ColumnValueConfig {

    private Integer columnValueSampleLimit = 1000;

    private Integer columnValueLengthLimit = 256;
}
```

- [ ] **Step 2: 在 application.yaml 中添加默认值**

```yaml
system:
  column-value-sample-limit: 1000
  column-value-length-limit: 256
```

- [ ] **Step 3: 写测试验证配置读取**

```java
package com.dati.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnValueConfigTest {

    @Test
    void testDefaultValues() {
        ColumnValueConfig config = new ColumnValueConfig();
        assertEquals(1000, config.getColumnValueSampleLimit());
        assertEquals(256, config.getColumnValueLengthLimit());
    }
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd backend && mvn test -Dtest=ColumnValueConfigTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/dati/config/ColumnValueConfig.java backend/src/main/resources/application.yaml backend/src/test/java/com/dati/config/ColumnValueConfigTest.java
git commit -m "feat: add column value system config"
```

---

## Task 2: ColumnInfo 模型和 PO 变更

**Files:**
- Modify: `backend/src/main/java/com/dati/datasource/domain/model/ColumnInfo.java`
- Modify: `backend/src/main/java/com/dati/datasource/repository/po/ColumnInfoPO.java`
- Modify: `backend/src/main/java/com/dati/datasource/repository/mapper/ColumnMapper.java`

- [ ] **Step 1: 在 ColumnInfo 添加字段**

```java
// 在 ColumnInfo.java 添加
private Boolean extractValueEnabled = false;

private Integer valueSampleLimit;
```

- [ ] **Step 2: 在 ColumnInfoPO 添加字段**

```java
// 在 ColumnInfoPO.java 添加
@Column(nullable = false)
private Boolean extractValueEnabled = false;

private Integer valueSampleLimit;
```

- [ ] **Step 3: 更新 ColumnMapper 添加字段映射**

```java
// 在 toColumnInfo 方法中添加
columnInfo.setExtractValueEnabled(columnInfoPO.getExtractValueEnabled());
columnInfo.setValueSampleLimit(columnInfoPO.getValueSampleLimit());

// 在 toColumnInfoPO 方法中添加
columnInfoPO.setExtractValueEnabled(columnInfo.getExtractValueEnabled());
columnInfoPO.setValueSampleLimit(columnInfo.getValueSampleLimit());
```

- [ ] **Step 4: 写测试验证 PO 和 Model 转换**

```java
@Test
void testColumnInfoExtractValueFields() {
    ColumnInfoPO po = new ColumnInfoPO();
    po.setExtractValueEnabled(true);
    po.setValueSampleLimit(500);

    ColumnInfo info = ColumnMapper.toColumnInfo(po);
    assertTrue(info.getExtractValueEnabled());
    assertEquals(500, info.getValueSampleLimit());
}
```

- [ ] **Step 5: 运行测试验证**

Run: `cd backend && mvn test -Dtest=ColumnMapperTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/dati/datasource/domain/model/ColumnInfo.java backend/src/main/java/com/dati/datasource/repository/po/ColumnInfoPO.java backend/src/main/java/com/dati/datasource/repository/mapper/ColumnMapper.java
git commit -m "feat: add extractValueEnabled and valueSampleLimit to ColumnInfo"
```

---

## Task 3: ColumnInfoVO 和 ColumnAssembler 变更

**Files:**
- Modify: `backend/src/main/java/com/dati/datasource/server/pojo/ColumnInfoVO.java`
- Modify: `backend/src/main/java/com/dati/datasource/server/assembler/ColumnAssembler.java`

- [ ] **Step 1: 在 ColumnInfoVO 添加字段**

```java
// 在 ColumnInfoVO.java 添加
private Boolean extractValueEnabled = false;

private Integer valueSampleLimit;
```

- [ ] **Step 2: 更新 ColumnAssembler 添加字段转换**

```java
// 在 toColumnInfoVO 方法中添加
columnInfoVO.setExtractValueEnabled(columnInfo.getExtractValueEnabled());
columnInfoVO.setValueSampleLimit(columnInfo.getValueSampleLimit());

// 在 toColumnInfo 方法中添加
columnInfo.setExtractValueEnabled(columnInfoVO.getExtractValueEnabled());
columnInfo.setValueSampleLimit(columnInfoVO.getValueSampleLimit());
```

- [ ] **Step 3: 写测试验证转换**

```java
@Test
void testAssemblerWithExtractValueFields() {
    ColumnInfo info = new ColumnInfo();
    info.setExtractValueEnabled(true);
    info.setValueSampleLimit(500);

    ColumnInfoVO vo = columnAssembler.toColumnInfoVO(info);
    assertTrue(vo.getExtractValueEnabled());
    assertEquals(500, vo.getValueSampleLimit());
}
```

- [ ] **Step 4: 运行测试验证**

Run: `cd backend && mvn test -Dtest=ColumnAssemblerTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/dati/datasource/server/pojo/ColumnInfoVO.java backend/src/main/java/com/dati/datasource/server/assembler/ColumnAssembler.java
git commit -m "feat: add extractValue fields to ColumnInfoVO and assembler"
```

---

## Task 4: 创建 ColumnValueService

**Files:**
- Create: `backend/src/main/java/com/dati/datasource/domain/service/ColumnValueService.java`
- Create: `backend/src/test/java/com/dati/datasource/domain/service/ColumnValueServiceTest.java`

- [ ] **Step 1: 创建 ColumnValueService**

```java
package com.dati.datasource.domain.service;

import com.dati.config.ColumnValueConfig;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.mapper.ColumnMapper;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ColumnValueService {

    private final ColumnInfoDAO columnInfoDAO;
    private final TableInfoDAO tableInfoDAO;
    private final JdbcMetaService jdbcMetaService;
    private final SemanticIndexService semanticIndexService;
    private final ColumnValueConfig columnValueConfig;

    public ColumnValueService(
            ColumnInfoDAO columnInfoDAO,
            TableInfoDAO tableInfoDAO,
            JdbcMetaService jdbcMetaService,
            SemanticIndexService semanticIndexService,
            ColumnValueConfig columnValueConfig) {
        this.columnInfoDAO = columnInfoDAO;
        this.tableInfoDAO = tableInfoDAO;
        this.jdbcMetaService = jdbcMetaService;
        this.semanticIndexService = semanticIndexService;
        this.columnValueConfig = columnValueConfig;
    }

    public void extractValues(String datasourceId, String columnId) throws SQLException {
        ColumnInfoPO columnPO = columnInfoDAO.findById(columnId).orElseThrow();
        ColumnInfo columnInfo = ColumnMapper.toColumnInfo(columnPO);

        String tableId = columnInfo.getTableId();
        String columnName = columnInfo.getName();

        Integer sampleLimit = Optional.ofNullable(columnInfo.getValueSampleLimit())
                .orElse(columnValueConfig.getColumnValueSampleLimit());
        Integer lengthLimit = columnValueConfig.getColumnValueLengthLimit();

        String sql = String.format("SELECT DISTINCT %s FROM %s LIMIT %d",
                columnName, getTableName(tableId), sampleLimit);

        List<Map<String, Object>> results = jdbcMetaService.executeSql(datasourceId, sql);

        List<SemanticSearchDocument> docs = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Object value = row.get(columnName);
            if (value != null) {
                String strValue = value.toString();
                if (strValue.length() > lengthLimit) {
                    strValue = strValue.substring(0, lengthLimit);
                }
                String id = UUID.randomUUID().toString();
                EntityReference entity = EntityReference.builder()
                        .tableId(tableId)
                        .field(columnName)
                        .build();
                SemanticSearchDocument doc = SemanticSearchDocument.builder()
                        .id(id)
                        .type(SemanticEntityType.FIELD_VALUE)
                        .keywords(List.of(strValue))
                        .entity(entity)
                        .build();
                docs.add(doc);
            }
        }
        semanticIndexService.saveBatch(docs);
    }

    public void saveValues(String datasourceId, String columnId, List<ValueItem> values, List<String> deletedIds) {
        if (deletedIds != null && !deletedIds.isEmpty()) {
            for (String id : deletedIds) {
                semanticIndexService.deleteById(id);
            }
        }

        if (values != null && !values.isEmpty()) {
            List<SemanticSearchDocument> docs = new ArrayList<>();
            for (ValueItem item : values) {
                ColumnInfoPO columnPO = columnInfoDAO.findById(columnId).orElseThrow();
                EntityReference entity = EntityReference.builder()
                        .tableId(columnPO.getTableId())
                        .field(columnPO.getName())
                        .build();

                List<String> keywords = new ArrayList<>();
                keywords.add(item.getValue());
                if (item.getSynonyms() != null) {
                    keywords.addAll(item.getSynonyms());
                }

                SemanticSearchDocument doc = SemanticSearchDocument.builder()
                        .id(item.getId())
                        .type(SemanticEntityType.FIELD_VALUE)
                        .keywords(keywords.stream().distinct().toList())
                        .entity(entity)
                        .build();
                docs.add(doc);
            }
            semanticIndexService.saveBatch(docs);
        }
    }

    public List<ValueItem> getValues(String columnId) {
        ColumnInfoPO columnPO = columnInfoDAO.findById(columnId).orElseThrow();
        List<SemanticSearchDocument> docs = semanticIndexService.findByColumnId(
                columnPO.getTableId(), columnPO.getName());

        return docs.stream().map(doc -> {
            ValueItem item = new ValueItem();
            item.setId(doc.getId());
            List<String> keywords = doc.getKeywords();
            if (keywords != null && !keywords.isEmpty()) {
                item.setValue(keywords.get(0));
                if (keywords.size() > 1) {
                    item.setSynonyms(keywords.subList(1, keywords.size()));
                }
            }
            return item;
        }).toList();
    }

    private String getTableName(String tableId) {
        return tableInfoDAO.findById(tableId)
                .map(TableInfoPO::getName)
                .orElse("unknown_table");
    }

    @Data
    public static class ValueItem {
        private String id;
        private String value;
        private List<String> synonyms = new ArrayList<>();
    }
}
```

- [ ] **Step 2: 运行编译验证**

Run: `cd backend && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/dati/datasource/domain/service/ColumnValueService.java
git commit -m "feat: add ColumnValueService for value extraction"
```

---

## Task 5: 实现 ES 查询方法

**Files:**
- Modify: `backend/src/main/java/com/dati/semantic/repository/dao/SemanticSearchDAO.java`
- Modify: `backend/src/main/java/com/dati/semantic/domain/service/SemanticIndexService.java`
- Modify: `backend/src/main/java/com/dati/datasource/domain/service/ColumnValueService.java`

- [ ] **Step 1: 在 SemanticSearchDAO 添加根据 columnId 查询方法**

```java
// 在 SemanticSearchDAO.java 添加
List<SemanticSearchDocument> findByEntity_TableIdAndEntity_Field(String tableId, String field);
```

- [ ] **Step 2: 在 SemanticIndexService 添加查询方法**

```java
// 在 SemanticIndexService.java 添加
public List<SemanticSearchDocument> findByColumnId(String tableId, String field) {
    return semanticSearchDAO.findByEntity_TableIdAndEntity_Field(tableId, field);
}
```

- [ ] **Step 3: 运行编译验证**

Run: `cd backend && mvn compile`
Expected: 编译成功

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/dati/semantic/repository/dao/SemanticSearchDAO.java backend/src/main/java/com/dati/semantic/domain/service/SemanticIndexService.java
git commit -m "feat: add ES query method for column values"
```

---

## Task 6: ColumnController API 实现

**Files:**
- Modify: `backend/src/main/java/com/dati/datasource/server/controller/ColumnController.java`

- [ ] **Step 1: 添加新的 API 端点**

```java
// 在 ColumnController.java 添加

private final ColumnValueService columnValueService;

@PostMapping("/{columnId}/values/extract")
public IdResponse extractValues(
        @PathVariable String datasourceId,
        @PathVariable String columnId) {
    try {
        columnValueService.extractValues(datasourceId, columnId);
        return new IdResponse(columnId);
    } catch (SQLException e) {
        log.error("Failed to extract values for column {}", columnId, e);
        throw new DatiException("SQL Error: " + e.getMessage());
    }
}

@GetMapping("/{columnId}/values")
public ColumnValueListResponse getValues(@PathVariable String columnId) {
    List<ColumnValueService.ValueItem> values = columnValueService.getValues(columnId);
    return new ColumnValueListResponse(values);
}

@PutMapping("/{columnId}/values")
public IdResponse saveValues(
        @PathVariable String datasourceId,
        @PathVariable String columnId,
        @RequestBody ColumnValueListRequest request) {
    columnValueService.saveValues(datasourceId, columnId, request.getValues(), request.getDeletedIds());
    return new IdResponse(columnId);
}
```

- [ ] **Step 2: 创建请求和响应类**

Create: `backend/src/main/java/com/dati/datasource/server/pojo/ColumnValueListRequest.java`
Create: `backend/src/main/java/com/dati/datasource/server/pojo/ColumnValueListResponse.java`

```java
// ColumnValueListRequest.java
package com.dati.datasource.server.pojo;

import lombok.Data;
import java.util.List;

@Data
public class ColumnValueListRequest {
    private List<ValueItemVO> values;
    private List<String> deletedIds;

    @Data
    public static class ValueItemVO {
        private String id;
        private String value;
        private List<String> synonyms = new ArrayList<>();
    }
}
```

```java
// ColumnValueListResponse.java
package com.dati.datasource.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ColumnValueListResponse {
    private List<ValueItemVO> values;

    @Data
    @AllArgsConstructor
    public static class ValueItemVO {
        private String id;
        private String value;
        private List<String> synonyms;
    }
}
```

- [ ] **Step 3: 运行编译验证**

Run: `cd backend && mvn compile`
Expected: 编译成功

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/dati/datasource/server/controller/ColumnController.java backend/src/main/java/com/dati/datasource/server/pojo/ColumnValueListRequest.java backend/src/main/java/com/dati/datasource/server/pojo/ColumnValueListResponse.java
git commit -m "feat: add column value APIs to ColumnController"
```

---

## Task 7: 前端 API

**Files:**
- Modify: `frontend/src/api/column.ts`

- [ ] **Step 1: 添加值相关的 API 函数**

```typescript
export type ColumnValueVO = {
  id: string;
  value: string;
  synonyms: string[];
};

export type SaveColumnValuesRequest = {
  values: ColumnValueVO[];
  deleted_ids: string[];
};

export async function extractColumnValues(
  datasourceId: string | number,
  tableId: string,
  columnId: string,
) {
  return post(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${columnId}/values/extract`,
    {},
  );
}

export async function getColumnValues(
  datasourceId: string | number,
  tableId: string,
  columnId: string,
) {
  return get<{ values: ColumnValueVO[] }>(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${columnId}/values`,
  );
}

export async function saveColumnValues(
  datasourceId: string | number,
  tableId: string,
  columnId: string,
  request: SaveColumnValuesRequest,
) {
  return put(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${columnId}/values`,
    request,
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/column.ts
git commit -m "feat: add column value API functions"
```

---

## Task 8: 前端 UI - 列配置区更新

**Files:**
- Modify: `frontend/src/pages/datasources/[id]/tables/[tableId]/columns.vue`

- [ ] **Step 1: 在 metadataDialog 中添加抽取配置区**

在 `column.description` 配置项后添加:

```vue
<el-form-item :label="t('column.extractValues')">
  <div class="flex gap-4">
    <el-switch v-model="currentColumn.extract_value_enabled" />
    <el-input-number
      v-if="currentColumn.extract_value_enabled"
      v-model="currentColumn.value_sample_limit"
      :min="1"
      :max="10000"
      size="small"
    />
  </div>
</el-form-item>

<el-form-item v-if="currentColumn.extract_value_enabled">
  <el-button type="primary" @click="handleExtractValues">
    {{ t('column.extractValues') }}
  </el-button>
  <el-button @click="handleOpenValuesDialog">
    {{ t('column.viewValues') }}
  </el-button>
</el-form-item>
```

- [ ] **Step 2: 添加状态和方法**

```typescript
const valuesDialogVisible = ref(false);
const columnValues = ref<ColumnValueVO[]>([]);

const handleExtractValues = async () => {
  try {
    await extractColumnValues(datasourceId.value, tableId.value, currentColumn.value!.id!);
    ElMessage.success(t('common.operationSuccess'));
    await handleOpenValuesDialog();
  } catch (error) {
    ElMessage.error(t('common.operationFailed'));
  }
};

const handleOpenValuesDialog = async () => {
  try {
    const resp = await getColumnValues(datasourceId.value, tableId.value, currentColumn.value!.id!);
    columnValues.value = resp.values || [];
    valuesDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(t('common.loadFailed'));
  }
};
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/pages/datasources/[id]/tables/[tableId]/columns.vue
git commit -m "feat: add extract value toggle to column config"
```

---

## Task 9: 前端 UI - 值列表弹窗

**Files:**
- Modify: `frontend/src/pages/datasources/[id]/tables/[tableId]/columns.vue`

- [ ] **Step 1: 添加值列表弹窗组件**

在 metadataDialog 后添加:

```vue
<el-dialog v-model="valuesDialogVisible" :title="t('column.valuesTitle')" width="700px">
  <div class="mb-4 flex gap-2">
    <el-input v-model="newValue" :placeholder="t('column.enterValue')" />
    <el-button type="primary" @click="handleAddValue">
      {{ t('common.add') }}
    </el-button>
  </div>

  <el-table :data="columnValues" stripe>
    <el-table-column :label="t('column.value')" prop="value" />
    <el-table-column :label="t('common.synonyms')">
      <template #default="{ row }">
        <el-tag v-for="syn in row.synonyms" :key="syn" size="small" class="mr-1">
          {{ syn }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('common.actions')" width="100">
      <template #default="{ row }">
        <el-button link type="danger" size="small" @click="handleDeleteValue(row)">
          {{ t('common.delete') }}
        </el-button>
      </template>
    </el-table-column>
  </el-table>

  <template #footer>
    <el-button @click="valuesDialogVisible = false">
      {{ t('common.cancel') }}
    </el-button>
    <el-button type="primary" @click="handleSaveValues">
      {{ t('common.save') }}
    </el-button>
  </template>
</el-dialog>
```

- [ ] **Step 2: 添加相关状态和方法**

```typescript
const newValue = ref('');
const pendingDeletedIds = ref<string[]>([]);

const handleAddValue = () => {
  if (!newValue.value.trim()) return;
  columnValues.value.push({
    id: '',
    value: newValue.value.trim(),
    synonyms: [],
  });
  newValue.value = '';
};

const handleDeleteValue = (row: ColumnValueVO) => {
  if (row.id) {
    pendingDeletedIds.value.push(row.id);
  }
  columnValues.value = columnValues.value.filter(v => v !== row);
};

const handleSaveValues = async () => {
  try {
    const valuesToSave = columnValues.value
      .filter(v => !v.id)
      .map(v => ({ id: v.id || null, value: v.value, synonyms: v.synonyms }));
    await saveColumnValues(datasourceId.value, tableId.value, currentColumn.value!.id!, {
      values: valuesToSave,
      deleted_ids: pendingDeletedIds.value,
    });
    ElMessage.success(t('common.saveSuccess'));
    valuesDialogVisible.value = false;
    pendingDeletedIds.value = [];
  } catch (error) {
    ElMessage.error(t('common.operationFailed'));
  }
};
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/pages/datasources/[id]/tables/[tableId]/columns.vue
git commit -m "feat: add column values management dialog"
```

---

## Task 10: 后端单元测试

**Files:**
- Modify: `backend/src/test/java/com/dati/datasource/domain/service/ColumnValueServiceTest.java`

- [ ] **Step 1: 写 ColumnValueService 的单元测试**

```java
package com.dati.datasource.domain.service;

import com.dati.config.ColumnValueConfig;
import com.dati.datasource.repository.dao.ColumnInfoDAO;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.semantic.domain.SemanticEntityType;
import com.dati.semantic.domain.service.SemanticIndexService;
import com.dati.semantic.repository.po.EntityReference;
import com.dati.semantic.repository.po.SemanticSearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("ColumnValueService 单元测试")
class ColumnValueServiceTest {

    @Mock
    private ColumnInfoDAO columnInfoDAO;

    @Mock
    private TableInfoDAO tableInfoDAO;

    @Mock
    private JdbcMetaService jdbcMetaService;

    @Mock
    private SemanticIndexService semanticIndexService;

    private ColumnValueConfig columnValueConfig;

    private ColumnValueService columnValueService;

    private ColumnInfoPO testColumnPO;

    private TableInfoPO testTablePO;

    @BeforeEach
    void setUp() {
        columnValueConfig = new ColumnValueConfig();
        columnValueConfig.setColumnValueSampleLimit(1000);
        columnValueConfig.setColumnValueLengthLimit(256);

        testColumnPO = new ColumnInfoPO();
        testColumnPO.setId("col1");
        testColumnPO.setTableId("table1");
        testColumnPO.setName("status");

        testTablePO = new TableInfoPO();
        testTablePO.setId("table1");
        testTablePO.setName("users");

        columnValueService = new ColumnValueService(
                columnInfoDAO, tableInfoDAO, jdbcMetaService, semanticIndexService, columnValueConfig);
    }

    private SemanticSearchDocument createValueDoc(String id, List<String> keywords) {
        return SemanticSearchDocument.builder()
                .id(id)
                .type(SemanticEntityType.FIELD_VALUE)
                .keywords(keywords)
                .entity(EntityReference.builder()
                        .tableId("table1")
                        .field("status")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("extractValues")
    class ExtractValuesTests {

        @Test
        @DisplayName("正常流程：提取多个不重复值，生成对应 ES 文档")
        void extractValues_normalCase() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(
                            Map.of("status", "active"),
                            Map.of("status", "inactive")
                    ));

            columnValueService.extractValues("ds1", "col1");

            ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(semanticIndexService).saveBatch(captor.capture());
            List<SemanticSearchDocument> saved = captor.getValue();

            assertEquals(2, saved.size());
            assertEquals(List.of("active"), saved.get(0).getKeywords());
            assertEquals(List.of("inactive"), saved.get(1).getKeywords());
        }

        @Test
        @DisplayName("跳过 null 值")
        void extractValues_skipsNullValues() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(
                            Map.of("status", "active"),
                            Map.of("status", null)
                    ));

            columnValueService.extractValues("ds1", "col1");

            ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(semanticIndexService).saveBatch(captor.capture());
            assertEquals(1, captor.getValue().size());
        }

        @Test
        @DisplayName("空结果集：不调用 saveBatch")
        void extractValues_emptyResult() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString())).thenReturn(List.of());

            columnValueService.extractValues("ds1", "col1");

            verify(semanticIndexService, never()).saveBatch(anyList());
        }

        @Test
        @DisplayName("长值截断：超过 256 字符的值被截断")
        void extractValues_truncatesLongValues() throws SQLException {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            String longValue = "a".repeat(300);
            when(jdbcMetaService.executeSql(eq("ds1"), anyString()))
                    .thenReturn(List.of(Map.of("status", longValue)));

            columnValueService.extractValues("ds1", "col1");

            ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(semanticIndexService).saveBatch(captor.capture());
            assertEquals(256, captor.getValue().get(0).getKeywords().get(0).length());
        }

        @Test
        @DisplayName("列级别 sampleLimit 覆盖系统默认")
        void extractValues_usesColumnSampleLimit() throws SQLException {
            testColumnPO.setValueSampleLimit(500);
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString())).thenReturn(List.of());

            columnValueService.extractValues("ds1", "col1");

            verify(jdbcMetaService).executeSql(eq("ds1"), contains("LIMIT 500"));
        }

        @Test
        @DisplayName("列级别 sampleLimit 为空时使用系统默认")
        void extractValues_usesDefaultSampleLimit() throws SQLException {
            testColumnPO.setValueSampleLimit(null);
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));
            when(tableInfoDAO.findById("table1")).thenReturn(Optional.of(testTablePO));
            when(jdbcMetaService.executeSql(eq("ds1"), anyString())).thenReturn(List.of());

            columnValueService.extractValues("ds1", "col1");

            verify(jdbcMetaService).executeSql(eq("ds1"), contains("LIMIT 1000"));
        }

        @Test
        @DisplayName("columnId 不存在时抛出 NoSuchElementException")
        void extractValues_columnNotFound() {
            when(columnInfoDAO.findById("invalid")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () ->
                    columnValueService.extractValues("ds1", "invalid")
            );
        }
    }

    @Nested
    @DisplayName("saveValues")
    class SaveValuesTests {

        @Test
        @DisplayName("新增值：id 为 null 时创建新 ES 文档，keywords 包含值和同义词")
        void saveValues_addNewValue() {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            ColumnValueService.ValueItem newItem = new ColumnValueService.ValueItem();
            newItem.setId(null);
            newItem.setValue("北京");
            newItem.setSynonyms(List.of("帝都", "北漂之城"));

            columnValueService.saveValues("ds1", "col1", List.of(newItem), null);

            ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(semanticIndexService).saveBatch(captor.capture());
            List<SemanticSearchDocument> saved = captor.getValue();

            assertEquals(1, saved.size());
            assertEquals(List.of("北京", "帝都", "北漂之城"), saved.get(0).getKeywords());
        }

        @Test
        @DisplayName("更新值：id 存在时更新对应 ES 文档")
        void saveValues_updateExistingValue() {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            ColumnValueService.ValueItem updateItem = new ColumnValueService.ValueItem();
            updateItem.setId("existing_id");
            updateItem.setValue("上海");
            updateItem.setSynonyms(List.of());

            columnValueService.saveValues("ds1", "col1", List.of(updateItem), null);

            ArgumentCaptor<List<SemanticSearchDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(semanticIndexService).saveBatch(captor.capture());
            assertEquals("existing_id", captor.getValue().get(0).getId());
        }

        @Test
        @DisplayName("删除值：根据 deletedIds 调用 deleteById")
        void saveValues_deleteValues() {
            columnValueService.saveValues("ds1", "col1", null, List.of("id1", "id2"));

            verify(semanticIndexService).deleteById("id1");
            verify(semanticIndexService).deleteById("id2");
            verify(semanticIndexService, never()).saveBatch(anyList());
        }

        @Test
        @DisplayName("混合场景：同时删除旧值和新增新值")
        void saveValues_mixedOperations() {
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            ColumnValueService.ValueItem newItem = new ColumnValueService.ValueItem();
            newItem.setId(null);
            newItem.setValue("广州");
            newItem.setSynonyms(List.of());

            columnValueService.saveValues("ds1", "col1", List.of(newItem), List.of("id_to_delete"));

            verify(semanticIndexService).deleteById("id_to_delete");
            verify(semanticIndexService).saveBatch(anyList());
        }

        @Test
        @DisplayName("空操作：values 和 deletedIds 都为空时不做任何操作")
        void saveValues_emptyInput() {
            columnValueService.saveValues("ds1", "col1", null, null);

            verify(semanticIndexService, never()).saveBatch(anyList());
            verify(semanticIndexService, never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("getValues")
    class GetValuesTests {

        @Test
        @DisplayName("正常返回：解析 ES 文档，提取第一个 keyword 为值，其余为同义词")
        void getValues_normal() {
            SemanticSearchDocument doc1 = createValueDoc("doc1", List.of("北京", "帝都"));
            SemanticSearchDocument doc2 = createValueDoc("doc2", List.of("上海"));
            when(semanticIndexService.findByColumnId("table1", "status"))
                    .thenReturn(List.of(doc1, doc2));
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            List<ColumnValueService.ValueItem> result = columnValueService.getValues("col1");

            assertEquals(2, result.size());

            ColumnValueService.ValueItem first = result.get(0);
            assertEquals("doc1", first.getId());
            assertEquals("北京", first.getValue());
            assertEquals(List.of("帝都"), first.getSynonyms());

            ColumnValueService.ValueItem second = result.get(1);
            assertEquals("doc2", second.getId());
            assertEquals("上海", second.getValue());
            assertEquals(List.of(), second.getSynonyms());
        }

        @Test
        @DisplayName("空结果：ES 中没有值时返回空列表")
        void getValues_empty() {
            when(semanticIndexService.findByColumnId("table1", "status")).thenReturn(List.of());
            when(columnInfoDAO.findById("col1")).thenReturn(Optional.of(testColumnPO));

            List<ColumnValueService.ValueItem> result = columnValueService.getValues("col1");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("columnId 不存在时抛出 NoSuchElementException")
        void getValues_columnNotFound() {
            when(columnInfoDAO.findById("invalid")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () ->
                    columnValueService.getValues("invalid")
            );
        }
    }
}
```

- [ ] **Step 2: 运行测试验证**

Run: `cd backend && mvn test -Dtest=ColumnValueServiceTest`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add backend/src/test/java/com/dati/datasource/domain/service/ColumnValueServiceTest.java
git commit -m "test: add ColumnValueService unit tests"
```

---

## Task 11: 后端集成测试

**Files:**
- Create: `backend/src/test/java/com/dati/datasource/server/controller/ColumnControllerIntegrationTest.java`

- [ ] **Step 1: 写 ColumnController 集成测试**

```java
package com.dati.datasource.server.controller;

import com.dati.datasource.server.pojo.ColumnValueListRequest;
import com.dati.datasource.server.pojo.ColumnValueListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ColumnControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testExtractValuesEndpoint() {
        String url = "/v1/data-sources/{dsId}/tables/{tableId}/columns/{columnId}/values/extract";

        ResponseEntity<String> response = restTemplate.postForEntity(
                url.replace("{dsId}", "ds1").replace("{tableId}", "table1").replace("{columnId}", "col1"),
                null,
                String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError());
    }
}
```

- [ ] **Step 2: 运行测试验证**

Run: `cd backend && mvn test -Dtest=ColumnControllerIntegrationTest`
Expected: PASS (或跳过如果依赖外部服务)

- [ ] **Step 3: 提交**

```bash
git add backend/src/test/java/com/dati/datasource/server/controller/ColumnControllerIntegrationTest.java
git commit -m "test: add ColumnController integration tests"
```

---

## Task 12: 前端 E2E 测试

**Files:**
- Create: `frontend/tests/column-values.spec.ts`

- [ ] **Step 1: 使用 playwright-cli 编写 E2E 测试**

```typescript
import { test, expect } from '@playwright/test';

test('column values extraction', async ({ page }) => {
  await page.goto('/datasources');

  await page.click('text=TestDS');
  await page.click('text=tables');
  await page.click('text=users');
  
  await page.click('text=Edit');
  
  await page.locator('.el-switch').first().click();
  await page.click('text=Extract Values');
  
  await page.click('text=View Values');
  
  await page.fill('input[placeholder="Enter value"]', 'test_value');
  await page.click('text=Add');
  
  await expect(page.locator('text=test_value')).toBeVisible();
});
```

- [ ] **Step 2: 运行测试验证**

Run: `cd frontend && npx playwright test tests/column-values.spec.ts --headed`
Expected: 测试通过

- [ ] **Step 3: 提交**

```bash
git add frontend/tests/column-values.spec.ts
git commit -m "test: add column values e2e test"
```

---

## 实施顺序

1. Task 1: 系统配置 (独立)
2. Task 2: ColumnInfo 模型和 PO 变更 (依赖 1)
3. Task 3: ColumnInfoVO 和 ColumnAssembler 变更 (依赖 2)
4. Task 4: ColumnValueService (依赖 1, 2, 3)
5. Task 5: ES 查询方法 (依赖 4)
6. Task 6: ColumnController API (依赖 4, 5)
7. Task 7: 前端 API (依赖 6)
8. Task 8: 前端 UI - 列配置区 (依赖 7)
9. Task 9: 前端 UI - 值列表弹窗 (依赖 8)
10. Task 10: 后端单元测试 (依赖 6)
11. Task 11: 后端集成测试 (依赖 6, 10)
12. Task 12: 前端 E2E 测试 (依赖 8, 9)
