import {Api} from './use-api';
import type {SubmissionAttachment} from '../models/entities/submission-attachment';
import {Submission, SubmissionListProjection} from '../models/entities/submission';
import {SubmissionStatus} from '../data/submission-status';

interface SubmissionsApi {
    list(filter: {
        includePaymentPending?: boolean | undefined,
        includeArchived?: boolean | undefined,
        includeTest?: boolean | undefined,
        assigneeId?: string | undefined,
        formId?: number | undefined,
    }): Promise<SubmissionListProjection[]>;

    retrieve(submissionId: string): Promise<Submission>;

    listAttachments(submissionId: string): Promise<SubmissionAttachment[]>;

    resentDestination(submissionId: string): Promise<Submission>;

    reprintPdf(submissionId: string): Promise<Submission>;

    downloadPdf(submissionId: string): Promise<Blob>;

    downloadAttachment(submissionId: string, attachmentId: string): Promise<Blob>;

    downloadDestinationData(submissionId: string): Promise<object>;

    save(submission: Submission): Promise<Submission>;
}

export function useSubmissionsApi(api: Api): SubmissionsApi {
    const list = async (filter: {
        includePaymentPending: boolean | undefined,
        includeDestinationPending: boolean | undefined,
        includeArchived?: boolean | undefined,
        includeTest?: boolean | undefined,
        assigneeId?: string | undefined,
        formId?: number | undefined,
    }): Promise<SubmissionListProjection[]> => {
        const filters = {
            includeTest: filter.includeTest,
            assigneeId: filter.assigneeId,
            formId: filter.formId,
            includeStatus: [
                SubmissionStatus.OpenForManualWork,
                SubmissionStatus.HasPaymentError,
                SubmissionStatus.HasDestinationError,
                SubmissionStatus.ManualWorkingOn,
            ] as any[],
        };

        if (filter.includePaymentPending) {
            filters.includeStatus.push(SubmissionStatus.PaymentPending);
        }

        if (filter.includeDestinationPending) {
            filters.includeStatus.push(SubmissionStatus.DestinationCallbackPending);
        }

        if (filter.includeArchived) {
            filters.includeStatus.push(SubmissionStatus.Archived);
        }


        return await api.get<SubmissionListProjection[]>('submissions', filters);
    };

    const retrieve = async (submissionId: string): Promise<Submission> => {
        return await api.get(`submissions/${submissionId}`);
    };

    const listAttachments = async (submissionId: string): Promise<SubmissionAttachment[]> => {
        return await api.get(`submissions/${submissionId}/attachments`);
    };

    const resentDestination = async (submissionId: string): Promise<Submission> => {
        return await api.post(`submissions/${submissionId}/resend`, {});
    };

    const reprintPdf = async (submissionId: string): Promise<Submission> => {
        return await api.put<Submission>(`submissions/${submissionId}/reprint`, {});
    };

    const downloadPdf = async (submissionId: string): Promise<Blob> => {
        return await api.getBlob(`submissions/${submissionId}/print`);
    };

    const downloadDestinationData = async (submissionId: string): Promise<Blob> => {
        return await api.get(`submissions/${submissionId}/destination-data`);
    };

    const downloadAttachment = async (submissionId: string, attachmentId: string): Promise<Blob> => {
        return await api.getBlob(`submissions/${submissionId}/attachments/${attachmentId}`);
    };

    const save = async (submission: Submission): Promise<Submission> => {
        return await api.put(`submissions/${submission.id}`, submission);
    };

    return {
        list,
        retrieve,
        listAttachments,
        resentDestination,
        downloadPdf,
        reprintPdf,
        downloadAttachment,
        downloadDestinationData,
        save,
    };
}
