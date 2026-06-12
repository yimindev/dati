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
    editorView.value?.destroy();
    editorView.value = null;
  });

  return { containerRef, editorView };
}
