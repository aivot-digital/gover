const environment = process.env.REACT_APP_ENVIRONMENT ?? process.env.NODE_ENV;

const apiAddresses: Record<string, string> = {
    'production': `${window.location.protocol}//${window.location.host}/api`,
    'development': 'http://localhost:8080/api',
};

export const ApiConfig = {
    address: apiAddresses[environment],
};
