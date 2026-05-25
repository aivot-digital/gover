import {RouteObject} from 'react-router-dom';
import {DepartmentsDetailsPage} from './pages/details/departments-details-page';
import {DepartmentsDetailsPageIndex} from './pages/details/departments-details-page-index';
import React from 'react';
import {DepartmentsListPage} from './pages/list/departments-list-page';
import {DepartmentsDetailsPageMembers} from './pages/details/departments-details-page-members';
import {DepartmentsDetailsPageForms} from './pages/details/departments-details-page-forms';
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
                path: 'forms',
                element: <DepartmentsDetailsPageForms />,
            },
            {
                path: 'members',
                element: <DepartmentsDetailsPageMembers />,
            },
        ],
    },
];
