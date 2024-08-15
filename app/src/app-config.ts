import ProjectPackage from '../package.json';

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

    bundId: {
        client: string;
        realm: string;
        host: string;
        broker: string;
    };

    bayernId: {
        client: string;
        realm: string;
        host: string;
        broker: string;
    };

    schleswigHolsteinId: {
        client: string;
        realm: string;
        host: string;
        broker: string;
    };

    muk: {
        client: string;
        realm: string;
        host: string;
        broker: string;
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

    bundId: {
        client: 'app',
        realm: 'customer',
        host: '/idp',
        broker: 'bundid',
    },

    bayernId: {
        client: 'app',
        realm: 'customer',
        host: '/idp',
        broker: 'bayernid',
    },

    schleswigHolsteinId: {
        client: 'app',
        realm: 'customer',
        host: '/idp',
        broker: 'schleswigholstein',
    },

    muk: {
        client: 'app',
        realm: 'customer',
        host: '/idp',
        broker: 'muk',
    },
};

export const AppConfig = DevelopmentAppConfig;