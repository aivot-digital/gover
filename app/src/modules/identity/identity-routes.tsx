import {RouteObject} from 'react-router-dom';
import {IdentityProvidersListPage} from './pages/list/identity-providers-list-page';
import {IdentityProviderDetailsPage} from './pages/details/identity-provider-details-page';
import {IdentityProviderDetailsPageIndex} from './pages/details/identity-provider-details-page-index';
import {IdentityProviderDetailsPageTest} from './pages/details/identity-provider-details-page-test';
import {IdentityProviderDetailsPageSetup} from './pages/details/identity-provider-details-page-setup';
import {IdentityProviderDetailsPageCommunication} from './pages/details/identity-provider-details-page-communication';
import {
    duplicatePageWarningRouteHandle,
} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const identityRoutes: RouteObject[] = [
    {
        path: '/identity-providers',
        element: <IdentityProvidersListPage/>,
    },
    {
        path: '/identity-providers/:key',
        element: <IdentityProviderDetailsPage/>,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <IdentityProviderDetailsPageIndex/>,
            },
            {
                path: '/identity-providers/:key/test',
                element: <IdentityProviderDetailsPageTest/>,
            },
            {
                path: '/identity-providers/:key/communication',
                element: <IdentityProviderDetailsPageCommunication/>,
            },
            {
                path: '/identity-providers/:key/setup',
                element: <IdentityProviderDetailsPageSetup/>,
            },
        ],
    },
];
