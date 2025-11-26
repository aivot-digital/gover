import {DepartmentRequestDTO} from './dtos/department-request-dto';
import {DepartmentResponseDTO, VOrganizationalUnitShadowedResponseDTO} from './dtos/department-response-dto';
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

export class ShadowedOrganizationalUnitsApiService extends BaseCrudApiService<DepartmentRequestDTO, VOrganizationalUnitShadowedResponseDTO, VOrganizationalUnitShadowedResponseDTO, DepartmentRequestDTO, number, DepartmentFilters> {
    public constructor() {
        super('api/organizational-units-shadowed/');
    }

    public initialize(): VOrganizationalUnitShadowedResponseDTO {
        return {
            additionalInfo: undefined,
            address: undefined,
            commonAccessibility: undefined,
            commonPrivacy: undefined,
            departmentMail: undefined,
            depth: 0,
            id: 0,
            imprint: undefined,
            name: '',
            parentOrgUnitId: undefined,
            specialSupportAddress: undefined,
            specialSupportInfo: undefined,
            specialSupportPhone: undefined,
            technicalSupportAddress: undefined,
            technicalSupportInfo: undefined,
            technicalSupportPhone: undefined,
            themeId: undefined,
            parentNames: undefined,
            parentIds: undefined,
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

        // Helper function to sort children recursively
        function sortTree(nodes: OrganizationalUnitWithChildren[]): OrganizationalUnitWithChildren[] {
            nodes.sort((a, b) => a.name.localeCompare(b.name, undefined, {sensitivity: 'base'}));
            for (const node of nodes) {
                if (node.children && node.children.length > 0) {
                    node.children = sortTree(node.children);
                }
            }
            return nodes;
        }

        return sortTree(rootOrgUnits);
    }
}

export interface OrganizationalUnitWithChildren extends DepartmentResponseDTO {
    children: OrganizationalUnitWithChildren[];
}