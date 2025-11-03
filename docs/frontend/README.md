# Frontend 文档（Vue 3 + Vite + TypeScript）

本模块采用 Vue 3（`<script setup>` + TS），构建工具为 Vite，组件库 Element Plus，样式使用 TailwindCSS（v4 插件）。

## 快速命令

```bash
cd frontend
npm install
npm run dev       # 本地开发
npm run build     # 生产构建（包含 vue-tsc 类型检查）
npm run preview   # 预览生产构建
```

## 工程约定

- 组件命名：PascalCase，按功能就近存放于 `src/components`。
- 自动导入：已配置 `unplugin-auto-import` 与 `unplugin-vue-components`。
- 类型检查：`npm run build` 会执行 `vue-tsc -b`。可单独运行：
  ```bash
  npx vue-tsc --noEmit
  ```
- 样式：TailwindCSS，优先使用原子类，按需抽取组件级样式。
- ESLint/Prettier：依赖已存在但暂无脚本，需要时可在 `package.json` 中添加：
  ```json
  {
    "scripts": {
      "lint": "eslint --ext .ts,.vue src",
      "format": "prettier --write ."
    }
  }
  ```

## 与后端交互

- 默认后端端口 `8085`。如有跨域需求，请在后端开启 CORS。
- JSON 命名策略为 `SNAKE_CASE`（dev），请在前端适配字段命名或在 API 层做转换。

## 测试（可选）

- 目前未内置测试框架。建议引入 Vitest：
  ```bash
  npm i -D vitest @vitest/ui jsdom
  ```
  在 `package.json` 添加脚本：
  ```json
  {
    "scripts": {
      "test": "vitest",
      "test:ui": "vitest --ui"
    }
  }
  ```
  在 `src` 目录内新增 `*.test.ts` 并运行：`npm run test`
