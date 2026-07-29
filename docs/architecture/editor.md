# 编辑器架构

## 概述

前端模板编辑器基于 **CodeMirror 6** 构建，提供三种编辑器：**Prompt 模板编辑器**、**SQL 模板编辑器** 和**纯 SQL 编辑器**（用于工具测试）。核心目标：让 US-5.5 定义的模板语法（`{{var}}` / `{{{var}}}` / `{{#if}}` / `{{#where}}`）在编辑器中获得清晰、一致的视觉呈现，且模板高亮**无条件优先于**宿主语言（SQL）的语法高亮。

## 目录结构

```
frontend/src/
├── components/common/editors/
│   ├── PromptTemplateEditor.vue    # Prompt 模板编辑器（纯文本）
│   ├── SqlTemplateEditor.vue       # SQL 模板编辑器（SQL + 模板语法）
│   └── SqlEditor.vue               # 纯 SQL 编辑器（工具测试用，无模板功能）
├── composables/
│   ├── useCodeMirror.ts            # CodeMirror 实例管理 composable
│   └── useEditorFullscreen.ts      # 编辑器全屏切换 composable
├── utils/codemirror/
│   ├── completions/
│   │   ├── template-completions.ts          # 模板补全源（变量 + 块指令）
│   │   ├── template-completions.test.ts     # 补全源单元测试（33 用例）
│   │   ├── template-auto-close.ts           # }} 闭合 → 自动插入 {{/if}}
│   │   └── template-auto-close.test.ts      # 闭合检测单元测试（17 用例）
│   ├── editor-theme.css            # 编辑器外观 + 模板语法颜色
│   ├── sql-highlight.ts            # SQL token 颜色定义（HighlightStyle）
│   ├── template-decorations.ts     # 模板语法正则高亮 + 未闭合错误检测
│   └── template-decorations.test.ts          # 高亮/错误检测单元测试（21 用例）
└── main.ts                         # 全局导入 editor-theme.css
```

## 架构图

```
┌──────────────────────────────────────────────────────────┐
│                    编辑器组件层                            │
│  PromptTemplateEditor.vue  SqlTemplateEditor.vue           │
│  extensions: [              extensions: [                  │
│    autocompletion({           sql()                        │
│      override: [              syntaxHighlighting(...)      │
│        templateCompletions()  autocompletion({             │
│      ]                          override: [                │
│    }),                           templateCompletions()     │
│    templateAutoClose()         ]                           │
│    templateDecorations()     }),                           │
│    lineWrapping              templateAutoClose()           │
│  ]                            templateDecorations()        │
│  props: label, required       bracketMatching()            │
│                              ]                             │
│  SqlEditor.vue               props: label, required        │
│  extensions: [                                              │
│    sql()                     ├─────────────────────────────┤
│    syntaxHighlighting(...)   │ 全屏切换（共享）              │
│  ]                           │ · useEditorFullscreen()      │
│  props: label, required      │ · cm-editor-fullscreen CSS   │
│                              │ · 头部行：[label *] [⛶]     │
│                              │ · capture 拦截 ESC 防弹窗关闭 │
└──────────────┬───────────────────────────────────────────┘
               │ 共享
               ▼
┌──────────────────────────────────────────────────────────┐
│                  useCodeMirror.ts                         │
│  · EditorState.create() → EditorView                     │
│  · v-model 双向绑定（updateListener + watch）              │
│  · 生命周期管理（onMounted / onBeforeUnmount）             │
└──────────────┬───────────────────────────────────────────┘
               │
    ┌──────────┼──────────────┬───────────────────┐
    ▼          ▼              ▼                   ▼
┌────────┐ ┌──────────┐ ┌─────────────┐ ┌──────────────────┐
│ sql-   │ │ template-│ │ completions/│ │ editor-theme.css │
│ highlight│ │decorations│ │             │ │                  │
│ .ts    │ │.ts       │ │ · template-  │ │ · 编辑器外观      │
│        │ │          │ │   completions│ │ · 模板 token 颜色 │
│ · SQL  │ │ · 正则匹配│ │   .ts       │ │ · !important     │
│   token│ │   {{ }}   │ │ · template-  │ │   单向覆盖 SQL    │
│   颜色 │ │ · ViewPlugin│   auto-close │ │                  │
│        │ │ · Decoration│   .ts       │ │                  │
│ · High-│ │   .mark() │ │             │ │                  │
│   light│ │          │ │             │ │                  │
│   Style│ │          │ │             │ │                  │
└────────┘ └──────────┘ └─────────────┘ └──────────────────┘
```

