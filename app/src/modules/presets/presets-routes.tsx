import {RouteObject} from 'react-router-dom';
import {PresetListPage} from '../../pages/staff-pages/preset-pages/preset-list-page';
import {PresetEditPage} from '../../pages/staff-pages/preset-pages/preset-edit-page';

export const presetsRoutes: RouteObject[] = [
    {
        path: '/presets',
        element: <PresetListPage />,
    },
    {
        path: '/presets/edit/:key/:version',
        element: <PresetEditPage />,
    },
];