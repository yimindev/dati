#!/bin/bash
# E2E 服务生命周期管理
#
# 用法: source scripts/service.sh
#   start_service      启动后端，等待就绪
#   stop_service       停止后端
#   wait_ready         等待服务就绪（不启动）
#
# 环境变量（可选）:
#   BASE_URL    后端地址（默认 http://localhost:8085）
#   WORK_DIR    项目根目录（默认 /Users/zhangyimin/IdeaProjects/dati）
#   LOG_FILE    启动日志路径（默认 /tmp/dati-e2e-service.log）

BASE_URL="${BASE_URL:-http://localhost:8085}"
WORK_DIR="${WORK_DIR:-/Users/zhangyimin/IdeaProjects/dati}"
START_CMD="mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.workingDirectory=${WORK_DIR}"
LOG_FILE="${LOG_FILE:-/tmp/dati-e2e-service.log}"

# 检查后端是否响应
is_alive() {
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL" 2>/dev/null)
    [ "$code" != "000" ]
}

# 等待服务就绪（最多 120 秒）
wait_ready() {
    echo "[service] 等待 $BASE_URL 就绪..." >&2
    for i in $(seq 1 60); do
        if is_alive; then
            echo "[service] 就绪" >&2
            return 0
        fi
        sleep 2
    done
    echo "[service] 超时！" >&2
    return 1
}

# 启动后端服务
start_service() {
    if is_alive; then
        echo "[service] 已在运行，跳过启动" >&2
        return 0
    fi
    echo "[service] 启动后端..." >&2
    cd "$WORK_DIR" || exit 1
    nohup $START_CMD > "$LOG_FILE" 2>&1 &
    echo "[service] PID=$!" >&2
    wait_ready
}

# 停止后端服务
stop_service() {
    echo "[service] 停止..." >&2
    pkill -f "spring-boot:run" 2>/dev/null
    echo "[service] 已停止" >&2
}
