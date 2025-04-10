import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {DepartmentRequestDTO} from './dtos/department-request-dto';
import {DepartmentResponseDTO} from './dtos/department-response-dto';

interface DepartmentFilters {
    userId: string;
    departmentId: number;
    membershipId: number;
    departmentName: string;
    userEmail: string;
    userName: string;
    userEnabled: boolean;
    userVerified: boolean;
    userGlobalAdmin: boolean;
    membershipRole: string;
    ignoreMemberships: boolean;
    deletedInIdp: boolean;
    disabledInIdp: boolean;
}

export class DepartmentsApiService extends CrudApiService<DepartmentRequestDTO, DepartmentResponseDTO, DepartmentResponseDTO, DepartmentResponseDTO, DepartmentResponseDTO, number, DepartmentFilters> {
    public constructor(api: Api) {
        super(api, 'departments/');
    }

    public initialize(): DepartmentResponseDTO {
        return {
            id: 0,
            name: '',
            address: '',
            accessibility: '',
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
            imprint: '',
            privacy: '',
            specialSupportAddress: '',
            technicalSupportAddress: '',
        };
    }
}