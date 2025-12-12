import {RouteObject} from 'react-router-dom';
import React from 'react';
import {ProcessListPage} from "./pages/list/process-list-page";
import {ProcessDetailsPage} from "./pages/details/process-details-page";
import {ReactFlowProvider} from "@xyflow/react";
import {ProcessInstanceListPage} from "./pages/list/process-instance-page";
import {ProcessInstanceTaskListPage} from "./pages/list/process-instance-task-page";
import {ProcessTaskViewPage} from "./pages/details/process-task-view-page";

export const processRoutes: RouteObject[] = [
    {
        path: '/processes',
        element: <ProcessListPage/>,
    },
    {
        path: '/processes/:processId/versions/:processVersion',
        element: <ProcessDetailsPage/>,
    },
    {
        path: '/processes/:processId/versions/:processVersion/instances',
        element: <ProcessInstanceListPage/>,
    },
    {
        path: '/processes/:processId/versions/:processVersion/instances/:instanceId/tasks',
        element: <ProcessInstanceTaskListPage/>,
    },
    {
        path: '/tasks/:instanceAccessKey/:taskAccessKey',
        element: <ProcessTaskViewPage/>,
    },
];