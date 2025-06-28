export const AppConfig = {
    version: '@buildVersion',
    number: '@buildNumber',
    date: '@buildTimestamp',

    mode: import.meta.env.VITE_APP_MODE === 'customer' ? 'customer' : 'staff',

    maxFileSizeMB: 10,

    staff: {
        client: 'app',
        realm: 'staff',
        host: '/idp',
    },
};