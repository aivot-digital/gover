import React from 'react';
import {RouteObject} from 'react-router-dom';
import {UserListPage} from './pages/user/list/user-list-page';
import {UserDetailsPage} from './pages/user/details/user-details-page';
import {UserDetailsPageIndex} from './pages/user/details/user-details-page-index';
import {UserDetailsPageDepartmentMemberships} from './pages/user/details/user-details-page-department-memberships';

export const usersRoutes: RouteObject[] = [
    {
        path: '/users',
        element: <UserListPage />,
    },
    {
        path: '/users/:id',
        element: <UserDetailsPage />,
        children: [
            {
                index: true,
                element: <UserDetailsPageIndex />,
            },
            {
                path: '/users/:id/departments-and-roles',
                element: <UserDetailsPageDepartmentMemberships />,
            },
        ],
    },
];