import {RouteObject} from 'react-router-dom';
import {AssetListPage} from './pages/asset-list-page';
import {AssetDetailsPage} from './pages/asset-details-page';
import {AssetDetailsPageIndex} from './pages/asset-details-page-index';
import {AssetDetailsPageNew} from './pages/asset-details-page-new';

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
    },
    {
        path: '/assets/providers/:storageProviderId/files',
        element: <AssetDetailsPage />,
        children: [
            {
                path: '*',
                element: <AssetDetailsPageIndex />,
            },
        ],
    },
];
