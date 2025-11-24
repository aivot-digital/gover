import {RouteObject} from 'react-router-dom';
import React from 'react';
import {UserRolesListPage} from './pages/list/user-roles-list-page';
import {UserRolesDetailsPage} from './pages/details/user-roles-details-page';
import {UserRolesDetailsPageIndex} from './pages/details/user-roles-details-page-index';

export const userRolesRoutes: RouteObject[] = [
    {
        path: '/user-roles',
        element: <UserRolesListPage />,
    },
    {
        path: '/user-roles/:id',
        element: <UserRolesDetailsPage />,
        children: [
            {
                index: true,
                element: <UserRolesDetailsPageIndex />,
            },
        ],
    },
];