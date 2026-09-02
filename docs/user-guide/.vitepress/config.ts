
// @ts-ignore
import { defineConfig } from 'vitepress'

const SKILLS_URL =
  process.env.VITE_SKILLS_URL ||
  process.env.SKILLS_URL ||
  'https://github.com/yimindev/dati/tree/main/skills'

const SKILL_URL =
  process.env.VITE_SKILL_URL ||
  process.env.SKILL_URL ||
  `${SKILLS_URL.replace(/\/+$/, '')}/dati-ops`

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
    skillsUrl: SKILLS_URL,
    skillUrl: SKILL_URL,
  },

  transformPageData(pageData) {
    pageData.frontmatter = {
      ...pageData.frontmatter,
      skillsUrl: SKILLS_URL,
      skillUrl: SKILL_URL,
    }
  },

  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'DatI 帮助中心',
      description: 'DatI 数据智能平台使用文档',
      themeConfig: {
        skillsUrl: SKILLS_URL,
        skillUrl: SKILL_URL,
        nav: [
          { text: '平台介绍', link: '/' },
          { text: '快速上手', link: '/quickstart' },
          { text: '模板语法', link: '/template-syntax' },
          { text: '常见问题', link: '/faq' },
        ],
        sidebar: [
          {
            text: '使用指南',
            items: [
              { text: '平台介绍', link: '/' },
              { text: '快速上手', link: '/quickstart' },
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
        skillsUrl: SKILLS_URL,
        skillUrl: SKILL_URL,
        nav: [
          { text: 'Introduction', link: '/en/' },
          { text: 'Quick Start', link: '/en/quickstart' },
          { text: 'Template Syntax', link: '/en/template-syntax' },
          { text: 'FAQ', link: '/en/faq' },
        ],
        sidebar: [
          {
            text: 'Guide',
            items: [
              { text: 'Introduction', link: '/en/' },
              { text: 'Quick Start', link: '/en/quickstart' },
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
