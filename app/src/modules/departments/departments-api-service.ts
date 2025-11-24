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
        super('api/organizational-units/');
    }

    public initialize(): DepartmentResponseDTO {
        return {
            additionalInfo: undefined,
            address: undefined,
            commonAccessibility: undefined,
            commonPrivacy: undefined,
            created: '',
            departmentMail: undefined,
            depth: 0,
            id: 0,
            imprint: undefined,
            name: new Date().toISOString(),
            parentOrgUnitId: undefined,
            specialSupportAddress: undefined,
            specialSupportInfo: undefined,
            specialSupportPhone: undefined,
            technicalSupportAddress: undefined,
            technicalSupportInfo: undefined,
            technicalSupportPhone: undefined,
            themeId: undefined,
            updated: new Date().toISOString(),
        };
    }

    public retrievePublic(id: number): Promise<DepartmentResponseDTO> {
        return this.get<DepartmentResponseDTO>(`api/public/organizational-units/${id}/`, {
            skipAuthCheck: true,
        });
    }
}