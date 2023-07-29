export interface SubmissionListDto {
    id: string;
    application: number;
    created: string;
    archived: string | null;
    assignee: number | null;
    fileNumber: string | null;
    destination: number | null;
    destinationSuccess: boolean | null;
    isTestSubmission: boolean;
    reviewScore: number | null;
}
