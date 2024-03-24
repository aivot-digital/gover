import {Api} from './use-api';
import type {SubmissionAttachment} from '../models/entities/submission-attachment';
import {Submission, SubmissionListProjection} from '../models/entities/submission';

interface SubmissionsApi {
    list(filter: {
        includeArchived?: boolean | undefined,
        includeTest?: boolean | undefined,
        assigneeId?: string | undefined,
        formId?: number | undefined,
    }): Promise<SubmissionListProjection[]>;

    retrieve(submissionId: string): Promise<Submission>;

    listAttachments(submissionId: string): Promise<SubmissionAttachment[]>;

    resentDestination(submissionId: string): Promise<Submission>;

    downloadPdf(submissionId: string): Promise<Blob>;

    downloadAttachment(submissionId: string, attachmentId: string): Promise<Blob>;

    save(submission: Submission): Promise<Submission>;
}

export function useSubmissionsApi(api: Api): SubmissionsApi {
    const list = async (filter: {
        includeArchived?: boolean | undefined,
        includeTest?: boolean | undefined,
        assigneeId?: string | undefined,
        formId?: number | undefined,
    }): Promise<SubmissionListProjection[]> => {
        return await api.get<SubmissionListProjection[]>('submissions', filter);
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

    const downloadPdf = async (submissionId: string): Promise<Blob> => {
        return await api.getBlob(`submissions/${submissionId}/print`);
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
        downloadAttachment,
        save,
    };
}
