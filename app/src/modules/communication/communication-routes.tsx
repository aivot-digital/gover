import {RouteObject} from 'react-router-dom';
import {CommunicationProvidersListPage} from './pages/communication-providers-list-page';
import {CommunicationProviderDetailsPage} from './pages/communication-provider-details-page';
import {CommunicationProviderDetailsPageIndex} from './pages/communication-provider-details-page-index';
import {CommunicationProviderDetailsPageTest} from './pages/communication-provider-details-page-test';
import {
    CommunicationProviderDetailsPageIdentityProviders,
} from './pages/communication-provider-details-page-identity-providers';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const communicationRoutes: RouteObject[] = [
    {
        path: '/communication-providers',
        element: <CommunicationProvidersListPage/>,
    },
    {
        path: '/communication-providers/:id',
        element: <CommunicationProviderDetailsPage/>,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <CommunicationProviderDetailsPageIndex/>,
            },
            {
                path: '/communication-providers/:id/test',
                element: <CommunicationProviderDetailsPageTest/>,
            },
            {
                path: '/communication-providers/:id/identity-providers',
                element: <CommunicationProviderDetailsPageIdentityProviders/>,
            },
        ],
    },
];
