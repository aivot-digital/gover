import {RouteObject} from 'react-router-dom';
import React from 'react';
import {DestinationListPage} from './pages/list/destination-list-page';
import {DestinationDetailsPage} from './pages/details/destination-details-page';
import {DestinationDetailsPageIndex} from './pages/details/destination-details-page-index';
import {DestinationDetailsPageForms} from './pages/details/destination-details-page-forms';

export const destinationRoutes: RouteObject[] = [
    {
        path: '/destinations',
        element: <DestinationListPage />,
    },
    {
        path: '/destinations/:id',
        element: <DestinationDetailsPage />,
        children: [
            {
                index: true,
                element: <DestinationDetailsPageIndex />,
            },
            {
                path: 'forms',
                element: <DestinationDetailsPageForms />,
            },
        ],
    },
];