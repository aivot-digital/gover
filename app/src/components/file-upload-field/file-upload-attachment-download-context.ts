import {createContext, useContext} from 'react';
import {BaseApiService} from '../../services/base-api-service';

export type DownloadFileUploadAttachment = (attachmentKey: string) => Promise<Blob>;

// Staff views download stored attachments with the staff member's permissions. Customer task views have no staff
// session and provide their own endpoint, which only serves the attachments that the task view shows.
const FileUploadAttachmentDownloadContext = createContext<DownloadFileUploadAttachment>((attachmentKey) => (
    new BaseApiService().getBlob(`/api/process-instance-attachments/${encodeURIComponent(attachmentKey)}/file/?download=true`)
));

export const FileUploadAttachmentDownloadProvider = FileUploadAttachmentDownloadContext.Provider;

export function useFileUploadAttachmentDownload(): DownloadFileUploadAttachment {
    return useContext(FileUploadAttachmentDownloadContext);
}
