import {RouteObject} from 'react-router-dom';
import React from 'react';
import {CommonSettingsPage} from './pages/common-settings-page';
import {EmailPage} from './pages/email-page';
import {SystemStatusPage} from './pages/system-status-page';
import {ExtensionsPage} from './pages/extensions-page/extensions-page';
import {ExtensionsPageIndex} from './pages/extensions-page/extensions-page-index';
import {ExtensionsPageList} from './pages/extensions-page/extensions-page-list';

export const configsRoutes: RouteObject[] = [
    {
        path: '/settings/app',
        element: <CommonSettingsPage />,
    },
    {
        path: '/mail',
        element: <EmailPage />,
    },
    {
        path: '/mail/test',
        element: <EmailPage />,
    },
    {
        path: '/settings/status',
        element: <SystemStatusPage />,
    },
    {
        path: '/settings/extensions/*',
        element: <ExtensionsPage />,
        children: [
            {
                index: true,
                element: <ExtensionsPageIndex />,
            },
            {
                path: 'list',
                element: <ExtensionsPageList />,
            },
        ],
    },
];
