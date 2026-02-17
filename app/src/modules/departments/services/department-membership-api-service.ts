import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {DepartmentMembershipEntity} from '../entities/department-membership-entity';

interface DepartmentMembershipFilter {
    departmentId: number;
    departmentIds: number[];
    userId: string;
    userIds: string[];
}

export class DepartmentMembershipApiService extends BaseCrudApiService<
    DepartmentMembershipEntity,
    DepartmentMembershipEntity,
    DepartmentMembershipEntity,
    DepartmentMembershipEntity,
    number,
    DepartmentMembershipFilter
> {
    public constructor() {
        super('api/department-memberships/');
    }

    public initialize(): DepartmentMembershipEntity {
        return DepartmentMembershipApiService.initialize();
    }

    public static initialize(): DepartmentMembershipEntity {
        return {
            id: 0,
            userId: '',
            departmentId: 0,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }
}