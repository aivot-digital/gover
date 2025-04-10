import React from 'react';
import {RouteObject} from 'react-router-dom';

export const usersRoutes: RouteObject[] = [
    {
        path: '/users',
        element: <div />,
    },
    {
        path: '/users/:id',
        element: <div />,
        children: [
            {
                index: true,
                element: <div />,
            },
        ],
    },
];