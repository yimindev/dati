# Backend Coding Conventions

## Package Structure (DDD)

Each module follows this structure:
```
com.dati.<module>/
├── domain/
│   ├── model/          # Domain entities (extends BaseResource)
│   └── service/        # Business logic
├── repository/
│   ├── dao/            # JPA Repositories
│   ├── po/             # Persistence Objects (extends BasePO; use BaseResourcePO for business entities)
│   └── mapper/         # PO ↔ Model conversion (static methods)
└── server/
    ├── controller/     # REST controllers (base path /v1/...)
    ├── pojo/           # Request/Response VO
    └── assembler/      # Model ↔ VO conversion
```

## Layer Responsibilities

### Mapper (repository/mapper/)

PO (Persistence Object) ↔ Domain Model conversion:
- Static methods only, stateless
- Public API: `toPO(Model)` → new PO, `toModel(PO)` → Model
- `copyProperties()` is **private** — used internally by `toPO()` only, never called from Service
- Uses `MapperUtils.copyBaseResourceInfo()` to copy all fields including audit fields (`createdBy`, `updatedBy`) from Model to PO
- Handles encryption/decryption (e.g., `EncryptionUtils.encrypt()` for passwords)
- Example: `SubjectMapper.toPO(subject)`, `DSMapper.toDataSource()`

```java
// Pattern: toPO copies everything including audit fields set by Controller
public static SubjectPO toPO(Subject subject) {
    SubjectPO po = new SubjectPO();
    MapperUtils.copyBaseResourceInfo(subject, po);  // name, description, createdBy, updatedBy
    po.setDatasourceId(subject.getDatasourceId());
    po.setAliases(subject.getAliases());
    return po;
}
```

### Assembler (server/assembler/)

Domain Model ↔ VO conversion + user info enrichment:
- Spring `@Component`, extends `BaseAssembler`
- Internal `mapFields(model)` — pure field copy, no enrichment
- `toVO(model)` — single item: `mapFields` + `fillUserInfo`
- `toPageResponse(Page<model>)` — list: batch `mapFields` + batch `fillUserInfo` → `PageResponse<VO>`
- `fillUsersFromRequest(model)` / `fillUpdateUserFromRequest(model)` — set `createdBy`/`updatedBy` from current user (called **by Controller** before passing to Service)

```java
// Internal — pure mapping
private DatasourceVO mapFields(DataSource ds) { ... }

// Public — single item with user names
public DatasourceVO toDatasourceVO(DataSource ds) {
    DatasourceVO vo = mapFields(ds);
    super.fillUserInfo(List.of(vo));
    return vo;
}

// Public — page with batch user name lookup (1 DB query for all rows)
public PageResponse<DatasourceVO> toPageResponse(Page<DataSource> page) {
    List<DatasourceVO> vos = page.getContent().stream().map(this::mapFields).toList();
    super.fillUserInfo(vos);
    return PageResponse.of(new PageImpl<>(vos, page.getPageable(), page.getTotalElements()));
}
```

## Service Layer Patterns

### Audit Field Flow (all modules must follow)

Audit fields (`createdBy`, `updatedBy`) flow from Controller → Model → Mapper → PO:

```
Controller:  build Model → fillUsersFromRequest(model)     // sets createdBy, updatedBy
             service.createSubject(model)
Service:     SubjectMapper.toPO(model)                      // copies all fields to PO
             dao.save(po)
```

- **Controller** is responsible for setting audit fields via `assembler.fillUsersFromRequest(model)` before passing to Service
- **Mapper.toPO()** uses `MapperUtils.copyBaseResourceInfo()` which copies `createdBy`/`updatedBy` along with other base fields
- **Service** never calls `RequestContext.getUser()` — audit fields come from the Model passed in

### Create

Convert Domain Model → PO via Mapper, then save:

