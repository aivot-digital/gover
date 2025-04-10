export interface SubmissionStatusResponseDTO {
    submissionId: string;
    paymentProviderName?: string | null;
    paymentProviderUrl?: string | null;
    paymentDone?: boolean | null;
    paymentFailed?: boolean | null;
    accessExpired: boolean;
    copySent: boolean;
}