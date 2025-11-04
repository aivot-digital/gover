import React from 'react';
import * as Sentry from '@sentry/react';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {CustomerShell} from './customer-shell';
import {AppProvider} from '../../providers/app-provider';
import {store} from '../../store.customer';
import {Provider as StoreProvide} from 'react-redux';
import {CustomerListPage} from '../../pages/customer-pages/customer-list-page';
import {CustomerFormPage} from '../../pages/customer-pages/customer-form-page';

const sentryCreateBrowserRouter = Sentry.wrapCreateBrowserRouterV7(
    createBrowserRouter,
);

const router = sentryCreateBrowserRouter(
    [
        {
            element: <CustomerShell />,
            errorElement: <CustomerShell />,
            children: [
                {
                    index: true,
                    element: <CustomerListPage />,
                },
                {
                    path: '/:slug/:version?',
                    element: <CustomerFormPage />,
                },
            ],
        },
    ],
    {
        basename: '/',
    },
);

export function CustomerShellRouter() {
    return (
        <StoreProvide store={store}>
            <AppProvider>
                <RouterProvider router={router} />
            </AppProvider>
        </StoreProvide>
    );
}