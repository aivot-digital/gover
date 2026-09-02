import '@testing-library/jest-dom/vitest';
import {cleanup} from '@testing-library/react';
import {afterEach} from 'vitest';

// Some modules read AppConfig and localStorage at import time. Provide the minimal
// browser/runtime globals that components need before test modules are loaded.
const localStorageMock = (() => {
    let values: Record<string, string> = {};

    return {
        getItem: (key: string) => values[key] ?? null,
        setItem: (key: string, value: string) => {
            values[key] = value;
        },
        removeItem: (key: string) => {
            delete values[key];
        },
        clear: () => {
            values = {};
        },
    };
})();

Object.defineProperty(globalThis, 'localStorage', {
    value: localStorageMock,
    configurable: true,
});

Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
    configurable: true,
});

Object.defineProperty(globalThis, 'AppConfig', {
    value: {
        knownFileExtensions: [],
        providerName: 'Prosuna',
        systemTheme: {
            id: 1,
            name: 'Default',
            primaryColor: '#0b6bcb',
            secondaryColor: '#5f6368',
            primaryColorDark: null,
            secondaryColorDark: null,
            faviconKey: null,
            logoKey: null,
            logoKeyDark: null,
        },
        systemConfigs: {
            ProviderName: 'Prosuna',
            SystemTheme: 'Default',
        },
        faviconUrl: '',
        logoUrl: '',
        apiHostname: 'http://localhost',
        registryHostname: 'http://localhost',
        supportUrl: null,
        applicationTimeZone: 'Europe/Berlin',
        departmentLevelLabels: [],
        sentryDsn: '',
        oidc: {
            hostname: 'http://localhost',
            realm: 'test',
            clientId: 'prosuna',
        },
        moduleFlags: [],
        processNodeLimits: {},
    },
    configurable: true,
});

afterEach(() => {
    cleanup();
    localStorage.clear();
});
