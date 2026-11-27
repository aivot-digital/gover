import {createContext, useContext} from 'react';

// Upload fields protect stored attachments. A task view names the stored uploads of its own task, which it may remove.
const RemovableFileUploadAttachmentsContext = createContext<ReadonlySet<string>>(new Set());

export const RemovableFileUploadAttachmentsProvider = RemovableFileUploadAttachmentsContext.Provider;

export function useRemovableFileUploadAttachments(): ReadonlySet<string> {
    return useContext(RemovableFileUploadAttachmentsContext);
}