## 模块详解

### 1. `useCodeMirror.ts` — 编辑器实例管理

**职责**：创建并管理一个 CodeMirror 6 编辑器实例，提供 `v-model` 风格的双向绑定。

**接口**：
```typescript
interface UseCodeMirrorOptions {
  modelValue: Ref<string>;   // 绑定值（双向）
  extensions: Extension[];    // CodeMirror 扩展列表
  placeholder?: string;       // 占位文本
}

function useCodeMirror(options: UseCodeMirrorOptions): {
  containerRef: Ref<HTMLElement | null>;  // 挂载容器 ref
  editorView: ShallowRef<EditorView | null>;  // 编辑器实例（内部调试用）
}
```

**数据流**：
```
modelValue (props) ──→ EditorState.create({ doc })  [初始化]
                           │
                   view.dispatch()                  [用户输入]
                           │
                   updateListener → modelValue      [回写父组件]
                           
modelValue (外部变更) ──→ watch → view.dispatch()    [外部写入]
```

**生命周期**：
- `onMounted`：创建 `EditorState` → 创建 `EditorView` → 挂载到 DOM → 注册 `mousedown` 监听（见下）
- `onBeforeUnmount`：移除 `mousedown` 监听 → `editorView.destroy()`

**点击空白区激活**：

`.cm-editor` 设置了 `min-height: 120px`（见 `editor-theme.css`），当内容不足一行时会产生空白区。但 CM6 的所有鼠标事件只绑定在 `.cm-content` 上——空白区点击不会触发 CM6 原生处理。

解决方案：在 `view.dom`（`.cm-editor`）上挂载原生 `mousedown` 监听，检测到点击在 `.cm-content` 下方时，拦截事件并将光标移到文档末尾并聚焦。不能用 `EditorView.domEventHandlers`（它也绑定在 `.cm-content` 上，空白区事件走不到）。

```typescript
// 在 onMounted 中，EditorView 创建之后
const handleMousedown = (e: MouseEvent) => {
  const view = editorView.value;
  if (!view) return;
  if (e.clientY > view.contentDOM.getBoundingClientRect().bottom) {
    e.preventDefault();
    view.dispatch({ selection: { anchor: view.state.doc.length } });
    view.focus();
  }
};
editorView.value.dom.addEventListener('mousedown', handleMousedown);
```

### 2. `sql-highlight.ts` — SQL Token 颜色

**职责**：通过 CodeMirror 原生的 `HighlightStyle` 机制定义 SQL 各 token 类型的颜色。

**为什么用 HighlightStyle 而非 CSS**：
- `HighlightStyle.define()` 通过 CodeMirror 的 `StyleModule` 系统注入颜色，生成 `<span style="color: #xxx">` 而非 CSS class
- 这种方式不与模板 CSS 形成 `!important` 军备竞赛——SQL 颜色由 StyleModule 管理，模板颜色由 CSS 管理，两者在优先级链上互不干扰
- 符合 CodeMirror 的设计意图

**定义**：
```typescript
export const datiSqlHighlight = HighlightStyle.define([
  { tag: tags.keyword,      color: "#8959a8", fontWeight: "600" },  // SELECT, FROM, WHERE
  { tag: tags.string,       color: "#718c00" },                     // 'string literal'
  { tag: tags.number,       color: "#f5871f" },                     // 123, 3.14
  { tag: tags.comment,      color: "#8e908c", fontStyle: "italic" },// -- comment
  { tag: tags.typeName,     color: "#4271ae" },                     // INT, VARCHAR
  { tag: tags.operator,     color: "#3e999f" },                     // =, <, >
  { tag: tags.variableName, color: "#c82829" },                     // table.column
]);
```

**依赖**：`@lezer/highlight`（提供 `tags` 常量，已作为直接依赖加入 `package.json`）。

### 3. `template-decorations.ts` — 模板语法高亮 + 错误检测

**职责**：通过正则匹配 + CodeMirror `ViewPlugin` 实现 US-5.5 模板语法的高亮和未闭合检测。

**设计原则**：着色不依赖 `}}` 闭合——所有 Pattern 的 `}}` 均为可选，确保输入过程中的半成品也能正确着色。闭合与否仅影响「错误检测」是否报橙色波浪线。

**正则模式**（4 个 Pattern，按优先级）：

