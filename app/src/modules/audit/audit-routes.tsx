import React from 'react';
import {RouteObject} from 'react-router-dom';
import {AuditLogsListPage} from './pages/list/audit-logs-list-page';

export const auditRoutes: RouteObject[] = [
    {
        path: '/audit-log',
        element: <AuditLogsListPage/>,
    },
];
