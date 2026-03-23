export interface ResourceAccessControlRequestDto {
    sourceTeamId?: number | null;
    sourceOrgUnitId?: number | null;
    targetFormId?: number | null;
    targetProcessId?: number | null;
    targetProcessInstanceId?: number | null;
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

