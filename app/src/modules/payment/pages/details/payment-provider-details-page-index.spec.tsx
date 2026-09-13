import {fireEvent, render, screen} from '@testing-library/react';
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
        isNewItem: true,
    }),
}));

vi.mock('../../../../hooks/use-form-manager', () => ({
    useFormManager: () => ({
        currentItem: testState.provider,
        errors: {},
        hasNotChanged: true,
        handleInputBlur: () => vi.fn(),
        handleInputChange: () => vi.fn(),
        handleInputPatch: testState.handleInputPatch,
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

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Zahlungsdienstleister'}));
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
});
