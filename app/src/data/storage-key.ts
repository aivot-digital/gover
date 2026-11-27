export enum StorageKey {
    AuthDataAccessToken = 'access_token',
    AuthDataRefreshToken = 'refresh_token',

    PreReleaseVersionNoticeDismissed = 'pre_release_version_notice_dismissed',
    ColorMode = 'color_mode',

    SubmissionsIncludePaymentPending = 'submissions_include_payment_pending',
    SubmissionsIncludeArchived = 'submissions_include_archived',
    SubmissionsOnlyAssigned = 'submissions_only_assigned',
    SubmissionsIncludeTest = 'submissions_include_test',

    SavedModule = 'saved_module',

    ProcessInstanceListColumns = 'process_instance_list_columns',
    ProcessTaskListColumns = 'process_task_list_columns',
    ProcessInstanceTaskListColumns = 'process_instance_task_list_columns',

    TokenDebuggerActive = 'debug_tokens',
    CaptchaDebuggerActive = 'debug_captcha',

    ShowExperimentalFeatures = 'show_experimental_features',
}
