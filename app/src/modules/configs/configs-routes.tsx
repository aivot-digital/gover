import {RouteObject} from 'react-router-dom';
import React from 'react';
import {CommonSettingsPage} from './pages/common-settings-page';
import {SmtpTestPage} from './pages/smtp-test-page';
import {SystemStatusPage} from './pages/system-status-page';
import {MediaSettingsPage} from './pages/media-settings-page';

export const configsRoutes: RouteObject[] = [
    {
        path: '/settings/app',
        element: <CommonSettingsPage />,
    },
    {
        path: '/settings/media',
        element: <MediaSettingsPage />,
    },
    {
        path: '/settings/smtp',
        element: <SmtpTestPage />,
    },
    {
        path: '/settings/status',
        element: <SystemStatusPage />,
    },
];