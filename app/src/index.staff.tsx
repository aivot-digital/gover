import React from 'react';
import reportWebVitals from './reportWebVitals';
import {Provider as StoreProvide} from 'react-redux';
import {store} from './store';
import * as Sentry from '@sentry/react';

import './index.scss';
import {CssBaseline, ThemeProvider} from '@mui/material';
import {BaseTheme} from './theming/base-theme';
import {
    useLocation,
    useNavigationType,
    createRoutesFromChildren,
    matchRoutes,
} from 'react-router';
import {createRoot} from 'react-dom/client';
import {StaffApp} from './apps/staff-app';
import {isStringNotNullOrEmpty} from './utils/string-utils';
//import {StaffShellRouter} from './shells/staff/staff-shell-router';

const rootElement = document.getElementById('root')!;
const root = createRoot(rootElement);
root.render(
    localStorage.getItem('new-shell') != null ?
        /*<StaffShellRouter /> */ <div/> :
        <ThemeProvider theme={BaseTheme}>
            <CssBaseline />
            <StoreProvide store={store}>
                <StaffApp />
            </StoreProvide>
        </ThemeProvider>,
);

if (isStringNotNullOrEmpty(AppConfig.sentry.dsn)) {
    Sentry.init({
        dsn: AppConfig.sentry.dsn,
        integrations: [
            Sentry.reactRouterV7BrowserTracingIntegration({
                useEffect: React.useEffect,
                useLocation,
                useNavigationType,
                createRoutesFromChildren, // Included for type compliance; unused with createBrowserRouter setup.
                matchRoutes,
            }),
            Sentry.replayIntegration({
                maskAllText: true,
                blockAllMedia: true,
            }),
        ],

        // Set tracesSampleRate to 0.2 to capture 20%
        // of transactions for performance monitoring
        tracesSampleRate: 0.2,

        // Set distributed tracing to default
        tracePropagationTargets: ['localhost', /^\//],

        // Capture Replay for 10% of all sessions,
        // plus for 100% of sessions with an error
        replaysSessionSampleRate: 0.1,
        replaysOnErrorSampleRate: 1.0,
    });
}

// If you want to start measuring performance in your root, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals(/*console.log*/);
