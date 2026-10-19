export interface SystemRoleEntity {
    id: number;
    name: string;
    description: string | null;
    permissions: string[];
    created: string; // ISO date string
    updated: string; // ISO date string
}