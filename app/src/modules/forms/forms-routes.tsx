import {RouteObject} from 'react-router-dom';
import React from 'react';
import {FormsListPage} from './pages/list/forms-list-page';

export const formsRoutes: RouteObject[] = [
    {
        path: '/forms',
        element: <FormsListPage />,
    },
];