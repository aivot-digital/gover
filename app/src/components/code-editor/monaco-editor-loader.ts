let editorPromise: Promise<typeof import('./monaco-editor-runtime')> | undefined;

export function isMonacoStylesheetLoadError(error: unknown): boolean {
    // Keep this guard aligned with the error emitted by Vite's CSS preload helper.
    return error instanceof Error && error.message.startsWith('Unable to preload CSS for ');
}

export function loadMonacoEditor(): Promise<typeof import('./monaco-editor-runtime')> {
    editorPromise ??= import('./monaco-editor-runtime').catch((error: unknown) => {
        // Vite's preload helper remembers failed CSS URLs and skips them on a second
        // import. Retain that failure until a page reload instead of mounting an
        // unstyled editor. Other load failures may be retried without losing inputs.
        if (!isMonacoStylesheetLoadError(error)) {
            editorPromise = undefined;
        }
        throw error;
    });
    return editorPromise;
}
