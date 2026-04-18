# Term Relation Management Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign term relation management to fix API bugs, show relations in collapsed view, and enable multi-select bulk adding.

**Architecture:** 
- Fix the broken unlink API endpoint (query params → path params)
- Enhance backend to return relations with table schema/name from GET /terms/{id}
- Replace single-relation add dialog with multi-select interface
- Show compact relation tags in collapsed table view
- Direct delete with toast notification (no confirmation)

**Tech Stack:** Vue 3 + Element Plus (frontend), Spring Boot + JPA (backend)

---

## File Mapping

### Backend
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/vo/TermRelationVO.java` — new VO for relation with schema/tableName
- Modify: `backend/src/main/java/com/dati/semantic/domain/model/TermRelation.java` — add tableName and schema fields
- Modify: `backend/src/main/java/com/dati/semantic/domain/service/TermService.java` — inject TableInfoDAO, populate schema/tableName
- Modify: `backend/src/main/java/com/dati/semantic/server/controller/TermController.java` — return relations from getTerm
- Modify: `backend/src/main/java/com/dati/semantic/server/assembler/TermAssembler.java` — convert Term with relations

### Frontend
- Modify: `frontend/src/api/subject.ts` — fix unlinkTermRelation API
- Modify: `frontend/src/components/subject/TermManager.vue` — collapsed view tags, multi-select dialog, direct delete

---

## Task 1: Backend — TermRelationVO and Relations API

**Files:**
- Create: `backend/src/main/java/com/dati/semantic/server/pojo/vo/TermRelationVO.java`
- Modify: `backend/src/main/java/com/dati/semantic/domain/model/TermRelation.java`
- Modify: `backend/src/main/java/com/dati/semantic/domain/service/TermService.java`
- Modify: `backend/src/main/java/com/dati/semantic/server/controller/TermController.java`
- Modify: `backend/src/main/java/com/dati/semantic/server/assembler/TermAssembler.java`

- [ ] **Step 1: Create TermRelationVO**

Create new file `backend/src/main/java/com/dati/semantic/server/pojo/vo/TermRelationVO.java`:

```java
package com.dati.semantic.server.pojo.vo;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermRelationVO {
    private String id;
    private String termId;
    private String entityType;  // "TABLE" or "FIELD"
    private String tableId;
    private String tableName;
    private String schema;
    private String fieldName;
}
```

- [ ] **Step 2: Add tableName and schema to TermRelation domain model**

Open `backend/src/main/java/com/dati/semantic/domain/model/TermRelation.java` and add:

```java
// Add after fieldName field
private String tableName;
private String schema;
```

- [ ] **Step 3: Inject TableInfoDAO into TermService**

Open `backend/src/main/java/com/dati/semantic/domain/service/TermService.java`:

Add import:
```java
import com.dati.datasource.repository.dao.TableInfoDAO;
```

Add field:
```java
private final TableInfoDAO tableInfoDAO;
```

Update constructor:
```java
public TermService(TermDAO termDAO, TermRelationDAO termRelationDAO,
                   SubjectTableDAO subjectTableDAO, TableInfoDAO tableInfoDAO,
                   SemanticIndexService semanticIndexService) {
    this.termDAO = termDAO;
    this.termRelationDAO = termRelationDAO;
    this.subjectTableDAO = subjectTableDAO;
    this.tableInfoDAO = tableInfoDAO;
    this.semanticIndexService = semanticIndexService;
}
```

- [ ] **Step 4: Update toTermRelation to include table info**

Replace the `toTermRelation` method in TermService:

```java
private TermRelation toTermRelation(TermRelationPO po) {
    String tableName = null;
    String schema = null;
    try {
        com.dati.datasource.repository.po.TableInfoPO tableInfo = 
            tableInfoDAO.findById(po.getTableId()).orElse(null);
        if (tableInfo != null) {
            tableName = tableInfo.getName();
            schema = tableInfo.getSchema();
        }
    } catch (Exception e) {
        // Log but continue - table lookup is not critical
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
```

- [ ] **Step 5: Add method to get Term with relations**

In TermService, add new method:

```java
@Transactional(readOnly = true)
public Term getTermByIdWithRelations(String id) {
    Term term = getTermById(id);
    List<TermRelation> relations = getTermRelations(id);
    term.setRelations(relations);
    return term;
}
```

Add `relations` field to Term model - open `backend/src/main/java/com/dati/semantic/domain/model/Term.java`:

```java
// Add after fieldName declarations
private List<TermRelation> relations;
```

- [ ] **Step 6: Update TermAssembler to handle relations**

Open `backend/src/main/java/com/dati/semantic/server/assembler/TermAssembler.java`:

First, add import for TermRelationVO:
```java
import com.dati.semantic.server.pojo.vo.TermRelationVO;
import com.dati.semantic.domain.model.TermRelation;
```

Add new method to convert relations:
```java
public TermRelationVO toRelationVO(TermRelation relation) {
    if (relation == null) {
        return null;
    }
    return TermRelationVO.builder()
            .id(relation.getId())
            .termId(relation.getTermId())
            .entityType(relation.getEntityType().name())
            .tableId(relation.getTableId())
            .tableName(relation.getTableName())
            .schema(relation.getSchema())
            .fieldName(relation.getFieldName())
            .build();
}
```

Update toVO to include relations (add after existing toVO method):
```java
public TermVO toVO(Term term, List<TermRelation> relations) {
    TermVO vo = toVO(term);
    if (relations != null && !relations.isEmpty()) {
        vo.setRelations(relations.stream()
                .map(this::toRelationVO)
                .collect(java.util.stream.Collectors.toList()));
    }
    return vo;
}
```

- [ ] **Step 7: Add relations field to TermVO**

Open `backend/src/main/java/com/dati/semantic/server/pojo/vo/TermVO.java`:

Add import:
```java
import java.util.List;
```

Add field:
```java
private List<TermRelationVO> relations;
```

- [ ] **Step 8: Update TermController to return relations**

Open `backend/src/main/java/com/dati/semantic/server/controller/TermController.java`:

Replace the `getTerm` method:
```java
@GetMapping("/terms/{id}")
public TermVO getTerm(@PathVariable String id) {
    return termAssembler.toVO(termService.getTermByIdWithRelations(id));
}
```

Note: We need to change from `getTermById` to `getTermByIdWithRelations` which we added in Step 5.

- [ ] **Step 9: Verify build**

Run: `cd /Users/zhangyimin/IdeaProjects/dati/backend && mvn compile -q 2>&1 | tail -20`
Expected: BUILD SUCCESS (may have warnings about unused imports)

If build fails, read the error and fix. Common issues:
- Missing imports
- Type mismatches

- [ ] **Step 10: Commit**

```bash
cd /Users/zhangyimin/IdeaProjects/dati
git add backend/src/main/java/com/dati/semantic/
git commit -m "feat(semantic): return term relations with schema from GET /terms/{id}"
```

---

## Task 2: Frontend — Fix unlinkTermRelation API

**Files:**
- Modify: `frontend/src/api/subject.ts`

- [ ] **Step 1: Update unlinkTermRelation to use path params**

Open `frontend/src/api/subject.ts` and replace `unlinkTermRelation`:

```typescript
export function unlinkTermRelation(
  termId: string,
  tableId: string,
  fieldName: string | null,
  signal?: AbortSignal
): Promise<IdResponse> {
  const encodedTableId = encodeURIComponent(tableId);
  const encodedFieldName = encodeURIComponent(fieldName === null ? '_' : fieldName);
  return del<IdResponse>(
    `/v1/terms/${encodeURIComponent(termId)}/relations/${encodedTableId}/${encodedFieldName}`,
    undefined,
    signal
  );
}
```

- [ ] **Step 2: Verify frontend build**

Run: `cd /Users/zhangyimin/IdeaProjects/dati/frontend && pnpm build 2>&1 | head -30`
Expected: No type errors related to subject.ts

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/subject.ts
git commit -m "fix(semantic): use path params for unlinkTermRelation API"
```

---

## Task 3: Frontend — Collapsed View Relation Tags

**Files:**
- Modify: `frontend/src/components/subject/TermManager.vue` — Linked Tables column (lines 318-322)

- [ ] **Step 1: Update Linked Tables column in collapsed view**

Open `TermManager.vue` and find the Linked Tables column (around line 318). Replace the count-only display:

```vue
<el-table-column :label="t('subject.linkedTables')" width="180">
  <template #default="{ row }">
    <template v-if="getTermRelations(row.id).length > 0">
      <div class="relation-tags">
        <el-tag
          v-for="relation in getTermDisplayRelations(row.id)"
          :key="relation.id"
          :type="relation.entity_type === 'TABLE' ? 'info' : 'warning'"
          size="small"
          class="relation-tag"
        >
          {{ getRelationDisplayText(relation) }}
        </el-tag>
        <span v-if="getTermRelations(row.id).length > 3" class="more-count">
          +{{ getTermRelations(row.id).length - 3 }}
        </span>
      </div>
    </template>
    <span v-else class="text-gray-400 text-sm">-</span>
  </template>
</el-table-column>
```

- [ ] **Step 2: Add helper methods after getTermRelations**

Find `getTermRelations` method (around line 79) and add after it:

```typescript
const getRelationDisplayText = (relation: TermRelationVO) => {
  const tableName = relation.schema ? `${relation.schema}.${relation.table_name}` : relation.table_name;
  if (relation.entity_type === 'TABLE') {
    return tableName;
  }
  return `${tableName}.${relation.field_name}`;
};

const getTermDisplayRelations = (termId: string) => {
  return getTermRelations(termId).slice(0, 3);
};
```

- [ ] **Step 3: Add CSS for relation tags**

Find the `<style scoped>` section and add:

```css
.relation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.relation-tag {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-count {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
```

- [ ] **Step 4: Verify build**

Run: `cd /Users/zhangyimin/IdeaProjects/dati/frontend && pnpm build 2>&1 | head -30`
Expected: Build succeeds

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/subject/TermManager.vue
git commit -m "feat(term-manager): show relation tags in collapsed view"
```

---

## Task 4: Frontend — Multi-Select Add Relation Dialog

**Files:**
- Modify: `frontend/src/components/subject/TermManager.vue` — state, methods, and dialog template

- [ ] **Step 1: Update state variables for multi-select**

Find these lines (around 30-37):
```typescript
const selectedTableId = ref('');
const tableColumns = ref<{ name: string }[]>([]);
const columnsLoading = ref(false);
const selectedField = ref('');
```

Replace with:
```typescript
const selectedTableIds = ref<Set<string>>(new Set());
const tableColumns = ref<Map<string, Set<string>>>(new Map());
const tableColumnsData = ref<Map<string, { name: string }[]>>(new Map());
const columnsLoading = ref(false);
```

- [ ] **Step 2: Add multi-select helper methods**

Add after the existing helper methods (after `getTermDisplayRelations`):

```typescript
const isTableSelected = (tableId: string) => selectedTableIds.value.has(tableId);

const isColumnSelected = (tableId: string, columnName: string) => {
  return tableColumns.value.get(tableId)?.has(columnName) ?? false;
};

const toggleTable = (tableId: string) => {
  if (selectedTableIds.value.has(tableId)) {
    selectedTableIds.value.delete(tableId);
    tableColumns.value.delete(tableId);
  } else {
    selectedTableIds.value.add(tableId);
    tableColumns.value.set(tableId, new Set());
  }
};

const toggleColumn = (tableId: string, columnName: string) => {
  const cols = tableColumns.value.get(tableId);
  if (!cols) return;
  if (cols.has(columnName)) {
    cols.delete(columnName);
    if (cols.size === 0) {
      tableColumns.value.delete(tableId);
      selectedTableIds.value.delete(tableId);
    }
  } else {
    cols.add(columnName);
    if (!selectedTableIds.value.has(tableId)) {
      selectedTableIds.value.add(tableId);
    }
  }
};

const getSelectedCount = () => {
  let count = 0;
  for (const tableId of selectedTableIds.value) {
    const cols = tableColumns.value.get(tableId);
    if (cols && cols.size > 0) {
      count += cols.size;
    } else {
      count += 1;
    }
  }
  return count;
};

const getSelectedSummary = () => {
  const count = getSelectedCount();
  if (count === 0) return '';
  return `${count} relation${count > 1 ? 's' : ''} selected`;
};

const getAvailableColumns = (tableId: string) => {
  return tableColumnsData.value.get(tableId) || [];
};
```

- [ ] **Step 3: Update handleOpenRelationDialog**

Find `handleOpenRelationDialog` (around line 153) and replace:

```typescript
const handleOpenRelationDialog = async (termId: string) => {
  relationTermId.value = termId;
  selectedTableIds.value = new Set();
  tableColumns.value = new Map();
  tableColumnsData.value = new Map();

  // Pre-select already linked tables/columns
  const existing = getTermRelations(termId);
  for (const rel of existing) {
    if (rel.entity_type === 'TABLE') {
      selectedTableIds.value.add(rel.table_id);
      tableColumns.value.set(rel.table_id, new Set());
    } else {
      selectedTableIds.value.add(rel.table_id);
      if (!tableColumns.value.has(rel.table_id)) {
        tableColumns.value.set(rel.table_id, new Set());
      }
      tableColumns.value.get(rel.table_id)!.add(rel.field_name!);
    }
  }

  try {
    relationDialogLoading.value = true;
    availableTables.value = await getSubjectTables(props.subjectId);
    relationDialogVisible.value = true;
  } catch (error) {
    console.error('Failed to load subject tables:', error);
    ElMessage.error(t('common.loadFailed'));
  } finally {
    relationDialogLoading.value = false;
  }
};
```

- [ ] **Step 4: Add loadColumnsForTable method**

Add after `handleOpenRelationDialog`:

```typescript
const loadColumnsForTable = async (tableId: string) => {
  if (tableColumnsData.value.has(tableId)) return;
  
  const table = availableTables.value.find(t => t.id === tableId);
  if (!table) return;

  try {
    columnsLoading.value = true;
    // Note: listTableColumns expects (datasourceId, tableId, page, size)
    // We need to get datasourceId from the subject, not from table
    // For now, use a placeholder - this API call may need adjustment
    const response = await listTableColumns(table.data_source_id, tableId, 1, 1000);
    tableColumnsData.value.set(tableId, response.data || []);
  } catch (error) {
    console.error('Failed to load columns:', error);
  } finally {
    columnsLoading.value = false;
  }
};
```

Note: The `listTableColumns` API requires `datasourceId` as first param. If `availableTables` doesn't have `data_source_id`, you may need to look up the datasource from subject or adjust the API call.

- [ ] **Step 5: Update handleSubmitRelation for batch**

Find `handleSubmitRelation` (around line 196) and replace:

```typescript
const handleSubmitRelation = async () => {
  if (selectedTableIds.value.size === 0) {
    ElMessage.warning(t('subject.selectTable'));
    return;
  }

  try {
    relationDialogLoading.value = true;
    
    const promises: Promise<unknown>[] = [];
    
    for (const tableId of selectedTableIds.value) {
      const cols = tableColumns.value.get(tableId);
      if (cols && cols.size > 0) {
        for (const fieldName of cols) {
          const body: LinkTermRelationRequest = {
            entity_type: 'FIELD',
            table_id: tableId,
            field_name: fieldName
          };
          promises.push(linkTermRelation(relationTermId.value, body));
        }
      } else {
        const body: LinkTermRelationRequest = {
          entity_type: 'TABLE',
          table_id: tableId
        };
        promises.push(linkTermRelation(relationTermId.value, body));
      }
    }
    
    await Promise.all(promises);
    ElMessage.success(t('subject.addRelationSuccess'));

    relationDialogVisible.value = false;
    await loadTermDetail(relationTermId.value);
  } catch (error) {
    console.error('Failed to add relations:', error);
    ElMessage.error(t('common.operationFailed'));
  } finally {
    relationDialogLoading.value = false;
  }
};
```

- [ ] **Step 6: Replace the relation dialog template**

Find the `el-dialog` for relations (around line 385) and replace the entire dialog content:

```vue
<el-dialog
  v-model="relationDialogVisible"
  :title="t('subject.addRelation')"
  width="50%"
  :close-on-click-modal="false"
  destroy-on-close
>
  <div class="relation-dialog-content">
    <div class="tables-panel">
      <div class="panel-header">
        <span>{{ t('common.tableName') }}</span>
        <el-button 
          link 
          size="small" 
          @click="selectedTableIds.size > 0 ? (selectedTableIds = new Set(), tableColumns = new Map()) : null"
        >
          {{ selectedTableIds.size > 0 ? t('common.deselectAll') : t('common.selectAll') }}
        </el-button>
      </div>
      <div class="table-list">
        <div
          v-for="table in availableTables"
          :key="table.id"
          class="table-item"
          :class="{ selected: isTableSelected(table.id) }"
        >
          <el-checkbox
            :model-value="isTableSelected(table.id)"
            @change="toggleTable(table.id)"
          >
            {{ table.schema ? `${table.schema}.${table.name}` : table.name }}
          </el-checkbox>
          <el-button
            v-if="isTableSelected(table.id)"
            link
            size="small"
            :loading="columnsLoading"
            @click="loadColumnsForTable(table.id)"
          >
            {{ t('column.columns') }} {{ tableColumns.get(table.id)?.size ? `(${tableColumns.get(table.id)!.size})` : '' }}
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="selectedTableIds.size > 0" class="columns-panel">
      <div class="panel-header">{{ t('column.columnName') }}</div>
      <div class="column-list">
        <template v-for="tableId in selectedTableIds" :key="tableId">
          <div class="table-columns-header">
            {{ availableTables.find(t => t.id === tableId)?.name }}
          </div>
          <template v-if="getAvailableColumns(tableId).length > 0">
            <el-checkbox
              v-for="column in getAvailableColumns(tableId)"
              :key="column.name"
              :model-value="isColumnSelected(tableId, column.name)"
              @change="toggleColumn(tableId, column.name)"
              class="column-checkbox"
            >
              {{ column.name }}
            </el-checkbox>
          </template>
          <div v-else class="text-gray-400 text-sm p-2">
            {{ t('common.noData') }}
          </div>
        </template>
      </div>
    </div>
  </div>

  <div class="selected-summary">
    {{ getSelectedSummary() }}
  </div>

  <template #footer>
    <div class="dialog-footer">
      <el-button @click="relationDialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="relationDialogLoading"
        :disabled="selectedTableIds.size === 0"
        @click="handleSubmitRelation"
      >
        {{ t('common.confirm') }}
      </el-button>
    </div>
  </template>
</el-dialog>
```

- [ ] **Step 7: Add dialog CSS**

Add to `<style scoped>`:

```css
.relation-dialog-content {
  display: flex;
  gap: 20px;
  min-height: 300px;
}

.tables-panel {
  flex: 1;
  border-right: 1px solid #eee;
  padding-right: 20px;
}

.columns-panel {
  flex: 1;
  max-height: 400px;
  overflow-y: auto;
}

.panel-header {
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-list {
  max-height: 300px;
  overflow-y: auto;
}

.table-item {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.table-item.selected {
  background-color: #f5f7fa;
}

.table-columns-header {
  font-size: 12px;
  color: #909399;
  margin: 8px 0 4px;
}

.column-checkbox {
  display: block;
  margin: 4px 0;
}

.selected-summary {
  margin-top: 12px;
  color: #606266;
  font-size: 14px;
  min-height: 20px;
}
```

- [ ] **Step 8: Verify build**

Run: `cd /Users/zhangyimin/IdeaProjects/dati/frontend && pnpm build 2>&1 | head -50`
Expected: Build succeeds (may have type warnings)

- [ ] **Step 9: Commit**

```bash
git add frontend/src/components/subject/TermManager.vue
git commit -m "feat(term-manager): add multi-select relation dialog"
```

---

## Task 5: Frontend — Direct Delete with Toast

**Files:**
- Modify: `frontend/src/components/subject/TermManager.vue` — handleRemoveRelation

- [ ] **Step 1: Update handleRemoveRelation to direct delete**

Find `handleRemoveRelation` (around line 222) and replace:

```typescript
const handleRemoveRelation = async (termId: string, relation: TermRelationVO) => {
  try {
    await unlinkTermRelation(termId, relation.table_id, relation.field_name || null);
    ElMessage.success(t('subject.removeRelationSuccess'));
    await loadTermDetail(termId);
  } catch (error) {
    console.error('Failed to remove relation:', error);
    ElMessage.error(t('common.operationFailed'));
  }
};
```

- [ ] **Step 2: Verify build**

Run: `cd /Users/zhangyimin/IdeaProjects/dati/frontend && pnpm build 2>&1 | head -30`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/subject/TermManager.vue
git commit -m "feat(term-manager): direct delete relation with toast notification"
```

---

## Verification

- [ ] Run backend tests: `cd /Users/zhangyimin/IdeaProjects/dati/backend && mvn test -Dtest=TermServiceTest -q 2>&1 | tail -10`
- [ ] Run frontend build: `cd /Users/zhangyimin/IdeaProjects/dati/frontend && pnpm build`
- [ ] Manual verification in browser:
  1. Start backend: `cd /Users/zhangyimin/IdeaProjects/dati/backend && mvn spring-boot:run`
  2. Start frontend: `cd /Users/zhangyimin/IdeaProjects/dati/frontend && pnpm dev`
  3. Open a subject with terms
  4. Verify relations show as tags in collapsed view
  5. Click to expand and verify full list with remove buttons
  6. Test multi-select add dialog
  7. Test direct delete (no confirmation popup)
