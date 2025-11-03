# 数据库与迁移（Database）

本项目在开发环境使用 H2 文件库；生产或其他环境可切换至 MySQL/PostgreSQL 等。

## 开发环境（dev）
- JDBC URL：`jdbc:h2:file:./db/dataconnai;QUERY_TIMEOUT=30`
- 数据文件：仓库根目录下 `./db/dataconnai.*`
- 控制台：应用运行后访问 `http://localhost:8085/h2-console/semantic`
- JPA DDL：`spring.jpa.hibernate.ddl-auto=update`（仅限 dev 使用）

## 迁移（Flyway）
- 目录：`backend/src/main/resources/db/migration`
- 命名规范：`V{版本号}__{描述}.sql`（例如：`V1__init.sql`）
- 推荐流程：
  1. 在本地基于 JPA 演进表结构并验证
  2. 抽取变更为 Flyway SQL 脚本
  3. 在测试/预发环境验证脚本
  4. 生产环境使用 Flyway 受控迁移

## 切换其他数据库
- 新增 profile（如 `application-mysql.yaml`），设置 `spring.datasource.*`
- 启动时指定 profile：
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=mysql
  # 或
  java -jar target/backend-*.jar --spring.profiles.active=mysql
  ```

## 调试与诊断
- 连接超时/失败：检查驱动依赖与连接串；可使用 `/v1/data-sources/test-connection` 进行联通性测试
- 元数据探查：利用现有接口获取 catalogs/schemas/tables/columns，快速诊断权限与可见性
