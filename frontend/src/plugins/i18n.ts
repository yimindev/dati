import { createI18n } from "vue-i18n"

export type AppLang = "en" | "zh"

const defaultLang: AppLang = (localStorage.getItem("locale") as AppLang) || "en"

export const i18n = createI18n({
  legacy: false,
  locale: defaultLang,
  messages: {}, // 先空，配合异步加载
})

const loaders: Record<AppLang, () => Promise<any>> = {
  en: () => import("../locales/en"),
  zh: () => import("../locales/zh"),
}

export async function setI18nLanguage(lang: AppLang) {
  if (!i18n.global.availableLocales.includes(lang)) {
    const mod = await loaders[lang]()
    i18n.global.setLocaleMessage(lang, mod.default || mod)
  }
  i18n.global.locale.value = lang
  document.querySelector("html")?.setAttribute("lang", lang)
  localStorage.setItem("locale", lang)
}

export async function setupI18n() {
  await setI18nLanguage(defaultLang)
}