| 优先级 | Pattern | CSS 类 | 覆盖 |
|--------|---------|--------|------|
| 0 | `\{{` | `cm-tpl-escape` | 转义序列（灰色，`var(--ep-text-color-placeholder)`） |
| 1 | `{{#if` `{{/if` `{{#where` `{{/where}}` | `cm-tpl-keyword` | 块指令（绿色 `#389e0d`），`}}` 可选 |
| 2 | `{{{var}}}` `{{{var:default}}}` | `cm-tpl-raw-var` | 原始变量（橙色 `#d48806`），`}}}` 可选 |
| 3 | `{{var}}` `{{var:default}}` | `cm-tpl-var` | 安全变量（蓝色 `#1677ff`），`}}` 可选，排除 `{` `#` `/` 开头 |

**Pattern 3 的排除逻辑**：用 `(?![\{#\/])` 一次性排除 `{{{` / `{{#` / `{{/`，替代旧的 `(?<!\{)`。

**未闭合错误检测**（导出 `detectUnclosedRanges` 供单测）：

```
逐行扫描:
  对行内每个 {{（非转义）:
    找配对 }}（在 {{ 和下一 {{ 之间）
    找不到 → 未闭合
      用 validOpenPrefix() 计算最大有效前缀长度（如 {{sta fsdfsd → {{sta）
      标记 [{{, {{+prefixLen) 为 cm-tpl-error（橙色波浪线）
```

**`validOpenPrefix` 逻辑**：对未闭合的 `{{`，用正则匹配从该位置开始的最大合法模板前缀：
- 指令：`{{#if` / `{{#if var` / `{{#where` / `{{#where var` / `{{/if` / `{{/where`
- 原始变量：`{{{var` / `{{{var:default`
- 变量：`{{var` / `{{var:default`
- 无匹配：最少标记 2 字符（`{{`）

**转义排除**：`\{{` 匹配为 `cm-tpl-escape`（灰色），其余 Pattern 均带 `(?<!\\)` 前置排除 + 未闭合检测用 `(?<!\\)\{\{` 计数。

**装饰生成**：
```
文本字符串 → 依次运行 3 个正则 → 按位置排序
          → Decoration.mark({ class }) → Decoration.set(..., filter:true)
```

`Decoration.set` 的 `filter: true` 允许同一文本范围被多个 decoration 覆盖（CodeMirror 将重叠部分渲染为嵌套 span）。

**插件架构**：
```typescript
ViewPlugin.fromClass(
  class {
    decorations: DecorationSet;
    constructor(view) { this.decorations = buildDecorations(view); }
    update(update) { if (update.docChanged) this.decorations = buildDecorations(update.view); }
  },
  { decorations: v => v.decorations }
)
```

每次文档变更时重新计算装饰集。模板规模小，实时正则匹配的性能开销可忽略。

### 4. `editor-theme.css` — 视觉层

**职责**：
1. 编辑器外观（边框、背景、聚焦态、光标）→ 映射 Element Plus CSS 变量
2. 模板 token 颜色（带 `!important` 单向覆盖 SQL）
3. 全屏覆盖层样式（`.cm-editor-fullscreen`）

**`cursor: text`**：`.cm-editor` 设置 `cursor: text`，确保鼠标在编辑器空白区（由 `min-height` 产生）也显示 I-beam 光标。

**全屏覆盖层**：
```css
.cm-editor-fullscreen {
  position: fixed; inset: 0; z-index: 3000;
  background: var(--ep-bg-color);
  display: flex; flex-direction: column;
  padding: 20px;
}
.cm-editor-fullscreen .cm-editor-wrapper { flex: 1; }
.cm-editor-fullscreen .cm-editor { height: 100%; }
```
`z-index: 3000` 确保高于 Element Plus 弹窗（2000），弹窗内的编辑器也能全屏。

**分层策略**：

```
┌────────────────────────────────────┐
│  .cm-editor 基础外观                │  ← var(--ep-*) 对接受众
│  .cm-editor.cm-focused             │
│  .cm-activeLine / .cm-cursor       │
├────────────────────────────────────┤
│  模板颜色（!important）             │  ← 单向覆盖层
│  .cm-tpl-var      → 蓝 #1677ff    │
│  .cm-tpl-raw-var  → 橙 #d48806    │
│  .cm-tpl-keyword  → 绿 #389e0d    │
└────────────────────────────────────┘
        ↓ 单向覆盖（非军备竞赛）
┌────────────────────────────────────┐
│  SQL 颜色                          │  ← sql-highlight.ts
│  HighlightStyle → StyleModule      │     无 !important
└────────────────────────────────────┘
```

