import {SubmissionStatus} from '../enums/submission-status';

export interface SubmissionListResponseDTO {
    id: string;
    formId: number;
    status: SubmissionStatus;
    assigneeId: string;
    fileNumber: string;
    isTestSubmission: boolean;
    destinationId: number;
    destinationSuccess: boolean;
    created: string;
}
