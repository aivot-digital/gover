import {RouteObject} from 'react-router-dom';
import React from 'react';
import {FormNodeEditorPage} from './pages/form-node-editor-page';
import {
    duplicatePageWarningRouteHandle,
} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const elementsRoutes: RouteObject[] = [
    {
        path: '/form-triggers/:nodeId',
        element: <FormNodeEditorPage/>,
        handle: duplicatePageWarningRouteHandle,
    },
];
