// frontend/src/composables/useEditorFullscreen.ts
import { ref, onBeforeUnmount } from "vue";

export function useEditorFullscreen() {
  const isFullscreen = ref(false);

  function enter() {
    isFullscreen.value = true;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", onKeydown, true);
  }

  function exit() {
    isFullscreen.value = false;
    document.body.style.overflow = "";
    document.removeEventListener("keydown", onKeydown, true);
  }

  function toggle() {
    isFullscreen.value ? exit() : enter();
  }

  function onKeydown(e: KeyboardEvent) {
    if (e.key === "Escape") {
      e.stopPropagation();
      exit();
    }
  }

  onBeforeUnmount(() => {
    if (isFullscreen.value) exit();
  });

  return { isFullscreen, toggle };
}
