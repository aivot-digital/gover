import {RouteObject} from 'react-router-dom';
import {FormEditPage} from '../../pages/staff-pages/application-pages/form-edit-page';
import React from 'react';
import {FormsListPage} from './pages/list/forms-list-page/forms-list-page';

export const formsRoutes: RouteObject[] = [
    {
        path: '/forms',
        element: <FormsListPage />,
    },
    {
        path: '/forms/:id/:version',
        element: <FormEditPage />,
    },
];