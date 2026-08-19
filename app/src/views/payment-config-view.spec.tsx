import {describe, expect, it, vi, beforeEach, type Mock, type MockInstance} from 'vitest';
import React from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {PaymentConfigView} from './payment-config-view';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../components/view-dispatcher/view-dispatcher.context';
import {ConfirmProvider} from '../providers/confirm-provider';
import {PaymentProvidersApiService} from '../modules/payment/payment-providers-api-service';
import type {BaseViewProps} from './base-view';
import {
    PaymentConfigElement,
    PaymentConfigElementValue,
    PaymentConfigElementValueItem,
    PaymentConfigElementValueItemCostType,
    PaymentConfigElementValueItemIdType,
    PaymentConfigElementValueItemQuantityType,
    PaymentConfigElementValueItemVariableValueCalculationType,
} from '../models/elements/form/input/payment-config-element';
import type {FormLayoutElement} from '../models/elements/form-layout-element';
import type {PaymentProviderResponseDTO} from '../modules/payment/dtos/payment-provider-response-dto';
import type {PaymentProviderDefinitionResponseDTO} from '../modules/payment/dtos/payment-provider-definition-response-dto';

vi.mock('./process-data-key-input-field-view', () => ({
    ProcessDataKeyInputComponent: (props: {
        label?: string | null;
        error?: string | null;
        required?: boolean | null;
    }) => React.createElement('div', {}, [
        props.label != null ? React.createElement('span', {
            key: 'label',
            'data-testid': `process-data-key-${props.label}`,
            'data-required': props.required === true ? 'true' : 'false',
        }, props.label) : null,
        props.error != null ? React.createElement('span', {key: 'error'}, props.error) : null,
    ]),
}));

vi.mock('../components/no-code-input-field/no-code-input-field-component', () => ({
    NoCodeInputFieldComponent: (props: {
        label?: string | null;
        error?: string | null;
    }) => React.createElement('div', {}, [
        props.label != null ? React.createElement('span', {key: 'label'}, props.label) : null,
        props.error != null ? React.createElement('span', {key: 'error'}, props.error) : null,
    ]),
}));

vi.mock('../components/code-input-field/code-input-field-component', () => ({
    CodeInputFieldComponent: (props: {
        label?: string | null;
        error?: string | null;
    }) => React.createElement('div', {}, [
        props.label != null ? React.createElement('span', {key: 'label'}, props.label) : null,
        props.error != null ? React.createElement('span', {key: 'error'}, props.error) : null,
    ]),
}));

vi.mock('../components/rich-text-input-component/rich-text-input-component', async () => {
    const React = await import('react');

    return {
        RichTextInputComponent: (props: {
            label?: string | null;
            value?: string | null;
            onChange: (value: string | null) => void;
            disabled?: boolean | null;
            readOnly?: boolean | null;
            error?: string | null;
        }) => React.createElement('div', {}, [
            React.createElement('textarea', {
                key: 'input',
                'aria-label': props.label ?? undefined,
                value: props.value ?? '',
                disabled: Boolean(props.disabled) || Boolean(props.readOnly),
                onChange: (event: { target: { value: string } }) => props.onChange(event.target.value === '' ? null : event.target.value),
            }),
            props.error != null ? React.createElement('span', {key: 'error'}, props.error) : null,
        ]),
    };
});

