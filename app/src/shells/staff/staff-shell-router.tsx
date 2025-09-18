import React from 'react';
import * as Sentry from '@sentry/react';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {StaffShell} from './staff-shell';
import {AppProvider} from '../../providers/app-provider';
import {store} from '../../store';
import {Provider as StoreProvide} from 'react-redux';
import {StaffShellError} from './shell-error';
import {assetsRoutes} from '../../modules/assets/assets-routes';
import {formsRoutes} from '../../modules/forms/forms-routes';
import {departmentsRoutes} from '../../modules/departments/departments-routes';
import {dataObjectsRoutes} from '../../modules/data-objects/data-objects-routes';
import {destinationRoutes} from '../../modules/destination/destination-routes';
import {identityRoutes} from '../../modules/identity/identity-routes';
import {paymentRoutes} from '../../modules/payment/payment-routes';
import {providerLinksRoutes} from '../../modules/provider-links/provider-links-routes';
import {secretsRoutes} from '../../modules/secrets/secrets-routes';
import {usersRoutes} from '../../modules/users/users-routes';
import {accountRoutes} from '../../modules/users/account-routes';
import {presetsRoutes} from '../../modules/presets/presets-routes';
import {submissionsRoutes} from '../../modules/submissions/submissions-routes';
import {themesRoutes} from '../../modules/themes/themes-routes';
import {ApplicationSettings} from '../../pages/staff-pages/settings/components/application-settings/application-settings';
import {SmtpTest} from '../../pages/staff-pages/settings/components/smtp-test/smtp-test';
import {SystemInformation} from '../../pages/staff-pages/settings/components/system-information/system-information';
import {SystemConfigs} from '../../modules/configs/pages/system-configs';
import {PageWrapper} from '../../components/page-wrapper/page-wrapper';
import {Dashboard} from '../../modules/dashboard/dashboard';

const sentryCreateBrowserRouter = Sentry.wrapCreateBrowserRouterV7(
    createBrowserRouter,
);

const router = sentryCreateBrowserRouter(
    [
        {
            element: <StaffShell />,
            errorElement: (
                <StaffShell>
                    <StaffShellError />
                </StaffShell>
            ),
            children: [
                {
                    index: true,
                    element: <Dashboard />,
                },

                ...assetsRoutes,
                ...dataObjectsRoutes,
                ...departmentsRoutes,
                ...destinationRoutes,
                ...formsRoutes,
                ...identityRoutes,
                ...paymentRoutes,
                ...presetsRoutes,
                ...providerLinksRoutes,
                ...secretsRoutes,
                ...submissionsRoutes,
                ...themesRoutes,
                ...usersRoutes,
                ...accountRoutes,

                {
                    path: '/settings/app',
                    element: <PageWrapper title="Anwendungseinstellungen">
                        <ApplicationSettings />
                    </PageWrapper>,
                },
                {
                    path: '/settings/smtp',
                    element: <PageWrapper title="SMTP-Test">
                        <SmtpTest />
                    </PageWrapper>,
                },
                {
                    path: '/settings/status',
                    element: <PageWrapper title="Systeminformationen">
                        <SystemInformation />
                    </PageWrapper>,
                },
                {
                    path: '/settings/misc',
                    element: <SystemConfigs />,
                },
            ],
        },
    ],
    {
        basename: '/staff',
    },
);

export function StaffShellRouter() {
    return (
        <StoreProvide store={store}>
            <AppProvider>
                <RouterProvider router={router} />
            </AppProvider>
        </StoreProvide>
    );
}