# 认证架构（Authentication Architecture）

本文档概述 DatI 认证框架的设计理念、核心组件与扩展机制。

## 设计理念

- **可插拔认证**：通过 `AuthenticationProvider` 接口，支持多种认证方式并存，默认提供本地账号密码认证。
- **Provider 自治**：每个 Provider 完全封装自己的凭证提取、验证、Token 签发逻辑，通用层（Interceptor/Service）不感知具体实现细节。
- **轻量无状态**：不引入 Spring Security，使用 JWT 实现无状态认证，适合分布式部署。
- **身份与授权分离**：`auth` 模块只解决"你是谁"（用户、登录、组成员关系），"你能做什么"由 `permission` 模块负责（见 [permission.md](permission.md)）。

## 核心组件

| 组件 | 职责 | 位置 |
|------|------|------|
| `AuthInterceptor` | 通用拦截：维护公开路径白名单，遍历 Provider 尝试认证，设置 `RequestContext` | `auth/authentication/` |
| `AuthenticationProvider` | 认证提供者接口（匹配请求 + 验证 + 登录） | `auth/authentication/` |
| `LocalAuthenticationProvider` | 默认实现：从 `Authorization: Bearer <token>` 提取 JWT，本地账号密码登录 | `auth/authentication/` |
| `JwtTokenUtil` | JWT 生成与验证（封装在 Local Provider 内部） | `auth/domain/service/` |
| `AuthenticationService` | 登录门面：按 type 路由到对应 Provider | `auth/domain/service/` |
| `UserService` | 用户领域服务：注册、查询、密码哈希 | `auth/domain/service/` |
| `UserGroupService` | 组成员解析：`groupIdsOf(userId)`，V1 返回隐式组 `ALL_USERS`，V2 合并真实组查询 | `auth/domain/service/` |
| `ApiKeyAuthenticationProvider` | 认证用户 API Key（`Authorization: Bearer sk_...`）；`@Order(HIGHEST_PRECEDENCE)` 保证在 JWT Provider 之前被询问（JWT Provider 会认领任意 Bearer 头） | `auth/authentication/` |
| `ApiKeyService` | API Key 领域服务：生成/掩码/列表/删除/过期校验 | `auth/domain/service/` |
| `ApiKeyRepository` | API Key 持久化（key_hash 唯一索引） | `auth/repository/dao/` |
| `ApiKeyPO` | API Key 持久化对象（继承 `BasePO`，表 `api_key`） | `auth/repository/po/` |
| `UserPO` | 用户持久化对象（继承 `BasePO`） | `auth/repository/po/` |

## 认证流程

### 登录流程

```
POST /v1/auth/login
    │
    ▼
AuthController ──→ AuthenticationService.login(type, name, password)
                         │
                         ▼
              查找 supports(type) 的 Provider
                         │
                         ▼
              LocalAuthenticationProvider.login()
                         │
                         ▼
              查数据库 → BCrypt 比对 → 生成 JWT Token
                         │
                         ▼
                   返回 Token 给客户端
```

### 请求拦截流程

```
请求 /v1/data-sources
    │
    ▼
AuthInterceptor.preHandle()
    │
    ├── auth.enabled = false？→ 直接放行
    │
    └── 遍历所有 AuthenticationProvider
            │
            ▼
    LocalAuthenticationProvider.canAuthenticate(request)
            │
            ├── 检查 "Authorization: Bearer <token>"
            │   不存在 → 返回 false（不是我的请求，继续下一个 Provider）
            │
            └── 返回 true（请求被我认领）
                    │
                    ▼
            LocalAuthenticationProvider.authenticate(request)
                    │
                    ├── 验证 JWT（签名、过期）
                    │   失败 → 返回 Optional.empty()（认领但认证失败，直接 401）
                    │
                    └── 返回 User
                            │
                            ▼
                    RequestContext.setUser(user)
                            │
                            ▼
                    进入 Controller
```

**方法语义约定**：

