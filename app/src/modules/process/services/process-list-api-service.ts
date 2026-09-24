import {BaseApiService} from '../../../services/base-api-service';
import {Page} from '../../../models/dtos/page';
import {
    ProcessInstanceListEntry,
    ProcessTaskListEntry,
    ProcessListFilter,
    ProcessListOptions,
} from '../entities/process-list';

export class ProcessListApiService extends BaseApiService {
    public instances(
        page: number,
        size: number,
        sort: string | undefined,
        order: string | undefined,
        filter: ProcessListFilter,
    ): Promise<Page<ProcessInstanceListEntry>> {
        return this.get('/api/process-lists/instances/', {
            query: {
                page,
                size,
                sort: sort ? `${sort},${order ?? 'DESC'}` : undefined,
                ...filter,
            },
        });
    }

    public tasks(
        page: number,
        size: number,
        sort: string | undefined,
        order: string | undefined,
        filter: ProcessListFilter,
    ): Promise<Page<ProcessTaskListEntry>> {
        return this.get('/api/process-lists/tasks/', {
            query: {
                page,
                size,
                sort: sort ? `${sort},${order ?? 'ASC'}` : undefined,
                ...filter,
            },
        });
    }

    public options(tasks: boolean, instanceId?: number): Promise<ProcessListOptions> {
        return this.get(`/api/process-lists/${tasks ? 'tasks' : 'instances'}/options/`, {query: {instanceId}});
    }
}
