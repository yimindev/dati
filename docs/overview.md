# 文档总览（Overview）

本目录收录 Data Conn AI（Dati）项目的主要文档入口。建议第一次接触项目的同学按如下顺序阅读：

1. 快速上手（开发环境）——搭建本地环境并启动服务：
   - [getting-started.md](getting-started.md)
2. 架构与设计概览——理解模块边界、关键技术栈与重要约定：
   - [architecture/overview.md](architecture/overview.md)
3. 模块文档——深入后端与前端：
   - [backend/README.md](backend/README.md)
   - [frontend/README.md](frontend/README.md)
4. 接口与数据（API、数据库）：
   - [api/README.md](api/README.md)
   - [database/README.md](database/README.md)
5. 开发与测试指南：
   - [development/README.md](development/README.md)
   - [testing/README.md](testing/README.md)
6. 部署与运维：
   - [deployment/README.md](deployment/README.md)
7. 团队协作：
   - [contributing.md](contributing.md)
   - [changelog.md](changelog.md)
   - [roadmap.md](roadmap.md)
   - ADR（重要架构决策记录）：[adr/0001-record-architecture-decisions.md](adr/0001-record-architecture-decisions.md)

## 项目要点

- 仓库根目录：`data-conn-ai`
- 模块：
  - `backend`: Spring Boot 3.5.x（Java 21）、Maven、JPA/JDBC、Flyway、H2/MySQL/PostgreSQL 驱动
  - `frontend`: Vue 3 + Vite + TypeScript、TailwindCSS、Element Plus
- 工具链基线：Java 21、Maven 3.9+、Node.js 20+、pnpm 10+

## 常用链接

- 根 README（入口）：../README.md
- 代码风格与异常处理约定：见 [development/README.md](development/README.md)
- 数据库/H2 控制台：后端启动后访问 `/h2-console/semantic`
