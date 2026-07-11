// frontend/src/utils/codemirror/click-below-content.ts
// CM6 extension: clicking below .cm-content moves cursor to end & focuses.
// Necessary because we set min-height on .cm-editor, creating empty space below
// the text content where CM6's native click handling (bound to .cm-content only)
// does not respond.
import { EditorView } from "@codemirror/view";

export const clickBelowContent = EditorView.domEventHandlers({
  mousedown: (event, view) => {
    const content = view.contentDOM;
    if (event.clientY > content.getBoundingClientRect().bottom) {
      event.preventDefault();
      const endPos = view.state.doc.length;
      view.dispatch({ selection: { anchor: endPos } });
      view.focus();
      return true;
    }
    return false;
  },
});
