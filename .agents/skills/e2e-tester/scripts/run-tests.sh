#!/bin/bash
# E2E 测试调度器
#
# 用法:
#   ./run-tests.sh                        # 全部模块
#   ./run-tests.sh datasource subject     # 指定模块
#   ./run-tests.sh --no-stop datasource   # 跑完不关服务
#
# 输出:
#   每个模块的测试报告直接打印到 stdout

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "用法: run-tests.sh [--no-stop] [--filter <条件>] <module>..."
    echo ""
    echo "  不带参数      全部模块"
    echo "  --no-stop     跑完不关服务（调试用）"
    echo "  --filter      施加于后续模块（可多次使用，如 --filter A datasource --filter B subject）"
    echo ""
    echo "示例:"
    echo "  run-tests.sh datasource                      # 全部用例"
    echo "  run-tests.sh --filter P0 datasource           # 只跑 P0 级别"
    echo "  run-tests.sh --filter 'TC-012' ds --filter 'TC-001' sm  # 不同模块不同筛选"
    echo ""
    echo "环境变量:"
    echo "  E2E_MODEL     子进程模型（覆盖 test-env.yaml runner.model）"
    exit 0
fi

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_DIR="$(cd "$SKILL_DIR/../../.." && pwd)"
ENV_FILE="$PROJECT_DIR/e2e-tests/test-env.yaml"

# 参数
MODULES=()
MODULE_FILTERS=()
NO_STOP=false
PENDING_FILTER=""
# 模型：E2E_MODEL 环境变量 > test-env.yaml runner.model > 硬编码兜底
if [ -n "${E2E_MODEL:-}" ]; then
    MODEL="$E2E_MODEL"
elif command -v yq &>/dev/null; then
    MODEL=$(yq -r '.runner.model // "deepseek-v4-flash"' "$ENV_FILE")
else
    MODEL="deepseek-v4-flash"
fi

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-stop) NO_STOP=true; shift ;;
        --filter) PENDING_FILTER="$2"; shift 2 ;;
        *) MODULES+=("$1"); MODULE_FILTERS+=("$PENDING_FILTER"); shift ;;
    esac
done

# 默认全部模块（test-cases 下的 *.md 去掉 .md）
if [ ${#MODULES[@]} -eq 0 ]; then
    MODULES=()
    MODULE_FILTERS=()
    for f in "$PROJECT_DIR/e2e-tests/test-cases"/*.md; do
        name=$(basename "$f" .md)
        MODULES+=("$name")
        MODULE_FILTERS+=("")
    done
fi

echo "========================================"
echo "  DatI E2E Test Runner"
echo "  模块: ${MODULES[*]}"
echo "  模型: ${MODEL}"
echo "========================================"

# 加载服务管理函数
source "$SKILL_DIR/scripts/service.sh"

# 1. 启动服务
start_service

# 2. 清理旧临时文件
rm -f /tmp/e2e-*.log

# 3. 并行启动子进程
PIDS=()

for i in "${!MODULES[@]}"; do
    module="${MODULES[$i]}"
    filter="${MODULE_FILTERS[$i]}"
    case_file="$PROJECT_DIR/e2e-tests/test-cases/${module}.md"
    if [ ! -f "$case_file" ]; then
        echo "[runner] 跳过 $module：用例文件不存在" >&2
        continue
    fi

    if [ -n "$filter" ]; then
        prompt="执行 ${module} 模块的 E2E 测试。服务已就绪（${BASE_URL}），${filter}。用例定义在 e2e-tests/test-cases/${module}.md 中，完成后输出测试报告。报告末尾用自然语言给出执行情况总计（共 X 个用例，通过 X 个，失败 X 个）。"
    else
        prompt="执行 ${module} 模块的 E2E 测试。服务已就绪（${BASE_URL}），按 e2e-tests/test-cases/${module}.md 中的用例逐个执行，完成后输出测试报告。报告末尾用自然语言给出执行情况总计（共 X 个用例，通过 X 个，失败 X 个）。"
    fi
    log="/tmp/e2e-${module}.log"

    echo "[runner] 启动 e2e-${module} ..."
    pi -p --name "e2e-${module}" --model "$MODEL" "$prompt" \
        < /dev/null > "$log" 2>&1 &
    PIDS+=("$!")
done

echo "[runner] 等待 ${#PIDS[@]} 个子进程完成..."

# 4. 等待全部完成
for pid in "${PIDS[@]}"; do
    wait "$pid" 2>/dev/null
done

echo "[runner] 全部完成"
echo ""

# 5. 输出每个模块的结果
for module in "${MODULES[@]}"; do
    log="/tmp/e2e-${module}.log"

    echo "========== $module =========="

    if [ ! -f "$log" ] || [ ! -s "$log" ]; then
        echo "⚠ 无输出（子进程可能启动失败）"
    else
        cat "$log"
    fi

    echo ""
done

# 6. 停止服务（除非 --no-stop）
if [ "$NO_STOP" = false ]; then
    stop_service
else
    echo "[runner] --no-stop，服务保持运行"
fi
