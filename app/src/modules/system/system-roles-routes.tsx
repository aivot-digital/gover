import {RouteObject} from 'react-router-dom';
import React from 'react';
import {SystemRolesListPage} from "./pages/list/system-roles-list-page";
import {SystemRolesDetailsPage} from "./pages/details/system-roles-details-page";
import {SystemRolesDetailsPageIndex} from "./pages/details/system-roles-details-page-index";
import {SystemRolesDetailsPageMembers} from "./pages/details/system-roles-details-page-members";
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const systemRolesRoutes: RouteObject[] = [
    {
        path: '/system-roles',
        element: <SystemRolesListPage/>,
    },
    {
        path: '/system-roles/:id',
        element: <SystemRolesDetailsPage/>,
        handle: duplicatePageWarningRouteHandle,
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
