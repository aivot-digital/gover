import {RouteObject} from 'react-router-dom';
import {SecretsListPage} from './pages/list/secrets-list-page';
import {SecretsDetailsPage} from './pages/details/secrets-details-page';
import {SecretsDetailsPageIndex} from './pages/details/secrets-details-page-index';
import React from 'react';

export const secretsRoutes: RouteObject[] = [
    {
        path: '/secrets',
        element: <SecretsListPage />,
    },
    {
        path: '/secrets/:id',
        element: <SecretsDetailsPage />,
        children: [
            {
                index: true,
                element: <SecretsDetailsPageIndex />,
            },
        ],
    },
];