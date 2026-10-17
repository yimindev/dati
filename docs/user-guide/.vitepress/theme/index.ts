import DefaultTheme from 'vitepress/theme'
import { watch } from 'vue'

export default {
  extends: DefaultTheme,
  enhanceApp({ router }) {
    if (typeof window !== 'undefined') {
      watch(
        () => router.route.path,
        (path) => {
          try {
            const isEn =
              path.startsWith('/en/') ||
              path === '/en' ||
              window.location.pathname.startsWith('/docs/en')
            localStorage.setItem('locale', isEn ? 'en' : 'zh')
          } catch (e) {}
        },
        { immediate: true }
      )
    }
  },
}
