import { ApiService } from './api-service';
import { type SubmissionListDto } from '../models/entities/submission-list-dto';
import { type SubmissionDetailsDto } from '../models/entities/submission-details-dto';
import { type SubmissionAttachmentListDto } from '../models/entities/submission-attachment-list-dto';


class _SubmissionService {
    public async list(application: number, includeArchived: boolean, includeTest: boolean, assignee?: number): Promise<SubmissionListDto[]> {
        const params = [];
        params.push(`includeArchived=${ includeArchived ? 'true' : 'false' }`);
        params.push(`includeTest=${ includeTest ? 'true' : 'false' }`);
        if (assignee != null) {
            params.push(`assignee=${ assignee }`);
        }

        return await ApiService.get(`submissions/${ application }?${ params.join('&') }`);
    }

    public async retrieve(application: number, submission: string): Promise<SubmissionDetailsDto> {
        return await ApiService.get(`submissions/${ application }/${ submission }`);
    }

    public async retrieveAttachments(application: number, submission: string): Promise<SubmissionAttachmentListDto[]> {
        return await ApiService.get(`submissions/${ application }/${ submission }/attachments`);
    }

    public async resentDestination(application: number, submission: string): Promise<SubmissionDetailsDto> {
        return await ApiService.post(`submissions/${ application }/${ submission }/resend`, {});
    }

    public async downloadPdf(application: number, submission: string): Promise<Blob> {
        return await ApiService.getBlob(`submissions/${ application }/${ submission }/print`);
    }

    public async downloadAttachment(application: number, submission: string, attachment: string): Promise<Blob> {
        return await ApiService.getBlob(`submissions/${ application }/${ submission }/attachments/${ attachment }`);
    }

    public async update(application: number, id: string, submission: SubmissionListDto): Promise<SubmissionDetailsDto> {
        return await ApiService.patch(`submissions/${ application }/${ id }`, submission);
    }
}

export const SubmissionService = new _SubmissionService();