| 方法 | 职责 | 返回 true / `Optional.of(user)` | 返回 false / `Optional.empty()` |
|------|------|--------------------------------|--------------------------------|
| `canAuthenticate(request)` | 快速形式匹配（看 Header、Cookie 等） | **认领该请求**，接下来调用 `authenticate` | 不匹配，跳过本 Provider |
| `authenticate(request)` | 执行实际验证 | 认证成功，放行 | **认领了但认证失败**（如 Token 过期），`AuthInterceptor` 立即返回 401，不再尝试其他 Provider |

### API Key 认证

API Key 用于程序化调用（如 MCP 服务），`sk_` 前缀用于与 JWT 区分。由于 `LocalAuthenticationProvider.canAuthenticate` 会认领任意 `Bearer ` 头（`sk_...` 也不例外），`ApiKeyAuthenticationProvider` 通过 `@Order(Ordered.HIGHEST_PRECEDENCE)` 保证被优先询问，`sk_` 请求不会落到 JWT Provider。

```
请求携带 Authorization: Bearer sk_...
    │
    ▼
ApiKeyAuthenticationProvider.canAuthenticate(request)
    │
    ├── Header 以 "Bearer sk_" 开头 → 返回 true（认领请求；因 @Order 优先，JWT Provider 不会被先询问）
    │
    └── 否则 → 返回 false（跳过，交给 JWT Provider）
            │
            ▼
ApiKeyAuthenticationProvider.authenticate(request)
    │
    ├── SHA-256 哈希明文 Key → 查 api_key.key_hash
    │    没查到 → Optional.empty()（401）
    │
    ├── 校验 expires_at 是否过期
    │    已过期 → Optional.empty()（401）
    │
    ├── 加载 Key 绑定的用户 → 设置 RequestContext
    │
    └── 更新 last_used_at
            │
            ▼
            进入 Controller
```

**安全要点**：

- 明文 Key 仅在创建时返回一次，库中只存 SHA-256 哈希 + 掩码（如 `sk_ab12***cd34`），数据库泄露也无法还原
- 删除 Key 即时生效（下次请求查不到哈希，直接 401）
- API Key 无登录流程（`login()` 不支持），也不参与 `AuthenticationService` 的 type 路由

## 扩展机制

新增一种认证方式（如 OAuth2）只需三步：

1. 实现 `AuthenticationProvider` 接口
2. 注入 Spring 容器（`@Component`）
3. 客户端请求时携带对应凭证（如 `Authorization: Bearer <oauth2-token>`）

`AuthInterceptor` 和 `AuthenticationService` 无需任何改动。

**多个 Provider 特征重叠时的处理**：

如果两个 Provider 都识别同一种 Header（如都用 `Authorization: Bearer`），`canAuthenticate` 可能同时为 true。此时按 Spring 注入顺序（可用 `@Order` 控制）第一个匹配的 Provider 优先，且认证失败后直接 401，不会落到第二个 Provider。建议通过额外特征区分（如 `X-Auth-Type: oauth2` 或 token 前缀），避免歧义。

## 配置项

```yaml
auth:
  enabled: true                    # 是否启用认证拦截
  admin-users: ${ADMIN_USERS:admin}  # 全局管理员（按登录名，逗号分隔；授权判定使用，见 permission.md）
  jwt:
    secret: ${JWT_SECRET}          # JWT 签名密钥（≥256位）
    expiration-seconds: 604800     # Token 有效期，默认 7 天
```

## API 端点

| 方法 | 路径 | 描述 | 认证要求 |
|------|------|------|----------|
| POST | `/v1/auth/register` | 用户注册 | 公开 |
| POST | `/v1/auth/login` | 用户登录 | 公开 |
| GET | `/v1/auth/me` | 获取当前用户信息 | 需 Token |
| GET | `/v1/users/search` | 用户搜索（授权弹窗选人，返回最小字段） | 需 Token |
| POST | `/v1/auth/api-keys` | 创建 API Key（明文仅此一次返回） | 需 Token |
| GET | `/v1/auth/api-keys` | 列表（仅掩码） | 需 Token |
| DELETE | `/v1/auth/api-keys/{id}` | 删除 API Key | 需 Token |

## 错误码

