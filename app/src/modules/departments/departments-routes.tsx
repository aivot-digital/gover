import {RouteObject} from 'react-router-dom';
import {DepartmentsDetailsPage} from './pages/details/departments-details-page';
import {DepartmentsDetailsPageIndex} from './pages/details/departments-details-page-index';
import React from 'react';
import {DepartmentsListPage} from './pages/list/departments-list-page';
import {DepartmentsDetailsPageMembers} from './pages/details/departments-details-page-members';
import {DepartmentsDetailsPageForms} from './pages/details/departments-details-page-forms';
import {OrganizationalUnitTree} from './pages/tree/organizational-unit-tree';

export const departmentsRoutes: RouteObject[] = [
    {
        path: '/departments',
        element: <DepartmentsListPage />,
    },
    {
        path: '/org-units',
        element: <OrganizationalUnitTree />,
    },
    {
        path: '/departments/:id',
        element: <DepartmentsDetailsPage />,
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