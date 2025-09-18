import {RouteObject} from 'react-router-dom';
import {ThemeListPage} from './pages/list/theme-list-page';
import {ThemeDetailsPage} from './pages/details/theme-details-page';
import {ThemeDetailsPageIndex} from './pages/details/theme-details-page-index';
import {ThemeDetailsPageForms} from './pages/details/theme-details-page-forms';

export const themesRoutes: RouteObject[] = [
    {
        path: '/themes',
        element: <ThemeListPage />,
    },
    {
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
];