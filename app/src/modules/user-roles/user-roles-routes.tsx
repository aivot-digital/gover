import {RouteObject} from 'react-router-dom';
import React from 'react';
import {UserRolesListPage} from './pages/list/user-roles-list-page';
import {UserRolesDetailsPage} from './pages/details/user-roles-details-page';
import {UserRolesDetailsPageIndex} from './pages/details/user-roles-details-page-index';
import {
    UserRolesDetailsPageDepartmentMemberships
} from "./pages/details/user-roles-details-page-department-memberships";
import {UserRolesDetailsPageTeamMemberships} from "./pages/details/user-roles-details-page-team-memberships";

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
            {
                path: '/user-roles/:id/department-memberships',
                element: <UserRolesDetailsPageDepartmentMemberships />,
            },
            {
                path: '/user-roles/:id/team-memberships',
                element: <UserRolesDetailsPageTeamMemberships />,
            },
        ],
    },
];