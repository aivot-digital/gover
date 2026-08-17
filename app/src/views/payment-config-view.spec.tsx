import {describe, expect, it, vi, beforeEach, type Mock, type MockInstance} from 'vitest';
import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {PaymentConfigView} from './payment-config-view';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../components/view-dispatcher/view-dispatcher.context';
import {ConfirmProvider} from '../providers/confirm-provider';
import {PaymentProvidersApiService} from '../modules/payment/payment-providers-api-service';
import type {BaseViewProps} from './base-view';
import type {
    PaymentConfigElement,
    PaymentConfigElementValue,
} from '../models/elements/form/input/payment-config-element';
import type {FormLayoutElement} from '../models/elements/form-layout-element';
import type {PaymentProviderResponseDTO} from '../modules/payment/dtos/payment-provider-response-dto';
import type {PaymentProviderDefinitionResponseDTO} from '../modules/payment/dtos/payment-provider-definition-response-dto';

vi.mock('./process-data-key-input-field-view', () => ({
    ProcessDataKeyInputComponent: () => null,
}));

vi.mock('../components/no-code-input-field/no-code-input-field-component', () => ({
    NoCodeInputFieldComponent: () => null,
}));

vi.mock('../components/code-input-field/code-input-field-component', () => ({
    CodeInputFieldComponent: () => null,
}));

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
});

function renderPaymentConfigView(options?: {
    value?: PaymentConfigElementValue | null;
    setValue?: Mock;
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
        legalSupportDepartmentId: null,
        technicalSupportDepartmentId: null,
        imprintDepartmentId: null,
        privacyDepartmentId: null,
        accessibilityDepartmentId: null,
        formSpecificPrivacyStatement: null,
        formSpecificAccessibilityStatement: null,
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
        },
        setValue: options?.setValue ?? vi.fn(),
        onBlur: vi.fn(),
        errors: undefined,
        errorDetails: undefined,
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
