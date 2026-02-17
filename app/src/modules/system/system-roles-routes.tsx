import {RouteObject} from 'react-router-dom';
import React from 'react';
import {SystemRolesListPage} from "./pages/list/system-roles-list-page";
import {SystemRolesDetailsPage} from "./pages/details/system-roles-details-page";
import {SystemRolesDetailsPageIndex} from "./pages/details/system-roles-details-page-index";
import {SystemRolesDetailsPageMembers} from "./pages/details/system-roles-details-page-members";

export const systemRolesRoutes: RouteObject[] = [
    {
        path: '/system-roles',
        element: <SystemRolesListPage/>,
    },
    {
        path: '/system-roles/:id',
        element: <SystemRolesDetailsPage/>,
        children: [
            {
                index: true,
                element: <SystemRolesDetailsPageIndex/>,
            },
            {
                path: '/system-roles/:id/members',
                element: <SystemRolesDetailsPageMembers/>,
            },
        ],
    },
];