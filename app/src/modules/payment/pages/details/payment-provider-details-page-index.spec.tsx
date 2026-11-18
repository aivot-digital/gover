import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {PaymentProviderDetailsPageIndex} from './payment-provider-details-page-index';

const testState = vi.hoisted(() => {
    const provider = {
        key: 'test-payment',
        providerKey: 'de.aivot.test.payment',
        providerVersion: 1,
        name: 'Test payment',
        description: 'Internal test payment',
        isTestProvider: false,
        isEnabled: false,
        config: {},
    };

    return {
        provider,
        definition: {
            key: 'de.aivot.test.payment',
            version: 1,
            name: 'Test payment definition',
            description: 'Test payment description.',
            documentationUrl: 'https://docs.example.com/payment/test',
            configLayout: null,
        },
        definitions: [] as Record<string, any>[],
        handleInputPatch: vi.fn(),
        isNewItem: true,
        hasNotChanged: true,
        changedInputs: [] as {field: string, value: unknown}[],
        dispatch: vi.fn(),
        update: vi.fn(),
    };
});

vi.mock('../../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({
        item: testState.provider,
        setItem: vi.fn(),
        additionalData: {definitions: testState.definitions},
        setAdditionalData: vi.fn(),
        isBusy: false,
        setIsBusy: vi.fn(),
        isEditable: true,
        isNewItem: testState.isNewItem,
    }),
}));

vi.mock('../../../../hooks/use-form-manager', () => ({
    useFormManager: () => ({
        currentItem: testState.provider,
        errors: {},
        hasNotChanged: testState.hasNotChanged,
        handleInputBlur: () => vi.fn(),
        handleInputChange: (field: string) => (value: unknown) => testState.changedInputs.push({field, value}),
        handleInputPatch: testState.handleInputPatch,
        validate: vi.fn(() => true),
        reset: vi.fn(),
    }),
}));

vi.mock('../../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: () => ({dialog: null}),
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../payment-providers-api-service', () => ({
    PaymentProvidersApiService: class {
        update = testState.update;
    },
}));

vi.mock('../../../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(async () => true),
}));

vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));

describe('PaymentProviderDetailsPageIndex', () => {
    beforeEach(() => {
        testState.provider.providerKey = testState.definition.key;
        testState.provider.providerVersion = testState.definition.version;
        testState.provider.config = {};
        testState.definitions = [testState.definition];
        testState.handleInputPatch.mockReset();
        testState.isNewItem = true;
        testState.hasNotChanged = true;
        testState.changedInputs = [];
        testState.dispatch.mockReset();
        testState.update.mockReset();
    });

    it('shows the selected definition documentation', () => {
        render(
            <MemoryRouter>
                <PaymentProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        expect(screen.getByText('Dokumentation öffnen').closest('a')).toHaveAttribute(
            'href',
            'https://docs.example.com/payment/test',
        );
        expect(screen.queryByRole('button', {name: /Auswahllisten neu laden/})).not.toBeInTheDocument();
    });

    it('selects the definition version and clears the configuration with the payment provider', () => {
        const latestDefinition = {
            ...testState.definition,
            version: 2,
            name: 'Latest test payment definition',
        };
        testState.provider.providerKey = '';
        testState.provider.providerVersion = 0;
        testState.provider.config = {legacy: 'value'};
        testState.definitions = [testState.definition, latestDefinition];

        render(
            <MemoryRouter>
                <PaymentProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Zahlungsanbieter'}));
        expect(screen.getAllByRole('option')).toHaveLength(1);
        fireEvent.click(screen.getByRole('option', {name: /Latest test payment definition/}));

        expect(testState.handleInputPatch).toHaveBeenCalledWith({
            providerKey: testState.definition.key,
            providerVersion: latestDefinition.version,
            config: {},
        });
    });

    it('keeps every payment provider version selectable', () => {
        testState.definitions = [
            testState.definition,
            {...testState.definition, version: 2},
        ];

        render(
            <MemoryRouter>
                <PaymentProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Version'}));
        expect(screen.getByRole('option', {name: 'Version 1'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Version 2'})).toBeInTheDocument();
    });

    it('reports a lock version conflict without enabling the payment provider again', async () => {
        const message = 'Der Datensatz wurde zwischenzeitlich geändert. Laden Sie ihn bitte neu, bevor Sie Ihre Änderungen erneut speichern.';
        testState.isNewItem = false;
        testState.hasNotChanged = false;
        testState.update.mockRejectedValue({
            status: 409,
            message,
            details: {reason: 'lock_version_conflict'},
            displayableToUser: true,
        });

        render(
            <MemoryRouter>
                <PaymentProviderDetailsPageIndex/>
            </MemoryRouter>,
        );
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message}),
        })));
        expect(testState.changedInputs).not.toContainEqual(expect.objectContaining({field: 'isEnabled'}));
    });

    it('enables the payment provider again when published forms still use it', async () => {
        testState.isNewItem = false;
        testState.hasNotChanged = false;
        testState.update.mockRejectedValue({
            status: 409,
            message: 'Der Zahlungsanbieter wird noch verwendet.',
            details: null,
            displayableToUser: true,
        });

        render(
            <MemoryRouter>
                <PaymentProviderDetailsPageIndex/>
            </MemoryRouter>,
        );
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(testState.changedInputs).toContainEqual({field: 'isEnabled', value: true}));
    });
});
