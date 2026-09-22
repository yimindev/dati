# 部署手册（Docker Compose）

[English](deployment.md) | [简体中文](deployment_zh.md)

本文档说明 DatI 单机容器化部署方案及运维注意事项。

---

## 架构组成

Docker Compose 由两个容器服务组成，默认硬件预算适配 2C4G 规格（峰值约 2.5GB 内存）：

1. **`dati-app`（业务应用容器）**
   - **前后端一体化镜像**：通过多阶段 `Dockerfile` 本地构建，将前端（Vue 3 控制台）与帮助中心（VitePress）编译后的静态资源直接打入后端 Spring Boot Fat JAR，单容器运行并监听 `8085` 端口。
   - **数据持久化**：默认使用嵌入式 H2 数据库，数据持久化于 `dati_data` 卷（亦可在 `.env` 中指定外接 MySQL/PostgreSQL）。
   - **资源预算**：内存上限默认 1.5GB（JVM 堆内存自适应约 75%）。

2. **`elasticsearch`（语义检索容器）**
   - **版本与插件**：使用 `davyinsa/elasticsearch-ik:8.18.7` 镜像（预置 IK 中文分词器），与后端 ES 8 Java Client 协议对齐，监听 `9200` 端口。
   - **数据持久化**：语义索引数据持久化于 `es_data` 卷。
   - **资源预算**：内存上限默认 1GB，JVM 堆内存固定 `-Xms512m -Xmx512m`。

---

## 快速部署

进入项目根目录：
```bash
cd dati
```

### 1. 配置环境变量
```bash
cp .env.example .env
```
编辑 `.env` 补充必填密钥：
- `JWT_SECRET`：Token 签名密钥（不少于 32 字符的随机字符串）。
- `SPRING_ELASTICSEARCH_PASSWORD`：Elasticsearch 访问密码。

### 2. 构建并启动服务
```bash
docker compose up -d --build
```

### 3. 验证与首次注册
- **健康检查**：访问 `http://<host>:8085/actuator/health`，响应 `{"status":"UP"}` 即表示应用与 ES 均已就绪。
- **管理员注册**：浏览器访问 `http://<host>:8085`。系统**无预置账号**，首个注册用户须使用用户名 `admin`（需与 `.env` 中 `ADMIN_USERS` 一致），系统会自动为其赋予超级管理员权限。

---

## 核心配置与注意点

1. **ES 密码生效机制（首次建卷固化）**
   - ES 密码仅在首次初始化 `es_data` 数据卷时生效。
   - 若后续在 `.env` 中修改了 `SPRING_ELASTICSEARCH_PASSWORD`，需销毁并重建 ES 数据卷：
     ```bash
     docker compose down
     docker volume rm dati_es_data
     docker compose up -d
     ```
     （语义索引可通过系统内元数据重新同步生成，存储在 H2/MySQL 中的业务配置不受影响）。

2. **外接数据库配置**
   - 默认使用内置 H2 数据库（持久化于 `dati_data` 卷）。
   - 若连接外部 MySQL / RDS，在 `.env` 中配置：
     ```env
     SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/dati?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
     SPRING_DATASOURCE_USERNAME=root
     SPRING_DATASOURCE_PASSWORD=your_password
     ```
     启动时将自动维护表结构（`ddl-auto=update`）。若使用云数据库，须将宿主机出口 IP 加入数据库白名单。

3. **公网访问控制**
   - 系统注册接口（`/v1/auth/register`）面向客户端开放。若部署于公网且需限制注册，应在反向代理或防火墙层面限制来源 IP。

---

## 常用运维命令

| 操作 | 命令 | 说明 |
|------|------|------|
| 更新升级 | `git pull && docker compose up -d --build` | 本地重新构建镜像并重启，已有数据卷保留 |
| 查看应用日志 | `docker compose logs -f dati-app` | 查看业务与接口调用日志 |
| 查看 ES 日志 | `docker compose logs -f elasticsearch` | 查看 ES 运行与索引日志 |
| 重启服务 | `docker compose restart` | 容器重启，数据保持不变 |
| 彻底重置 | `docker compose down -v` | 销毁容器与数据卷（清空全部数据，慎用） |

---

## 常见排障

| 现象 | 处理方式 |
|------|----------|
| 启动报 `must be set in .env` | 检查 `.env` 文件是否存在，且 `JWT_SECRET`、`SPRING_ELASTICSEARCH_PASSWORD` 均已填入有效值。 |
| 应用日志报 ES 401 | 检查是否曾改动过 ES 密码；若在已有卷上改过密码，需按前文说明重建 `dati_es_data` 卷。 |
| `/actuator/health` 报 DOWN | 执行 `docker compose logs dati-app` 查看具体是 `db` 还是 `elasticsearch` 组件连接异常。 |
| 容器发生 OOM 异常退出 | 宿主机可用内存须保持 2.5GB 以上；低配主机建议配置 1~2GB Swap 交换空间。 |