**为什么模板用 CSS `!important`**：
- 模板语法是跨宿主语言的 overlay，必须在视觉上**无条件优先**于任何宿主语言（当前是 SQL，未来可能是其他语言）
- SQL 颜色走 `HighlightStyle` → `StyleModule` 注入，与模板 CSS 不在同一优先级赛道，不存在军备竞赛

**为什么用 `*` 子选择器**：
```css
.cm-editor .cm-tpl-keyword,
.cm-editor .cm-tpl-keyword * { ... !important }
```
CodeMirror 在渲染重叠 decoration 时，不保证 SQL 和模板的嵌套顺序（谁外层谁内层）。当 SQL token span 被嵌套在模板 span 内部时，内层 SQL span 的直接声明会覆盖外层的继承值。`*` 子选择器直接命中内层 SQL span，以直接声明方式强制覆盖。

## 全屏功能

### 7. `useEditorFullscreen.ts` — 全屏切换 composable

**职责**：管理编辑器的全屏 / 退出全屏状态切换，供所有编辑器组件复用。

**接口**：
```typescript
function useEditorFullscreen(): {
  isFullscreen: Ref<boolean>;  // 当前是否全屏
  toggle: () => void;          // 切换全屏状态
}
```

**行为**：
- `enter()`：设置 `isFullscreen = true`，锁定 body 滚动，在 document 上注册 capture 阶段 `keydown` 监听
- `exit()`：恢复状态，解锁 body 滚动，移除 keydown 监听
- `onBeforeUnmount`：若全屏中则自动退出

**ESC 拦截**（capture 阶段 + `stopPropagation`）：

Element Plus 的 `el-dialog` 默认 `close-on-press-escape`，在 document 的 bubble 阶段监听 ESC。全屏时按 ESC 应当只退出全屏、不关闭弹窗。

解决方案：在 capture 阶段注册 listener 拦截 ESC——`e.stopPropagation()` 阻止事件传播到 bubble 阶段，Element Plus 收不到 ESC 事件。

```typescript
function enter() {
  document.addEventListener("keydown", onKeydown, true); // capture 阶段
}
function onKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") {
    e.stopPropagation();  // 阻止冒泡到 Element Plus
    exit();
  }
}
```

### 编辑器组件全屏布局

三个编辑器组件的模板结构统一为：

```html
<div class="w-full" :class="{ 'cm-editor-fullscreen': isFullscreen }">
  <div class="flex items-center justify-between mb-1">
    <!-- 标题行：label + 必填标记 + 全屏按钮 -->
    <span v-if="label">
      <span v-if="required" class="text-[var(--ep-color-danger)]">*</span>
      {{ label }}
    </span>
    <el-button :icon="isFullscreen ? undefined : FullScreen" @click="toggle">
      <span v-if="isFullscreen" class="icon-[mdi--fullscreen-exit]"></span>
    </el-button>
  </div>
  <div :ref="cm.containerRef" class="cm-editor-wrapper flex-1" />
</div>
```

**设计要点**：
- 标题行在全屏和非全屏模式下均可见，全屏时 label 保留在顶部
- 进入全屏用 Element Plus `FullScreen` 图标，退出全屏用 Iconify `mdi--fullscreen-exit`（需安装 `@iconify-json/mdi`）
- `label` 和 `required` 均为可选 prop：`label` 控制标题显示，`required` 控制红色 `*` 标记（弥补标签移入编辑器后，`el-form-item` 的必填标记丢失）
- 正常模式 `w-full` 撑满弹窗宽度，全屏模式 `fixed inset-0 z-3000`

## 自动补全模块

### 5. `template-completions.ts` — 模板补全源

**职责**：作为 CodeMirror `@codemirror/autocomplete` 的 completion source，提供模板变量名和块指令的补全。

**四个触发上下文**：

| 触发符 | 补全内容 | 说明 |
|--------|---------|------|
| `{{` | 模板已有变量名 + `#if` + `#where` | 变量名扫描全文 `{{var}}` / `{{{var}}}` / `{{#if var}}` 去重 |
| `{{{` | 模板已有变量名 | 同一套变量名，插入时保留三重大括号；不提示指令 |
| `{{#` | `if` + `where`（前缀过滤） | `{{#i` → 只显示 `if` |
| `{{/` | 仅未闭合的指令名 | 用栈分析光标前的 `{{#` 和 `{{/` 配对情况，无未闭合则不弹出 |

**变量名扫描来源**：

