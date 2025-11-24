export interface UserRoleRequestDTO {
    name: string;
    description?: string | null;
    orgUnitMemberPermissionEdit: boolean;
    teamMemberPermissionEdit: boolean;
    formPermissionCreate: boolean;
    formPermissionRead: boolean;
    formPermissionEdit: boolean;
    formPermissionDelete: boolean;
    formPermissionAnnotate: boolean;
    formPermissionPublish: boolean;
    processPermissionCreate: boolean;
    processPermissionRead: boolean;
    processPermissionEdit: boolean;
    processPermissionDelete: boolean;
    processPermissionAnnotate: boolean;
    processPermissionPublish: boolean;
    processInstancePermissionCreate: boolean;
    processInstancePermissionRead: boolean;
    processInstancePermissionEdit: boolean;
    processInstancePermissionDelete: boolean;
    processInstancePermissionAnnotate: boolean;
}