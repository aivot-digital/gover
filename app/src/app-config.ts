import ProjectPackage from '../package.json';

// TODO: Remove unused properties
interface AppConfig {
    version: string;
    date: string;

    mode: 'staff' | 'customer';

    host: string;
    path: string;
    environment: string;

    sentry: string;

    maxFileSizeMB: number;

    staff: {
        client: string;
        realm: string;
        host: string;
    };
}

const DevelopmentAppConfig: AppConfig = {
    version: ProjectPackage.version,
    date: '2000-01-01T00:00:00.000Z',

    mode: process.env.REACT_APP_BUILD_TARGET === 'customer' ? 'customer' : 'staff',

    host: 'http://localhost:8888', // TODO: Adjust in production
    path: process.env.REACT_APP_BUILD_TARGET === 'customer' ? '/' : '/staff',
    environment: 'development',

    sentry: '',

    maxFileSizeMB: 10,

    staff: {
        client: 'app',
        realm: 'staff',
        host: '/idp',
    },
};

export const AppConfig = DevelopmentAppConfig;