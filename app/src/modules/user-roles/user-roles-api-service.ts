import {UserRoleRequestDTO} from './dtos/user-role-request-dto';
import {UserRoleResponseDTO} from './dtos/user-role-response-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';

interface UserRoleFilter {
    name: string;
}

export class UserRolesApiService extends BaseCrudApiService<UserRoleRequestDTO, UserRoleResponseDTO, UserRoleResponseDTO, UserRoleRequestDTO, number, UserRoleFilter> {
    constructor() {
        super('/api/user-roles/');
    }


    public initialize(): UserRoleResponseDTO {
        return {
            id: 0,
            name: '',
            description: null,
            permissions: [],
            updated: '',
            created: '',
        };
    }
}