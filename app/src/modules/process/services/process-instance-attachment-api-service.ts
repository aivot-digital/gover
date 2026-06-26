import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {ProcessInstanceAttachmentEntity} from '../entities/process-instance-attachment-entity';

interface ProcessInstanceAttachmentFilter {
    processInstanceId: number;
    processInstanceTaskId: number;
    fileName: string;
    mimeType: string;
    uploadedByUserId: string;
}

export class ProcessInstanceAttachmentApiService extends BaseCrudApiService<
    ProcessInstanceAttachmentEntity,
    ProcessInstanceAttachmentEntity,
    ProcessInstanceAttachmentEntity,
    ProcessInstanceAttachmentEntity,
    string,
    ProcessInstanceAttachmentFilter
> {
    constructor() {
        super('/api/process-instance-attachments/');
    }

    initialize(): ProcessInstanceAttachmentEntity {
        return {
            key: '',
            fileName: '',
            processInstanceId: 0,
            processInstanceTaskId: null,
            storageProviderId: 0,
            storagePathFromRoot: '',
            uploadedByUserId: null,
        };
    }
}
