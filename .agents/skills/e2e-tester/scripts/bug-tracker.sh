#!/bin/bash
# Bug 生命周期管理
# 用法: ./bug-tracker.sh <command> [args...]
#
# 命令:
#   add      "<title>" "<module>" <severity> "<description>"  → 追加到 BUGS.yaml
#   fix      <BUG-ID> [fixed_at_date]                          → 从 BUGS.yaml 移到 BUGS-FIXED.yaml
#   regress  <BUG-ID>                                          → 从 BUGS-FIXED.yaml 移回 BUGS.yaml (标记回归)
#   list                                                        → 列出 BUGS.yaml 中所有待修复问题
#   list-fixed                                                  → 列出 BUGS-FIXED.yaml 中所有已修复问题
#   next-id                                                     → 打印下一个可用 BUG ID

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PROJECT_DIR="$(cd "$SKILL_DIR/../../.." && pwd)"
BUGS_FILE="$PROJECT_DIR/e2e-tests/BUGS.yaml"
FIXED_FILE="$PROJECT_DIR/e2e-tests/BUGS-FIXED.yaml"

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "用法: bug-tracker.sh <command> [args...]"
    echo ""
    echo "命令:"
    echo "  add         \"标题\" \"模块\" <severity> \"描述\"   新增 Bug"
    echo "  fix         <BUG-ID> [日期]                     标记已修复"
    echo "  regress     <BUG-ID>                            标记回归"
    echo "  suspend     <BUG-ID>                            挂起（暂不处理）"
    echo "  unsuspend   <BUG-ID>                            取消挂起"
    echo "  list        [--all]                             列出待修复（默认隐藏挂起）"
    echo "  list-suspended                                 列出挂起"
    echo "  list-fixed                                     列出已修复"
    echo "  next-id                                        打印下一个可用 ID"
    exit 0
fi

die() { echo "ERROR: $*" >&2; exit 1; }
export LC_ALL=C

# === 辅助函数 ===

# 获取今天日期下一条序号（同一天内递增）
next_seq_for_date() {
    local date="${1:-$(date +%Y%m%d)}"
    local max_seq=0
    local num
    for f in "$BUGS_FILE" "$FIXED_FILE"; do
        [ -f "$f" ] || continue
        while read -r num; do
            [ -n "$num" ] && [ "$num" -gt "$max_seq" ] && max_seq="$num"
        done <<< "$(grep -h "id: BUG-${date}-" "$f" 2>/dev/null | \
                     sed 's/.*id: BUG-[0-9]*-//' | sed 's/[^0-9].*//' | grep -v '^$')"
    done
    echo "$((10#${max_seq} + 1))"
}

# 获取下一个可用 ID（按日期：BUG-YYYYMMDD-NNN）
next_id() {
    local date
    date=$(date +%Y%m%d)
    local seq
    seq=$(next_seq_for_date "$date")
    printf "BUG-%s-%03d\n" "$date" "$seq"
}

# 检查 bug 是否存在于指定文件
exists_in() {
    local id="$1" file="$2"
    grep -q "id: $id" "$file" 2>/dev/null
}

# === 命令实现 ===

cmd_add() {
    local title="$1" module="$2" severity="$3" description="$4"
    local id found_at
    id=$(next_id)
    found_at=$(date +%Y-%m-%d)

    [ -z "$title" ] && die "用法: add \"标题\" \"模块\" severity \"描述\""
    [ -z "$severity" ] && severity="medium"

    # 确保 YAML 文件存在
    [ -f "$BUGS_FILE" ] || echo -e "# 待修复问题清单\n# Agent 跑完 E2E 测试后自动维护此文件\n\nbugs:" > "$BUGS_FILE"

    cat >> "$BUGS_FILE" <<EOF
  - id: $id
    title: $title
    module: $module
    severity: $severity
    found_at: $found_at
    description: |
      $description
EOF
    echo "$id"
}

