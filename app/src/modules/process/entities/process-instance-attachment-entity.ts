export interface ProcessInstanceAttachmentEntity {
    key: string;
    fileName: string;
    attachmentSetId: number;
    processInstanceId: number;
    processInstanceTaskId: number | null;
    storageProviderId: number;
    storagePathFromRoot: string;
    uploadedByUserId: string | null;
}
