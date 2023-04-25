export enum UserRole {
    Editor = "Editor",
    Publisher = "Publisher",
    Admin = "Admin",
}

export const UserRoleLabels: {
    [key in UserRole]: string;
} = {
    [UserRole.Admin]: 'Admin',
    [UserRole.Publisher]: 'Veröffentlicher',
    [UserRole.Editor]: 'Bearbeiter',
};

export const UserRoleLevels: {
    [key in UserRole]: number;
} = {
    [UserRole.Admin]: 999,
    [UserRole.Publisher]: 100,
    [UserRole.Editor]: 0,
};
