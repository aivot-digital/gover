import type {CustomerInput} from '../customer-input';
import {XBezahldienstePaymentRequest} from '../xbezahldienste/x-bezahldienste-payment-request';
import {XBezahldienstePaymentInformation} from '../xbezahldienste/x-bezahldienste-paymentInformation';
import {XBezahldienstePaymentStatus} from '../../data/xbezahldienste-payment-status';
import {SubmissionStatus} from '../../data/submission-status';

export interface Submission {
    id: string;
    formId: number;
    created: string;
    assigneeId: string | null;
    fileNumber: string | null;
    destinationId: number | null;
    customerInput: CustomerInput;
    destinationSuccess: boolean | null;
    isTestSubmission: boolean;
    copySent: boolean;
    copyTries: number;
    reviewScore: number | null;
    destinationResult: string | null;
    destinationTimestamp: string | null;
    archived: string | null;
    status: SubmissionStatus;
    paymentRequest: XBezahldienstePaymentRequest | null;
    paymentInformation: XBezahldienstePaymentInformation | null;
    paymentError: string | null;
    paymentProvider: string | null;
    paymentOriginatorId: string | null;
    paymentEndpointId: string | null;
}

export type SubmissionListProjection = Omit<Submission, 'customerInput'>;
