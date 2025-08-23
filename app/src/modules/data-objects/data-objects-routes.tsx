import {RouteObject} from 'react-router-dom';
import React from 'react';
import {DataObjectSchemaListPage} from './pages/list/data-object-schema-list-page';
import {DataObjectSchemaDetailsPage} from './pages/details/data-object-schema-details-page';
import {DataObjectSchemaDetailsPageIndex} from './pages/details/data-object-schema-details-page-index';
import {DataObjectItemListPage} from './pages/list/data-object-item-list-page';
import {DataObjectItemDetailsPage} from './pages/details/data-object-item-details-page';
import {DataObjectItemDetailsPageIndex} from './pages/details/data-object-item-details-page-index';

export const dataObjectsRoutes: RouteObject[] = [
    {
        path: '/data-objects',
        element: <DataObjectSchemaListPage />,
    },
    {
        path: '/data-objects/:key',
        element: <DataObjectSchemaDetailsPage />,
        children: [
            {
                index: true,
                element: <DataObjectSchemaDetailsPageIndex />,
            },
        ],
    },
    {
        path: '/data-objects/:schemaKey/items',
        element: <DataObjectItemListPage />,
    },
    {
        path: '/data-objects/:schemaKey/items/:id',
        element: <DataObjectItemDetailsPage />,
        children: [
            {
                index: true,
                element: <DataObjectItemDetailsPageIndex />,
            },
        ],
    },
];