describe('PaymentConfigView', () => {
    let listAllMock: MockInstance;
    let listDefinitionsMock: MockInstance;

    beforeEach(() => {
        listAllMock = vi.spyOn(PaymentProvidersApiService.prototype, 'listAll');
        listDefinitionsMock = vi.spyOn(PaymentProvidersApiService.prototype, 'listDefinitions');

        listAllMock.mockResolvedValue({
            content: [
                createPaymentProvider({
                    key: 'live-provider',
                    name: 'Stadtkasse Musterstadt',
                    providerKey: 'pmpayment',
                    providerVersion: 1,
                    isTestProvider: false,
                    isEnabled: true,
                }),
                createPaymentProvider({
                    key: 'test-provider',
                    name: 'Test-Anbindung Kasse',
                    providerKey: 'epaybl',
                    providerVersion: 1,
                    isTestProvider: true,
                    isEnabled: true,
                }),
                createPaymentProvider({
                    key: 'disabled-provider',
                    name: 'Abgeschaltete Kasse',
                    providerKey: 'pmpayment',
                    providerVersion: 1,
                    isTestProvider: false,
                    isEnabled: false,
                }),
            ],
        } as any);

        listDefinitionsMock.mockResolvedValue([
            createPaymentProviderDefinition({
                key: 'pmpayment',
                version: 1,
                name: 'pmPayment',
            }),
            createPaymentProviderDefinition({
                key: 'epaybl',
                version: 1,
                name: 'ePayBL',
            }),
        ]);
    });

    it('should list only enabled providers with type and system labels', async () => {
        renderPaymentConfigView();

        await waitFor(() => {
            expect(listAllMock).toHaveBeenCalledWith({isEnabled: true});
        });
        expect(listDefinitionsMock).toHaveBeenCalledWith();

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        fireEvent.mouseDown(screen.getByLabelText(/Zahlungsdienstleister/));

        expect(await screen.findByText('Stadtkasse Musterstadt (pmPayment) – Live-System')).toBeInTheDocument();
        expect(await screen.findByText('Test-Anbindung Kasse (ePayBL) – Test-System')).toBeInTheDocument();
        expect(screen.queryByText('Abgeschaltete Kasse (pmPayment) – Live-System')).not.toBeInTheDocument();
    });

    it('should clear an inactive saved provider when the dialog is saved', async () => {
        const setValue = vi.fn();

        renderPaymentConfigView({
            setValue,
            value: {
                paymentProviderKey: 'disabled-provider',
                purpose: 'Gebuehr',
                description: 'Beschreibung',
                mapRequestor: false,
                requestorMapping: null,
                items: [],
                successMessage: null,
                failureMessage: null,
            },
        });

        await waitFor(() => {
            expect(listAllMock).toHaveBeenCalledWith({isEnabled: true});
        });

        fireEvent.click(screen.getByText('Zahlungskonfiguration (disabled-provider)'));
        fireEvent.click(screen.getByText('Übernehmen'));

        expect(setValue).toHaveBeenCalledWith(expect.objectContaining({
            paymentProviderKey: null,
        }));
    });

    it('should save success and failure messages', async () => {
        const setValue = vi.fn();

        renderPaymentConfigView({setValue});

        await waitFor(() => {
            expect(listAllMock).toHaveBeenCalledWith({isEnabled: true});
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        fireEvent.change(screen.getByLabelText('Erfolgsmeldung nach Zahlung'), {
            target: {value: '# Danke\n**{{ $.name }}**'},
        });
        fireEvent.change(screen.getByLabelText('Fehlermeldung bei Zahlungsfehler'), {
            target: {value: '# Nicht bezahlt\nDie Zahlung fuer **{{ $.name }}** wurde nicht abgeschlossen.'},
        });
        fireEvent.click(screen.getByText('Übernehmen'));

        expect(setValue).toHaveBeenCalledWith(expect.objectContaining({
            successMessage: '# Danke\n**{{ $.name }}**',
            failureMessage: '# Nicht bezahlt\nDie Zahlung fuer **{{ $.name }}** wurde nicht abgeschlossen.',
        }));
    });

    it('should render top-level error details on payment config fields', async () => {
        renderPaymentConfigView({
            errorDetails: {
                paymentProviderKey: 'Bitte waehlen Sie einen Anbieter.',
                purpose: 'Bitte geben Sie einen Buchungstext an.',
                description: 'Bitte geben Sie eine Beschreibung an.',
                successMessage: 'Bitte korrigieren Sie die Erfolgsmeldung.',
                failureMessage: 'Bitte korrigieren Sie die Fehlermeldung.',
            },
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        await screen.findByLabelText(/Zahlungsdienstleister/);

        expect(await screen.findByText('Bitte waehlen Sie einen Anbieter.')).toBeInTheDocument();
        expect(screen.getByText('Bitte geben Sie einen Buchungstext an.')).toBeInTheDocument();
        expect(screen.getByText('Bitte geben Sie eine Beschreibung an.')).toBeInTheDocument();
        expect(screen.getByText('Bitte korrigieren Sie die Erfolgsmeldung.')).toBeInTheDocument();
        expect(screen.getByText('Bitte korrigieren Sie die Fehlermeldung.')).toBeInTheDocument();
    });

    it('should render requestor mapping error details on mapping fields', async () => {
        renderPaymentConfigView({
            value: {
                paymentProviderKey: null,
                purpose: null,
                description: null,
                mapRequestor: true,
                requestorMapping: {
                    requestorSourceType: 'processDataKey',
                    lastNameDestinationKey: null,
                    firstNameDestinationKey: null,
                    genderDestinationKey: null,
                    isOrganizationDestinationKey: null,
                    organizationNameDestinationKey: null,
                    streetDestinationKey: null,
                    houseNumberDestinationKey: null,
                    addressLineDestinationKey: null,
                    postalCodeDestinationKey: null,
                    cityDestinationKey: null,
                    countryDestinationKey: null,
                },
                items: [],
                successMessage: null,
                failureMessage: null,
            },
            errorDetails: {
                requestorMapping: {
                    requestorSourceType: 'Bitte waehlen Sie die Art der Zuordnung.',
                    isOrganizationDestinationKey: 'Bitte waehlen Sie das Organisationskennzeichen.',
                    lastNameDestinationKey: 'Bitte waehlen Sie den Nachnamen.',
                    cityDestinationKey: 'Bitte waehlen Sie den Ort.',
                },
            },
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        await screen.findByText('Personen- oder Organisationsangaben zuordnen');

        expect(await screen.findByText(/Bitte waehlen Sie die Art der Zuordnung\./)).toBeInTheDocument();
        expect(screen.getByText(/Bitte waehlen Sie das Organisationskennzeichen\./)).toBeInTheDocument();
        expect(screen.getByText(/Bitte waehlen Sie den Nachnamen\./)).toBeInTheDocument();
        expect(screen.getByText(/Bitte waehlen Sie den Ort\./)).toBeInTheDocument();
    });

    it('should mark only dynamic organization discriminator as required in requestor mapping', async () => {
        renderPaymentConfigView({
            value: {
                paymentProviderKey: null,
                purpose: null,
                description: null,
                mapRequestor: true,
                requestorMapping: {
                    requestorSourceType: 'processDataKey',
                    lastNameDestinationKey: null,
                    firstNameDestinationKey: null,
                    genderDestinationKey: null,
                    isOrganizationDestinationKey: null,
                    organizationNameDestinationKey: null,
                    streetDestinationKey: null,
                    houseNumberDestinationKey: null,
                    addressLineDestinationKey: null,
                    postalCodeDestinationKey: null,
                    cityDestinationKey: null,
                    countryDestinationKey: null,
                },
                items: [],
                successMessage: null,
                failureMessage: null,
            },
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        await screen.findByText('Personen- oder Organisationsangaben zuordnen');

        expect(screen.getByTestId('process-data-key-Datenfeld zur Kennzeichnung als Organisation')).toHaveAttribute('data-required', 'true');
        expect(screen.getByTestId('process-data-key-Nachname')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Vorname')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Geschlecht')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Organisationsname')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Straße')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Hausnummer')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Adresszusatz')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Postleitzahl')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Ort')).toHaveAttribute('data-required', 'false');
        expect(screen.getByTestId('process-data-key-Land')).toHaveAttribute('data-required', 'false');
    });

    it('should render the generic missing payment items error', async () => {
        renderPaymentConfigView({
            errorDetails: {
                items: 'Bitte legen Sie mindestens eine Zahlungsposition an.',
            },
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));

        await waitFor(() => {
            expect(screen.getByText('Bitte legen Sie mindestens eine Zahlungsposition an.')).toBeInTheDocument();
        });
        expect(screen.getByText('Keine Zahlungspositionen vorhanden.')).toBeInTheDocument();
    });

    it('should mark payment item rows with child errors', async () => {
        renderPaymentConfigView({
            value: {
                paymentProviderKey: null,
                purpose: null,
                description: null,
                mapRequestor: false,
                requestorMapping: null,
                items: [
                    createPaymentItem({
                        description: 'Erste Position',
                        reference: 'first',
                    }),
                    createPaymentItem({
                        description: 'Zweite Position',
                        reference: 'second',
                    }),
                ],
                successMessage: null,
                failureMessage: null,
            },
            errorDetails: {
                items: [
                    null,
                    {
                        description: 'Bitte korrigieren Sie die zweite Beschreibung.',
                    },
                ],
            },
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        await screen.findByText('Zahlungspositionen *');

        const firstRow = screen.getByText('Erste Position').closest('[role="button"]') as HTMLElement | null;
        const secondRow = screen.getByText('Zweite Position').closest('[role="button"]') as HTMLElement | null;

        expect(firstRow).not.toBeNull();
        expect(secondRow).not.toBeNull();
        expect(within(firstRow!).queryByLabelText('Fehler in diesem Eintrag')).not.toBeInTheDocument();
        expect(within(secondRow!).getByLabelText('Fehler in diesem Eintrag')).toBeInTheDocument();
    });

    it('should render item error details on the matching item fields', async () => {
        renderPaymentConfigView({
            value: {
                paymentProviderKey: null,
                purpose: null,
                description: null,
                mapRequestor: false,
                requestorMapping: null,
                items: [
                    createPaymentItem({
                        description: 'Erste Position',
                        reference: 'first',
                    }),
                    createPaymentItem({
                        description: 'Zweite Position',
                        reference: 'second',
                        costType: PaymentConfigElementValueItemCostType.VariableCosts,
                        variableCostsCalculationType: PaymentConfigElementValueItemVariableValueCalculationType.NoCode,
                    }),
                ],
                successMessage: null,
                failureMessage: null,
            },
            errorDetails: {
                items: [
                    null,
                    {
                        description: 'Bitte korrigieren Sie die zweite Beschreibung.',
                        variableCostsNoCodeCalculation: 'Bitte korrigieren Sie die zweite Betragsberechnung.',
                        fixedTaxRate: 'Bitte korrigieren Sie den zweiten Steuersatz.',
                    },
                ],
            },
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        await screen.findByText('Zahlungspositionen *');
        fireEvent.click(await screen.findByText('Zweite Position'));

        expect(await screen.findByText('Bitte korrigieren Sie die zweite Beschreibung.')).toBeInTheDocument();
        expect(screen.getByText('Bitte korrigieren Sie die zweite Betragsberechnung.')).toBeInTheDocument();
        expect(screen.getByText('Bitte korrigieren Sie den zweiten Steuersatz.')).toBeInTheDocument();
        expect(screen.queryByText('Bitte korrigieren Sie die erste Beschreibung.')).not.toBeInTheDocument();
    });
});

function renderPaymentConfigView(options?: {
    value?: PaymentConfigElementValue | null;
    setValue?: Mock;
    errorDetails?: Record<string, any> | null;
}) {
    const rootElement: FormLayoutElement = {
        id: 'root',
        type: ElementType.FormLayout,
        name: null,
        testProtocolSet: null,
        visibility: null,
        override: null,
        metadata: null,
        tabTitle: null,
        children: [],
        offlineSubmissionText: null,
        offlineSignatureNeeded: null,
        publicTitle: null,
        showOnFormIndexPage: null,
        managingDepartmentId: null,
        responsibleDepartmentId: null,
        themeId: null,
        pdfTemplateKey: null,
    };

    return render(
        <ConfirmProvider>
            <ViewDispatcherContextProvider
                value={{
                    mode: ViewDispatcherMode.Editor,
                    rootElement,
                    allElements: [rootElement],
                    rootAuthoredElementValues: {},
                    rootDerivedData: createDerivedRuntimeElementData(),
                }}
            >
                <PaymentConfigView
                    {...createBaseProps(options)}
                />
            </ViewDispatcherContextProvider>
        </ConfirmProvider>,
    );
}

function createBaseProps(options?: {
    value?: PaymentConfigElementValue | null;
    setValue?: Mock;
    errorDetails?: Record<string, any> | null;
}): BaseViewProps<PaymentConfigElement, PaymentConfigElementValue> {
    return {
        element: {
            id: 'payment',
            type: ElementType.PaymentConfigElement,
            weight: 12,
            label: 'Zahlungskonfiguration',
            hint: undefined,
            required: undefined,
            disabled: undefined,
            technical: undefined,
            destinationKey: undefined,
            validation: undefined,
            value: undefined,
            metadata: undefined,
            name: undefined,
            override: undefined,
            testProtocolSet: undefined,
            visibility: undefined,
        },
        isBusy: false,
        isDeriving: false,
        value: options?.value ?? {
            paymentProviderKey: null,
            purpose: null,
            description: null,
            mapRequestor: false,
            requestorMapping: null,
            items: [],
            successMessage: null,
            failureMessage: null,
        },
        setValue: options?.setValue ?? vi.fn(),
        onBlur: vi.fn(),
        errors: undefined,
        errorDetails: options?.errorDetails,
        authoredElementValues: {},
        onAuthoredElementValuesChange: vi.fn(),
        onElementBlur: vi.fn(),
        derivedData: createDerivedRuntimeElementData(),
        onDerive: vi.fn().mockResolvedValue(createDerivedRuntimeElementData()),
        onEvent: vi.fn().mockResolvedValue(undefined),
        onResetErrors: vi.fn(),
        suppressErrors: false,
        derivationTriggerIdQueue: [],
    };
}

function createPaymentItem(item?: Partial<PaymentConfigElementValueItem>): PaymentConfigElementValueItem {
    return {
        idType: PaymentConfigElementValueItemIdType.AutoGeneratedUUID,
        predefinedId: null,
        description: null,
        reference: null,
        costType: PaymentConfigElementValueItemCostType.FixedCosts,
        fixedCosts: 0,
        variableCostsCalculationType: PaymentConfigElementValueItemVariableValueCalculationType.NoCode,
        variableCostsNoCodeCalculation: null,
        variableCostsLowCodeCalculation: null,
        quantityType: PaymentConfigElementValueItemQuantityType.FixedQuantity,
        fixedQuantity: 1,
        variableQuantityCalculationType: PaymentConfigElementValueItemVariableValueCalculationType.NoCode,
        variableQuantityNoCodeCalculation: null,
        variableQuantityLowCodeCalculation: null,
        fixedTaxRate: 0,
        additionalBookingData: null,
        ...item,
    };
}

function createPaymentProvider(provider: Partial<PaymentProviderResponseDTO>): PaymentProviderResponseDTO {
    return {
        key: '',
        name: '',
        description: '',
        providerKey: '',
        providerVersion: 1,
        isTestProvider: false,
        isEnabled: true,
        config: {},
        ...provider,
    };
}

function createPaymentProviderDefinition(definition: Partial<PaymentProviderDefinitionResponseDTO>): PaymentProviderDefinitionResponseDTO {
    return {
        key: '',
        version: 1,
        name: '',
        description: '',
        configLayout: null,
        ...definition,
    };
}
