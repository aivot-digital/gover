export const AppInfo = {
    version: '@buildVersion',
    number: '@buildNumber',
    date: '@buildTimestamp',

    mode: import.meta.env.VITE_APP_MODE === 'customer' ? 'customer' : 'staff',

    maxFileSizeMB: 10,
};