| 错误码 | HTTP | 说明 |
|--------|------|------|
| `AUTH001` | 401 | 认证失败（用户名或密码错误） |
| `AUTH002` | 401 | Token 无效或过期 |
| `AUTH003` | 409 | 用户名已存在 |
| `AUTH004` | 400 | 不支持的认证类型 |
| `AUTH005` | 403 | 不能操作其他用户的 API Key |
| `AUTH006` | 400 | expiresInDays 必须是 30/90/180 |

## 前端架构

前端采用 **最简实现**，与后端 JWT 认证无缝衔接。

### 前端组件

| 组件 | 职责 | 位置 |
|------|------|------|
| `auth.ts` | 认证 API 封装（login/register/me），支持按 type 路由到不同 Provider | `frontend/src/api/` |
| `auth.ts` (store) | Pinia 状态管理：token/user/loading，login/logout/fetchUser | `frontend/src/stores/` |
| `login.vue` | 登录页，居中卡片布局，支持国际化 | `frontend/src/pages/` |
| `register.vue` | 注册页，支持用户名/密码/确认密码/显示名 | `frontend/src/pages/` |
| `http.ts` | Axios 拦截器：自动携带 Bearer Token，401 处理（认证接口除外） | `frontend/src/api/` |
| `main.ts` | 路由守卫：未登录拦截，已登录重定向 | `frontend/src/` |

### 前端数据流

```
用户访问 /datasources
    │
    ▼
main.ts 路由守卫
    │
    ├── 已登录？→ 放行
    │
    └── 未登录？→ 重定向到 /login
            │
            ▼
    login.vue → 填写表单 → 点击登录
            │
            ▼
    authStore.login()
            │
            ├── 调用 POST /v1/auth/login
            ├── 存储 token 到 localStorage
            ├── 调用 GET /v1/auth/me 获取用户信息
            └── 跳转到首页 /
                    │
                    ▼
    后续 API 请求
            │
            ▼
    http.ts 自动携带 Bearer Token
            │
            ▼
    后端验证 JWT → 返回数据
```

### 前端关键设计

**1. 无状态认证**
- JWT 存储于 `localStorage`，与后端无状态架构一致
- `http.ts` 请求拦截器自动携带 `Authorization: Bearer <token>`

**2. 认证页面隔离**
- `App.vue` 检测当前路由，登录/注册页**不显示** Header 和 Sidebar（全屏）
- 登录/注册页**不调用** `loadConfig()` 和 `fetchUser()`，避免 401 循环

**3. 401 处理策略**
- 认证接口（`/auth/*`）的 401 是**业务错误**（密码错误等），不跳转
- 其他接口的 401 是**认证失效**（token 过期等），清除 token 并跳转登录页
- 已在 `/login` 页面时，不重复跳转

**4. 多 Provider 扩展预留**
- `login(type, name, password)` 方法的 `type` 参数已预留
- 未来添加 OAuth2 时：
  - 新增 `loginWithOAuth()` 方法
  - 登录页添加 "使用 XXX 登录" 按钮
  - 无需修改 store 或守卫逻辑

**5. 错误处理**
- 前端页面 catch 错误并显示 `ElMessage`
- 错误信息优先级：后端返回 message > Axios 错误 > 国际化兜底文本
- 国际化支持：所有提示文本通过 `t()` 调用，支持中英文切换

### 前端文件结构

```
frontend/src/
├── api/
│   ├── auth.ts              # 认证 API
│   ├── http.ts              # Axios 实例 + 拦截器
│   └── types.ts             # User, LoginRequest, RegisterRequest, LoginResponse
├── stores/
│   └── auth.ts              # 认证状态管理
├── pages/
│   ├── login.vue            # 登录页（自动路由 /login）
│   └── register.vue         # 注册页（自动路由 /register）
├── locales/
│   ├── zh.ts                # 中文翻译（auth.xxx 键）
│   └── en.ts                # 英文翻译（auth.xxx 键）
└── main.ts                  # 路由守卫
```

## 参考

- 详细设计文档：[../superpowers/specs/2026-04-28-auth-design.md](../superpowers/specs/2026-04-28-auth-design.md)
- 授权架构：[permission.md](permission.md)
- 后端代码位置：`backend/src/main/java/com/dati/auth/`
- 前端代码位置：`frontend/src/`
