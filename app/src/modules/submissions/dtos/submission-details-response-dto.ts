import {SubmissionStatus} from '../enums/submission-status';
import {CustomerInput} from '../../../models/customer-input';

export interface SubmissionDetailsResponseDTO {
    id: string;
    formId: number;
    created: string;
    updated: string;
    archived: string;
    status: SubmissionStatus;
    assigneeId: string | null | undefined;
    fileNumber: string | null | undefined;
    customerInput: CustomerInput;
    destinationId: number;
    destinationSuccess: boolean;
    destinationResult: string;
    destinationTimestamp: string;
    isTestSubmission: boolean;
    copySent: boolean;
    copyTries: number;
    reviewScore: number;
    paymentTransactionKey: string;
}