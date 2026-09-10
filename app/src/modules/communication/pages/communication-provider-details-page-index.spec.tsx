import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {CommunicationProvidersApiService} from '../communication-providers-api-service';
import {CommunicationProviderDetailsPageIndex} from './communication-provider-details-page-index';

const testState = vi.hoisted(() => ({
    provider: {} as Record<string, any>,
    layout: {} as Record<string, any>,
    derivedData: {
        effectiveValues: {},
        elementStates: {},
    } as Record<string, any>,
    dispatch: vi.fn(),
    setItem: vi.fn(),
    setIsBusy: vi.fn(),
}));

vi.mock('../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({
        item: testState.provider,
        setItem: testState.setItem,
        isNewItem: true,
        additionalData: {
            definitions: [{
                key: testState.provider.communicationProviderDefinitionKey,
                version: testState.provider.communicationProviderDefinitionVersion,
                name: 'Test definition',
                description: 'Test definition',
                supportedIdentityProviderTypes: [],
            }],
        },
        isBusy: false,
        setIsBusy: testState.setIsBusy,
        isEditable: true,
    }),
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: () => ({dialog: null}),
}));

vi.mock('../../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(async () => true),
}));

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => true,
}));

vi.mock('../../elements/components/element-derivation-context', async () => {
    const React = await import('react');

    return {
        ElementDerivationContext: (props: {
            element: Record<string, any>;
            computedErrors?: Record<string, {error?: string | null}> | null;
            onDerivationFinished?: (data: Record<string, any>) => void;
        }) => {
            React.useEffect(() => {
                props.onDerivationFinished?.(testState.derivedData);
            }, [props.element, props.onDerivationFinished]);

            return React.createElement(
                'div',
                {'data-testid': 'configuration-layout'},
                Object.entries(props.computedErrors ?? {}).map(([id, error]) => (
                    error?.error == null
                        ? null
                        : React.createElement('span', {key: id}, error.error)
                )),
            );
        },
    };
});

function textField(id: string, label: string) {
    return {
        id,
        type: ElementType.Text,
        label,
        required: true,
    };
}

function configLayout(...children: Record<string, any>[]) {
    return {
        id: 'communication-provider-config',
        type: ElementType.ConfigLayout,
        children,
    };
}

async function renderChangedProvider() {
    render(
        <MemoryRouter>
            <CommunicationProviderDetailsPageIndex/>
        </MemoryRouter>,
    );

    await screen.findByTestId('configuration-layout');
    fireEvent.change(screen.getByRole('textbox', {name: /^Name/}), {
        target: {value: 'Changed provider'},
    });
    await waitFor(() => expect(screen.getByRole('button', {name: 'Speichern'})).toBeEnabled());
}

describe('CommunicationProviderDetailsPageIndex', () => {
    beforeEach(() => {
        testState.provider = {
            id: 0,
            communicationProviderDefinitionKey: 'de.aivot.test.communication',
            communicationProviderDefinitionVersion: 1,
            name: 'Test provider',
            description: 'Test provider description',
            configuration: {},
            isEnabled: false,
            isTestProvider: false,
        };
        testState.layout = configLayout();
        testState.derivedData = {
            effectiveValues: {},
            elementStates: {},
        };
        testState.dispatch.mockReset();
        testState.setItem.mockReset();
        testState.setIsBusy.mockReset();
        vi.restoreAllMocks();
    });

    it('marks a missing custom sender name and does not create the provider', async () => {
        testState.provider.configuration = {
            senderMode: 'custom',
            customSenderAddress: 'sender@example.test',
        };
        testState.layout = configLayout(
            textField('senderMode', 'Absender'),
            textField('customSenderName', 'From Name'),
            textField('customSenderAddress', 'From Adresse'),
        );
        testState.derivedData = {
            effectiveValues: testState.provider.configuration,
            elementStates: {
                senderMode: {visible: true},
                customSenderName: {visible: true},
                customSenderAddress: {visible: true},
            },
        };
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(await screen.findByText('From Name ist ein Pflichtfeld.')).toBeInTheDocument();
        expect(createProvider).not.toHaveBeenCalled();
        expect(testState.dispatch).toHaveBeenCalledWith(expect.objectContaining({
            payload: expect.objectContaining({message: 'Bitte überprüfen Sie Ihre Eingaben.'}),
        }));
    });

    it('rejects missing visible FIT-Connect fields before creating the provider', async () => {
        testState.layout = configLayout(
            textField('destinationId', 'Empfänger-Zustellpunkt-ID'),
            textField('senderClientId', 'Sender Client ID'),
        );
        testState.derivedData = {
            effectiveValues: {},
            elementStates: {
                destinationId: {visible: true},
                senderClientId: {visible: true},
            },
        };
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(await screen.findByText('Empfänger-Zustellpunkt-ID ist ein Pflichtfeld.')).toBeInTheDocument();
        expect(screen.getByText('Sender Client ID ist ein Pflichtfeld.')).toBeInTheDocument();
        expect(createProvider).not.toHaveBeenCalled();
    });

    it('creates a provider with a complete custom sender configuration', async () => {
        testState.provider.configuration = {
            senderMode: 'custom',
            customSenderName: 'Custom Service',
            customSenderAddress: 'sender@example.test',
        };
        testState.layout = configLayout(
            textField('senderMode', 'Absender'),
            textField('customSenderName', 'From Name'),
            textField('customSenderAddress', 'From Adresse'),
        );
        testState.derivedData = {
            effectiveValues: testState.provider.configuration,
            elementStates: {
                senderMode: {visible: true},
                customSenderName: {visible: true},
                customSenderAddress: {visible: true},
            },
        };
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(createProvider).toHaveBeenCalledOnce());
    });

    it('accepts a required value supplied by the configuration derivation', async () => {
        testState.layout = configLayout(
            textField('senderMode', 'Absender'),
        );
        testState.derivedData = {
            effectiveValues: {
                senderMode: 'default',
            },
            elementStates: {
                senderMode: {visible: true},
            },
        };
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(createProvider).toHaveBeenCalledOnce());
        expect(createProvider).toHaveBeenCalledWith(expect.objectContaining({
            configuration: {},
        }));
    });

    it('does not validate hidden sender fields', async () => {
        testState.layout = configLayout(
            textField('customSenderName', 'From Name'),
            textField('customSenderAddress', 'From Adresse'),
        );
        testState.derivedData = {
            effectiveValues: {},
            elementStates: {
                customSenderName: {visible: false},
                customSenderAddress: {visible: false},
            },
        };
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(createProvider).toHaveBeenCalledOnce());
    });
});
