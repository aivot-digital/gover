import React from 'react';
import ReactDOM from 'react-dom';
import reportWebVitals from './reportWebVitals';
import {ThemeProvider} from '@mui/material';
import {BaseTheme} from './theming/base-theme';
import {Provider as StoreProvide} from 'react-redux';
import {store} from './store';

import './index.scss';

function importAppTarget() {
    if (process.env.REACT_APP_BUILD_TARGET === 'customer') {
        return import('./apps/customer-app');
    } else if (process.env.REACT_APP_BUILD_TARGET === 'staff') {
        return import('./apps/staff-app');
    } else {
        return Promise.reject(
            new Error("No such build target: " + process.env.REACT_APP_BUILD_TARGET)
        );
    }
}

importAppTarget()
    .then(({default: Environment}) => {
        ReactDOM.render(
            <React.StrictMode>
                <StoreProvide store={store}>
                    <ThemeProvider theme={BaseTheme}>
                        <Environment/>
                    </ThemeProvider>
                </StoreProvide>
            </React.StrictMode>,
            document.getElementById('root')
        );
    });


// If you want to start measuring performance in your root, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