cmd_fix() {
    local id="$1" date="${2:-$(date +%Y-%m-%d)}"

    [ -z "$id" ] && die "用法: fix <BUG-ID> [日期]"

    exists_in "$id" "$BUGS_FILE" || die "$id 不在 BUGS.yaml 中"

    # 从 BUGS.yaml 提取该条目
    local entry
    entry=$(awk -v id="$id" '
        $0 ~ "id: " id { found=1; print; next }
        found { print; if ($0 ~ /^  - id:|^bugs:/) found=0 }
    ' "$BUGS_FILE")

    # 从 BUGS.yaml 中删除
    local tmp
    tmp=$(mktemp)
    awk -v id="$id" '
        $0 ~ "id: " id { skip=1; next }
        !skip { print }
        skip && /^  - id:|^bugs:/ { skip=0; print }
    ' "$BUGS_FILE" > "$tmp" && mv "$tmp" "$BUGS_FILE"

    # 追加到 BUGS-FIXED.yaml（去掉 severity 相关字段，加上 fixed_at）
    [ -f "$FIXED_FILE" ] || echo -e "# 已修复问题存档\nbugs:" > "$FIXED_FILE"

    # 提取 title、module、description，替换为 fixed 格式
    local t m d s
    t=$(echo "$entry" | grep "title:" | sed 's/.*title: //')
    m=$(echo "$entry" | grep "module:" | sed 's/.*module: //')
    s=$(echo "$entry" | grep "severity:" | sed 's/.*severity: //')
    d=$(echo "$entry" | sed -n '/description: |/,$p' | tail -n +2 | sed 's/^      //' | sed 's/^/      /')

    cat >> "$FIXED_FILE" <<EOF
  - id: $id
    title: $t
    module: $m
    severity: $s
    fixed_at: $date
    description: |
$d
EOF
    echo "$id → BUGS-FIXED.yaml"
}

cmd_regress() {
    local id="$1"

    [ -z "$id" ] && die "用法: regress <BUG-ID>"

    exists_in "$id" "$FIXED_FILE" || die "$id 不在 BUGS-FIXED.yaml 中"

    # 从 FIXED 移到 BUGS，标记回归
    local entry
    entry=$(awk -v id="$id" '
        $0 ~ "id: " id { found=1 }
        found { print; if ($0 ~ /^  - id:|^bugs:/) found=0 }
    ' "$FIXED_FILE")

    # 从 FIXED 删除
    local tmp
    tmp=$(mktemp)
    awk -v id="$id" '
        $0 ~ "id: " id { skip=1; next }
        !skip { print }
        skip && /^  - id:|^bugs:/ { skip=0; print }
    ' "$FIXED_FILE" > "$tmp" && mv "$tmp" "$FIXED_FILE"

    # 提取信息 + 追加到 BUGS.yaml，加 regression 标记
    local t m s d
    t=$(echo "$entry" | grep "title:" | sed 's/.*title: //')
    m=$(echo "$entry" | grep "module:" | sed 's/.*module: //')
    s=$(echo "$entry" | grep "severity:" | sed 's/.*severity: //')
    d=$(echo "$entry" | sed -n '/description: |/,$p' | tail -n +2 | sed 's/^      //' | sed 's/^/      /')
    found_at=$(date +%Y-%m-%d)

    cat >> "$BUGS_FILE" <<EOF
  - id: $id
    title: $t（回归）
    module: $m
    severity: $s
    found_at: $found_at
    regression: true
    description: |
${d}      曾标记为 FIXED，本次测试复现。
EOF
    echo "$id → BUGS.yaml (REGRESSION)"
}

cmd_list() {
    if [ ! -f "$BUGS_FILE" ]; then
        echo "（无待修复问题）"
        return
    fi
    local show_all=false
    [[ "$1" == "--all" ]] && show_all=true
    local label="待修复"
    $show_all && label="待修复（含挂起）"
    echo "=== $label ($BUGS_FILE) ==="
    grep "^  - id:" "$BUGS_FILE" | while read -r line; do
        id=$(echo "$line" | sed 's/.*id: //' | sed 's/[[:space:]].*//')
        # 检查是否挂起
        local suspended
        suspended=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /^  - id:/ { exit } found && /status: suspended/ { print "1"; exit }' "$BUGS_FILE")
        # 默认隐藏挂起
        if [ "$show_all" = false ] && [ -n "$suspended" ]; then
            continue
        fi
        local title sev reg mark
        title=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /title:/ { print; exit }' "$BUGS_FILE" | sed 's/.*title: //')
        sev=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /severity:/ { print; exit }' "$BUGS_FILE" | sed 's/.*severity: //')
        reg=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /regression:/ { print; exit }' "$BUGS_FILE")
        [ -n "$reg" ] && mark="🔄" || mark="  "
        [ -n "$suspended" ] && mark="⏸"
        printf "  %s %s [%s] %s\n" "$mark" "$id" "$sev" "$title"
    done
}

cmd_list_suspended() {
    if [ ! -f "$BUGS_FILE" ]; then
        echo "（无挂起问题）"
        return
    fi
    echo "=== 挂起 ($BUGS_FILE) ==="
    grep "^  - id:" "$BUGS_FILE" | while read -r line; do
        id=$(echo "$line" | sed 's/.*id: //' | sed 's/[[:space:]].*//')
        local suspended
        suspended=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /^  - id:/ { exit } found && /status: suspended/ { print "1"; exit }' "$BUGS_FILE")
        [ -z "$suspended" ] && continue
        local title sev
        title=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /title:/ { print; exit }' "$BUGS_FILE" | sed 's/.*title: //')
        sev=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /severity:/ { print; exit }' "$BUGS_FILE" | sed 's/.*severity: //')
        printf "  ⏸ %s [%s] %s\n" "$id" "$sev" "$title"
    done
}

cmd_list_fixed() {
    if [ ! -f "$FIXED_FILE" ]; then
        echo "（无已修复问题）"
        return
    fi
    echo "=== 已修复 ($FIXED_FILE) ==="
    grep "^  - id:" "$FIXED_FILE" | while read -r line; do
        id=$(echo "$line" | sed 's/.*id: //' | sed 's/[[:space:]].*//')
        title=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /title:/ { print; exit }' "$FIXED_FILE" | sed 's/.*title: //')
        fixed=$(awk -v id="$id" '$0 ~ "id: " id { found=1; next } found && /fixed_at:/ { print; exit }' "$FIXED_FILE" | sed 's/.*fixed_at: //')
        printf "  ✓ %s [%s] %s\n" "$id" "$fixed" "$title"
    done
}

cmd_next_id() {
    next_id
}

cmd_suspend() {
    local id="$1"
    [ -z "$id" ] && die "用法: suspend <BUG-ID>"
    exists_in "$id" "$BUGS_FILE" || die "$id 不在 BUGS.yaml 中"
    # 在 id 行后插入 status: suspended
    sed -i '' "/id: $id/a\\
    status: suspended" "$BUGS_FILE"
    echo "$id → 挂起"
}

cmd_unsuspend() {
    local id="$1"
    [ -z "$id" ] && die "用法: unsuspend <BUG-ID>"
    exists_in "$id" "$BUGS_FILE" || die "$id 不在 BUGS.yaml 中"
    sed -i '' "/id: $id/,/^  - id:|^bugs:/ { /status: suspended/d; }" "$BUGS_FILE"
    echo "$id → 已取消挂起"
}

# === 主入口 ===
CMD="${1:-list}"
shift 2>/dev/null || true

case "$CMD" in
    add)           cmd_add "$@" ;;
    fix)           cmd_fix "$@" ;;
    regress)       cmd_regress "$@" ;;
    suspend)       cmd_suspend "$@" ;;
    unsuspend)     cmd_unsuspend "$@" ;;
    list)          cmd_list "$@" ;;
    list-suspended) cmd_list_suspended ;;
    list-fixed)    cmd_list_fixed ;;
    next-id)       cmd_next_id ;;
    *)             echo "用法: $0 {add|fix|regress|suspend|unsuspend|list|list-suspended|list-fixed|next-id} [args...]" >&2; exit 1 ;;
esac
