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

export class ShadowedOrganizationalUnitsApiService extends BaseCrudApiService<DepartmentRequestDTO, DepartmentResponseDTO, DepartmentResponseDTO, DepartmentRequestDTO, number, DepartmentFilters> {
    public constructor() {
        super('api/organizational-units-shadowed/');
    }

    public initialize(): DepartmentResponseDTO {
        return {
            id: 0,
            name: '',
            address: null,
            accessibility: null,
            imprint: null,
            privacy: null,
            specialSupportAddress: null,
            technicalSupportAddress: null,
            themeId: null,
            contactTechnical: null,
            additionalInfo: null,
            contactLegal: null,
            depth: 0,
            parentOrgUnitId: null,
            departmentMail: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }

    public async retrieveOrgTree(): Promise<OrganizationalUnitWithChildren[]> {
        const allOrgUnits = await this.listAll();

        const orgUnitMap: Record<number, OrganizationalUnitWithChildren> = {};
        for (const orgUnit of allOrgUnits.content) {
            orgUnitMap[orgUnit.id] = {...orgUnit, children: []};
        }

        const rootOrgUnits: OrganizationalUnitWithChildren[] = [];
        for (const orgUnit of allOrgUnits.content) {
            if (orgUnit.parentOrgUnitId && orgUnitMap[orgUnit.parentOrgUnitId]) {
                orgUnitMap[orgUnit.parentOrgUnitId].children.push(orgUnitMap[orgUnit.id]);
            } else {
                rootOrgUnits.push(orgUnitMap[orgUnit.id]);
            }
        }

        return rootOrgUnits;
    }
}

export interface OrganizationalUnitWithChildren extends DepartmentResponseDTO {
    children: OrganizationalUnitWithChildren[];
}