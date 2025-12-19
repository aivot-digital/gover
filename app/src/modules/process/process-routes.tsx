import {RouteObject} from 'react-router-dom';
import React from 'react';
import {ProcessListPage} from "./pages/list/process-list-page";
import {ProcessDetailsPage} from "./pages/details/process-details-page";
import {ProcessInstanceListPage} from "./pages/list/process-instance-page";
import {ProcessInstanceTaskListPage} from "./pages/list/process-instance-task-page";
import {ProcessTaskViewPage} from "./pages/details/process-task-view-page";
import {ProcessAssignedTaskListPage} from "./pages/list/process-assigned-task-page";
import {ProcessFlowNodeEditor} from "./pages/details/components/process-flow-node-editor";

export const processRoutes: RouteObject[] = [
    {
        path: '/processes',
        element: <ProcessListPage/>,
    },
    {
        path: '/processes/:processId/versions/:processVersion',
        element: <ProcessDetailsPage/>,
        children: [
            {
                index: true,
                element: <div>Placeholder</div>,
            },
            {
                path: '/processes/:processId/versions/:processVersion/nodes/:nodeId',
                element: <ProcessFlowNodeEditor/>,
            }
        ],
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
        path: '/tasks',
        element: <ProcessAssignedTaskListPage/>,
    },
    {
        path: '/tasks/:instanceAccessKey/:taskAccessKey',
        element: <ProcessTaskViewPage/>,
    },
];