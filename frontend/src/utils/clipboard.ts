/**
 * Copies the given text to the clipboard.
 * Supports both modern navigator.clipboard (in secure contexts like HTTPS/localhost)
 * and fallback document.execCommand('copy') (in non-secure contexts like HTTP deployments).
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  if (!text) {
    return false;
  }

  // 1. Try modern Clipboard API if supported
  if (
    typeof navigator !== "undefined" &&
    navigator.clipboard &&
    typeof navigator.clipboard.writeText === "function"
  ) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // Fall through to fallback if writeText fails
    }
  }

  // 2. Fallback to document.execCommand('copy') for non-secure context (e.g. HTTP deployment)
  if (typeof document !== "undefined" && typeof document.createElement === "function") {
    try {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.setAttribute("readonly", "");
      textarea.setAttribute("aria-hidden", "true");
      textarea.style.position = "fixed";
      textarea.style.top = "0";
      textarea.style.left = "-9999px";
      textarea.style.opacity = "0";
      textarea.style.pointerEvents = "none";

      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      textarea.setSelectionRange(0, textarea.value.length);

      const successful = document.execCommand("copy");
      document.body.removeChild(textarea);

      return successful;
    } catch {
      return false;
    }
  }

  return false;
}
