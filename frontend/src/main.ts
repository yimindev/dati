
// import "~/styles/element/index.scss";

import { routes } from 'vue-router/auto-routes'
import App from './App.vue'

import '~/styles/index.scss'
import '~/styles/tailwind.css'

// If you want to use ElMessage, import it.
import 'element-plus/theme-chalk/src/message.scss'
import 'element-plus/theme-chalk/src/message-box.scss'

// if you do not need ssg:
import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";

const app = createApp(App);
app.use(createRouter({
  history: createWebHistory(),
  routes,
}))
app.mount("#app");