```java
SubjectPO po = SubjectMapper.toPO(subject);  // copies all fields including createdBy/updatedBy
po = subjectDAO.save(po);
return po.getId();
```

### Update (Pattern A — project standard)

**Fetch existing PO → set fields directly on PO → save.** Do NOT call `Mapper.copyProperties()` for updates.

```java
public void updateCustomTool(McpCustomTool tool) {
    McpCustomToolPO po = customToolDAO
        .findByServiceIdAndId(tool.getServiceId(), tool.getId())
        .orElseThrow(() -> new DatiException(ErrorCode.MS_TOOL_NOT_FOUND, tool.getId()));

    // Validation before mutation
    if (tool.getName() != null) {
        validateToolName(tool.getName());
        // uniqueness check...
        po.setName(tool.getName());
    }
    if (tool.getDescription() != null) {
        po.setDescription(tool.getDescription());
    }
    po.setEnabled(tool.isEnabled());
    if (tool.getConfig() != null) {
        // config-specific validation
        po.setConfig(JsonUtils.toJson(tool.getConfig()));
    }

    customToolDAO.save(po);
}
```

Key principles:
- **Do not call `Mapper.copyProperties()` from Service** — `copyProperties` is a private helper inside `toPO()` only, used for full-model conversion during create, not for partial-update merging.
- **Controller is responsible for constructing a complete Domain Model** before passing to Service, including `id` and `serviceId` populated from path variables.
- Service signature for update takes only the Domain Model (e.g., `updateCustomTool(McpCustomTool tool)`), not separate path-variable params.
- Each field is set explicitly with its own validation context, making the update logic transparent and avoiding silent null-overwrites.

### Delete

Fetch PO by id, then delete:

```java
McpCustomToolPO po = customToolDAO
    .findByServiceIdAndId(serviceId, toolId)
    .orElseThrow(() -> new DatiException(ErrorCode.MS_TOOL_NOT_FOUND, toolId));
customToolDAO.delete(po);
```

## Base Classes & Utilities

### Entity Base Classes

**`BasePO`** — All POs extend this. Contains audit fields (`id`, `createdBy`, `createdAt`, `updatedBy`, `updatedAt`). Join tables may use it directly.

**`BaseResourcePO extends BasePO`** — For business resource entities. Adds `name`, `description`, `deleted`.

**`BaseResource`** — Domain model base. Mirrors `BaseResourcePO` fields.

**`BaseResourceVO extends BaseResource`** — Response base. Adds `createdUserName`, `updatedUserName` (filled by assembler).

### Response Wrappers

- **`IdResponse`**: Mutation response (`{ id: string }`)
- **`PageResponse<T>`**: Paginated list response (`{ data, total, page, size, total_pages }`)
- **`PageReq`**: Pagination request. Fields: `page` (default 1), `size` (default 10). Methods: `toPageRequest()` returns Spring `PageRequest`

### Common Utilities

- **`MapperUtils`**: Static helpers for copying base fields between `BaseResource` and `BaseResourcePO`
- **`BaseAssembler`**: Base class for assemblers. Provides `copyBaseInfo()`, `fillUsersFromRequest()`, `fillUpdateUserFromRequest()`, `fillUserInfo()`
- **`RequestContext`**: Thread-local context holder for current user. Use `RequestContext.getUser()`
- **`EncryptionUtils`**: Password encryption/decryption (currently pass-through, TODO for actual encryption)
- **`DatiException`**: Business exception with structured error codes. Supports parameterized messages via `MessageFormat` templates.

**`ErrorCode`**: Enum defining all error codes with fixed prefixes:
  - `CM` (Common): Generic errors — e.g. `CM001` (400), `CM004` (404), `CM005` (500)
  - `DS` (DataSource): Data source module — e.g. `DS001` (connection failed)
  - `SM` (Semantic): Semantic module — e.g. `SM001` (subject not found)
  - Template messages use `{0}`, `{1}` placeholders (Java `MessageFormat`)

