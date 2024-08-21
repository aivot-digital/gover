import React from 'react';
import {FormListPage} from '../pages/staff-pages/application-pages/form-list-page';
import {FormEditPage} from '../pages/staff-pages/application-pages/form-edit-page';
import {Settings} from '../pages/staff-pages/settings/settings';
import {Profile} from '../pages/staff-pages/profile/profile';
import {PresetListPage} from '../pages/staff-pages/preset-pages/preset-list-page';
import {PresetEditPage} from '../pages/staff-pages/preset-pages/preset-edit-page';
import {UserListPage} from '../pages/staff-pages/user-pages/user-list-page';
import {UserEditPage} from '../pages/staff-pages/user-pages/user-edit-page';
import {DepartmentListPage} from '../pages/staff-pages/department-pages/department-list-page';
import {DepartmentEditPage} from '../pages/staff-pages/department-pages/department-edit-page';
import {SubmissionListPage} from '../pages/staff-pages/submission-pages/submission-list-page';
import {SubmissionEditPage} from '../pages/staff-pages/submission-pages/submission-edit-page';
import {DestinationListPage} from '../pages/staff-pages/destination-pages/destination-list-page';
import {DestinationEditPage} from '../pages/staff-pages/destination-pages/destination-edit-page';
import {ProviderLinkListPage} from '../pages/staff-pages/provider-link-pages/provider-link-list-page';
import {ProviderLinkEditPage} from '../pages/staff-pages/provider-link-pages/provider-link-edit-page';
import {AssetListPage} from '../pages/staff-pages/asset-pages/asset-list-page';
import {AssetEditPage} from '../pages/staff-pages/asset-pages/asset-edit-page';
import {ThemeListPage} from '../pages/staff-pages/theme-pages/theme-list-page';
import {ThemeEditPage} from '../pages/staff-pages/theme-pages/theme-edit-page';
import {type Route} from '../models/lib/route';
import {NotFound} from '../pages/shared/not-found/not-found';
import {ModuleSelectPage} from '../pages/staff-pages/module-select-pages/module-select-page';

export const staffAppRoutes: Record<string, Route> = {
    moduleSelect: {
        path: '/',
        element: <ModuleSelectPage/>,
    },

    applicationList: {
        path: '/forms',
        element: <FormListPage/>,
    },
    applicationEdit: {
        path: '/forms/:id',
        element: <FormEditPage/>,
    },

    settings: {
        path: '/settings',
        element: <Settings/>,
    },
    profile: {
        path: '/profile',
        element: <Profile/>,
    },

    presetList: {
        path: '/presets',
        element: <PresetListPage/>,
    },
    presetEdit: {
        path: '/presets/edit/:key/:version',
        element: <PresetEditPage/>,
    },

    departmentList: {
        path: '/departments',
        element: <DepartmentListPage/>,
    },
    departmentEdit: {
        path: '/departments/:id',
        element: <DepartmentEditPage/>,
    },

    destinationList: {
        path: '/destinations',
        element: <DestinationListPage/>,
    },
    destinationEdit: {
        path: '/destinations/:id',
        element: <DestinationEditPage/>,
    },

    userList: {
        path: '/users',
        element: <UserListPage/>,
    },
    userEdit: {
        path: '/users/:id',
        element: <UserEditPage/>,
    },

    linkList: {
        path: '/provider-links',
        element: <ProviderLinkListPage/>,
    },
    linkEdit: {
        path: '/provider-links/:id',
        element: <ProviderLinkEditPage/>,
    },

    submissionList: {
        path: '/submissions',
        element: <SubmissionListPage/>,
    },
    submissionEdit: {
        path: '/submissions/:id',
        element: <SubmissionEditPage/>,
    },

    assetList: {
        path: '/assets',
        element: <AssetListPage/>,
    },
    assetEdit: {
        path: '/assets/:key',
        element: <AssetEditPage/>,
    },

    themeList: {
        path: '/themes',
        element: <ThemeListPage/>,
    },
    themeEdit: {
        path: '/themes/:id',
        element: <ThemeEditPage/>,
    },

    notFound: {
        path: '*',
        element: <NotFound/>,
    },
};

export function getPath(page: keyof typeof staffAppRoutes): string {
    return staffAppRoutes[page].path;
}
