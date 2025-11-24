import {RouteObject} from 'react-router-dom';
import {TeamsDetailsPage} from './pages/details/teams-details-page';
import {TeamsDetailsPageIndex} from './pages/details/teams-details-page-index';
import React from 'react';
import {TeamsListPage} from './pages/list/teams-list-page';
import {TeamsDetailsPageMembers} from './pages/details/teams-details-page-members';

export const teamsRoutes: RouteObject[] = [
    {
        path: '/teams',
        element: <TeamsListPage />,
    },
    {
        path: '/teams/:id',
        element: <TeamsDetailsPage />,
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