import {Navigate, RouteObject} from 'react-router-dom';
import {DepartmentsDetailsPage} from './pages/details/departments-details-page';
import {DepartmentsDetailsPageIndex} from './pages/details/departments-details-page-index';
import React from 'react';
import {DepartmentsListPage} from './pages/list/departments-list-page';
import {DepartmentsDetailsPageMembers} from './pages/details/departments-details-page-members';
import {DepartmentsDetailsPageProcesses} from './pages/details/departments-details-page-processes';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const departmentsRoutes: RouteObject[] = [
    {
        path: '/departments',
        element: <DepartmentsListPage />,
    },
    {
        path: '/departments/:id',
        element: <DepartmentsDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <DepartmentsDetailsPageIndex />,
            },
            {
                path: 'processes',
                element: <DepartmentsDetailsPageProcesses />,
            },
            {
                path: 'members',
                element: <DepartmentsDetailsPageMembers />,
            },
        ],
    },
];
