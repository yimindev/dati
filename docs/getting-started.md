# 快速上手（Getting Started）

本文档帮助你在本地快速运行 Data Conn AI（Dati）项目的后端和前端。

## 先决条件
- Java 21
- Maven 3.9+
- Node.js 20+
- pnpm 10+（可选，项目当前使用 npm 脚本；如需 pnpm，请自行替换命令）

## 克隆仓库
```bash
git clone <your-repo-url> data-conn-ai
cd data-conn-ai
```

## 启动后端（开发环境）
- 默认端口：`8085`
- 默认 Profile：`dev`

```bash
cd backend
mvn -B -DskipTests package
mvn spring-boot:run
# 或者
# java -jar target/backend-*.jar
```

- H2 文件库位置：`./db/dataconnai`（相对仓库根目录）
- H2 Console：服务启动后访问 `http://localhost:8085/h2-console/semantic`
- JPA DDL 策略（dev）：`spring.jpa.hibernate.ddl-auto=update`（修改表结构需谨慎）

## 启动前端（开发环境）
```bash
cd frontend
npm install
npm run dev
```

- 生产构建与预览：
```bash
npm run build
npm run preview
```

## 运行测试
- 后端（JUnit 5 via Maven Surefire）：
```bash
cd backend && mvn test
# 指定测试类
mvn -Dtest=com.dati.BackendApplicationTests test
# 指定测试方法
mvn -Dtest=com.dati.BackendApplicationTests#contextLoads test
```

- 前端：尚未配置测试框架。如需单元测试，建议引入 Vitest；参考 [docs/testing/README.md](testing/README.md)。

## 常见问题（FAQ）
- 本地数据清空：删除根目录 `./db/dataconnai.*` 后重启后端。
- CORS 与端口：后端默认 8085，如新增跨域接口，请按需配置 CORS。
- JSON 命名策略：dev 环境为 `SNAKE_CASE`，请在 DTO 与前端接口中统一。
