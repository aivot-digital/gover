export enum SubmissionStatus {
    PaymentPending = -1,
    DestinationCallbackPending = -2,
    OpenForManualWork = 0,
    ManualWorkingOn = 1,
    HasPaymentError = 500,
    HasDestinationError = 501,
    Archived = 999,
}