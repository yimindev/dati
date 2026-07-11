// frontend/src/composables/useCodeMirror.ts
import { ref, watch, onMounted, onBeforeUnmount, type Ref, shallowRef } from "vue";
import { EditorView, placeholder as cmPlaceholder } from "@codemirror/view";
import { EditorState } from "@codemirror/state";
import type { Extension } from "@codemirror/state";
import type { ViewUpdate } from "@codemirror/view";

interface UseCodeMirrorOptions {
  modelValue: Ref<string>;
  extensions: Extension[];
  placeholder?: string;
}

export function useCodeMirror(options: UseCodeMirrorOptions) {
  const containerRef = ref<HTMLElement | null>(null);
  const editorView = shallowRef<EditorView | null>(null);
  let cleanupMousedown: (() => void) | null = null;

  onMounted(() => {
    if (!containerRef.value) return;

    const state = EditorState.create({
      doc: options.modelValue.value,
      extensions: [
        ...options.extensions,
        options.placeholder ? cmPlaceholder(options.placeholder) : [],
        EditorView.updateListener.of((update: ViewUpdate) => {
          if (update.docChanged) {
            const newValue = update.state.doc.toString();
            if (newValue !== options.modelValue.value) {
              options.modelValue.value = newValue;
            }
          }
        }),
      ],
    });

    editorView.value = new EditorView({
      state,
      parent: containerRef.value,
    });

    // Click below .cm-content → move cursor to end & focus.
    // CM6 binds mouse events only to .cm-content, so clicks in the empty
    // area created by min-height are not captured. We handle them here.
    const handleMousedown = (e: MouseEvent) => {
      const view = editorView.value;
      if (!view) return;
      if (e.clientY > view.contentDOM.getBoundingClientRect().bottom) {
        e.preventDefault();
        const endPos = view.state.doc.length;
        view.dispatch({ selection: { anchor: endPos } });
        view.focus();
      }
    };
    const editorDom = editorView.value.dom;
    editorDom.addEventListener('mousedown', handleMousedown);
    cleanupMousedown = () => editorDom.removeEventListener('mousedown', handleMousedown);
  });

  // External changes → write back to editor (e.g. loadForm reset)
  watch(options.modelValue, (newVal) => {
    const view = editorView.value;
    if (view && newVal !== view.state.doc.toString()) {
      view.dispatch({
        changes: { from: 0, to: view.state.doc.length, insert: newVal },
      });
    }
  });

  onBeforeUnmount(() => {
    cleanupMousedown?.();
    editorView.value?.destroy();
    editorView.value = null;
  });

  return { containerRef, editorView };
}
