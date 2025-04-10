export interface User {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    fullName: string;
    enabled: boolean;
    verified: boolean;
    globalAdmin: boolean;
    deletedInIdp: boolean;
}