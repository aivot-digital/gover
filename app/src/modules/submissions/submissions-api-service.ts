import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {SubmissionRequestDTO} from './dtos/submission-reqeust-dto';
import {SubmissionListResponseDTO} from './dtos/submission-list-response-dto';
import {SubmissionDetailsResponseDTO} from './dtos/submission-details-response-dto';
import {SubmissionStatusResponseDTO} from './dtos/submission-status-response-dto';
import {SubmissionStatus} from './enums/submission-status';
import {Page} from '../../models/dtos/page';
import {SubmissionAttachmentResponseDTO} from './dtos/submission-attachment-response-dto';

interface SubmissionFilters {
    formId: number;
    assigneeId: string;
    status: string;
    fileNumber: string;
    destinationId: number;
    notTestSubmission: boolean;
    notArchived: boolean;
    notPending: boolean;
}

export class SubmissionsApiService extends CrudApiService<SubmissionRequestDTO, SubmissionListResponseDTO, SubmissionListResponseDTO, SubmissionDetailsResponseDTO, SubmissionDetailsResponseDTO, string, SubmissionFilters> {
    public constructor(api: Api) {
        super(api, 'submissions/');
    }

    public initialize(): SubmissionDetailsResponseDTO {
        return {
            assigneeId: '',
            copySent: false,
            copyTries: 0,
            created: '',
            customerInput: {},
            destinationId: 0,
            destinationResult: '',
            destinationSuccess: false,
            destinationTimestamp: '',
            fileNumber: '',
            formId: 0,
            id: '',
            isTestSubmission: false,
            paymentTransactionKey: '',
            reviewScore: 0,
            status: SubmissionStatus.Pending,
            updated: '',
            archived: '',
        };
    }

    public async getStatus(id: string): Promise<SubmissionStatusResponseDTO> {
        return await this.api.getPublic<SubmissionStatusResponseDTO>(`status/${id}/`);
    }

    public resendDestination(id: string): Promise<SubmissionDetailsResponseDTO> {
        return this.api.post<SubmissionDetailsResponseDTO>(`submissions/${id}/resend/`, {});
    }

    public restartPayment(id: string): Promise<SubmissionDetailsResponseDTO> {
        return this.api.post<SubmissionDetailsResponseDTO>(`submissions/${id}/restart-payment/`, {});
    }

    public cancelPayment(id: string): Promise<SubmissionDetailsResponseDTO> {
        return this.api.post<SubmissionDetailsResponseDTO>(`submissions/${id}/cancel-payment/`, {});
    }

    public listAttachments(submissionId: string, page: number, limit: number): Promise<Page<SubmissionAttachmentResponseDTO>> {
        return this.api.get<Page<SubmissionAttachmentResponseDTO>>(`submissions/${submissionId}/attachments/`, {
            queryParams: {
                page: page,
                size: limit,
            },
        });
    }

    public downloadPdf(submissionId: string): Promise<Blob> {
        return this.api.getBlob(`submissions/${submissionId}/attachments/ausdruck.pdf`);
    }

    public downloadGoverData(submissionId: string): Promise<any> {
        return this.api.get<any>(`submissions/${submissionId}/attachments/gover-data.json`);
    }

    public downloadDestinationData(submissionId: string): Promise<any> {
        return this.api.get<any>(`submissions/${submissionId}/attachments/destination-data.json`);
    }

    public downloadAttachment(submissionId: string, attachmentId: string): Promise<Blob> {
        return this.api.getBlob(`submissions/${submissionId}/attachments/${attachmentId}/`);
    }
}