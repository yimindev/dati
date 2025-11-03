# DatI - AI 数据接入基础设施

DatI 是一个为 AI 大模型提供**统一数据接入能力**的基础设施平台。它能帮助你：

- 🔌 **快速接入多种数据源**：支持主流数据库、API、文件系统等多种数据源
- 🤖 **灵活构建 MCP 服务**：自动生成符合 [Model Context Protocol](https://modelcontextprotocol.io/) 标准的服务接口
- 🚀 **即插即用**：让 AI 大模型（如 Claude、GPT 等）能够无缝访问你的业务数据


## 适用场景

- 🏢 **NL2SQL应用构建**：接入业务数据库，无需配置即可进行元数据查询，SQL执行；
- 📊 **数据分析助手**：通过配置元数据、术语，为Agent提供业务数据分析能力
- 🛠️ **快速MCP服务**：通过自定义SQL，可以快速构建数据查询MCP服务

## 技术栈

- **后端**：Spring Boot 3.5.x + Java 21 + JPA + Flyway
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + TailwindCSS
- **数据库**：H2（开发）/ MySQL / PostgreSQL（生产）

## 快速开始（Quick Start）

本地快速运行（开发环境）：

- 后端（Dev Profile，端口 8085）：
  ```bash
  cd backend
  mvn -B -DskipTests package
  mvn spring-boot:run
  # 或：java -jar target/backend-*.jar （默认激活 dev 配置）
  ```
  - Dev 数据库：H2 文件库，路径 `./db/dataconnai`（相对项目根目录）
  - H2 Console：运行后可访问 `/h2-console/semantic`

- 前端（Vite Dev Server）：
  ```bash
  cd frontend
  npm install
  npm run dev
  ```

更多细节请参考：
- 文档导航（强烈推荐从这里开始）：[docs/overview.md](docs/overview.md)
- 本地开发指引：[docs/development/README.md](docs/development/README.md)
- 构建与发布：[docs/deployment/README.md](docs/deployment/README.md)


## 常用命令速查

- 后端测试：
  ```bash
  cd backend && mvn test
  # 只跑一个类
  mvn -Dtest=com.dati.BackendApplicationTests test
  # 只跑一个方法
  mvn -Dtest=com.dati.BackendApplicationTests#contextLoads test
  ```
- 前端构建与预览：
  ```bash
  cd frontend
  npm run build    # 先 vue-tsc 再 vite build
  npm run preview  # 预览生产构建
  ```

## 文档结构（Docs Skeleton）

本仓库已搭建基础文档骨架，建议按照下列入口阅读：

- 项目综述与路线图：[docs/overview.md](docs/overview.md)
- 快速上手（开发环境）：[docs/getting-started.md](docs/getting-started.md)
- 架构与设计：
  - 总览：[docs/architecture/overview.md](docs/architecture/overview.md)
  - 设计决策记录（ADR）：[docs/adr/0001-record-architecture-decisions.md](docs/adr/0001-record-architecture-decisions.md)
- 模块文档：
  - 后端：[docs/backend/README.md](docs/backend/README.md)
  - 前端：[docs/frontend/README.md](docs/frontend/README.md)
- 开发流程与规范：[docs/development/README.md](docs/development/README.md)

## 约定与注意事项

- Dev 环境默认 `spring.profiles.active=dev`，端口 `8085`。
- Dev JSON 命名策略：`SNAKE_CASE`，请在 DTO 与前端接口中统一。
- JPA 在 dev 使用 `ddl-auto=update`，修改表结构需谨慎；如需可重复迁移，请使用 Flyway（`backend/src/main/resources/db/migration`）。
- 若需清空本地 H2 数据，删除项目根目录下 `./db/dataconnai.*` 后重启后端。