**`ErrorResponse`**: Standard error response body `{ code, message, timestamp }`

**`GlobalExceptionHandler`**: `@RestControllerAdvice` that automatically converts exceptions to `ErrorResponse` with proper HTTP status codes

### Common Package (`com.dati.common`)

- **`StringUtils`**: `isEmpty()`, `isNotEmpty()` wrappers
- **`JsonUtils`**: Jackson-based JSON serialization/deserialization helpers

## Permission Control Standards

### Cascading Permission Checks
The platform manages access control via three root resources: `DATA_SOURCE`, `SUBJECT`, and `MCP_SERVICE`. Sub-resources must inherit and assert permissions of their corresponding root resource:
- **Data Source Sub-resources** (Tables, Columns, Column Values) → check parent `DATA_SOURCE` (`VIEW` for read/query, `EDIT` for mutations/sync/extract/save).
- **Semantic Sub-resources** (Terms, Term Relations, Subject Tables) → check parent `SUBJECT` (`VIEW` for queries, `EDIT` for mutations).
- **MCP Service Sub-resources** (Tools, Prompts, Snapshots, Data Scope) → check parent `MCP_SERVICE` (`VIEW` for read/list, `EDIT` for mutations/testing/publishing).

Use semantic assertion methods on `PermissionService`:
```java
// Root resource assertions (supports both String ID and PO entity overloads):
permissionService.requireDataSource(datasourceId, Permission.EDIT);
permissionService.requireSubject(subjectId, Permission.VIEW);
permissionService.requireMcpService(servicePO, Permission.EDIT); // PO overload avoids redundant DB lookup
```

### Key Security & Consistency Rules
1. **Cross-Resource Consistency**: Always verify that sub-resources actually belong to the parent resource specified in the request (e.g. In `ColumnValueService.extractValues`, verify that the column's table actually belongs to the given `datasourceId`).
2. **Eliminate Redundant Lookups**: If the service has already loaded the root resource's PO, prefer passing the PO entity to `permissionService.requireXxx(po, permission)` or calling `requireCurrentUser(type, id, perm, po.getCreatedBy())`.
3. **Dual-Channel Data Source Access**:
   - **User Channel** (`DataSourceService.getDataSource(id)`): For console REST APIs. Strictly verifies current user's `DATA_SOURCE (VIEW)` permission.
   - **Internal Channel** (`DataSourceService.getDataSourceInternal(id)`): For MCP tool execution engines (e.g., `ExecuteSqlExecutor`, `ParameterizedSqlExecutor`). Authorization is governed at the `MCP_SERVICE` layer with DataScope table-level constraints; direct data source permission checks on the invoking user are bypassed.

## Key Rules

- **English only in code artifacts**: Log messages (`@Slf4j`), test `@DisplayName`, and code comments use English. Chinese is allowed only in user-facing text (i18n messages, PRD docs).
- **Naming**: `PascalCase` classes, `camelCase` methods, `lowercase` packages.
- **Braces**: Always use braces `{}` for `if` / `for` / `while` blocks, even for single statements. One-liners are not allowed.
- **Injection**: Constructor injection only (`private final`).
- **Lombok**: Used throughout the project. POs use `@Getter`/`@Setter`; `@FieldNameConstants` is optional and PO-only; `@Slf4j` as needed.
- **Response**: Return `IdResponse` for mutations. Return `PageResponse<T>` for paginated lists; raw `List<T>` is acceptable for metadata queries (schemas, tables, columns) where pagination is unnecessary.
- **Exception**: Use `DatiException` for business errors, log with `@Slf4j`.
  - Add new `ErrorCode` entries for new business scenarios (follow existing prefix conventions)
  - All exceptions return unified `ErrorResponse` with proper HTTP status codes
- **JSON**: Dev profile uses `SNAKE_CASE` naming strategy.
- **Security**: Passwords encrypted via `EncryptionUtils` in mapper layer.
