import {RouteObject} from 'react-router-dom';
import {CommunicationProvidersListPage} from './pages/communication-providers-list-page';
import {CommunicationProviderDetailsPage} from './pages/communication-provider-details-page';
import {CommunicationProviderDetailsPageIndex} from './pages/communication-provider-details-page-index';
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
        ],
    },
];
