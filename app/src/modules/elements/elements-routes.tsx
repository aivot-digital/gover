import {RouteObject} from 'react-router-dom';
import React from 'react';
import {FormNodeEditorPage} from './pages/form-node-editor-page';

export const elementsRoutes: RouteObject[] = [
    {
        path: '/form-triggers/:nodeId/:fieldKey/:elementType',
        element: <FormNodeEditorPage/>,
    },
];