import {RouteObject} from 'react-router-dom';
import React from 'react';
import {ProviderLinksListPage} from './pages/list/provider-links-list-page';
import {ProviderLinksDetailsPage} from './pages/details/provider-links-details-page';
import {ProviderLinksDetailsPageIndex} from './pages/details/provider-links-details-page-index';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const providerLinksRoutes: RouteObject[] = [
    {
        path: '/provider-links',
        element: <ProviderLinksListPage />,
    },
    {
        path: '/provider-links/:id',
        element: <ProviderLinksDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <ProviderLinksDetailsPageIndex />,
            },
        ],
    },
];
