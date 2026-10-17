import {BaseReadApiService} from '../../../services/base-read-api-service';
import {ProcessInstanceAttachmentEntity} from '../entities/process-instance-attachment-entity';

interface ProcessInstanceAttachmentFilter {
    processInstanceId: number;
    processInstanceTaskId: number;
    fileName: string;
    mimeType: string;
    uploadedByUserId: string;
}

export class ProcessInstanceAttachmentApiService extends BaseReadApiService<
    ProcessInstanceAttachmentEntity,
    ProcessInstanceAttachmentEntity,
    string,
    ProcessInstanceAttachmentFilter
> {
    constructor() {
        super('/api/process-instance-attachments/');
    }

    preview(key: string): Promise<Blob> {
        return this.getBlob(`${this.path}${encodeURIComponent(key)}/file/?download=false`);
    }

    initialize(): ProcessInstanceAttachmentEntity {
        return {
            key: '',
            fileName: '',
            originalFileName: '',
            attachmentSetId: 0,
            processInstanceId: 0,
            processInstanceTaskId: null,
            storageProviderId: 0,
            storagePathFromRoot: '',
            uploadedByUserId: null,
        };
    }
}
