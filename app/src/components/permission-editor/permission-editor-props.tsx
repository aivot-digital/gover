export interface PermissionDetails {
    contextLabel: string;
    permissions: Array<{
        permission: string;
        label: string;
        description: string;
    }>;
}

export interface PermissionEditorProps {
    /** Persisted permissions (used for diff view). */
    originalPermissions?: string[];
    /** Current permissions (controlled). */
    value: string[];
    /** Controlled change callback. */
    onChange: (next: string[]) => void;
    /** When false, disables selection and bulk actions. */
    isEditable?: boolean;
    /** When true, disables inputs and copy action. */
    isBusy?: boolean;
    /** Optional label above editor. */
    title?: string;
    /** Scope of the permissions. */
    scope?: 'System' | 'Domain' | Array<'System' | 'Domain'>;
}