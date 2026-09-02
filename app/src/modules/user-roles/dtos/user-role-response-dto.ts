import {Permission} from "../../../data/permissions/permission";

export interface UserRoleResponseDTO {
    id: number;
    name: string | null;
    description: string | null;
    permissions: Permission[];
    created: string; // ISO date string
    updated: string; // ISO date string
}

export function isUserRoleResponseDTO(object: any): object is UserRoleResponseDTO {
    return (
        typeof object === 'object' &&
        object !== null &&
        typeof object.id === 'number' &&
        (typeof object.name === 'string' || object.name === null) &&
        (typeof object.description === 'string' || object.description === null) &&
        typeof object.departmentPermissionEdit === 'boolean' &&
        typeof object.teamPermissionEdit === 'boolean' &&
        typeof object.formPermissionCreate === 'boolean' &&
        typeof object.formPermissionRead === 'boolean' &&
        typeof object.formPermissionEdit === 'boolean' &&
        typeof object.formPermissionDelete === 'boolean' &&
        typeof object.formPermissionAnnotate === 'boolean' &&
        typeof object.formPermissionPublish === 'boolean' &&
        typeof object.processPermissionCreate === 'boolean' &&
        typeof object.processPermissionRead === 'boolean' &&
        typeof object.processPermissionEdit === 'boolean' &&
        typeof object.processPermissionDelete === 'boolean' &&
        typeof object.processPermissionAnnotate === 'boolean' &&
        typeof object.processPermissionPublish === 'boolean' &&
        typeof object.processInstancePermissionCreate === 'boolean' &&
        typeof object.processInstancePermissionRead === 'boolean' &&
        typeof object.processInstancePermissionEdit === 'boolean' &&
        typeof object.processInstancePermissionDelete === 'boolean' &&
        typeof object.processInstancePermissionAnnotate === 'boolean' &&
        typeof object.created === 'string' &&
        typeof object.updated === 'string'
    );
}