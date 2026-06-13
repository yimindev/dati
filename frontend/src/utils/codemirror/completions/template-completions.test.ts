// frontend/src/utils/codemirror/completions/template-completions.test.ts
import { describe, it, expect } from 'vitest'
import { EditorState } from '@codemirror/state'
import { CompletionContext } from '@codemirror/autocomplete'
import { templateCompletions } from './template-completions'

/** Create a CompletionContext: cursor is at `|` in the document string. */
function ctx(docWithPipe: string): CompletionContext {
  const pos = docWithPipe.indexOf('|')
  if (pos === -1) throw new Error(`Missing cursor marker '|' in "${docWithPipe}"`)
  const doc = docWithPipe.replace('|', '')
  const state = EditorState.create({ doc })
  return new CompletionContext(state, pos, false)
}

function labels(result: ReturnType<ReturnType<typeof templateCompletions>>): string[] {
  return result?.options?.map((o) => o.label) ?? []
}

function applyTexts(result: ReturnType<ReturnType<typeof templateCompletions>>): string[] {
  return result?.options?.map((o) => (typeof o.apply === 'string' ? o.apply : '')) ?? []
}

const completions = templateCompletions()

describe('templateCompletions', () => {
  // ---- {{ trigger: variables + directives ----
  describe('{{ trigger', () => {
    it('suggests variables from existing {{var}} in template', () => {
      const result = completions(ctx('{{dept_id}} = {{|}}'))
      expect(labels(result)).toContain('dept_id')
    })

    it('suggests variables from both {{var}} and {{{var}}}', () => {
      const result = completions(ctx('{{name}} {{{table}}} {{|}}'))
      expect(labels(result)).toContain('name')
      expect(labels(result)).toContain('table')
    })

    it('deduplicates same variable name from {{ and {{{', () => {
      const result = completions(ctx('{{x}} {{{x}}} {{|}}'))
      const count = labels(result).filter((l) => l === 'x').length
      expect(count).toBe(1)
    })

    it('suggests #if and #where directives', () => {
      const result = completions(ctx('{{|}}'))
      expect(labels(result)).toContain('#if')
      expect(labels(result)).toContain('#where')
    })

    it('sorts variables by proximity to cursor (nearest first)', () => {
      // {{far}} at pos 0, {{near}} at pos 9, cursor after {{near}} → near first
      const result = completions(ctx('{{far}}   {{near}} {{|}}'))
      const vars = labels(result).filter((l) => !l.startsWith('#'))
      expect(vars[0]).toBe('near')
      expect(vars[1]).toBe('far')
    })

    it('same variable appears nearest, deduplicated', () => {
      // {{x}} at pos 0, {{x}} at pos 7, cursor after second {{x}} → x once
      const result = completions(ctx('{{x}}   {{x}} {{|}}'))
      const vars = labels(result).filter((l) => !l.startsWith('#'))
      expect(vars).toEqual(['x'])
    })

    it('closer variables ranked before farther ones', () => {
      const result = completions(ctx('{{far}}   {{close}} {{|}}'))
      const vars = labels(result).filter((l) => !l.startsWith('#'))
      expect(vars[0]).toBe('close')
      expect(vars[1]).toBe('far')
    })

    it('after-cursor variables still appear at end', () => {
      const result = completions(ctx('{{|}}  {{after}}'))
      const vars = labels(result).filter((l) => !l.startsWith('#'))
      expect(vars).toContain('after')
    })

    it('multiple occurrences use closest distance', () => {
      // {{x}} at 0 (far), {{x}} right before cursor at 16 (near)
      const result = completions(ctx('{{x}}              {{x}} {{|}}'))
      const vars = labels(result).filter((l) => !l.startsWith('#'))
      expect(vars).toEqual(['x'])
    })

    it('filters variables by partial prefix', () => {
      const result = completions(ctx('{{dept}} {{status}} {{d|}}'))
      const vars = labels(result).filter((l) => !l.startsWith('#'))
      expect(vars).toContain('dept')
      expect(vars).not.toContain('status')
    })

    it('#if apply leaves }} open (no closing braces)', () => {
      const result = completions(ctx('{{|}}'))
      const ifApply = applyTexts(result).find((a) => a.startsWith('{{#if'))
      expect(ifApply).toBe('{{#if ')
    })

    it('#where apply is a function (no $1 snippet) in {{ trigger', () => {
      const result = completions(ctx('{{|}}'))
      const whereOption = result?.options?.find((o) => o.label === '#where')
      expect(whereOption).toBeDefined()
      expect(whereOption!.type).toBe('keyword')
      expect(typeof whereOption!.apply).toBe('function')
    })

    it('scans variables defined in {{#if var}}', () => {
      const result = completions(ctx('{{#if status}} ... {{|}}'))
      expect(labels(result)).toContain('status')
    })

    it('scans variable names from {{var:default}}', () => {
      const result = completions(ctx('{{dept_id:default}} {{|}}'))
      expect(labels(result)).toContain('dept_id')
      expect(labels(result)).not.toContain('dept_id:default')
    })
  })

  // ---- {{{ trigger: variables only ----
  describe('{{{ trigger', () => {
    it('suggests variables with triple-brace insertion', () => {
      const result = completions(ctx('{{dept_id}} {{{|}}}'))
      const deptApply = applyTexts(result).find((a) => a.includes('dept_id'))
      expect(deptApply).toBe('{{{dept_id}}}}')
    })

    it('does not suggest directives', () => {
      const result = completions(ctx('{{{ |}}}'))
      expect(labels(result)).not.toContain('#if')
      expect(labels(result)).not.toContain('#where')
    })

    it('returns null when no variables exist in template', () => {
      const result = completions(ctx('{{{ |}}}'))
      expect(result).toBeNull()
    })
  })

  // ---- {{# trigger: directive names ----
  describe('{{# trigger', () => {
    it('suggests if and where after {{#', () => {
      const result = completions(ctx('{{#|}}'))
      expect(labels(result)).toEqual(['if', 'where'])
    })

    it('filters by partial prefix: {{#i → only if', () => {
      const result = completions(ctx('{{#i|}}'))
      expect(labels(result)).toEqual(['if'])
    })

    it('filters by partial prefix: {{#w → only where', () => {
      const result = completions(ctx('{{#w|}}'))
      expect(labels(result)).toEqual(['where'])
    })

    it('#if apply leaves }} open', () => {
      const result = completions(ctx('{{#i|}}'))
      const ifApply = applyTexts(result).find((a) => a.startsWith('{{#if'))
      expect(ifApply).toBe('{{#if ')
    })

    it('#where apply is a function (no $1 snippet) in {{# trigger', () => {
      const result = completions(ctx('{{#w|}}'))
      const whereOption = result?.options?.find((o) => o.label === 'where')
      expect(whereOption).toBeDefined()
      expect(typeof whereOption!.apply).toBe('function')
    })
  })

  // ---- {{/ trigger: only unclosed ----
  describe('{{/ trigger', () => {
    it('suggests unclosed directive', () => {
      const result = completions(ctx('{{#if status}} some text {{/|}}'))
      expect(labels(result)).toContain('if')
    })

    it('suggests multiple unclosed directives', () => {
      const result = completions(ctx('{{#if status}} {{#where}} text {{/|}}'))
      expect(labels(result)).toContain('if')
      expect(labels(result)).toContain('where')
    })

    it('returns null when all directives are closed', () => {
      const result = completions(ctx('{{#if status}} {{/if}} {{/|}}'))
      expect(result).toBeNull()
    })

    it('returns null when no directive is open (blank template)', () => {
      const result = completions(ctx('{{/|}}'))
      expect(result).toBeNull()
    })

    it('filters unclosed by partial prefix', () => {
      const result = completions(ctx('{{#if status}} {{#where}} text {{/i|}}'))
      expect(labels(result)).toContain('if')
      expect(labels(result)).not.toContain('where')
    })

    it('ignores mismatched close: {{#if}}...{{/where}} leaves if open', () => {
      const result = completions(ctx('{{#if a}} {{#where}} {{/where}} {{/|}}'))
      expect(labels(result)).toContain('if')
      expect(labels(result)).not.toContain('where')
    })
  })

  // ---- escaped \{{ ----
  describe('escaped {{', () => {
    it('returns null for \\{{ trigger (double brace)', () => {
      const result = completions(ctx('\\{{|}}'))
      expect(result).toBeNull()
    })

    it('returns null for \\{{{ trigger (triple brace)', () => {
      const result = completions(ctx('\\{{{|}}}'))
      expect(result).toBeNull()
    })
  })

  // ---- unrelated context ----
  describe('no trigger', () => {
    it('returns null for plain text', () => {
      const result = completions(ctx('SELECT * FROM users|'))
      expect(result).toBeNull()
    })

    it('returns null for SQL context', () => {
      const result = completions(ctx('WHERE dept_id = |'))
      expect(result).toBeNull()
    })

    it('returns null after closed {{var}}', () => {
      const result = completions(ctx('{{dept_id}} |'))
      expect(result).toBeNull()
    })
  })
})
