import {RouteObject} from 'react-router-dom';
import {IdentityProvidersListPage} from './pages/list/identity-providers-list-page';
import {IdentityProviderDetailsPage} from './pages/details/identity-provider-details-page';
import {IdentityProviderDetailsPageIndex} from './pages/details/identity-provider-details-page-index';
import {IdentityProviderDetailsPageTest} from './pages/details/identity-provider-details-page-test';
import {IdentityProviderDetailsPageForms} from './pages/details/identity-provider-details-page-forms';

export const identityRoutes: RouteObject[] = [
    {
        path: '/identity-providers',
        element: <IdentityProvidersListPage />,
    },
    {
        path: '/identity-providers/:key',
        element: <IdentityProviderDetailsPage />,
        children: [
            {
                index: true,
                element: <IdentityProviderDetailsPageIndex />,
            },
            {
                path: '/identity-providers/:key/test',
                element: <IdentityProviderDetailsPageTest />,
            },
            {
                path: '/identity-providers/:key/forms',
                element: <IdentityProviderDetailsPageForms />,
            },
        ],
    },
];