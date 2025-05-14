export enum StorageKey {
    AuthDataAccessToken = 'access_token',
    AuthDataRefreshToken = 'refresh_token',

    IntroDismissed = 'intro_dismissed',

    SubmissionsIncludePaymentPending = 'submissions_include_payment_pending',
    SubmissionsIncludeArchived = 'submissions_include_archived',
    SubmissionsOnlyAssigned = 'submissions_only_assigned',
    SubmissionsIncludeTest = 'submissions_include_test',

    SavedModule = 'saved_module',

    TokenDebuggerActive = 'debug_tokens',
    CaptchaDebuggerActive = 'debug_captcha',
}
