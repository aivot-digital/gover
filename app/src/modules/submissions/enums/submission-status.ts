export enum SubmissionStatus {
    Pending = -1,
    OpenForManualWork = 0,
    ManualWorkingOn = 1,
    HasPaymentError = 500,
    HasDestinationError = 501,
    Archived = 999,
}