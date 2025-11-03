# Backend 文档（Spring Boot 3 / Java 21）

本模块基于 Spring Boot 3.5.x（Java 21），使用 Maven 构建，支持 JPA/JDBC、Flyway 迁移，开发环境使用 H2 文件库。

## 快速命令

- 构建（跳过测试）：
```bash
cd backend && mvn -B -DskipTests package
```
- 本地运行（dev，默认）：
```bash
mvn spring-boot:run
# 或
java -jar target/backend-*.jar
```
- 运行测试：
```bash
mvn test
mvn -Dtest=com.dati.BackendApplicationTests test
mvn -Dtest=com.dati.BackendApplicationTests#contextLoads test
```

## 运行配置

- 端口：`8085`
- 默认 Profile：`dev`（见 `src/main/resources/application.yaml`）
- Dev 配置：`application-dev.yaml`
  - 数据库：H2 文件库，JDBC URL `jdbc:h2:file:./db/dataconnai;QUERY_TIMEOUT=30`
  - H2 Console：`/h2-console/semantic`
  - JSON 命名：`SNAKE_CASE`
  - JPA：`spring.jpa.hibernate.ddl-auto=update`

## 数据库与迁移

- 开发环境使用 H2 文件库，位于仓库根目录 `./db/dataconnai.*`
- 如需清空数据，删除上述文件后重启应用
- Flyway 迁移脚本目录：`src/main/resources/db/migration`（命名示例：`V1__init.sql`）
- 生产环境建议使用受控迁移（Flyway），避免 `ddl-auto=update`

## 主要包结构（示例）

- `com.dati.datasource.*`：数据源连接、探查与 SQL 执行（`DataSourceController` 等）
- `com.dati.semantic.*`：语义检索与实体（`SemanticSearchDocument`、`EntityReference` 等）
- `com.dati.base.*`：异常、分页等基础设施（`DciException`、`IdResponse`、`PageResponse`）
- `com.dati.db.*`：数据库工具（`DbClientFactory`、`JdbcUtils` 等）

## API 示例

数据源管理相关：
- `POST /v1/data-sources/test-connection`：测试连接
- `POST /v1/data-sources`：新增数据源
- `PUT /v1/data-sources/{id}`：更新数据源
- `DELETE /v1/data-sources/{id}`：删除数据源
- `GET /v1/data-sources`：分页列出数据源
- `GET /v1/data-sources/{id}/catalogs`：获取 catalogs
- `GET /v1/data-sources/{id}/schemas?catalog=...`：获取 schemas
- `GET /v1/data-sources/{id}/schemas/{schema}/tables?catalog=...`：获取表
- `GET /v1/data-sources/{id}/schemas/{schema}/tables/{table}/columns?catalog=...`：获取列
- `POST /v1/data-sources/{id}/execute-sql`：执行 SQL

更多接口与契约请见 [../api/README.md](../api/README.md)

## 开发约定

- 异常：优先使用 `DciException` 等领域/基础异常封装
- JSON：遵循 dev 下 `SNAKE_CASE` 命名策略
- Lombok：已启用，请确保 IDE 注解处理已打开
- CORS：如需跨域，按控制器或全局配置开启

## 故障排查

- 连接失败：检查驱动与 JDBC URL；使用 `POST /v1/data-sources/test-connection` 自检
- H2 Console 无法访问：确认应用运行，访问路径 `/h2-console/semantic`
- 表结构不一致：dev 下 `ddl-auto=update` 会尝试自动演进；如不生效，考虑补充 Flyway 脚本
