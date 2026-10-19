export interface ProcessInstanceAttachmentSetEntity {
    id: number;
    name: string;
    dataKey: string;
    processInstanceId: number;
    processInstanceTaskId: number | null;
}
