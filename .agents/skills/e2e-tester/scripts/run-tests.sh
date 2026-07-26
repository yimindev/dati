#!/bin/bash
# E2E 测试调度器
#
# 用法:
#   ./run-tests.sh                        # 全部模块
#   ./run-tests.sh datasource subject     # 指定模块
#   ./run-tests.sh --no-stop datasource   # 跑完不关服务
#
# 输出:
#   报告写入 e2e-tests/reports/<module>-<date>.md
#   汇总打印到 stdout

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "用法: run-tests.sh [--no-stop] [module...]"
    echo ""
    echo "  不带参数      全部模块"
    echo "  module...     指定模块（如 datasource subject）"
    echo "  --no-stop     跑完不关服务（调试用）"
    echo ""
    echo "环境变量:"
    echo "  E2E_MODEL     子进程模型（覆盖 test-env.yaml runner.model）"
    exit 0
fi

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_DIR="$(cd "$SKILL_DIR/../../.." && pwd)"
REPORT_DIR="$PROJECT_DIR/e2e-tests/reports"
ENV_FILE="$PROJECT_DIR/e2e-tests/test-env.yaml"

# 参数
MODULES=()
NO_STOP=false
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
        *) MODULES+=("$1"); shift ;;
    esac
done

# 默认全部模块（test-cases 下的 *.md 去掉 .md）
if [ ${#MODULES[@]} -eq 0 ]; then
    MODULES=()
    for f in "$PROJECT_DIR/e2e-tests/test-cases"/*.md; do
        name=$(basename "$f" .md)
        MODULES+=("$name")
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

# 2. 清理旧临时文件，创建报告目录
rm -f /tmp/e2e-*.log
mkdir -p "$REPORT_DIR"

# 3. 并行启动子进程
DATE=$(date +%Y-%m-%d)
PIDS=()

for module in "${MODULES[@]}"; do
    case_file="$PROJECT_DIR/e2e-tests/test-cases/${module}.md"
    if [ ! -f "$case_file" ]; then
        echo "[runner] 跳过 $module：用例文件不存在" >&2
        continue
    fi

    prompt="执行 ${module} 模块的 E2E 测试。服务已就绪（${BASE_URL}），按 e2e-tests/test-cases/${module}.md 中的用例逐个执行，完成后输出测试报告。"
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

# 5. 汇总输出
PASS_COUNT=0
FAIL_COUNT=0
ALL_REPORTS=""

for module in "${MODULES[@]}"; do
    log="/tmp/e2e-${module}.log"
    report="$REPORT_DIR/${module}-${DATE}.md"

    echo "--- $module ---"

    if [ ! -f "$log" ] || [ ! -s "$log" ]; then
        echo "  ⚠ 无输出（子进程可能启动失败）"
        continue
    fi

    # 提取「报告」之后的内容写入 report 文件
    awk '/^## E2E Test Report|^# E2E Test Report/{found=1} found{print}' "$log" > "$report"

    if [ -s "$report" ]; then
        cat "$report"
        echo ""
        ALL_REPORTS+=$'\n'"$(cat "$report")"
    else
        echo "  ⚠ 未生成报告（查看原始日志: $log）"
        tail -5 "$log" | sed 's/^/  /'
    fi

    # 统计 PASS/FAIL
    if grep -q 'FAIL' "$log" 2>/dev/null; then
        FAIL_COUNT=$((FAIL_COUNT + 1))
    else
        PASS_COUNT=$((PASS_COUNT + 1))
    fi

    echo ""
done

# 6. 汇总
echo "========================================"
echo "  汇总: ${PASS_COUNT} 通过, ${FAIL_COUNT} 失败"
echo "  报告目录: $REPORT_DIR"
echo "========================================"

# 7. 停止服务（除非 --no-stop）
if [ "$NO_STOP" = false ]; then
    stop_service
else
    echo "[runner] --no-stop，服务保持运行"
fi

# 8. 退出码
[ "$FAIL_COUNT" -eq 0 ] && exit 0 || exit 1
