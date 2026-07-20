import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {ProcessInstanceAttachmentSetEntity} from '../entities/process-instance-attachment-set-entity';

interface ProcessInstanceAttachmentSetFilter {
    processInstanceId: number;
    processInstanceTaskId: number;
    name: string;
    dataKey: string;
}

export class ProcessInstanceAttachmentSetApiService extends BaseCrudApiService<
    ProcessInstanceAttachmentSetEntity,
    ProcessInstanceAttachmentSetEntity,
    ProcessInstanceAttachmentSetEntity,
    ProcessInstanceAttachmentSetEntity,
    number,
    ProcessInstanceAttachmentSetFilter
> {
    constructor() {
        super('/api/process-instance-attachment-sets/');
    }

    initialize(): ProcessInstanceAttachmentSetEntity {
        return {
            id: 0,
            name: '',
            dataKey: '',
            processInstanceId: 0,
            processInstanceTaskId: null,
        };
    }
}
