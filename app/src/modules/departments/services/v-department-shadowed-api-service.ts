import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {VDepartmentShadowedEntity, VDepartmentShadowedEntityWithChildren} from '../entities/v-department-shadowed-entity';
import {DepartmentApiService} from './department-api-service';

interface VDepartmentShadowedFilter {
    id: number;
    ids: number[];
    name: string;
    themeId: number;
}

export class VDepartmentShadowedApiService extends BaseCrudApiService<
    VDepartmentShadowedEntity,
    VDepartmentShadowedEntity,
    VDepartmentShadowedEntity,
    VDepartmentShadowedEntity,
    number,
    VDepartmentShadowedFilter
> {
    public constructor() {
        super('api/departments-shadowed/');
    }

    public initialize(): VDepartmentShadowedEntity {
        return DepartmentApiService.initialize();
    }

    public async retrieveOrgTree(): Promise<VDepartmentShadowedEntityWithChildren[]> {
        const allOrgUnits = await this.listAll();

        const orgUnitMap: Record<number, VDepartmentShadowedEntityWithChildren> = {};
        for (const orgUnit of allOrgUnits.content) {
            orgUnitMap[orgUnit.id] = {...orgUnit, children: []};
        }

        const rootOrgUnits: VDepartmentShadowedEntityWithChildren[] = [];
        for (const orgUnit of allOrgUnits.content) {
            if (orgUnit.parentDepartmentId && orgUnitMap[orgUnit.parentDepartmentId]) {
                orgUnitMap[orgUnit.parentDepartmentId].children.push(orgUnitMap[orgUnit.id]);
            } else {
                rootOrgUnits.push(orgUnitMap[orgUnit.id]);
            }
        }

        // Helper function to sort children recursively
        function sortTree(nodes: VDepartmentShadowedEntityWithChildren[]): VDepartmentShadowedEntityWithChildren[] {
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