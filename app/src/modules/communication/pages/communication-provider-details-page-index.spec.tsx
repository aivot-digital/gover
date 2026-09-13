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
    confirm: vi.fn(async () => true),
    isNewItem: true,
    definitions: [] as Record<string, any>[],
}));

vi.mock('../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({
        item: testState.provider,
        setItem: testState.setItem,
        isNewItem: testState.isNewItem,
        additionalData: {
            definitions: testState.definitions,
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
    useConfirm: () => testState.confirm,
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

const EMAIL_PATTERN = {
    regex: '^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$',
    message: 'Bitte geben Sie eine gültige E-Mail-Adresse ein.',
};

const OPTIONAL_EMAIL_PATTERN = {
    regex: '^(?:$|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$',
    message: EMAIL_PATTERN.message,
};

function textField(id: string, label: string, properties: Record<string, any> = {}) {
    return {
        id,
        type: ElementType.Text,
        label,
        required: true,
        ...properties,
    };
}

function configLayout(...children: Record<string, any>[]) {
    return {
        id: 'communication-provider-config',
        type: ElementType.ConfigLayout,
        children,
    };
}

function configureMailProvider(configuration: Record<string, any>) {
    testState.provider.configuration = configuration;
    testState.layout = configLayout(
        textField('senderMode', 'Absender'),
        textField('customSenderName', 'From Name'),
        textField('customSenderAddress', 'From Adresse', {pattern: EMAIL_PATTERN}),
        textField('replyToAddress', 'Reply-To-Adresse', {
            required: false,
            pattern: OPTIONAL_EMAIL_PATTERN,
        }),
    );
    testState.derivedData = {
        effectiveValues: configuration,
        elementStates: {
            senderMode: {visible: true},
            customSenderName: {visible: true},
            customSenderAddress: {visible: true},
            replyToAddress: {visible: true},
        },
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
        vi.restoreAllMocks();
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
        testState.isNewItem = true;
        testState.definitions = [{
            key: testState.provider.communicationProviderDefinitionKey,
            version: testState.provider.communicationProviderDefinitionVersion,
            name: 'Test definition',
            description: 'Test definition',
            supportedIdentityProviderTypes: [],
        }];
        testState.derivedData = {
            effectiveValues: {},
            elementStates: {},
        };
        testState.dispatch.mockReset();
        testState.setItem.mockReset();
        testState.setIsBusy.mockReset();
        testState.confirm.mockReset();
        testState.confirm.mockResolvedValue(true);
    });

    it('lists each definition once, selects its latest version, and keeps all versions selectable', async () => {
        testState.provider.communicationProviderDefinitionKey = '';
        testState.provider.communicationProviderDefinitionVersion = 0;
        testState.provider.configuration = {legacy: 'value'};
        testState.definitions = [
            {
                key: 'de.aivot.test.communication',
                version: 1,
                name: 'Old test definition',
                description: 'Old test definition',
                supportedIdentityProviderTypes: [],
            },
            {
                key: 'de.aivot.test.communication',
                version: 2,
                name: 'Latest test definition',
                description: 'Latest test definition',
                supportedIdentityProviderTypes: [],
            },
        ];
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);

        render(
            <MemoryRouter>
                <CommunicationProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Definition'}));
        expect(screen.getAllByRole('option')).toHaveLength(1);
        fireEvent.click(screen.getByRole('option', {name: /Latest test definition/}));

        await waitFor(() => expect(screen.getByRole('combobox', {name: 'Version'})).toHaveTextContent('Version 2'));
        fireEvent.mouseDown(screen.getByRole('combobox', {name: 'Version'}));
        expect(screen.getByRole('option', {name: 'Version 1'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Version 2'})).toBeInTheDocument();
    });

    it('marks a missing custom sender name and does not create the provider', async () => {
        configureMailProvider({
            senderMode: 'custom',
            customSenderAddress: 'sender@example.test',
        });
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

    it.each([
        {
            fieldLabel: 'From Adresse',
            configuration: {
                senderMode: 'custom',
                customSenderName: 'Custom Service',
                customSenderAddress: 'keine-email',
                replyToAddress: 'replies@example.test',
            },
        },
        {
            fieldLabel: 'Reply-To-Adresse',
            configuration: {
                senderMode: 'custom',
                customSenderName: 'Custom Service',
                customSenderAddress: 'sender@example.test',
                replyToAddress: 'keine-email',
            },
        },
    ])('marks an invalid $fieldLabel and does not create the provider', async ({configuration}) => {
        configureMailProvider(configuration);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        expect(await screen.findByText(EMAIL_PATTERN.message)).toBeInTheDocument();
        expect(createProvider).not.toHaveBeenCalled();
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
        configureMailProvider({
            senderMode: 'custom',
            customSenderName: 'Custom Service',
            customSenderAddress: 'sender@example.test',
        });
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const createProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'createProvider')
            .mockResolvedValue({...testState.provider, id: 17} as any);

        await renderChangedProvider();
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(createProvider).toHaveBeenCalledOnce());
    });

    it('creates a provider with an empty optional reply-to address', async () => {
        configureMailProvider({
            senderMode: 'custom',
            customSenderName: 'Custom Service',
            customSenderAddress: 'sender@example.test',
            replyToAddress: '',
        });
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

    it('updates the test status of an existing provider', async () => {
        testState.provider.id = 17;
        testState.isNewItem = false;
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const updateProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'updateProvider')
            .mockImplementation(async (id, request) => ({id, ...request}));

        render(
            <MemoryRouter>
                <CommunicationProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        await screen.findByTestId('configuration-layout');
        const testProviderSwitch = screen.getByRole('switch', {name: 'Vorproduktive Konfiguration'});
        expect(testProviderSwitch).toBeEnabled();

        fireEvent.click(testProviderSwitch);
        fireEvent.click(await screen.findByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(updateProvider).toHaveBeenCalledWith(17, expect.objectContaining({
            isTestProvider: true,
        })));
    });

    it('requires the provider name before deleting an existing provider', async () => {
        testState.provider.id = 17;
        testState.isNewItem = false;
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const deleteProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'deleteProvider')
            .mockResolvedValue();

        render(
            <MemoryRouter>
                <CommunicationProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        await screen.findByTestId('configuration-layout');
        fireEvent.click(screen.getByRole('button', {name: 'Löschen'}));

        await waitFor(() => expect(testState.confirm).toHaveBeenCalledWith(expect.objectContaining({
            title: 'Kommunikationsanbieter löschen',
            confirmationText: 'Test provider',
            confirmButtonText: 'Ja, endgültig löschen',
            isDestructive: true,
        })));
        await waitFor(() => expect(deleteProvider).toHaveBeenCalledWith(17));
    });

    it('does not delete the provider when the confirmation is cancelled', async () => {
        testState.provider.id = 17;
        testState.isNewItem = false;
        testState.confirm.mockResolvedValue(false);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderConfigurationLayout')
            .mockResolvedValue(testState.layout as any);
        const deleteProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'deleteProvider')
            .mockResolvedValue();

        render(
            <MemoryRouter>
                <CommunicationProviderDetailsPageIndex/>
            </MemoryRouter>,
        );

        await screen.findByTestId('configuration-layout');
        fireEvent.click(screen.getByRole('button', {name: 'Löschen'}));

        await waitFor(() => expect(testState.confirm).toHaveBeenCalledOnce());
        expect(deleteProvider).not.toHaveBeenCalled();
    });
});
