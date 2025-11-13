import {RouteObject} from 'react-router-dom';
import {SubmissionListPage} from '../../pages/staff-pages/submission-pages/submission-list-page';
import {SubmissionEditPage} from '../../pages/staff-pages/submission-pages/submission-edit-page';

export const submissionsRoutes: RouteObject[] = [
    {
        path: '/submissions',
        element: <SubmissionListPage />,
    },
    {
        path: '/submissions/:id',
        element: <SubmissionEditPage />,
    },
];