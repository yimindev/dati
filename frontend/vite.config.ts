import path from "node:path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import VueRouter from "vue-router/vite";
import tailwindcss from "@tailwindcss/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

// https://vite.dev/config/
export default defineConfig({
  server: {
    allowedHosts: [
      '.zhangyimin.me'
    ],
    proxy: {
      "/docs": {
        target: "http://localhost:5174",
        changeOrigin: true,
      },
      "/api": {
        target: "http://localhost:8085",
        rewrite: (path) => path.replace(/^\/api/, '')
      },
    },
  },

  resolve: {
    alias: {
      "~/": `${path.resolve(__dirname, "src")}/`,
    },
  },

  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "~/styles/element/index.scss" as *;`,
      },
    },
  },

  plugins: [
    vue(),

    // https://github.com/posva/unplugin-vue-router
    VueRouter({
      extensions: [".vue", ".md"],
      dts: "src/route-map.d.ts",
    }),

    AutoImport({
      // 自动导入这些库的 API（按需）
      imports: ["vue", "vue-router"],
      // 也可按需添加：'pinia', '@vueuse/core', 等
      dts: "src/auto-imports.d.ts",
      // 让 ESLint 识别到这些全局 API（可选）
      eslintrc: {
        enabled: true,
        filepath: "./.eslintrc-auto-import.json",
        globalsPropValue: true,
      },
      resolvers: [
        // 对某些库提供扩展能力（比如 Element Plus 指令等）
        ElementPlusResolver(),
      ],
    }),

    Components({
      // 自动按需注册本地组件 + UI 库组件
      dts: "src/components.d.ts",
      dirs: ["src/components"],
      extensions: ["vue", "tsx", "md"],
      // allow auto import and register components used in markdown
      include: [/\.vue$/, /\.vue\?vue/, /\.md$/],
      deep: true,
      resolvers: [
        // 按需引入 Element Plus 组件与样式
        ElementPlusResolver({
          importStyle: "sass",
          directives: true,
        }),
      ],
    }),

    tailwindcss(),
  ],
});
