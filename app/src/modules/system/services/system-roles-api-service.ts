import {SystemRoleEntity} from "../entities/system-role-entity";
import {BaseCrudApiService} from "../../../services/base-crud-api-service";

interface SystemRoleFilter {
    name: string;
}

export class SystemRolesApiService extends BaseCrudApiService<
    SystemRoleEntity,
    SystemRoleEntity,
    SystemRoleEntity,
    SystemRoleEntity,
    number,
    SystemRoleFilter
> {
    constructor() {
        super('/api/system-roles/');
    }

    public initialize(): SystemRoleEntity {
        return SystemRolesApiService.initialize();
    }

    public static initialize(): SystemRoleEntity {
        return {
            id: 0,
            name: '',
            description: null,
            permissions: [],
            updated: '',
            created: '',
        };
    }

    public async destroyWithMigration(
        id: number,
        replacementSystemRoleId: number | null,
    ): Promise<void> {
        await this.destroy(id, {
            query: {
                replacementSystemRoleId: replacementSystemRoleId ?? undefined,
            },
        });
    }
}
