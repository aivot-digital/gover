import {AccountDetailsPage} from './pages/account/account-details-page';
import {AccountDetailsPageIndex} from './pages/account/account-details-page-index';
import {AccountDetailsPageDepartmentMemberships} from './pages/account/account-details-page-department-memberships';
import {AccountDetailsPageNotifications} from './pages/account/account-details-page-notifications';
import React from 'react';

export const accountRoutes = [
    {
        path: '/account',
        element: <AccountDetailsPage />,
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