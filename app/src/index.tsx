import React from 'react';
import ReactDOM from 'react-dom';
import reportWebVitals from './reportWebVitals';
import {Provider as StoreProvide} from 'react-redux';
import {store} from './store';

import './index.scss';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from './theming/base-theme';

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

importAppTarget()
    .then(({default: Environment}) => {
        ReactDOM.render(
            <React.StrictMode>
                <ThemeProvider theme={BaseTheme}>
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
reportWebVitals();
