import {RouteObject} from 'react-router-dom';
import {AssetListPage} from './pages/asset-list-page';
import {AssetDetailsPage} from './pages/asset-details-page';
import {AssetDetailsPageIndex} from './pages/asset-details-page-index';
import {AssetDetailsPageNew} from './pages/asset-details-page-new';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const assetsRoutes: RouteObject[] = [
    {
        path: '/assets',
        element: <AssetListPage />,
    },
    {
        path: '/assets/providers/:storageProviderId',
        element: <AssetListPage />,
    },
    {
        path: '/assets/providers/:storageProviderId/files/new',
        element: <AssetDetailsPageNew />,
        handle: duplicatePageWarningRouteHandle,
    },
    {
        path: '/assets/providers/:storageProviderId/files',
        element: <AssetDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                path: '*',
                element: <AssetDetailsPageIndex />,
            },
        ],
    },
];
