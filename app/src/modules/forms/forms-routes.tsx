import {RouteObject} from 'react-router-dom';
import {FormsListPage} from './pages/list/forms-list-page/forms-list-page';

export const formsRoutes: RouteObject[] = [
    {
        path: '/forms',
        element: <FormsListPage />,
    },
];