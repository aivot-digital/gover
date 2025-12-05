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
            formPermissionAnnotate: false,
            formPermissionCreate: false,
            formPermissionDelete: false,
            formPermissionEdit: false,
            formPermissionPublish: false,
            formPermissionRead: false,
            departmentPermissionEdit: false,
            teamPermissionEdit: false,
            processInstancePermissionAnnotate: false,
            processInstancePermissionCreate: false,
            processInstancePermissionDelete: false,
            processInstancePermissionEdit: false,
            processInstancePermissionRead: false,
            processPermissionAnnotate: false,
            processPermissionCreate: false,
            processPermissionDelete: false,
            processPermissionEdit: false,
            processPermissionPublish: false,
            processPermissionRead: false,
            updated: '',
            created: '',
        };
    }
}