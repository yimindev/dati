
// import "~/styles/element/index.scss";

import { routes } from 'vue-router/auto-routes'
import App from './App.vue'

import '~/styles/index.scss'
import '~/styles/tailwind.css'

import { i18n, setupI18n } from "./plugins/i18n"

// If you want to use ElMessage, import it.
import 'element-plus/theme-chalk/src/message.scss'
import 'element-plus/theme-chalk/src/message-box.scss'

// if you do not need ssg:
import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";
import { pinia } from "./stores";
import { useAuthStore } from "~/stores/auth";

const app = createApp(App);

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const publicPaths = ["/login", "/register"];

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore();

  if (authStore.isLoggedIn && publicPaths.includes(to.path)) {
    return next("/");
  }

  if (!authStore.isLoggedIn && !publicPaths.includes(to.path)) {
    return next("/login");
  }

  next();
});

app.use(pinia)
app.use(router)
app.use(i18n)

await setupI18n()

app.mount("#app");
