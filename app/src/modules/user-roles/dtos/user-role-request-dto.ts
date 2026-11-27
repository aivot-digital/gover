export interface UserRoleRequestDTO {
    name: string;
    description?: string | null;
    permissions: string[];
}