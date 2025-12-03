export enum SystemUserRole {
    Default = 0,
    SystemAdmin = 1,
    SuperAdmin = 2,
}

export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    fullName: string;
    enabled: boolean;
    verified: boolean;
    globalRole: SystemUserRole;
    isSuperAdmin: boolean;
    isSystemAdmin: boolean;
    deletedInIdp: boolean;
}