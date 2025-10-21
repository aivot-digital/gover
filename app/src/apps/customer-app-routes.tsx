import React from 'react';
import {type Route} from '../models/lib/route';
import {NotFound} from '../pages/shared/not-found/not-found';
import {CustomerFormPage} from "../pages/customer-pages/customer-form-page";
import {CustomerListPage} from "../pages/customer-pages/customer-list-page";

export const customerAppRoutes: Record<string, Route> = {
    listPage: {
        path: '/',
        element: <CustomerListPage/>,
    },
    formView: {
        path: '/:slug/:version?',
        element: <CustomerFormPage/>,
    },
    notFound: {
        path: '*',
        element: <NotFound/>,
    },
};
