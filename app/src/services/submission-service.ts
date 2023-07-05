import {ApiService} from "./api-service";
import {SubmissionListDto} from "../models/entities/submission-list-dto";
import {SubmissionDetailsDto} from "../models/entities/submission-details-dto";
import {SubmissionAttachmentListDto} from "../models/entities/submission-attachment-list-dto";


class _SubmissionService {

    public async list(application: number, includeArchived: boolean, assignee?: number): Promise<SubmissionListDto[]> {
        return ApiService.get(`submissions/${application}?archived=${includeArchived ? 'true' : 'false'}${assignee != null ? '&assignee=' + assignee : ''}`);
    }

    public async retrieve(application: number, submission: string): Promise<SubmissionDetailsDto> {
        return ApiService.get(`submissions/${application}/${submission}`);
    }

    public async retrieveAttachments(application: number, submission: string): Promise<SubmissionAttachmentListDto[]> {
        return ApiService.get(`submissions/${application}/${submission}/attachments`);
    }

    public async resentDestination(application: number, submission: string): Promise<SubmissionDetailsDto> {
        return ApiService.post(`submissions/${application}/${submission}/resend`, {});
    }

    public async downloadPdf(application: number, submission: string): Promise<Blob> {
        return ApiService.getBlob(`submissions/${application}/${submission}/print`);
    }

    public async downloadAttachment(application: number, submission: string, attachment: string): Promise<Blob> {
        return ApiService.getBlob(`submissions/${application}/${submission}/attachments/${attachment}`);
    }

    public async update(application: number, id: string, submission: SubmissionListDto): Promise<SubmissionDetailsDto> {
        return ApiService.patch(`submissions/${application}/${id}`, submission);
    }
}

export const SubmissionService = new _SubmissionService();
