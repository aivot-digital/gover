import {beforeEach, describe, expect, it, vi} from 'vitest';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {type ReactNode} from 'react';
import {StorageProvidersListPage} from './storage-providers-list-page';
import {StorageProvidersApiService} from '../../storage-providers-api-service';
import {Permission} from '../../../../data/permissions/permission';

const mocks = vi.hoisted(() => ({api: {}, dispatch: vi.fn()}));
vi.mock('../../../../hooks/use-api', () => ({useApi: () => mocks.api}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../hooks/use-app-selector', () => ({
    useAppSelector: () => ({
        systemPermissions: [{userId: 'me', permissions: [Permission.STORAGE_PROVIDER_READ]}],
        departmentPermissions: [],
        teamPermissions: [],
        domainPermissions: [],
        processPermissions: [],
        processInstancePermissions: [],
    }),
}));
vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: ReactNode}) => children,
}));
vi.mock('../../../../components/generic-page-header/generic-page-header', () => ({GenericPageHeader: () => null}));
vi.mock('../../../../utils/with-async-wrapper', () => ({
    withAsyncWrapper: ({main}: {main: () => Promise<unknown>}) => main(),
}));

function Location() {
    return <output aria-label="URL">{useLocation().search}</output>;
}

describe('Storage provider list filters', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('offers all storage types explicitly and sends no definition filter when selected', async () => {
        vi.spyOn(StorageProvidersApiService.prototype, 'listDefinitions').mockResolvedValue([{
            key: 'test.s3',
            version: 1,
            name: 'S3',
            abstractDescription: '',
            description: '',
            documentationUrl: null,
            providerConfigLayout: null,
            supportsMetadataAttributes: false,
        }]);
        const fetch = vi.spyOn(StorageProvidersApiService.prototype, 'list').mockResolvedValue({
            content: [],
            page: {number: 0, size: 12, totalElements: 0, totalPages: 0},
        });
        render(
            <MemoryRouter initialEntries={['/?storageProviderDefinitionKey=test.s3&page=3&search=Archiv']}>
                <StorageProvidersListPage />
                <Location />
            </MemoryRouter>,
        );
        const input = screen.getByRole('combobox', {name: 'Speichertyp'});
        await waitFor(() => expect(input).toHaveTextContent('S3'));
        const user = userEvent.setup();
        await user.click(input);
        expect(screen.getAllByRole('option')[0]).toHaveTextContent('Alle Speichertypen');
        expect(screen.queryByRole('button', {name: 'Clear'})).not.toBeInTheDocument();
        await user.click(screen.getByRole('option', {name: 'Alle Speichertypen'}));
        await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(0, 12, 'name', 'ASC', {name: 'Archiv'}));
        expect(input).toHaveTextContent('Alle Speichertypen');
        expect(screen.getByLabelText('URL')).not.toHaveTextContent('storageProviderDefinitionKey');
        expect(screen.getByLabelText('URL')).toHaveTextContent('page=1');

        await user.click(input);
        await user.click(screen.getByRole('option', {name: 'S3'}));
        await waitFor(() =>
            expect(fetch).toHaveBeenLastCalledWith(0, 12, 'name', 'ASC', {
                name: 'Archiv',
                storageProviderDefinitionKey: 'test.s3',
            }),
        );
    });
});
