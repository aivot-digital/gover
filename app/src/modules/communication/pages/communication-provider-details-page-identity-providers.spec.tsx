import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {IdentityProviderType} from '../../identity/enums/identity-provider-type';
import {type IdentityProviderListDTO} from '../../identity/models/identity-provider-list-dto';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {type CommunicationProvider} from '../models';
import {CommunicationProviderDetailsPageIdentityProviders} from './communication-provider-details-page-identity-providers';

const testState = vi.hoisted(() => ({
    provider: undefined as CommunicationProvider | undefined,
    permissions: {system: []} as Record<string, any>,
    genericListProps: null as Record<string, any> | null,
    row: undefined as IdentityProviderListDTO | undefined,
    requireSystemPermission: vi.fn(),
}));

vi.mock('../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({item: testState.provider}),
}));

vi.mock('../../../hooks/use-app-selector', () => ({
    useAppSelector: () => testState.permissions,
}));

vi.mock('../../permissions/utils/permission-utils', () => ({
    requireSystemPermission: testState.requireSystemPermission,
}));

vi.mock('../../../components/generic-list/generic-list', () => ({
    GenericList: (props: Record<string, any>) => {
        testState.genericListProps = props;
        if (testState.row == null) {
            return props.noDataPlaceholder;
        }

        return (
            <div data-testid="identity-provider-row">
                {props.columnDefinitions.map((column: Record<string, any>) => {
                    const value = testState.row?.[column.field as keyof IdentityProviderListDTO];
                    return (
                        <div key={column.field}>
                            {column.renderCell == null
                                ? String(value ?? '')
                                : column.renderCell({
                                    id: testState.row?.key,
                                    row: testState.row,
                                    value,
                                })}
                        </div>
                    );
                })}
            </div>
        );
    },
}));

describe('CommunicationProviderDetailsPageIdentityProviders', () => {
    beforeEach(() => {
        testState.provider = createCommunicationProvider();
        testState.permissions = {system: [Permission.IDENTITY_PROVIDER_READ]};
        testState.genericListProps = null;
        testState.row = createIdentityProvider();
        testState.requireSystemPermission.mockReset();
    });

    it('loads identity providers filtered by the current communication provider', async () => {
        const response = {
            content: [testState.row],
            page: {size: 12, number: 1, totalElements: 1, totalPages: 1},
        };
        const list = vi.spyOn(IdentityProvidersApiService.prototype, 'list').mockResolvedValue(response as any);

        renderPage();
        const result = await testState.genericListProps?.fetch({
            page: 1,
            size: 12,
            sort: 'name',
            order: 'DESC',
            search: 'Bund',
        });

        expect(result).toEqual(response);
        expect(list).toHaveBeenCalledWith(1, 12, 'name', 'DESC', {
            name: 'Bund',
            communicationProviderId: 7,
        });
        expect(testState.requireSystemPermission).toHaveBeenCalledWith(
            testState.permissions,
            Permission.IDENTITY_PROVIDER_READ,
        );
    });

    it('links the provider and displays its test and activity status', () => {
        renderPage();

        expect(screen.getByRole('link', {name: /BundID/})).toHaveAttribute(
            'href',
            '/identity-providers/00000000-0000-0000-0000-000000000001',
        );
        expect(screen.getByText('Test')).toBeInTheDocument();
        expect(screen.getByText('Inaktiv')).toBeInTheDocument();
        expect(screen.getByText('BundID für Tests')).toBeInTheDocument();
    });

    it('configures clear empty, loading and search states', () => {
        testState.row = undefined;
        renderPage();

        expect(screen.getByText('Keine Nutzerkontenanbieter verknüpft')).toBeInTheDocument();
        expect(screen.getByText(
            'Dieser Kommunikationsanbieter wird aktuell von keinem Nutzerkontenanbieter verwendet.',
        )).toBeInTheDocument();
        expect(testState.genericListProps?.loadingPlaceholder).toBe('Lade Nutzerkontenanbieter…');
        expect(testState.genericListProps?.noSearchResultsPlaceholder).toBe('Keine Nutzerkontenanbieter gefunden');
    });
});

function renderPage() {
    return render(
        <MemoryRouter>
            <CommunicationProviderDetailsPageIdentityProviders/>
        </MemoryRouter>,
    );
}

function createCommunicationProvider(): CommunicationProvider {
    return {
        id: 7,
        communicationProviderDefinitionKey: 'mail',
        communicationProviderDefinitionVersion: 1,
        name: 'Test-Mail',
        description: 'Test provider',
        configuration: {},
        isEnabled: true,
        isTestProvider: true,
    };
}

function createIdentityProvider(): IdentityProviderListDTO {
    return {
        key: '00000000-0000-0000-0000-000000000001',
        metadataIdentifier: 'bund-id',
        type: IdentityProviderType.BundID,
        name: 'BundID',
        description: 'BundID für Tests',
        iconAssetKey: null,
        attributes: [],
        isEnabled: false,
        isTestProvider: true,
    };
}
