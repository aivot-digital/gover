import {type PermissionScope} from '../../modules/permissions/enums/permission-scope';
import {type PermissionProvider} from '../../modules/permissions/models/permission-provider';

export type PermissionGroup = Pick<PermissionProvider, 'contextLabel' | 'permissions'>;

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
    scope?: PermissionScope | PermissionScope[];
}
