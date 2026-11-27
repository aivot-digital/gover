export interface ProcessInstanceAttachmentEntity {
    key: string;
    fileName: string;
    originalFileName: string;
    sizeInBytes?: number | null;
    group?: string | null;
    attachmentSetId: number;
    processInstanceId: number;
    processInstanceTaskId: number | null;
    storageProviderId: number;
    storagePathFromRoot: string;
    uploadedByUserId: string | null;
}
