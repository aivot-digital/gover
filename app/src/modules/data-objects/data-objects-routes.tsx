import {RouteObject} from 'react-router-dom';
import React from 'react';
import {DataObjectSchemaListPage} from './pages/list/data-object-schema-list-page';
import {DataObjectSchemaDetailsPage} from './pages/details/data-object-schema-details-page';
import {DataObjectSchemaDetailsPageIndex} from './pages/details/data-object-schema-details-page-index';
import {DataObjectItemListPage} from './pages/list/data-object-item-list-page';
import {DataObjectItemDetailsPage} from './pages/details/data-object-item-details-page';
import {DataObjectItemDetailsPageIndex} from './pages/details/data-object-item-details-page-index';
import { DataObjectListPage } from './pages/list/data-object-list-page';

export const dataObjectsRoutes: RouteObject[] = [
    {
        path: '/data-models',
        element: <DataObjectSchemaListPage />,
    },
    {
        path: '/data-objects',
        element: <DataObjectListPage />,
    },
    {
        path: '/data-models/:key',
        element: <DataObjectSchemaDetailsPage />,
        children: [
            {
                index: true,
                element: <DataObjectSchemaDetailsPageIndex />,
            },
        ],
    },
    {
        path: '/data-objects/:schemaKey',
        element: <DataObjectItemListPage />,
    },
    {
        path: '/data-objects/:schemaKey/:id',
        element: <DataObjectItemDetailsPage />,
        children: [
            {
                index: true,
                element: <DataObjectItemDetailsPageIndex />,
            },
        ],
    },
];