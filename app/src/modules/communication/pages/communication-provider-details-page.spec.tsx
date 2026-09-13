import {render} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {communicationRoutes} from '../communication-routes';
import {CommunicationProviderDetailsPage} from './communication-provider-details-page';

const testState = vi.hoisted(() => ({
    detailsPageProps: null as Record<string, any> | null,
}));

vi.mock('../../../components/generic-details-page/generic-details-page', () => ({
    GenericDetailsPage: (props: Record<string, any>) => {
        testState.detailsPageProps = props;
        return null;
    },
}));

vi.mock('../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => children,
}));

describe('CommunicationProviderDetailsPage', () => {
    it('enables the test tab only for existing providers with update permission', () => {
        render(<CommunicationProviderDetailsPage/>);

        const testTab = testState.detailsPageProps?.tabs.find((tab: Record<string, any>) => (
            tab.path === '/communication-providers/:id/test'
        ));

        expect(testTab).toMatchObject({
            label: 'Testen',
            onlyExisting: true,
            requiredPermission: Permission.COMMUNICATION_PROVIDER_UPDATE,
        });
        expect(testTab.isDisabled).toBeUndefined();
    });

    it('enables the linked identity providers tab only for existing providers with read permission', () => {
        render(<CommunicationProviderDetailsPage/>);

        const identityProvidersTab = testState.detailsPageProps?.tabs.find((tab: Record<string, any>) => (
            tab.path === '/communication-providers/:id/identity-providers'
        ));

        expect(identityProvidersTab).toMatchObject({
            label: 'Verknüpfte Nutzerkontenanbieter',
            onlyExisting: true,
            requiredPermission: Permission.IDENTITY_PROVIDER_READ,
        });
    });

    it('registers the test page as a child route', () => {
        const detailsRoute = communicationRoutes.find(route => route.path === '/communication-providers/:id');

        expect(detailsRoute?.children).toEqual(expect.arrayContaining([
            expect.objectContaining({
                path: '/communication-providers/:id/test',
            }),
        ]));
    });

    it('registers the linked identity providers page as a child route', () => {
        const detailsRoute = communicationRoutes.find(route => route.path === '/communication-providers/:id');

        expect(detailsRoute?.children).toEqual(expect.arrayContaining([
            expect.objectContaining({
                path: '/communication-providers/:id/identity-providers',
            }),
        ]));
    });
});
