import React from 'react';
import {type RouteObject} from 'react-router-dom';
import {StorageProvidersListPage} from './pages/list/storage-providers-list-page';
import {StorageProviderDetailsPage} from './pages/details/storage-provider-details-page';
import {StorageProviderDetailsPageIndex} from './pages/details/storage-provider-details-page-index';

export const storageRoutes: RouteObject[] = [
    {
        path: '/storage-providers',
        element: <StorageProvidersListPage />,
    },
    {
        path: '/storage-providers/:id',
        element: <StorageProviderDetailsPage />,
        children: [
            {
                index: true,
                element: <StorageProviderDetailsPageIndex />,
            },
        ],
    },
];
