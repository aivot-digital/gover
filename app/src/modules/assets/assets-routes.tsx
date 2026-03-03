import {RouteObject} from 'react-router-dom';
import {AssetListPage} from './pages/asset-list-page';
import {AssetDetailsPage} from './pages/asset-details-page';
import {AssetDetailsPageIndex} from './pages/asset-details-page-index';

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
        path: '/assets/providers/:storageProviderId/:key',
        element: <AssetDetailsPage />,
        children: [
            {
                index: true,
                element: <AssetDetailsPageIndex />,
            },
        ],
    },
];
