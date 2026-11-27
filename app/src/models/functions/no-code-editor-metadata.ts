import {NoCodeDataType} from '../../data/no-code-data-type';

export type NoCodeTestSource = 'processData' | 'instanceData' | 'nodeData';
export type NoCodeTestValueMode = 'value' | 'json' | 'missing' | 'null' | 'empty';

export interface NoCodeTestValueDraft {
    text: string;
    type: NoCodeDataType;
    mode: NoCodeTestValueMode;
}

export interface NoCodeTestSourceDraft {
    mode?: 'fields' | 'json';
    json?: string;
    values?: Record<string, NoCodeTestValueDraft>;
}

/** Authored examples, including unfinished input; never evaluation results or live case data. */
export interface NoCodeTestData {
    version: 1;
    elementValues: Record<string, NoCodeTestValueDraft>;
    sourceValues: Partial<Record<NoCodeTestSource, NoCodeTestSourceDraft>>;
    visibility: Record<string, string>;
}

/** Belongs to the whole authored expression, even when its root is a leaf. */
export interface NoCodeEditorMetadata {
    notes?: string | null;
    testData?: NoCodeTestData | null;
}
