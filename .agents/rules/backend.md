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
- Uses `MapperUtils.copyBaseInfo()` / `copyBaseResourceInfo()` to handle base fields
- Handles encryption/decryption (e.g., `EncryptionUtils.encrypt()` for passwords)
- Example: `DSMapper.toDataSourcePO()`, `DSMapper.toDataSource()`

### Assembler (server/assembler/)

Domain Model ↔ VO conversion + request context enrichment:
- Spring `@Component`, extends `BaseAssembler`
- Converts Model to VO (e.g., `toDatasourceVO()`)
- Enriches VO with user display names via `fillUserInfo()`
- Fills audit user IDs into Domain Model via `fillUsersFromRequest()` / `fillUpdateUserFromRequest()` (called in Controller before passing to Service)
- Example: `DSAssembler.toDatasourceVO()`, `DSAssembler.toDatasourceVOList()`

## Service Layer Patterns

### Create

Convert Domain Model → PO via Mapper, then save:

```java
McpCustomToolPO po = McpCustomToolMapper.toPO(tool);
po = customToolDAO.save(po);
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

## Key Rules

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
