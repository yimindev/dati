# DatI 部署手册（Docker Compose 单机）

最简单、推荐的部署方式：clone 代码后一条命令拉起全栈，镜像本地构建，无需镜像仓库与推送。资源预算默认适配 2C4G（app 1.5g + ES 1g ≈ 2.5G 峰值）。

## 快速开始

前置：Docker Engine + Compose v2。

```bash
git clone <your-repo-url> && cd dati
cp .env.example .env     # 填两个密钥：JWT_SECRET、SPRING_ELASTICSEARCH_PASSWORD
docker compose up -d --build
curl http://localhost:8085/actuator/health    # {"status":"UP"} 即成功
```

浏览器打开 `http://<host>:8085`，注册用户名 `admin`（须与 `.env` 的 `ADMIN_USERS` 一致，默认 admin）——**平台无内置账号，第一个注册的管理员即是你**。

## 注意点（项目相关）

- **ES 版本要求**：原生匹配 ES 8.x 版本；换 7.x 会导致 `/actuator/health` 的 ES 组件解码失败、健康状态卡 DOWN。
- **ES 密码只在首次创建 `es_data` 卷时生效**：改密码后需 `docker compose down && docker volume rm dati_es_data && docker compose up -d`（语义索引可重建，平台数据在 H2/RDS 不受影响）。
- **元数据库默认内置 H2**（落在 `dati_data` 卷，零依赖）。外接 MySQL/RDS：`.env` 配 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`，首次启动自动建表（ddl-auto=update）；云 RDS 记得把主机 IP 加入白名单。
- **健康入口**：`/actuator/health`（含 db/elasticsearch 组件），容器内置 HEALTHCHECK 也用它。
- **开放注册是产品设计**（`/v1/auth/register` 免认证）；不希望公网注册就在安全组限定来源 IP。
- `JWT_SECRET` / `SPRING_ELASTICSEARCH_PASSWORD` 勿提交仓库。

## 升级与重置

| 操作 | 命令 |
|------|------|
| 升级（代码更新后） | `git pull && docker compose up -d --build`（数据卷保留） |
| 看日志 | `docker compose logs -f dati-app` |
| 完全重置（删全部数据） | `docker compose down -v`（慎用） |

数据都在数据卷里：`dati_data`（平台 H2）/ `es_data`（语义索引），备份即备份这两个卷。

## 排障

| 现象 | 处理 |
|------|------|
| 启动报 `must be set in .env` | `.env` 与 docker-compose.yml 不同级或键名拼错 |
| app 日志 ES 401 | 见上「ES 密码首次建卷生效」，删 `dati_es_data` 卷重建 |
| health DOWN | `docker compose logs dati-app` 看 db / elasticsearch 组件报错 |
| 2C4G 内存吃紧 | 保持默认预算；宿主机可加 swap 兜底 |
