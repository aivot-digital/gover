export interface ProcessInstanceAttachmentEntity {
    key: string;
    fileName: string;
    processInstanceId: number;
    processInstanceTaskId: number | null;
    storageProviderId: number;
    storagePathFromRoot: string;
    uploadedByUserId: string | null;
}
