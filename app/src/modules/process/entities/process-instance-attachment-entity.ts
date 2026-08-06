export interface ProcessInstanceAttachmentEntity {
    key: string;
    fileName: string;
    originalFileName: string;
    group?: string | null;
    attachmentSetId: number;
    processInstanceId: number;
    processInstanceTaskId: number | null;
    storageProviderId: number;
    storagePathFromRoot: string;
    uploadedByUserId: string | null;
}
