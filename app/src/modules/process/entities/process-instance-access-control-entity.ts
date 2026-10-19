export interface ProcessInstanceAccessControlEntity {
    id: number;
    sourceDepartmentId: number | null;
    sourceTeamId: number | null;
    targetProcessInstanceId: number;
    permissions: string[];
    created: string;
    updated: string;
}
