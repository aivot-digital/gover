export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    fullName: string;
    enabled: boolean;
    verified: boolean;
    deletedInIdp: boolean;
    systemRoleId: number | null;
    artificialUser?: boolean | null;
}
