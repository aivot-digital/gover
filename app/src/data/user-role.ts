export enum UserRole {
    Editor = 0,
    Publisher = 1,
    Admin = 2,
}

export const UserRoleLabels: Record<UserRole, string> = {
    [UserRole.Admin]: 'Fachbereichs-Administrator:in',
    [UserRole.Publisher]: 'Veröffentlicher:in',
    [UserRole.Editor]: 'Bearbeiter:in',
};

export const UserRoleIdentifier: Record<UserRole, string> = {
    [UserRole.Admin]: 'Admin',
    [UserRole.Publisher]: 'Publisher',
    [UserRole.Editor]: 'Editor',
};