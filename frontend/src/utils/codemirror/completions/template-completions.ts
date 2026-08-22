// frontend/src/utils/codemirror/completions/template-completions.ts
import { type CompletionContext, type CompletionResult } from "@codemirror/autocomplete"
import type { EditorView } from "@codemirror/view"

const DIRECTIVES = ['if', 'where'] as const

export interface SystemVariable {
  name: string
  detail: string
  i18nKey: string
}

// Keep in sync with backend SystemVariableResolver.SYSTEM_VARIABLES
export const SYSTEM_VARIABLES: readonly SystemVariable[] = [
  { name: '_user.id', detail: 'Current user ID', i18nKey: 'mcpService.tool.sysVarUserId' },
  { name: '_user.name', detail: 'Current username', i18nKey: 'mcpService.tool.sysVarUserName' },
  { name: '_user.display_name', detail: 'Current user display name', i18nKey: 'mcpService.tool.sysVarUserDisplayName' },
  { name: '_now', detail: 'Current timestamp (yyyy-MM-dd HH:mm:ss)', i18nKey: 'mcpService.tool.sysVarNow' },
  { name: '_date', detail: 'Current date (yyyy-MM-dd)', i18nKey: 'mcpService.tool.sysVarDate' },
] as const

/**
 * Extract variable names from `{{var}}`, `{{{var}}}`, `{{#if var}}`,
 * and default-value forms like `{{var:default}}` in the template.
 * Returns deduplicated sorted array.
 */
