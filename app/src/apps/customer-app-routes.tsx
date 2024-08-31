import React from 'react';
import {type Route} from '../models/lib/route';
import {NotFound} from '../pages/shared/not-found/not-found';
import {FormPage} from "../pages/customer-pages/form-page";
import {ListPage} from "../pages/customer-pages/list-page";

export const customerAppRoutes: Record<string, Route> = {
    listPage: {
        path: '/',
        element: <ListPage/>,
    },
    formView: {
        path: '/:slug/:version?',
        element: <FormPage/>,
    },
    notFound: {
        path: '*',
        element: <NotFound/>,
    },
};
