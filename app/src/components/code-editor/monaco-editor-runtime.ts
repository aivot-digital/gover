import './configure-monaco';
import Editor from '@monaco-editor/react';

// The local worker and loader configuration must run before any editor mounts.
export const MonacoEditorImplementation = Editor;
