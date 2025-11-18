import {DepartmentRequestDTO} from './dtos/department-request-dto';
import {DepartmentResponseDTO} from './dtos/department-response-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';

interface DepartmentFilters {
    userId: string;
    departmentId: number;
    membershipId: number;
    themeId: number;
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

export class DepartmentsApiService extends BaseCrudApiService<DepartmentRequestDTO, DepartmentResponseDTO, DepartmentResponseDTO, DepartmentRequestDTO, number, DepartmentFilters> {
    public constructor() {
        super('api/departments/');
    }

    public initialize(): DepartmentResponseDTO {
        return {
            additionalInfo: null,
            contactLegal: null,
            contactTechnical: null,
            departmentMail: null,
            depth: 0,
            parentOrgUnitId: null,
            themeId: null,
            id: 0,
            name: '',
            address: null,
            accessibility: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
            imprint: null,
            privacy: null,
            specialSupportAddress: null,
            technicalSupportAddress: null,
        };
    }

    public retrievePublic(id: number): Promise<DepartmentResponseDTO> {
        return this.get<DepartmentResponseDTO>(`api/public/departments/${id}/`, {
            skipAuthCheck: true,
        });
    }
}