# 本地开发指南（Development）

本文档覆盖本地环境的准备、启动、常用命令与开发约定，适用于后端（Java / Spring Boot）与前端（Vue 3 + TypeScript）。

## 先决条件

- Java 21
- Maven 3.9+
- Node.js 20+
- pnpm 10+

## 启动后端（端口 8085）

```bash
cd backend
mvn -B -DskipTests package
mvn spring-boot:run
# 或：java -jar target/backend-*.jar（默认激活 dev 配置）
```

- Dev 数据库：H2 文件库，路径 `./db/dataconnai`（相对项目根目录）
- H2 Console：运行后可访问 `/h2-console/semantic`

## 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

## 常用命令

### 后端测试

```bash
cd backend && mvn test
mvn -Dtest=ClassName test                    # 只跑一个类
mvn -Dtest=ClassName#MethodName test         # 只跑一个方法
```

### 前端构建与测试

```bash
cd frontend
pnpm build        # 先 vue-tsc 类型检查再 vite build
pnpm preview      # 预览生产构建
pnpm test         # Vitest 单元测试
pnpm test:watch   # 持续运行
pnpm docs:dev     # 本地运行帮助中心（VitePress，端口 5174）
```

## 开发约定

- Dev 环境默认 `spring.profiles.active=dev`，端口 `8085`。
- Dev JSON 命名策略：`SNAKE_CASE`，请在 DTO 与前端接口中统一。
- JPA 在 dev 使用 `ddl-auto=update`，修改表结构需谨慎；如需可重复迁移，请使用 Flyway。
- 如需跨域，在后端按控制器或全局配置开启 CORS。
- 测试要求：后端采用 TDD（先写失败测试再实现），前端使用 Vitest；详见 [AGENTS.md](../AGENTS.md)。

## 数据库与迁移

- 开发环境使用 H2 文件库，数据文件为仓库根目录下 `./db/dataconnai.*`；清空本地数据只需删除这些文件后重启后端。
- Flyway 迁移脚本目录：`backend/src/main/resources/db/migration`，命名规范 `V{版本号}__{描述}.sql`。
- 生产环境使用 Flyway 受控迁移（不使用 `ddl-auto=update`），推荐流程：
  1. 在本地基于 JPA 演进表结构并验证
  2. 抽取变更为 Flyway SQL 脚本
  3. 在测试/预发环境验证脚本
  4. 生产环境执行 Flyway 迁移

## 切换其他数据库

新增 profile（如 `application-mysql.yaml`）并设置 `spring.datasource.*`：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
# 或
java -jar target/backend-*.jar --spring.profiles.active=mysql
```

## 故障排查

- 数据库连接失败：检查驱动与 JDBC URL，可用 `POST /v1/data-sources/test-connection` 自检
- H2 Console 无法访问：确认应用运行，访问 `/h2-console/semantic`
- 表结构不一致：dev 下 `ddl-auto=update` 会尝试自动演进；如不生效，补充 Flyway 脚本
