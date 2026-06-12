// frontend/src/utils/codemirror/template-decorations.ts
import { ViewPlugin, ViewUpdate, Decoration } from "@codemirror/view";
import type { EditorView, DecorationSet } from "@codemirror/view";
import type { Extension } from "@codemirror/state";

/**
 * Regex patterns for template syntax, ordered by priority (longest match first).
 * Uses negative lookbehind to exclude escaped sequences (\{{ → literal text, no highlight).
 * - Pattern 1: block directives {{#if}} {{/if}} {{#where}} {{/where}}
 * - Pattern 2: raw variables {{{var}}} {{{var:default}}}
 * - Pattern 3: safe variables {{var}} {{var:default}}
 */
const PATTERNS: { regex: RegExp; cls: string }[] = [
  { regex: /(?<!\\)\{\{([#\/])(if|where)\s*(\w*)}}/g, cls: "cm-tpl-keyword" },
  { regex: /(?<!\\)\{\{\{(\w+)(:[^}]+)?}}}/g, cls: "cm-tpl-raw-var" },
  { regex: /(?<!\\)\{\{(\w+)(:[^}]+)?}}/g, cls: "cm-tpl-var" },
];

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
