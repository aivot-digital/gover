import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessInstanceAccessControlEntity} from '../entities/process-instance-access-control-entity';

export type ProcessInstanceAccessSelectableItemType = 'orgUnit' | 'team' | 'user';

export interface ProcessInstanceAccessSelectableItem {
    type: ProcessInstanceAccessSelectableItemType;
    id: string;
    label?: string | null;
    subLabel?: string | null;
    departmentDepth?: number | null;
    eligibleUserCount?: number | null;
}

export class ProcessInstanceAccessControlApiService extends BaseCrudApiService<
    ProcessInstanceAccessControlEntity,
    ProcessInstanceAccessControlEntity,
    ProcessInstanceAccessControlEntity,
    ProcessInstanceAccessControlEntity,
    number,
    {targetProcessInstanceId: number}
> {
    constructor() {
        super('/api/process-instance-access-controls/');
    }

    initialize(): ProcessInstanceAccessControlEntity {
        return {
            id: 0,
            sourceDepartmentId: null,
            sourceTeamId: null,
            targetProcessInstanceId: 0,
            permissions: [],
            created: '',
            updated: '',
        };
    }

    /**
     * Replaces all rules of the instance. `loaded` are the stored rules the change is based on; the server rejects the
     * save with a conflict when they were changed, removed or joined by other rules meanwhile.
     */
    public replace(
        instanceId: number,
        entries: ProcessInstanceAccessControlEntity[],
        loaded: ProcessInstanceAccessControlEntity[],
    ): Promise<ProcessInstanceAccessControlEntity[]> {
        const rules = entries.map(({sourceDepartmentId, sourceTeamId, permissions}) => ({
            sourceDepartmentId,
            sourceTeamId,
            permissions,
        }));
        const basedOn = loaded.every((entry) => entry.lockVersion != null)
            ? loaded.map((entry) => `${entry.id}:${entry.lockVersion}`).join(',')
            : undefined;
        return this.put(`${this.path}instance/${instanceId}/`, rules, {query: {basedOn}});
    }

    public async listPotentialSelectableItems(
        processId: number,
        processVersion: number,
        requiredPermissions: string[] | undefined,
    ): Promise<ProcessInstanceAccessSelectableItem[]> {
        return await this.get<ProcessInstanceAccessSelectableItem[]>(
            '/api/process-instance-access-controls/potential-options/',
            {
                query: {
                    processId,
                    processVersion,
                    requiredPermissions:
                        requiredPermissions != null && requiredPermissions.length > 0 ? requiredPermissions : undefined,
                },
            },
        );
    }
}
