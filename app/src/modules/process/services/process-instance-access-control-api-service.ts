import {BaseApiService} from '../../../services/base-api-service';

export type ProcessInstanceAccessSelectableItemType = 'orgUnit' | 'team' | 'user';

export interface ProcessInstanceAccessSelectableItem {
    type: ProcessInstanceAccessSelectableItemType;
    id: string;
}

export class ProcessInstanceAccessControlApiService extends BaseApiService {
    public async listPotentialSelectableItems(
        processId: number,
        processVersion: number,
        requiredPermissions: string[] | undefined,
    ): Promise<ProcessInstanceAccessSelectableItem[]> {
        return await this.get<ProcessInstanceAccessSelectableItem[]>('/api/process-instance-access-controls/potential-options/', {
            query: {
                processId,
                processVersion,
                requiredPermissions: requiredPermissions != null && requiredPermissions.length > 0
                    ? requiredPermissions
                    : undefined,
            },
        });
    }
}
