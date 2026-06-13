// frontend/src/utils/codemirror/completions/template-auto-close.ts
import { EditorState } from "@codemirror/state"
import type { Extension } from "@codemirror/state"

/**
 * Pure logic: given full document text and cursor position,
 * determines whether `}}` just completed a `{{#if condition` and
 * `{{/if}}` should be auto-inserted.
 *
 * Scans backwards from cursor to find the most recent `{{#if` opening
 * (anywhere on the line, not just at line start).
 *
 * Returns null if no auto-close is needed, otherwise the indent
 * string to use for the inserted block.
 */
export function detectIfClose(fullDocText: string, cursorPos: number): string | null {
  if (cursorPos < 2) return null
  const last2 = fullDocText.slice(cursorPos - 2, cursorPos)
  if (last2 !== '}}') return null

  // Ensure {{/if}} isn't already immediately after cursor
  const textAfter = fullDocText.slice(cursorPos, cursorPos + 20)
  if (/^{{\s*\/\s*if\s*}}/i.test(textAfter)) return null

  // Scan backwards from cursor (skipping the `}}`) to find `{{#if variable`
  let i = cursorPos - 2

  // Walk backwards through variable name chars (word, dot)
  while (i > 0 && /[\w.]/.test(fullDocText[i - 1])) {
    i--
  }
  // Now i points to just before the variable name; expect `{{#if ` (with space)
  const ifOpenLen = 6 // '{{#if '.length
  if (i < ifOpenLen) return null
  const before = fullDocText.slice(i - ifOpenLen, i)
  if (!/^\{\{#if\s$/i.test(before)) return null

  // No other `{{` between this `{{#if` and cursor (to avoid ambiguity)
  const between = fullDocText.slice(i - ifOpenLen, cursorPos)
  if (/\{\{/.test(between.slice(ifOpenLen))) return null

  // Extract indent from the start of THIS line (where {{#if appears)
  let lineStart = i - ifOpenLen
  while (lineStart > 0 && fullDocText[lineStart - 1] !== '\n') {
    lineStart--
  }
  const indentMatch = fullDocText.slice(lineStart).match(/^(\s*)/)
  return indentMatch?.[1] ?? ''
}

/**
 * CodeMirror extension: detects `}}` completing `{{#if condition`
 * and automatically inserts a matching `{{/if}}` on the next line.
 *
 * Uses `EditorState.transactionFilter` — the official CodeMirror 6
 * mechanism for extending transaction specs before they are applied.
 * This avoids the nested-update error that `view.dispatch` in a
 * `ViewPlugin.update` would cause.
 */
export const templateAutoClose = (): Extension => {
  return EditorState.transactionFilter.of((tr) => {
    if (!tr.docChanged) return tr
    if (!tr.isUserEvent('input.type') && !tr.isUserEvent('input.complete')) return tr
    // Only react to insertions, never deletions (backspace, delete key)
    if (tr.newDoc.length <= tr.startState.doc.length) return tr

    const pos = tr.newSelection.main.from
    const docText = tr.newDoc.toString()
    const indent = detectIfClose(docText, pos)

    if (indent === null) return tr

    return [
      tr,
      {
        changes: { from: pos, insert: `\n${indent}\n${indent}{{/if}}` },
        selection: { anchor: pos + 1 + indent.length },
        filter: false,
        sequential: true,
      },
    ]
  })
}
