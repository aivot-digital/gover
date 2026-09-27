import {RouteObject} from 'react-router-dom';
import {TeamsDetailsPage} from './pages/details/teams-details-page';
import {TeamsDetailsPageIndex} from './pages/details/teams-details-page-index';
import React from 'react';
import {TeamsListPage} from './pages/list/teams-list-page';
import {TeamsDetailsPageMembers} from './pages/details/teams-details-page-members';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const teamsRoutes: RouteObject[] = [
    {
        path: '/teams',
        element: <TeamsListPage />,
    },
    {
        path: '/teams/:id',
        element: <TeamsDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <TeamsDetailsPageIndex />,
            },
            {
                path: 'members',
                element: <TeamsDetailsPageMembers />,
            },
        ],
    },
];
