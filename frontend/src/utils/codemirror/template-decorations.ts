// frontend/src/utils/codemirror/template-decorations.ts
import { ViewPlugin, ViewUpdate, Decoration } from "@codemirror/view";
import type { EditorView, DecorationSet } from "@codemirror/view";
import type { Extension } from "@codemirror/state";

/**
 * Regex patterns for template syntax, ordered by priority (longest match first).
 * Uses negative lookbehind to exclude escaped sequences (\{{ → literal text).
 * All patterns allow optional closing braces — color is applied regardless
 * of whether the expression is complete; only error detection cares about closure.
 * - Pattern 0: escape prefix \{{ (grey, literal)
 * - Pattern 1: block directives {{#if, {{/if, {{#where, {{/where (}} optional)
 * - Pattern 2: raw variables {{{var, {{{var:default (}}} optional)
 * - Pattern 3: safe variables {{var, {{var:default (}} optional, excludes { # /)
 */
const PATTERNS: { regex: RegExp; cls: string }[] = [
  { regex: /\\\{\{/g, cls: "cm-tpl-escape" },
  { regex: /(?<!\\)\{\{[#\/](?:if|where)(?:\s+[\w.]+)?(?:}})?/g, cls: "cm-tpl-keyword" },
  { regex: /(?<!\\)\{\{\{[\w.]+(?::[^}\s]*)?(?:}}})?/g, cls: "cm-tpl-raw-var" },
  { regex: /(?<!\\)\{\{(?![{#\/])[\w.]+(?::[^}\s]*)?(?:}})?/g, cls: "cm-tpl-var" },
];

/**
 * Given a line and a position where `{{` starts, return the length of
 * the longest valid (or in-progress) template prefix. This determines
 * how far the error underline extends.
 */
function validOpenPrefix(line: string, start: number): number {
  const rest = line.slice(start)

  // Block directive: {{#if, {{#if varname, {{#where, {{#where varname, {{/if, {{/where
  const dir = rest.match(/^\{\{[#\/](?:if|where)(?:\s+[\w.]+)?/)
  if (dir) return dir[0].length

  // Raw variable: {{{var, {{{var:default
  const raw = rest.match(/^\{\{\{[\w.]+(?::[^}\s]*)?/)
  if (raw) return raw[0].length

  // Variable: {{var, {{var:default
  const vr = rest.match(/^\{\{[\w.]+(?::[^}\s]+)?/)
  if (vr) return vr[0].length

  // Bare {{ (no valid syntax following, but still an unclosed opener)
  return 2
}

/**
 * Detect unclosed `{{` ranges on a single line.
 * For each `{{` (excluding escaped `\{{`), check whether a matching `}}`
 * exists before the next `{{`. If not, the range from this `{{` to the
 * end of its syntax-aware valid prefix is reported as unclosed.
 */
export function detectUnclosedRanges(line: string): { from: number; to: number }[] {
  const openPositions: number[] = []
  for (const m of line.matchAll(/(?<!\\)\{\{/g)) {
    openPositions.push(m.index!)
  }
  if (openPositions.length === 0) return []

  const closePositions: number[] = []
  for (const m of line.matchAll(/}}/g)) {
    closePositions.push(m.index!)
  }

  const errors: { from: number; to: number }[] = []
  for (let i = 0; i < openPositions.length; i++) {
    const open = openPositions[i]
    const boundary = i + 1 < openPositions.length ? openPositions[i + 1] : line.length

    // Is there a `}}` after this `{{` but before the next `{{`?
    const close = closePositions.find((c) => c > open && c <= boundary)
    if (close === undefined) {
      errors.push({ from: open, to: Math.min(open + validOpenPrefix(line, open), boundary) })
    }
  }

  return errors
}

function buildDecorations(view: EditorView): DecorationSet {
  const decorations: { from: number; to: number; deco: Decoration }[] = [];
  const text = view.state.doc.toString();

  for (const { regex, cls } of PATTERNS) {
    let match: RegExpExecArray | null;
    regex.lastIndex = 0;
    while ((match = regex.exec(text)) !== null) {
      decorations.push({
        from: match.index,
        to: match.index + match[0].length,
        deco: Decoration.mark({ class: cls }),
      });
    }
  }

  // Error detection: unclosed {{ on each line
  let offset = 0;
  for (const line of text.split('\n')) {
    for (const range of detectUnclosedRanges(line)) {
      decorations.push({
        from: offset + range.from,
        to: offset + range.to,
        deco: Decoration.mark({ class: 'cm-tpl-error' }),
      })
    }
    offset += line.length + 1 // +1 for the \n we split on
  }

  // Sort by position for Decoration.set
  decorations.sort((a, b) => a.from - b.from);
  return Decoration.set(
    decorations.map((d) => d.deco.range(d.from, d.to)),
    true, // filter: allows overlapping decorations
  );
}

export const templateDecorations = (): Extension => {
  return ViewPlugin.fromClass(
    class {
      decorations: DecorationSet;
      constructor(view: EditorView) {
        this.decorations = buildDecorations(view);
      }
      update(update: ViewUpdate) {
        if (update.docChanged) {
          this.decorations = buildDecorations(update.view);
        }
      }
    },
    { decorations: (v: { decorations: DecorationSet }) => v.decorations },
  );
};
