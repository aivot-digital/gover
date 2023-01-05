export interface BaseEditorProps<T> {
    component: T;
    onPatch: (patch: Partial<T>) => void;
}
