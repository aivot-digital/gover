export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    fullName: string;
    enabled: boolean;
    verified: boolean;
    globalRole: 0 | 1 | 2; // 0: User, 1: System Admin, 2: Super Admin
    superAdmin: boolean;
    systemAdmin: boolean;
    deletedInIdp: boolean;
}