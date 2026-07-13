import React from 'react';
import {RouteObject} from 'react-router-dom';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';
import {CodeListsListPage} from './pages/list/code-lists-list-page';
import {CodeListDetailsPage} from './pages/details/code-list-details-page';
import {CodeListDetailsPageIndex} from './pages/details/code-list-details-page-index';
import {CodeListDetailsPageItems} from './pages/details/code-list-details-page-items';

export const codeListsRoutes: RouteObject[] = [
    {
        path: '/code-lists',
        element: <CodeListsListPage />,
    },
    {
        path: '/code-lists/:id',
        element: <CodeListDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <CodeListDetailsPageIndex />,
            },
            {
                path: 'items',
                element: <CodeListDetailsPageItems />,
            },
        ],
    },
];
