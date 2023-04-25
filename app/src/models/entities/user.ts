import {UserRole} from "../../data/user-role";


export interface User {
    id: number;
    name: string;
    email: string;
    active: boolean;
    role: UserRole;
}
