export interface ProcessAccessControlEntity {
    id: number;
    sourceTeamId: number | null;
    sourceDepartmentId: number | null;
    targetProcessId: number;
    permissions: string[];
    created: string; // ISO date string
    updated: string; // ISO date string
}
