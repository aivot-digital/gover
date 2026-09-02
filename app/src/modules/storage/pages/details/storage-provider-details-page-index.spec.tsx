import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {StorageProviderDetailsPageIndex} from './storage-provider-details-page-index';

const testState = vi.hoisted(() => {
    const provider = {
        id: 0,
        configuration: {},
        created: '',
        description: 'Internal test storage',
        lastSync: null,
        maxFileSizeInBytes: 10_000_000,
        metadataAttributes: [],
        name: 'Test storage',
        readOnlyStorage: false,
        status: 'SyncPending',
        statusMessage: null,
        storageProviderDefinitionKey: 'de.aivot.test.storage',
        storageProviderDefinitionVersion: 1,
        systemProvider: false,
        testProvider: false,
        type: 'Assets',
        updated: '',
    };

    return {
        provider,
        definition: {
            key: 'de.aivot.test.storage',
            version: 1,
            name: 'Test storage definition',
            abstractDescription: 'Test storage abstract.',
            description: 'Test storage description.',
            documentationUrl: 'https://docs.example.com/storage/test',
            providerConfigLayout: null,
            supportsMetadataAttributes: false,
        },
        registerSyncPreparationHandler: vi.fn(),
    };
});

vi.mock('../../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({
        item: testState.provider,
        setItem: vi.fn(),
        additionalData: {definitions: [testState.definition]},
        setAdditionalData: vi.fn(),
        isBusy: false,
        setIsBusy: vi.fn(),
        isEditable: true,
        isExistingItem: false,
    }),
}));

vi.mock('../../../../hooks/use-form-manager', () => ({
    useFormManager: () => ({
        currentItem: testState.provider,
        errors: {},
        hasNotChanged: true,
        handleInputBlur: () => vi.fn(),
        handleInputChange: () => vi.fn(),
        validate: vi.fn(() => true),
        reset: vi.fn(),
    }),
}));

vi.mock('../../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: () => ({dialog: null}),
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../../hooks/use-app-selector', () => ({
    useAppSelector: () => null,
}));

vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));

vi.mock('./storage-provider-details-page', () => ({
    useStorageProviderDetailsPageSyncContext: () => ({
        registerSyncPreparationHandler: testState.registerSyncPreparationHandler,
    }),
}));

describe('StorageProviderDetailsPageIndex', () => {
    it('shows the selected definition documentation', () => {
        render(
            <MemoryRouter>
                <StorageProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        expect(screen.getByText('Dokumentation öffnen').closest('a')).toHaveAttribute(
            'href',
            'https://docs.example.com/storage/test',
        );
    });
});
