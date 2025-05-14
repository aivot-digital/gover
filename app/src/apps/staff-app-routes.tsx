import React from 'react';
import {FormListPage} from '../pages/staff-pages/application-pages/form-list-page';
import {FormEditPage} from '../pages/staff-pages/application-pages/form-edit-page';
import {Settings} from '../pages/staff-pages/settings/settings';
import {PresetEditPage} from '../pages/staff-pages/preset-pages/preset-edit-page';
import {SubmissionListPage} from '../pages/staff-pages/submission-pages/submission-list-page';
import {SubmissionEditPage} from '../pages/staff-pages/submission-pages/submission-edit-page';
import {type Route} from '../models/lib/route';
import {NotFound} from '../pages/shared/not-found/not-found';
import {ModuleSelectPage} from '../pages/staff-pages/module-select-pages/module-select-page';
import {PaymentProvidersListPage} from '../pages/staff-pages/payment-providers/list/payment-providers-list-page';
import {PaymentProviderDetailsPage} from '../pages/staff-pages/payment-providers/details/payment-provider-details-page';
import {PaymentProviderDetailsPageIndex} from '../pages/staff-pages/payment-providers/details/payment-provider-details-page-index';
import {PaymentProviderDetailsPageTest} from '../pages/staff-pages/payment-providers/details/payment-provider-details-page-test';
import {
    PaymentProviderDetailsPageForms
} from "../pages/staff-pages/payment-providers/details/payment-provider-details-page-forms";
import {AccountDetailsPage} from "../modules/users/pages/account/account-details-page";
import {AccountDetailsPageIndex} from "../modules/users/pages/account/account-details-page-index";
import {AccountDetailsPageDepartmentMemberships} from "../modules/users/pages/account/account-details-page-department-memberships";
import {AccountDetailsPageNotifications} from "../modules/users/pages/account/account-details-page-notifications";
import {ThemeListPage} from "../modules/themes/pages/list/theme-list-page";
import {ThemeDetailsPage} from "../modules/themes/pages/details/theme-details-page";
import {ThemeDetailsPageIndex} from "../modules/themes/pages/details/theme-details-page-index";
import {ThemeDetailsPageForms} from "../modules/themes/pages/details/theme-details-page-forms";
import {UserListPage} from "../modules/users/pages/user/list/user-list-page";
import {UserDetailsPage} from "../modules/users/pages/user/details/user-details-page";
import {UserDetailsPageIndex} from "../modules/users/pages/user/details/user-details-page-index";
import {
    UserDetailsPageDepartmentMemberships
} from "../modules/users/pages/user/details/user-details-page-department-memberships";
import {AssetListPage} from "../modules/assets/pages/asset-list-page";
import {AssetDetailsPageIndex} from "../modules/assets/pages/asset-details-page-index";
import {AssetDetailsPage} from "../modules/assets/pages/asset-details-page";
import {DestinationListPage} from "../modules/destination/pages/list/destination-list-page";
import {DestinationDetailsPage} from "../modules/destination/pages/details/destination-details-page";
import {DestinationDetailsPageIndex} from "../modules/destination/pages/details/destination-details-page-index";
import {DestinationDetailsPageForms} from "../modules/destination/pages/details/destination-details-page-forms";
import {PresetListPage} from "../pages/staff-pages/preset-pages/preset-list-page";
import {PaymentProviderDetailsPageTransactions} from '../pages/staff-pages/payment-providers/details/payment-provider-details-page-transactions';

export const staffAppRoutes: Record<string, Route> = {
    moduleSelect: {
        path: '/',
        element: <ModuleSelectPage />,
    },

    applicationList: {
        path: '/forms',
        element: <FormListPage />,
    },
    applicationEdit: {
        path: '/forms/:id',
        element: <FormEditPage />,
    },

    settings: {
        path: '/settings',
        element: <Settings />,
    },
    account: {
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
    },
    presetList: {
        path: '/presets',
        element: <PresetListPage />,
    },
    presetEdit: {
        path: '/presets/edit/:key/:version',
        element: <PresetEditPage />,
    },

    /* Department rules are defined in module */

    destinationList: {
        path: '/destinations',
        element: <DestinationListPage />,
    },
    destinationEdit: {
        path: '/destinations/:id',
        element: <DestinationDetailsPage />,
        children: [
            {
                index: true,
                element: <DestinationDetailsPageIndex />,
            },
            {
                path: '/destinations/:id/forms',
                element: <DestinationDetailsPageForms />,
            },
        ],
    },

    userList: {
        path: '/users',
        element: <UserListPage />,
    },
    userEdit: {
        path: '/users/:id',
        element: <UserDetailsPage />,
        children: [
            {
                index: true,
                element: <UserDetailsPageIndex />,
            },
            {
                path: '/users/:id/departments-and-roles',
                element: <UserDetailsPageDepartmentMemberships />,
            },
        ],
    },

    submissionList: {
        path: '/submissions',
        element: <SubmissionListPage />,
    },
    submissionEdit: {
        path: '/submissions/:id',
        element: <SubmissionEditPage />,
    },

    assetList: {
        path: '/assets',
        element: <AssetListPage />,
    },
    assetEdit: {
        path: '/assets/:key',
        element: <AssetDetailsPage />,
        children: [
            {
                index: true,
                element: <AssetDetailsPageIndex />,
            },
        ],
    },

    themeList: {
        path: '/themes',
        element: <ThemeListPage />,
    },
    themesEdit: {
        path: '/themes/:id',
        element: <ThemeDetailsPage />,
        children: [
            {
                index: true,
                element: <ThemeDetailsPageIndex />,
            },
            {
                path: '/themes/:id/forms',
                element: <ThemeDetailsPageForms />,
            },
        ],
    },

    paymentProvidersList: {
        path: '/payment-providers',
        element: <PaymentProvidersListPage />,
    },
    paymentProvidersEdit: {
        path: '/payment-providers/:id',
        element: <PaymentProviderDetailsPage />,
        children: [
            {
                index: true,
                element: <PaymentProviderDetailsPageIndex />,
            },
            {
                path: '/payment-providers/:id/test',
                element: <PaymentProviderDetailsPageTest />,
            },
            {
                path: '/payment-providers/:id/forms',
                element: <PaymentProviderDetailsPageForms />,
            },
            {
                path: '/payment-providers/:id/tx',
                element: <PaymentProviderDetailsPageTransactions />,
            },
        ],
    },


    notFound: {
        path: '*',
        element: <NotFound />,
    },
};

export function getPath(page: keyof typeof staffAppRoutes): string {
    return staffAppRoutes[page].path;
}
