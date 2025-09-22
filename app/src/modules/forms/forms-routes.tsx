import {RouteObject} from 'react-router-dom';
import React from 'react';
import {FormsListPage} from './pages/list/forms-list-page/forms-list-page';
import {FormDetailsPage} from './pages/details/form-details-page';

export const formsRoutes: RouteObject[] = [
    {
        path: '/forms',
        element: <FormsListPage />,
    },
    {
        path: '/forms/:id/:version',
        element: <FormDetailsPage />,
    },
];