import React from 'react';
import {type RouteObject} from 'react-router-dom';
import {StorageProvidersListPage} from './pages/list/storage-providers-list-page';
import {StorageProviderDetailsPage} from './pages/details/storage-provider-details-page';
import {StorageProviderDetailsPageIndex} from './pages/details/storage-provider-details-page-index';
import {StorageProviderDetailsPageExplore} from './pages/details/storage-provider-details-page-explore';
import {StorageProviderDetailsPageTest} from './pages/details/storage-provider-details-page-test';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const storageRoutes: RouteObject[] = [
    {
        path: '/storage-providers',
        element: <StorageProvidersListPage/>,
    },
    {
        path: '/storage-providers/:id',
        element: <StorageProviderDetailsPage/>,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <StorageProviderDetailsPageIndex/>,
            },
            {
                path: '/storage-providers/:id/explore',
                element: <StorageProviderDetailsPageExplore/>,
            },
            {
                path: '/storage-providers/:id/test',
                element: <StorageProviderDetailsPageTest/>,
            },
        ],
    },
];
