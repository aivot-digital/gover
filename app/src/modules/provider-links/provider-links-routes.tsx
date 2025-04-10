import {RouteObject} from 'react-router-dom';
import React from 'react';
import {ProviderLinksListPage} from './pages/list/provider-links-list-page';
import {ProviderLinksDetailsPage} from './pages/details/provider-links-details-page';
import {ProviderLinksDetailsPageIndex} from './pages/details/provider-links-details-page-index';

export const providerLinksRoutes: RouteObject[] = [
    {
        path: '/provider-links',
        element: <ProviderLinksListPage />,
    },
    {
        path: '/provider-links/:id',
        element: <ProviderLinksDetailsPage />,
        children: [
            {
                index: true,
                element: <ProviderLinksDetailsPageIndex />,
            },
        ],
    },
];