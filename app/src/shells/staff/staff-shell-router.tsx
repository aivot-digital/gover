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
import {identityRoutes} from '../../modules/identity/identity-routes';
import {paymentRoutes} from '../../modules/payment/payment-routes';
import {providerLinksRoutes} from '../../modules/provider-links/provider-links-routes';
import {secretsRoutes} from '../../modules/secrets/secrets-routes';
import {usersRoutes} from '../../modules/users/users-routes';
import {accountRoutes} from '../../modules/users/account-routes';
import {presetsRoutes} from '../../modules/presets/presets-routes';
import {themesRoutes} from '../../modules/themes/themes-routes';
import {Dashboard} from '../../modules/dashboard/dashboard';
import {configsRoutes} from '../../modules/configs/configs-routes';
import {Testinghall} from '../../modules/testinghall/testinghall';
import {userRolesRoutes} from '../../modules/user-roles/user-roles-routes';
import {teamsRoutes} from '../../modules/teams/teams-routes';
import {OrganizationChart} from "../../modules/system/pages/organization-chart";
import {processRoutes} from "../../modules/process/process-routes";
import {systemRolesRoutes} from "../../modules/system/system-roles-routes";
import {storageRoutes} from '../../modules/storage/storage-routes';
import {auditRoutes} from '../../modules/audit/audit-routes';
import {elementsRoutes} from '../../modules/elements/elements-routes';
import {NodeConfigMaker} from '../../pages/staff-pages/node-config-maker/node-config-maker';
import {codeListsRoutes} from '../../modules/code-lists/code-lists-routes';

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
                ...formsRoutes,
                ...identityRoutes,
                ...paymentRoutes,
                ...presetsRoutes,
                ...processRoutes,
                ...providerLinksRoutes,
                ...secretsRoutes,
                ...storageRoutes,
                ...auditRoutes,
                ...codeListsRoutes,
                ...teamsRoutes,
                ...themesRoutes,
                ...userRolesRoutes,
                ...usersRoutes,
                ...accountRoutes,
                ...systemRolesRoutes,
                ...elementsRoutes,

                {
                    path: '/testinghall',
                    element: <Testinghall />,
                },
                {
                    path: '/node-config-maker',
                    element: <NodeConfigMaker />,
                },
                {
                    path: '/organization-chart',
                    element: <OrganizationChart />,
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
