import React from 'react';
import * as Sentry from '@sentry/react';
import {createBrowserRouter, RouterProvider} from 'react-router-dom';
import {StaffShell} from './staff-shell';
import {AppProvider} from '../../providers/app-provider';
import {store} from '../../store.staff';
import {Provider as StoreProvide} from 'react-redux';
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
import {Dashboard} from '../../modules/dashboard/dashboard';
import {configsRoutes} from '../../modules/configs/configs-routes';
import {UnderConstructionPage} from '../../pages/shared/under-construction-page';
import {Testinghall} from '../../modules/testinghall/testinghall';
import {userRolesRoutes} from '../../modules/user-roles/user-roles-routes';
import {teamsRoutes} from '../../modules/teams/teams-routes';

const sentryCreateBrowserRouter = Sentry.wrapCreateBrowserRouterV7(
    createBrowserRouter,
);

const router = sentryCreateBrowserRouter(
    [
        {
            element: <StaffShell />,
            errorElement: <StaffShell />,
            children: [
                {
                    index: true,
                    element: <Dashboard />,
                },

                ...assetsRoutes,
                ...configsRoutes,
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
                ...teamsRoutes,
                ...themesRoutes,
                ...userRolesRoutes,
                ...usersRoutes,
                ...accountRoutes,

                {
                    path: '/processes',
                    element: <UnderConstructionPage />,
                },
                {
                    path: '/testinghall',
                    element: <Testinghall />,
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