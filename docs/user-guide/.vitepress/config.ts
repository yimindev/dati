
// @ts-ignore
import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/docs/',
  outDir: '../../frontend/public/docs',

  title: 'DatI 帮助中心',
  description: 'DatI 数据智能平台使用文档',

  head: [['link', { rel: 'icon', type: 'image/svg+xml', href: '/dati.svg' }]],

  themeConfig: {
    logo: '/dati.svg',
    search: {
      provider: 'local',
    },
  },

  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'DatI 帮助中心',
      description: 'DatI 数据智能平台使用文档',
      themeConfig: {
        nav: [
          { text: '快速入门', link: '/' },
          { text: '模板语法', link: '/template-syntax' },
          { text: '常见问题', link: '/faq' },
        ],
        sidebar: [
          {
            text: '使用指南',
            items: [
              { text: '快速入门', link: '/' },
              { text: '模板语法详解', link: '/template-syntax' },
              { text: '常见问题', link: '/faq' },
            ],
          },
        ],
        outline: { level: [2, 3] },
        docFooter: { prev: '上一篇', next: '下一篇' },
        darkModeSwitchLabel: '主题',
        sidebarMenuLabel: '菜单',
        returnToTopLabel: '回到顶部',
        lastUpdated: { text: '最后更新' },
      },
    },
    en: {
      label: 'English',
      lang: 'en',
      title: 'DatI Help Center',
      description: 'DatI Data Intelligence Platform User Guide',
      themeConfig: {
        nav: [
          { text: 'Quick Start', link: '/en/' },
          { text: 'Template Syntax', link: '/en/template-syntax' },
          { text: 'FAQ', link: '/en/faq' },
        ],
        sidebar: [
          {
            text: 'Guide',
            items: [
              { text: 'Quick Start', link: '/en/' },
              { text: 'Template Syntax', link: '/en/template-syntax' },
              { text: 'FAQ', link: '/en/faq' },
            ],
          },
        ],
        outline: { level: [2, 3] },
      },
    },
  },
})