function scanVariables(text: string, cursorPos: number): string[] {
  const names = new Map<string, { dist: number; before: boolean }>()

  const collect = (regex: RegExp) => {
    for (const m of text.matchAll(regex)) {
      const name = m[1]
      const pos = m.index!
      const dist = Math.abs(pos - cursorPos)
      const before = pos <= cursorPos
      const existing = names.get(name)
      if (!existing || dist < existing.dist || (dist === existing.dist && before && !existing.before)) {
        names.set(name, { dist, before })
      }
    }
  }

  // {{var}}, {{var:default}}
  collect(/\{\{(\w[\w.]*)(?::[^}]+)?}}/g)
  // {{{var}}}, {{{var:default}}}
  collect(/\{\{\{(\w[\w.]*)(?::[^}]+)?}}}/g)
  // {{#if var}}
  collect(/\{\{#if\s+(\w[\w.]*)}}/gi)

  return [...names.entries()]
    .sort((a, b) => {
      // 1. Distance (ascending)
      if (a[1].dist !== b[1].dist) return a[1].dist - b[1].dist
      // 2. Before cursor first
      if (a[1].before !== b[1].before) return a[1].before ? -1 : 1
      // 3. Alphabetical fallback
      return a[0].localeCompare(b[0])
    })
    .map(([name]) => name)
}

/**
 * Analyse unclosed pair count: {{#if|where}} vs {{/if|where}} before cursor.
 * Returns list of directive names that are still open.
 */
function analyseUnclosed(textBeforeCursor: string): string[] {
  const tokens = textBeforeCursor.matchAll(/\{\{([#\/])(if|where)(}})?/g)

  const stack: string[] = []
  for (const m of tokens) {
    const prefix = m[1]
    const name = m[2]
    if (prefix === '#') {
      stack.push(name)
    } else {
      const top = stack[stack.length - 1]
      if (top === name) {
        stack.pop()
      }
    }
  }
  return [...new Set(stack)]
}

export function templateCompletions(t?: (key: string) => string): (ctx: CompletionContext) => CompletionResult | null {
  return (ctx: CompletionContext): CompletionResult | null => {
    const pos = ctx.pos

    // ----- {{# name| → directive names (opening) -----
    const hashMatch = ctx.matchBefore(/\{\{#(\w*)$/)
    if (hashMatch) {
      const partial = (hashMatch.text.match(/\{\{#(\w*)$/) ?? [])[1] ?? ''
      const options = DIRECTIVES
        .filter((d) => d.startsWith(partial))
        .map((d) => {
          if (d === 'where') {
            return {
              label: d,
              type: 'keyword' as const,
              apply: (view: EditorView, _completion: unknown, from: number, to: number) => {
                view.dispatch({
                  changes: { from, to, insert: '{{#where}}\n  \n{{/where}}' },
                  selection: { anchor: from + 13 },
                })
              },
            }
          }
          return { label: d, type: 'keyword' as const, apply: `{{#${d} ` }
        })
      return options.length ? { from: hashMatch.from, options, filter: false } : null
    }

    // ----- {{/ name| → only unclosed directives -----
    const slashMatch = ctx.matchBefore(/\{\{\/(\w*)$/)
    if (slashMatch) {
      const textBefore = ctx.state.doc.sliceString(0, pos)
      const unclosed = analyseUnclosed(textBefore)
      if (unclosed.length === 0) return null

      const partial = (slashMatch.text.match(/\{\{\/(\w*)$/) ?? [])[1] ?? ''
      const options = unclosed
        .filter((d) => d.startsWith(partial))
        .map((d) => ({ label: d, type: 'keyword' as const, apply: `{{/${d}}}` }))
      return options.length ? { from: slashMatch.from, options, filter: false } : null
    }

    // ----- {{{ name| → variables only -----
    const tripleMatch = ctx.matchBefore(/\{\{\{([\w.]*)$/)
    if (tripleMatch) {
      const hasBackslash = ctx.state.doc.sliceString(tripleMatch.from - 1, tripleMatch.from) === '\\'
      if (hasBackslash) return null

      const partial = (tripleMatch.text.match(/\{\{\{([\w.]*)$/) ?? [])[1] ?? ''
      const fullText = ctx.state.doc.toString()
      const varNames = scanVariables(fullText, pos)
      const sysNames = SYSTEM_VARIABLES.map((s) => s.name)
      const combined = [...new Set([...varNames, ...sysNames])]

      const options = combined
        .filter((v) => v.startsWith(partial))
        .map((v) => ({ label: v, type: 'variable' as const, apply: `{{{${v}}}}}` }))
      return options.length ? { from: tripleMatch.from, options, filter: false } : null
    }

    // ----- {{ name| → variables + directives -----
    const doubleMatch = ctx.matchBefore(/\{\{(?!\{)([\w.]*)$/)
    if (doubleMatch) {
      const hasBackslash =
        ctx.state.doc.sliceString(doubleMatch.from - 1, doubleMatch.from) === '\\'
      if (hasBackslash) return null

      const partial = (doubleMatch.text.match(/\{\{(?!\{)([\w.]*)$/) ?? [])[1] ?? ''
      const fullText = ctx.state.doc.toString()
      const varNames = scanVariables(fullText, pos)

      const options: Array<CompletionResult['options'][number]> = []
      const seen = new Set<string>()
      const sysNameSet = new Set(SYSTEM_VARIABLES.map((s) => s.name))

      // 1. Template scanned variables (non-system)
      for (const v of varNames) {
        if (v.startsWith(partial) && !sysNameSet.has(v)) {
          options.push({ label: v, type: 'variable' as const, apply: `{{${v}}}` })
          seen.add(v)
        }
      }

      // 2. System built-in variables
      for (const sys of SYSTEM_VARIABLES) {
        if (sys.name.startsWith(partial) && !seen.has(sys.name)) {
          options.push({
            label: sys.name,
            type: 'variable' as const,
            detail: t ? t(sys.i18nKey) : sys.detail,
            apply: `{{${sys.name}}}`,
          })
          seen.add(sys.name)
        }
      }

      // 3. Directive #if
      if ('if'.startsWith(partial)) {
        options.push({ label: '#if', type: 'keyword' as const, apply: '{{#if ' })
      }
      // 4. Directive #where
      if ('where'.startsWith(partial)) {
        options.push({
          label: '#where',
          type: 'keyword' as const,
          apply: (view: EditorView, _completion: unknown, from: number, to: number) => {
            view.dispatch({
              changes: { from, to, insert: '{{#where}}\n  \n{{/where}}' },
              selection: { anchor: from + 13 },
            })
          },
        })
      }

      return options.length ? { from: doubleMatch.from, options, filter: false } : null
    }

    return null
  }
}
