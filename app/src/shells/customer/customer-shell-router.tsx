import React from 'react';
import * as Sentry from '@sentry/react';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {CustomerShell} from './customer-shell';
import {AppProvider} from '../../providers/app-provider';
import {store} from '../../store.customer';
import {Provider as StoreProvide} from 'react-redux';
import {CustomerFormPage} from '../../pages/customer-pages/customer-form-page';
import {CustomerListPage} from '../../pages/customer-pages/customer-list-page';
import {isFormModuleEnabled} from '../../utils/module-flags';
import {NotFoundPage} from '../../components/not-found-page/not-found-page';
import {MetaElement} from '../../components/meta-element/meta-element';
import {CustomerInstanceView} from '../../pages/customer-pages/customer-instance-view/customer-instance-view';
import {CustomerInstanceTaskView} from '../../pages/customer-pages/customer-instance-view/customer-instance-task-view';

const sentryCreateBrowserRouter = Sentry.wrapCreateBrowserRouterV7(
    createBrowserRouter,
);

const formModuleEnabled = isFormModuleEnabled();

const router = sentryCreateBrowserRouter(
    [
        {
            element: <CustomerShell/>,
            errorElement: <CustomerShell/>,
            children: [
                {
                    index: true,
                    element: formModuleEnabled ? (
                        <CustomerListPage/>
                    ) : (
                        <>
                            <MetaElement
                                title="Prosuna-Instanz"
                                titlePrefix={AppConfig.providerName}
                            />
                            <NotFoundPage
                                title={'Prosuna-Instanz von ' + AppConfig.providerName}
                                msg="Unter dieser Domain wird eine Prosuna-Instanz betrieben. Aktuell sind keine öffentlichen Angebote freigeschaltet."
                            />
                        </>
                    ),
                },
                ...(
                    formModuleEnabled ? [
                        {
                            path: '/form/:processSlug/:formSlug',
                            element: <CustomerFormPage/>,
                        },
                    ] : []
                ),
                {
                    path: '/process/:instanceAccessKey',
                    element: <CustomerInstanceView/>,
                    children: [{
                        path: '/process/:instanceAccessKey/tasks/:taskAccessKey',
                        element: <CustomerInstanceTaskView/>,
                    }]
                },
                {
                    path: '*',
                    element: <NotFoundPage/>,
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
                <RouterProvider router={router}/>
            </AppProvider>
        </StoreProvide>
    );
}
