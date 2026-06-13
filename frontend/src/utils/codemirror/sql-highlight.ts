// frontend/src/utils/codemirror/sql-highlight.ts
import { HighlightStyle } from "@codemirror/language";
import { tags } from "@lezer/highlight";

/**
 * Custom SQL highlight style.
 * Colors chosen to be legible in both light and dark themes.
 * Injected via CodeMirror's native HighlightStyle mechanism (→ style attribute),
 * NOT via CSS !important — see design doc for rationale.
 */
export const datiSqlHighlight = HighlightStyle.define([
  { tag: tags.keyword, color: "#8959a8", fontWeight: "600" },
  { tag: tags.string, color: "#718c00" },
  { tag: tags.number, color: "#f5871f" },
  { tag: tags.comment, color: "#8e908c", fontStyle: "italic" },
  { tag: tags.typeName, color: "#4271ae" },
  { tag: tags.operator, color: "#3e999f" },
  { tag: tags.variableName, color: "#c82829" },
]);
