export enum UserRole {
    Editor = 0,
    Publisher = 1,
    Admin = 2,
}

export const UserRoleLabels: {
    [key in UserRole]: string;
} = {
    [UserRole.Admin]: 'Fachbereichs-Administrator:in',
    [UserRole.Publisher]: 'Veröffentlicher:in',
    [UserRole.Editor]: 'Bearbeiter:in',
};

export const UserRoleLevels: {
    [key in UserRole]: number;
} = {
    [UserRole.Admin]: 999,
    [UserRole.Publisher]: 100,
    [UserRole.Editor]: 0,
};
