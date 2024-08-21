import React from 'react';
import ReactDOM from 'react-dom';
import reportWebVitals from './reportWebVitals';
import {Provider as StoreProvide} from 'react-redux';
import {store} from './store';
import * as Sentry from '@sentry/react';

import './index.scss';
import {CssBaseline, ThemeProvider} from '@mui/material';
import {BaseTheme} from './theming/base-theme';
import {createRoutesFromChildren, matchRoutes, useLocation, useNavigationType} from 'react-router-dom';
import {getSentryDsn} from "./hooks/use-system-api";

async function importAppTarget() {
    if (process.env.REACT_APP_BUILD_TARGET === 'customer') {
        return await import('./apps/customer-app');
    } else if (process.env.REACT_APP_BUILD_TARGET === 'staff') {
        return await import('./apps/staff-app');
    } else {
        return await Promise.reject(
            new Error(`No such build target: ${process.env.REACT_APP_BUILD_TARGET ?? 'undefined'}`),
        );
    }
}

getSentryDsn()
    .then((sentryDsn) => {
        if (sentryDsn != null) {
            Sentry.init({
                dsn: sentryDsn,
                integrations: [
                    new Sentry.BrowserTracing({
                        routingInstrumentation: Sentry.reactRouterV6Instrumentation(
                            React.useEffect,
                            useLocation,
                            useNavigationType,
                            createRoutesFromChildren,
                            matchRoutes,
                        ),
                    }),
                    new Sentry.Replay(),
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
        } else {
            console.warn('Initializing without Sentry');
        }
    })
    .catch((err) => {
        console.warn('Failed to initialize Sentry', err);
    });

importAppTarget()
    .then(({default: Environment}) => {
        ReactDOM.render(
            <React.StrictMode>
                <ThemeProvider theme={BaseTheme}>
                    <CssBaseline/>
                    <StoreProvide store={store}>
                        <Environment/>
                    </StoreProvide>
                </ThemeProvider>
            </React.StrictMode>,
            document.getElementById('root'),
        );
    });

// If you want to start measuring performance in your root, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals(/*console.log*/);