```
扫描全文:
  {{var}}         → var      （安全变量）
  {{{var}}}       → var      （原始变量，同一套名）
  {{var:default}} → var      （忽略 :default 部分）
  {{#if var}}     → var      （if 条件里的变量）
去重 → 排序 → 作为 {{ / {{{ 触发时的补全候选项
```

**变量排序**：按距光标的最近出现距离升序（近者优先），同距离时光标前的变量优先，字母序兜底。光标后的变量不会被遗漏，只是排在后面。

**`#if` 和 `#where` 的插入行为**：

| 指令 | apply | 设计意图 |
|------|-------|---------|
| `#if` | `{{#if `（开放，不闭合 `}}`） | 用户先填条件，再手动 `}}` → 触发 `templateAutoClose` 自动插入 `{{/if}}` |
| `#where` | 用 `apply` 函数 dispatch 插入 `{{#where}}\n  \n{{/where}}`，光标定位在中间行 | `#where` 无额外条件参数，直接自动闭合省步骤 |

**实现细节**：

- 直接用 `ctx.matchBefore()` 检测上下文，因 `{`、`#`、`/` 不是 CodeMirror 的 word character，设置 `filter: false` 手动过滤
- `\{{` 排除通过检查 `matchBefore.from - 1` 位置是否为 `\` 实现（`matchBefore` 不支持 lookbehind）
- 补全源注册在 `autocompletion({ override: [...] })`，追加在 `sql()` 内置 SQL 补全源之上，互不干扰

### 6. `template-auto-close.ts` — `}}` 闭合自动插入 `{{/if}}`

**职责**：检测用户输入 `}}` 恰好补全了 `{{#if condition}}` 模式时，自动插入匹配的 `{{/if}}`。

**架构**：拆为两层，纯逻辑可单测：

```
detectIfClose(docText, cursorPos) → indent | null     ← 纯函数，无依赖
         ▲
         │ 被调用
         │
EditorState.transactionFilter.of(tr => ...)            ← CodeMirror 集成层
```

**`detectIfClose` 算法**（从光标向前扫描，不依赖行首锚定）：

```
← cursor 在 }} 后
  → 跳过 varname（word + dot 字符）
  → 期望前面是 "{{#if "（6 字符）
  → 检查 {{#if 和 }} 之间无其他 {{（防歧义）
  → 提取 {{#if 所在行的缩进
  → 返回 indent 或 null
```

**支持的场景**（行中 `{{#if`，非行首）：

```
SELECT * FROM users {{#if status}}        ✅
WHERE dept_id = {{dept}} {{#if status}}   ✅
  {{#if status}}                          ✅ （保留缩进）
```

**`transactionFilter` 的正确用法（经验教训）**：

| # | 注意事项 | 说明 |
|---|---------|------|
| 1 | **必须用 `EditerState.transactionFilter`，不能是 `ViewPlugin.update + dispatch`** | `view.dispatch()` 在 `ViewPlugin.update()` 中会触发 "Calls to EditorView.update are not allowed while an update is in progress" 错误 |
| 2 | **位置坐标用 `tr.newSelection.main.from`，不是 `tr.selection?.main.from`** | 用户输入时 `tr.selection` 为 `undefined`（选择被自动映射），fallback 到 `tr.startState` 会拿到旧文档坐标，导致新文档中超界 `RangeError` |
| 3 | **追加的 spec 必须设 `sequential: true`** | `transactionFilter` 返回 `[tr, spec]` 时，两个 spec 的坐标默认都相对于**原始文档**。`spec` 的 `from` 指向的是 `tr` 执行后的新位置（如 `}}` 之后），必须声明 `sequential: true` 告知 CodeMirror 使用前一 spec 执行后的文档坐标 |
| 4 | **必须过滤删除操作** | 退格键也是 `input.type` 事件，需加 `tr.newDoc.length <= tr.startState.doc.length` 跳过 |
| 5 | **设置 `filter: false` 防止递归** | 避免 filter 对自己的变更再次触发 |

**安全约束**：

- 已有 `{{/if}}` 紧跟光标后 → 不插入（避免重复闭合）
- 两个 `{{` 之间 → 不插入（避免歧义）
- 仅插入操作触发，退格/删除不触发
- 仅 `input.type` 和 `input.complete` 事件触发

## 测试基础设施

### Vitest 单元测试

前端使用 **Vitest 4.x** 作为单元测试框架（零配置复用 Vite 的 `~/` → `src/` 路径别名）。

**运行命令**：
```bash
pnpm test          # 单次运行（vitest run）
pnpm test:watch    # 持续运行（vitest）
pnpm build         # 类型检查 + 构建（CI 管道中代替 lint）
```

**测试策略**：

| 被测模块 | 测试方式 | 用例数 |
|---------|---------|--------|
| `templateCompletions` | 用 `EditorState.create()` + `new CompletionContext()` 构造上下文 | 33 |
| `detectIfClose` | 纯函数，无需 mock | 17 |
| `detectUnclosedRanges` | 纯函数，无需 mock | 21 |

**CompletionContext 构造方式**：

```typescript
function ctx(docWithPipe: string): CompletionContext {
  const pos = docWithPipe.indexOf('|')  // `|` 标记光标位置
  const doc = docWithPipe.replace('|', '')
  const state = EditorState.create({ doc })
  return new CompletionContext(state, pos, false)
}
```

**无法用单测覆盖的场景**：

- `EditorView` 真实生命周期（需要 jsdom 环境）
- `transactionFilter` 在两个 spec 合并时的坐标系统行为
- `view.dispatch()` 的嵌套调用检测

这些属于集成层，适合 e2e 测试覆盖。

## Extension 注册顺序

在 `SqlTemplateEditor.vue` 中，extension 的注册顺序是有意设计的：

```typescript
extensions: [
  sql(),                                 // ① SQL 语言解析（生成 token + 内置补全源）
  syntaxHighlighting(datiSqlHighlight),  // ② SQL token → 颜色
  autocompletion({                       // ③ 激活补全系统 + 模板补全源
    override: [templateCompletions()]
  }),
  templateAutoClose(),                   // ④ }} 闭合检测 → 自动插入 {{/if}}（transactionFilter）
  templateDecorations(),                 // ⑤ 模板语法 → 高亮 decoration
  bracketMatching(),                     // ⑥ 括号匹配
]
```

**顺序的影响**：
- CodeMirror 的 `decorations` facet 按注册顺序收集 decoration 集
- 先注册的 decoration 在 DOM 中生成**外层** span，后注册的生成**内层** span
- 模板 decoration 后注册 → 内层 span → CSS 直接声明优先于继承值 → 天然优势
- `autocompletion()` 放在 `sql()` 之后，模板补全源与 SQL 内置补全源通过 `override` 共存
- `templateAutoClose()` 是 `transactionFilter`，不依赖注册顺序

## 编辑器行为

| 编辑器 | 折行 | 宿主语言高亮 | 模板语法高亮 | 模板补全 | SQL 关键字补全 | `#if` 自动闭合 | 全屏 |
|--------|------|-------------|-------------|---------|--------------|--------------|:--:|
| `PromptTemplateEditor` | ✅ | 无 | ✅ | ✅ 变量+指令 | ❌ | ✅ | ✅ |
| `SqlTemplateEditor` | ❌ | ✅ SQL | ✅ | ✅ 变量+指令 | ✅ | ✅ | ✅ |
| `SqlEditor` | ❌ | ✅ SQL | ❌ | ❌ | ❌ | ❌ | ✅ |

## CodeMirror 6 关键经验

以下是在实现自动补全和闭合检测时踩过的坑，总结为可复用的实践准则：

### `transactionFilter` 的正确打开方式

当需要在用户输入后自动修改文档（如自动插入闭合标签）时，**必须用 `EditorState.transactionFilter`**，不能是 `ViewPlugin.update` 内调用 `view.dispatch`。

```typescript
// ✅ 正确：事务构建阶段注入
EditorState.transactionFilter.of((tr) => {
  if (shouldExtend(tr)) {
    return [tr, { changes: {...}, sequential: true, filter: false }]
  }
  return tr
})

// ❌ 错误：update 周期内 dispatch
ViewPlugin.fromClass(class {
  update(update) {
    update.view.dispatch({...}) // Error: not allowed while an update is in progress
  }
})
```

**三个必填属性**：

| 属性 | 作用 | 不设的后果 |
|------|------|-----------|
| `sequential: true` | 声明此 spec 的坐标相对于前一 spec 执行后的文档 | `RangeError: Invalid change range`，因为默认坐标相对于原始文档（少 2 个 `}}` 字符） |
| `filter: false` | 阻止此变更被 filter 再次拦截 | 无限递归 |
| `tr.newSelection.main.from`（不用 `tr.selection`） | 获取事务后的光标位置 | 用户输入时 `tr.selection` 为 `undefined`，fallback 到 `startState` 拿到旧坐标 |

### 如何 mock `CompletionContext`

```typescript
import { EditorState } from '@codemirror/state'
import { CompletionContext } from '@codemirror/autocomplete'

const state = EditorState.create({ doc: '{{dept_id}} = {{' })
new CompletionContext(state, pos, false)  // pos 是光标位置
```

`CompletionContext` 可直接用 `new` 构造，`EditorState.create()` 不需要 DOM。这使得补全源逻辑可以完全单测覆盖。

### `matchBefore` 不支持 lookbehind

排除 `\{{` 的场景，不能用 `/(?<!\\)\{\{.../` 传给 `matchBefore`。替代方案：在 `matchBefore.from - 1` 位置手动检查是否为 `\`。

### `CompletionResult.options` 是 `readonly`

新建数组时直接赋值给 `options` 即可；不要试图 `.push()` 到 `CompletionResult['options']` 类型的变量上（它是 `readonly`）。

### `autocompletion({ override })` 会替换所有源

`override` 字面含义——替换**全部**内置补全源，包括 `sql()` 注册的 SQL 关键字补全。所以要保留 SQL 补全，必须把 `sql()` 放在 `autocompletion()` 之前注册，且我们的源追加在 `override` 数组中（不会覆盖 `sql()`，因为 `sql()` 的补全源在 `autocompletion()` 外部注册，`override` 只替换 `autocompletion` 内部管理的源）。

---

## 设计决策记录

| # | 决策 | 理由 |
|---|---|---|
| 1 | CodeMirror 6（非 Monaco） | 更轻量，Vue 集成更友好（vue-codemirror），包体积小 |
| 2 | SQL 颜色走 `HighlightStyle.define()` | CodeMirror 原生颜色系统，不与模板 CSS 竞争 |
| 3 | 模板颜色走 CSS class + `!important` | 模板是跨语言 overlay，必须无条件优先于宿主语言 |
| 4 | 模板解析用正则而非 Lezer 语法 | 模板语法元素只有 4 个，正则足够且简单；写 Lezer 语法过度工程 |
| 5 | SQL 编辑器禁用折行 | 代码编辑器惯例：水平滚动而非软折行。折行会让 SQL 结构难以阅读 |
| 6 | Prompt 编辑器启用折行 | Prompt 是自然语言文本，段落折行是正确行为 |
| 7 | `TemplateDecorations` 每次文档变更全量重算 | 模板通常 < 500 字符，正则匹配 < 1ms，缓存带来的失效管理复杂度远超收益 |
| 8 | 正则模式按最长匹配优先排序 | `{{{var}}}` 必须在 `{{var}}` 之前匹配，否则后者会截走前者的子串 |
| 9 | `(?<!\{)` 保护 `{{var}}` 不匹配 `{{{var}}}` 内部 | `{{{var}}}` 中 `{{var}}` 是合法子串，需用负向零宽断言排除 |
| 10 | `@lezer/highlight` 作为直接依赖 | 需使用 `tags` 常量定义 SQL token 颜色，虽已是间接依赖但显式声明可避免 pnpm 严格模式下的解析问题 |
| 11 | `@codemirror/autocomplete` 补全系统 | CodeMirror 6 官方补全扩展，与 `sql()` 内置补全源可共存 |
| 12 | 补全源放在 `autocompletion({ override })` 而非替换 | 追加而非替换 CodeMirror 默认补全，保留 `sql()` 的 SQL 关键字补全 |
| 13 | 模板补全用自定义 source 而非 `completeFromList` | `{{` 触发逻辑涉及非 word character 匹配，需要 `filter: false` 手动控制 |
| 14 | `#if` 插入时开放不闭合 `}}`，`#where` 自动闭合 | `#if` 需要用户填条件再闭合，`#where` 无额外参数，直接包围可省步骤 |
| 15 | `{{/` 仅提示未闭合的指令（栈分析，不匹配不弹） | 防止误插入多余闭合标签破坏语法，参考 HTML 编辑器惯例 |
| 16 | `}}` 闭合检测用 `EditorState.transactionFilter`，不用 `ViewPlugin.update + dispatch` | `view.dispatch()` 在 update 回调中会触发 "not allowed while an update is in progress" 错误；`transactionFilter` 在事务构建阶段注入变更，是 CodeMirror 6 官方推荐方式 |
| 17 | 追加 spec 必须设 `sequential: true` | `transactionFilter` 返回 `[tr, spec]` 时，两个 spec 默认相对于原始文档；`spec` 的坐标指向 `tr` 执行后的位置，需声明 `sequential: true` |
| 18 | 变量名扫描全三种来源（`{{var}}` / `{{{var}}}` / `{{#if var}}`） | `{{` 和 `{{{` 共享变量名，`{{#if var}}` 中的条件变量也应被提示 |
| 19 | 变量名正则支持 `:default` 语法 | `(\w[\w.]*)(?::[^}]+)?` 捕获变量名但忽略默认值部分 |
| 20 | `detectIfClose` 向前扫描而非行首锚定 | `{{#if` 可在行中间（如 `SELECT * FROM users {{#if status}}`），不能依赖 `^` |
| 21 | 前端单测框架选用 Vitest | 原生 Vite 集成，零配置复用路径别名，兼容 CodeMirror `EditorState.create()` 轻量 mock |
| 22 | 两套测试粒度：纯函数单测 + 集成 e2e | 补全源和闭合检测的纯逻辑用 Vitest 单测；`transactionFilter` 坐标系统等集成行为用 e2e 测试覆盖 |
| 23 | 模板高亮着色与闭合状态解耦（所有 Pattern 的 `}}` 可选） | 输入过程中 `{{var` 也应获得蓝色高亮，不被 SQL tokenizer 误染其他颜色。闭合与否仅影响错误检测的橙色波浪线 |
| 24 | Pattern 3 用 `(?![\{#\/])` 替代 `(?<!\{)` | 一次性排除 `{{{` / `{{#` / `{{/`，比两个 lookbehind 更清晰 |
| 25 | 未闭合错误用「语法感知」范围而非整行 | `{{sta fsdfsd` 只标 `{{sta` 为错误，`fsdfsd` 是普通文本不应被标。`validOpenPrefix` 按最大合法模板前缀截断 |
| 26 | 未闭合错误用橙色波浪线（`#d48806`）而非红色 | 橙色更轻量，与 `cm-tpl-raw-var` 同色系，表示「未完成」而非「错误」 |
| 27 | 变量补全按距光标最近出现排序 | 用户在补全 `{{` 时最可能想填光标附近的变量；字母序在搜索场景不如就近序直观 |
| 28 | `SqlEditor` 单独作为纯 SQL 编辑器 | 工具测试场景只需 SQL 高亮，不需要模板语法、补全、自动闭合等模板扩展，保持极简 |
| 29 | 编辑器标题行（`label` + `required` + 全屏按钮）内置在组件中 | 标签从 `el-form-item` 移入编辑器组件后，全屏时标题保留可见；`required` prop 弥补 `el-form-item` 必填标记丢失 |
| 30 | 全屏用 `position: fixed; z-index: 3000` | 高于 Element Plus 弹窗（2000），确保弹窗内的编辑器也能全屏覆盖 |
| 31 | 全屏 ESC 退出用 capture 阶段 + `stopPropagation` | Element Plus 弹窗的 ESC 关闭在 bubble 阶段；capture 拦截可阻止事件传播到弹窗，做到「退出全屏但不关弹窗」 |
| 32 | 全屏图标：进入用 Element Plus `FullScreen`，退出用 Iconify `mdi--fullscreen-exit` | 两种状态视觉区分更清晰，优于同一个图标旋转 180° 的方案 |
| 33 | 编辑器空白区点击用原生 `mousedown` 监听在 `view.dom` 上 | CM6 的事件系统（`domEventHandlers`）只绑定在 `.cm-content`，空白区事件进不来；裸 DOM 监听是唯一可靠方案 |
| 34 | `.cm-editor` 设置 `cursor: text` | 编辑器 `min-height` 产生的空白区也显示 I-beam 光标，与原生 textarea 行为一致 |

## 依赖关系

```
package.json
├── @codemirror/autocomplete   → 补全系统核心（completionSource, CompletionContext, autocompletion）
├── @codemirror/lang-sql       → SQL 语法解析（Lezer grammar）+ 内置关键字补全
├── @codemirror/language       → HighlightStyle, syntaxHighlighting, bracketMatching
├── @codemirror/state          → EditorState, Transaction, transactionFilter
├── @codemirror/view           → EditorView, Decoration, ViewPlugin
├── @lezer/highlight           → tags 常量（tags.keyword 等）
├── vue-codemirror             → Vue 3 封装（本项目中通过 useCodeMirror composable 替代）
└── vitest (dev)               → 单元测试框架
├── @iconify-json/mdi          → 全屏退出图标（mdi--fullscreen-exit）
├── @iconify/tailwind4         → Iconify Tailwind CSS 集成（`icon-[...]` 语法）
```
