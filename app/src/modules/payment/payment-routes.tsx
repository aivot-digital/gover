import {RouteObject} from 'react-router-dom';
import {PaymentProvidersListPage} from './pages/list/payment-providers-list-page';
import {PaymentProviderDetailsPage} from './pages/details/payment-provider-details-page';
import {PaymentProviderDetailsPageIndex} from './pages/details/payment-provider-details-page-index';
import {PaymentProviderDetailsPageTransactions} from './pages/details/payment-provider-details-page-transactions';
import {PaymentProviderDetailsPageForms} from './pages/details/payment-provider-details-page-forms';
import {PaymentProviderDetailsPageTest} from './pages/details/payment-provider-details-page-test';
import {duplicatePageWarningRouteHandle} from '../../components/duplicate-page-warning/duplicate-page-warning-route-handle';

export const paymentRoutes: RouteObject[] = [
    {
        path: '/payment-providers',
        element: <PaymentProvidersListPage />,
    },
    {
        path: '/payment-providers/:id',
        element: <PaymentProviderDetailsPage />,
        handle: duplicatePageWarningRouteHandle,
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
];
