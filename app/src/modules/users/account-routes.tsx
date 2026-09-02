import {AccountDetailsPage} from './pages/account/account-details-page';
import {AccountDetailsPageIndex} from './pages/account/account-details-page-index';
import {AccountDetailsPageDepartmentMemberships} from './pages/account/account-details-page-department-memberships';
import {AccountDetailsPageNotifications} from './pages/account/account-details-page-notifications';
import React from 'react';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const accountRoutes = [
    {
        path: '/account',
        element: <AccountDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
        children: [
            {
                index: true,
                element: <AccountDetailsPageIndex />,
            },
            {
                path: '/account/memberships-and-roles',
                element: <AccountDetailsPageDepartmentMemberships />,
            },
            {
                path: '/account/notifications',
                element: <AccountDetailsPageNotifications />,
            },
        ],
    }
];
