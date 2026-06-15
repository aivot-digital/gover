import {createContext, useContext} from 'react';
import type {ProcessInstanceAttachmentEntity} from '../../entities/process-instance-attachment-entity';

interface ProcessTaskViewAttachmentContextType {
    attachments: ProcessInstanceAttachmentEntity[];
    isLoadingAttachments: boolean;
    viewAttachment: (attachment: ProcessInstanceAttachmentEntity) => Promise<void>;
    downloadAttachment: (attachment: ProcessInstanceAttachmentEntity) => Promise<void>;
}

const ProcessTaskViewAttachmentContext = createContext<ProcessTaskViewAttachmentContextType | null>(null);

export const ProcessTaskViewAttachmentProvider = ProcessTaskViewAttachmentContext.Provider;

export function useOptionalProcessTaskViewAttachmentContext(): ProcessTaskViewAttachmentContextType | null {
    return useContext(ProcessTaskViewAttachmentContext);
}
