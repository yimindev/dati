import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { copyToClipboard } from "./clipboard";

describe("copyToClipboard", () => {
  const originalNavigator = globalThis.navigator;
  const originalDocument = globalThis.document;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    Object.defineProperty(globalThis, "navigator", {
      value: originalNavigator,
      configurable: true,
      writable: true,
    });
    Object.defineProperty(globalThis, "document", {
      value: originalDocument,
      configurable: true,
      writable: true,
    });
  });

  it("should return false when text is empty string", async () => {
    const result = await copyToClipboard("");
    expect(result).toBe(false);
  });

  it("should use navigator.clipboard.writeText when available and successful", async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(globalThis, "navigator", {
      value: { clipboard: { writeText: writeTextMock } },
      configurable: true,
      writable: true,
    });

    const result = await copyToClipboard("test content");
    expect(result).toBe(true);
    expect(writeTextMock).toHaveBeenCalledWith("test content");
  });

  it("should fallback to document.execCommand when navigator.clipboard is undefined (HTTP environment)", async () => {
    // navigator without clipboard (typical in non-secure HTTP context)
    Object.defineProperty(globalThis, "navigator", {
      value: {},
      configurable: true,
      writable: true,
    });

    const execCommandMock = vi.fn().mockReturnValue(true);
    const appendChildMock = vi.fn();
    const removeChildMock = vi.fn();
    const mockTextarea = {
      value: "",
      setAttribute: vi.fn(),
      style: {},
      focus: vi.fn(),
      select: vi.fn(),
      setSelectionRange: vi.fn(),
    };

    Object.defineProperty(globalThis, "document", {
      value: {
        createElement: vi.fn().mockReturnValue(mockTextarea),
        body: {
          appendChild: appendChildMock,
          removeChild: removeChildMock,
        },
        execCommand: execCommandMock,
      },
      configurable: true,
      writable: true,
    });

    const result = await copyToClipboard("endpoint url");
    expect(result).toBe(true);
    expect(mockTextarea.value).toBe("endpoint url");
    expect(appendChildMock).toHaveBeenCalledWith(mockTextarea);
    expect(execCommandMock).toHaveBeenCalledWith("copy");
    expect(removeChildMock).toHaveBeenCalledWith(mockTextarea);
  });

  it("should fallback to document.execCommand when navigator.clipboard.writeText throws", async () => {
    const writeTextMock = vi.fn().mockRejectedValue(new Error("Permission denied"));
    Object.defineProperty(globalThis, "navigator", {
      value: { clipboard: { writeText: writeTextMock } },
      configurable: true,
      writable: true,
    });

    const execCommandMock = vi.fn().mockReturnValue(true);
    const appendChildMock = vi.fn();
    const removeChildMock = vi.fn();
    const mockTextarea = {
      value: "",
      setAttribute: vi.fn(),
      style: {},
      focus: vi.fn(),
      select: vi.fn(),
      setSelectionRange: vi.fn(),
    };

    Object.defineProperty(globalThis, "document", {
      value: {
        createElement: vi.fn().mockReturnValue(mockTextarea),
        body: {
          appendChild: appendChildMock,
          removeChild: removeChildMock,
        },
        execCommand: execCommandMock,
      },
      configurable: true,
      writable: true,
    });

    const result = await copyToClipboard("api-key-secret");
    expect(result).toBe(true);
    expect(writeTextMock).toHaveBeenCalledWith("api-key-secret");
    expect(execCommandMock).toHaveBeenCalledWith("copy");
  });

  it("should return false when fallback document.execCommand fails", async () => {
    Object.defineProperty(globalThis, "navigator", {
      value: {},
      configurable: true,
      writable: true,
    });

    const execCommandMock = vi.fn().mockReturnValue(false);
    const mockTextarea = {
      value: "",
      setAttribute: vi.fn(),
      style: {},
      focus: vi.fn(),
      select: vi.fn(),
      setSelectionRange: vi.fn(),
    };

    Object.defineProperty(globalThis, "document", {
      value: {
        createElement: vi.fn().mockReturnValue(mockTextarea),
        body: {
          appendChild: vi.fn(),
          removeChild: vi.fn(),
        },
        execCommand: execCommandMock,
      },
      configurable: true,
      writable: true,
    });

    const result = await copyToClipboard("test");
    expect(result).toBe(false);
  });
});
