import type {CustomerInput} from '../customer-input';

export interface Submission {
    id: string;
    formId: number;
    created: string;
    archived: string | null;
    assigneeId: string | null;
    fileNumber: string | null;
    destinationId: number | null;
    destinationSuccess: boolean | null;
    destinationResult: string | null;
    isTestSubmission: boolean;
    reviewScore: number | null;
    customerInput: CustomerInput;
}

export type SubmissionListProjection = Omit<Submission, 'customerInput'>;
