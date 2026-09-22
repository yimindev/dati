# 本地开发指南（Development）

[English](development.md) | [简体中文](development_zh.md)

本文档用于指导 DatI 系统的本地开发环境搭建与调试。

---

## 先决条件与依赖

### 基础环境
- **Java 21**
- **Maven 3.9+**
- **Node.js 20+** 与 **pnpm 10+**

### 外部依赖：Elasticsearch 8.x
- **版本要求**：项目语义检索模块原生基于 ES 8.x Java Client
- **分词器要求**：当前版本语义检索模型依赖 **IK 中文分词器**（`ik_max_word` / `ik_smart`），若 ES 实例未安装 IK 插件，数据源同步与术语建索引时会报 500（`analyzer [ik_max_word] not found`）。
- **本地快速拉起**：使用已预置 IK 插件的容器镜像（与 `docker-compose.yml` 保持一致），开发环境可开启 `-e "xpack.security.enabled=false"` 免密运行：
  ```bash
  docker run -d --name dati-es-dev -p 9200:9200 \
    -e "discovery.type=single-node" \
    -e "xpack.security.enabled=false" \
    -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
    davyinsa/elasticsearch-ik:8.18.7
  ```

---

## 后端启动与配置说明

### Dev Profile 默认提供的开箱即用配置
开发环境默认激活 `spring.profiles.active=dev`，提供了以下零配置开发支持：
- **嵌入式 H2 数据库**：默认使用本地文件数据库（路径 `${user.dir}/db/dati`），启动自动维护表结构（`ddl-auto: update`），**无需在本地安装 MySQL**。
- **H2 Web 控制台**：默认开启，访问路径 `http://localhost:8085/h2-console/dati`（JDBC URL: `jdbc:h2:file:./db/dati`，账号 `sa`，密码留空，直接点击「Connect」即可进入）。
- **接口文档（Swagger）**：默认开启，访问地址 `http://localhost:8085/swagger-ui.html`。
- **安全与管理员**：`JWT_SECRET` 内置本地默认密钥，无需配置；管理员标识 `ADMIN_USERS` 默认为 `admin`。
- **MCP 端点**：`dati.mcp.endpoint-base-url` 自动推导为 `http://localhost:8085`。

### 启动前关键需要修改/确认的配置（`application-dev.yaml`）
在启动后端前，请根据您的本地环境检查 `backend/app/src/main/resources/application-dev.yaml`：

1. **Elasticsearch 连接与凭据**
   ```yaml
   spring:
     elasticsearch:
       uris: ${SPRING_ELASTICSEARCH_URIS:http://localhost:9200}
       username: ${SPRING_ELASTICSEARCH_USERNAME:elastic}
       password: ${SPRING_ELASTICSEARCH_PASSWORD:}
   ```
   - **默认行为**：密码默认为空，配合上述免密模式拉起的 ES 容器，**无需修改任何配置直接启动**。

2. **切换外部数据库（可选）**
   - 如需使用 MySQL 代替 H2，修改 `application-dev.yaml` 中的 `spring.datasource.*` 连接串及凭据即可。

3. **ES 交互调试日志（可选）**
   - 若需排查 ES 查询 DSL 或底层 HTTP 报文交互，可解开注释：
     ```yaml
     logging:
       level:
         org.apache.http.wire: DEBUG
     ```

### 启动后端服务
- **IDE 启动**：直接运行 `com.dati.DatIApplication` 主类即可。
- **命令行启动**：
  ```bash
  cd backend
  mvn spring-boot:run
  ```
启动后访问 `http://localhost:8085/actuator/health`，当 `status` 为 `UP`（且包含 `elasticsearch` 与 `db` 状态均为 UP）时即表示后端就绪。

---

## 前端与帮助文档启动

前端采用 Vue 3 + TypeScript + Vite 构建，并内嵌了基于 VitePress 的用户帮助中心：

```bash
cd frontend
pnpm install

# 启动前端控制台（端口 5173）
pnpm dev

# （可选）启动帮助中心文档服务（端口 5174）
pnpm docs:dev
```

- 前端开发服务器运行在 `http://localhost:5173`。
- **接口代理**：已自动将 `/v1`、`/api` 转发至本地后端 `http://localhost:8085`。
- **帮助文档联调**：页面顶部的「帮助文档」链接指向 `/docs`（反向代理至 `5174`）。若需本地编辑或预览帮助文档，保持 `pnpm docs:dev` 运行即可。
- **全量生产打包**：使用 `pnpm build:all`（先编译文档再打包前端）。

---

## 注意点

1. **首次管理员注册规则**
   - 系统**无预置管理员账号**。
   - 本地启动后首次进入系统时，必须使用用户名 `admin` 进行注册，系统会自动识别并为其赋予超级管理员权限（此规则与配置 `ADMIN_USERS=admin` 一致）。
2. **JSON 命名风格**
   - 后端 Dev 环境全局启用 `SNAKE_CASE` 蛇形命名策略。新增 Controller / DTO 时请保持字段为驼峰（代码），前后端交互自动转为蛇形下划线。
