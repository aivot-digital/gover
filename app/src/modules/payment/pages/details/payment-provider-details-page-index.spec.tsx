import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
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
    });
});
