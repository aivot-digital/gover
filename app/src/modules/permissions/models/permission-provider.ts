import {type PermissionScope} from '../enums/permission-scope';

export interface PermissionProvider {
    contextLabel: string;
    permissions: PermissionEntry[];
    scope: PermissionScope;
}

export interface PermissionEntry {
    permission: string;
    label: string;
    description: string;
}