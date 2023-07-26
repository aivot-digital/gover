export interface ElementEditorActionsProps {
    onSave: () => void;
    onCancel: () => void;
    onDelete?: () => void;
    onSaveAsPreset?: () => void;
    onClone?: () => void;
    editable: boolean;
}
