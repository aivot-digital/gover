import {type RouteObject} from 'react-router-dom';
import React from 'react';
import {ProcessListPage} from './pages/list/process-list-page';
import {ProcessDetailsPage} from './pages/details/process-details-page';
import {ProcessInstanceListPage} from './pages/list/process-instance-page';
import {ProcessInstanceTaskListPage} from './pages/list/process-instance-task-page';
import {ProcessTaskViewPage} from './pages/details/process-task-view-page';
import {ProcessAssignedTaskListPage} from './pages/list/process-assigned-task-page';
import {ProcessNodeEditor} from './pages/details/components/process-node-editor/process-node-editor';
import {
    ProcessNodeEditorConfigurationTab,
} from './pages/details/components/process-node-editor/tabs/process-node-editor-configuration-tab';
import {
    ProcessNodeEditorMoreTab,
} from './pages/details/components/process-node-editor/tabs/process-node-editor-more-tab';
import {
    ProcessNodeEditorOutputsTab,
} from './pages/details/components/process-node-editor/tabs/process-node-editor-outputs-tab';

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
                element: <ProcessNodeEditor/>,
                children: [
                    {
                        index: true,
                        element: <ProcessNodeEditorConfigurationTab/>,
                    },
                    {
                        path: '/processes/:processId/versions/:processVersion/nodes/:nodeId/tabs/configuration',
                        element: <ProcessNodeEditorConfigurationTab/>,
                    },
                    {
                        path: '/processes/:processId/versions/:processVersion/nodes/:nodeId/tabs/outputs',
                        element: <ProcessNodeEditorOutputsTab/>,
                    },
                    {
                        path: '/processes/:processId/versions/:processVersion/nodes/:nodeId/tabs/more',
                        element: <ProcessNodeEditorMoreTab/>,
                    },
                ],
            },
        ],
    },
    {
        path: '/process-instances',
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
        path: '/tasks/:instanceId/:taskId',
        element: <ProcessTaskViewPage/>,
    },
];
