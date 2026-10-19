import {describe, expect, it, vi, beforeEach, type Mock, type MockInstance} from 'vitest';
import React from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
import {InputVariableSource} from '../models/input-mode';

// The variable picker's copy action uses the app dispatch; these view tests render without a Redux store.
vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

vi.mock('./process-data-key-input-field-view', () => ({
    ProcessDataKeyInputFieldView: () => null,
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
        RichTextInputComponent: React.forwardRef((props: {
            label?: string | null;
            value?: string | null;
            onChange: (value: string | null) => void;
            disabled?: boolean | null;
            readOnly?: boolean | null;
            busy?: boolean | null;
            error?: string | null;
            endAction?: {
                ariaLabel?: string;
                tooltip?: string;
                onClick: () => void;
            };
        }, ref) => {
            React.useImperativeHandle(ref, () => ({
                focus: () => undefined,
                insertMarkdown: (markdown: string) => props.onChange(`${props.value ?? ''}${markdown}`),
                insertVariableReference: (reference: string) => props.onChange(`${props.value ?? ''}{{ ${reference} }}`),
            }), [props.onChange, props.value]);

            const disabled = Boolean(props.disabled) || Boolean(props.readOnly) || Boolean(props.busy);
            return React.createElement('div', {}, [
                React.createElement('textarea', {
                    key: 'input',
                    'aria-label': props.label ?? undefined,
                    value: props.value ?? '',
                    disabled,
                    onChange: (event: { target: { value: string } }) => props.onChange(event.target.value === '' ? null : event.target.value),
                }),
                props.endAction != null ? React.createElement('button', {
                    key: 'action',
                    type: 'button',
                    'aria-label': props.endAction.ariaLabel ?? props.endAction.tooltip,
                    disabled,
                    onClick: props.endAction.onClick,
                }, props.endAction.tooltip) : null,
                props.error != null ? React.createElement('span', {key: 'error'}, props.error) : null,
            ]);
        }),
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
        fireEvent.mouseDown(screen.getByLabelText(/Zahlungsanbieter/));

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

    it('should keep payment configuration and item details viewable when the form is read-only', async () => {
        renderPaymentConfigView({
            isBusy: true,
            value: {
                paymentProviderKey: 'live-provider',
                purpose: 'Gebühr',
                description: 'Beschreibung',
                mapRequestor: false,
                requestorMapping: null,
                items: [createPaymentItem()],
                successMessage: null,
                failureMessage: null,
            },
        });

        const viewPaymentConfig = screen.getByRole('button', {name: 'Ansehen'});
        await waitFor(() => expect(viewPaymentConfig).toBeEnabled());
        fireEvent.click(viewPaymentConfig);

        expect(screen.getByText('Zahlungskonfiguration ansehen')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Übernehmen'})).not.toBeInTheDocument();

        const viewPaymentItem = screen.getByRole('button', {name: 'Ansehen'});
        expect(viewPaymentItem).toBeEnabled();
        fireEvent.click(viewPaymentItem);

        expect(screen.getByText('Zahlungspositionen ansehen')).toBeInTheDocument();
    });

    it('should save success and failure messages', async () => {
        const setValue = vi.fn();

        renderPaymentConfigView({setValue});

        await waitFor(() => {
            expect(listAllMock).toHaveBeenCalledWith({isEnabled: true});
        });

        fireEvent.click(screen.getByText('Neue Zahlungskonfiguration'));
        fireEvent.change(screen.getByLabelText('Erfolgsmeldung'), {
            target: {value: '# Danke\n**{{ $.name }}**'},
        });
        fireEvent.change(screen.getByLabelText('Fehlermeldung'), {
            target: {value: '# Nicht bezahlt\nDie Zahlung fuer **{{ $.name }}** wurde nicht abgeschlossen.'},
        });
        fireEvent.click(screen.getByText('Übernehmen'));

        expect(setValue).toHaveBeenCalledWith(expect.objectContaining({
            successMessage: '# Danke\n**{{ $.name }}**',
            failureMessage: '# Nicht bezahlt\nDie Zahlung fuer **{{ $.name }}** wurde nicht abgeschlossen.',
        }));
    });

    it('should insert variable references into success and failure messages', async () => {
        const setValue = vi.fn();
        const user = userEvent.setup();

        renderPaymentConfigView({setValue});

        await waitFor(() => {
            expect(listAllMock).toHaveBeenCalledWith({isEnabled: true});
        });
        await user.click(screen.getByText('Neue Zahlungskonfiguration'));

        await user.click(screen.getAllByRole('button', {name: 'Variable referenzieren'})[0]);
        await user.click(await screen.findByText('Name der Person'));
        await user.click(screen.getByTestId('use-variable-reference'));
        await waitFor(() => {
            expect(screen.queryByRole('heading', {name: 'Variable referenzieren'})).not.toBeInTheDocument();
            expect(screen.getByLabelText('Erfolgsmeldung')).toHaveValue('{{ $.person.name }}');
        });

        await user.click(screen.getAllByRole('button', {name: 'Variable referenzieren'})[1]);
        await user.click(await screen.findByText('Name der Person'));
        await user.click(screen.getByTestId('use-variable-reference'));
        await waitFor(() => {
            expect(screen.queryByRole('heading', {name: 'Variable referenzieren'})).not.toBeInTheDocument();
            expect(screen.getByLabelText('Fehlermeldung')).toHaveValue('{{ $.person.name }}');
        });

        await user.click(screen.getByText('Übernehmen'));

        expect(setValue).toHaveBeenCalledWith(expect.objectContaining({
            successMessage: '{{ $.person.name }}',
            failureMessage: '{{ $.person.name }}',
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
        await screen.findByLabelText(/Zahlungsanbieter/);

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

    it('should preserve four decimal places when entering and saving a tax rate', async () => {
        const setValue = vi.fn();
        const user = userEvent.setup();

        renderPaymentConfigView({
            setValue,
            value: {
                paymentProviderKey: null,
                purpose: null,
                description: null,
                mapRequestor: false,
                requestorMapping: null,
                items: [createPaymentItem({description: 'Position', reference: 'fee'})],
                successMessage: null,
                failureMessage: null,
            },
        });

        await user.click(await screen.findByText('Neue Zahlungskonfiguration'));
        await user.click(await screen.findByText('Position'));

        const taxRate = screen.getByRole('textbox', {name: /Steuersatz/});
        await user.clear(taxRate);
        await user.type(taxRate, '7,1234');
        await user.tab();
        expect(taxRate).toHaveValue('7,1234');

        const itemDialog = screen.getByRole('dialog', {name: 'Zahlungspositionen bearbeiten'});
        await user.click(within(itemDialog).getByRole('button', {name: 'Übernehmen'}));
        // The fields are cleared before the dialog's exit animation ends. Wait for the entire inner dialog
        // to leave, otherwise the second click can hit its now inactive save button instead of the outer one.
        await waitFor(() => {
            expect(itemDialog).not.toBeInTheDocument();
        });
        await user.click(within(screen.getByRole('dialog', {name: 'Zahlungskonfiguration bearbeiten'}))
            .getByRole('button', {name: 'Übernehmen'}));

        expect(setValue).toHaveBeenCalledWith(expect.objectContaining({
            items: [expect.objectContaining({fixedTaxRate: 7.1234})],
        }));
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
        await screen.findByText('Zahlungspositionen');

        const firstRow = screen.getByText('Erste Position').closest('button');
        const secondRow = screen.getByText('Zweite Position').closest('button');

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
        await screen.findByText('Zahlungspositionen');
        fireEvent.click(await screen.findByText('Zweite Position'));

        expect(await screen.findByText('Bitte korrigieren Sie die zweite Beschreibung.')).toBeInTheDocument();
        expect(screen.getByText('Bitte korrigieren Sie die zweite Betragsberechnung.')).toBeInTheDocument();
        expect(screen.getByText('Bitte korrigieren Sie den zweiten Steuersatz.')).toBeInTheDocument();
        expect(screen.queryByText('Bitte korrigieren Sie die erste Beschreibung.')).not.toBeInTheDocument();
    });
    it('reports an empty key and duplicate keys on their respective rows', async () => {
        const config = validPaymentConfig();
        config.items![0].additionalBookingData = {one: '1', two: '2', three: '3'};
        renderPaymentConfigView({value: config});
        fireEvent.click(await screen.findByText('Stadtkasse Musterstadt (pmPayment) – Live-System'));
        fireEvent.click(screen.getByText('Position'));
        const dialog = within(screen.getAllByRole('dialog').at(-1)!);
        const keys = dialog.getAllByRole('textbox', {name: 'Schlüssel'});
        fireEvent.change(keys[0], {target: {value: ''}});
        fireEvent.change(keys[1], {target: {value: 'three '}});
        expect(keys[0]).toHaveAccessibleDescription('Geben Sie für diesen Buchungseintrag einen Schlüssel an.');
        expect(keys[1]).toHaveAccessibleDescription('Die Schlüssel der Buchungsdaten müssen eindeutig sein.');
        expect(keys[2]).toHaveAccessibleDescription('Die Schlüssel der Buchungsdaten müssen eindeutig sein.');
    });

    it('allows clearing the purpose in an editable payment draft', async () => {
        const setValue = vi.fn();
        renderPaymentConfigView({value: validPaymentConfig(), setValue});
        fireEvent.click(await screen.findByText('Stadtkasse Musterstadt (pmPayment) – Live-System'));
        fireEvent.change(screen.getByRole('textbox', {name: 'Buchungstext'}), {target: {value: ''}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(setValue).toHaveBeenCalledWith(expect.objectContaining({purpose: null}));
    });

    it('keeps booking values when deleting, adding, and correcting duplicate keys', async () => {
        const config = validPaymentConfig();
        config.items![0].additionalBookingData = {schluessel1: 'erster Wert', schluessel2: 'zweiter Wert'};
        renderPaymentConfigView({value: config});
        fireEvent.click(await screen.findByText('Stadtkasse Musterstadt (pmPayment) – Live-System'));
        fireEvent.click(screen.getByText('Position'));
        const dialog = within(screen.getAllByRole('dialog').at(-1)!);
        fireEvent.click(dialog.getByRole('button', {name: 'Buchungseintrag 1 entfernen'}));
        fireEvent.click(dialog.getByRole('button', {name: 'Hinzufügen'}));
        expect(dialog.getAllByRole('textbox', {name: 'Schlüssel'})).toHaveLength(2);
        expect(dialog.getAllByRole('textbox', {name: /^Wert/})[0]).toHaveValue('zweiter Wert');
        const keys = dialog.getAllByRole('textbox', {name: 'Schlüssel'});
        fireEvent.change(keys[1], {target: {value: ''}});
        fireEvent.click(dialog.getByRole('button', {name: 'Übernehmen'}));
        expect(keys[1]).toHaveValue('');
        expect(dialog.getAllByText('Geben Sie für diesen Buchungseintrag einen Schlüssel an.').length).toBeGreaterThan(0);
        fireEvent.change(keys[1], {target: {value: 'schluessel2 '}});
        fireEvent.change(dialog.getAllByRole('textbox', {name: /^Wert/})[1], {target: {value: 'dritter Wert'}});
        fireEvent.click(dialog.getByRole('button', {name: 'Übernehmen'}));
        expect(dialog.getAllByRole('textbox', {name: /^Wert/})[0]).toHaveValue('zweiter Wert');
        expect(dialog.getAllByRole('textbox', {name: /^Wert/})[1]).toHaveValue('dritter Wert');
        expect(dialog.getAllByText('Die Schlüssel der Buchungsdaten müssen eindeutig sein.').length).toBeGreaterThan(0);
        fireEvent.change(keys[1], {target: {value: 'neuerSchluessel'}});
        fireEvent.click(dialog.getByRole('button', {name: 'Übernehmen'}));
        await waitFor(() => expect(screen.queryByText('Zahlungspositionen bearbeiten')).not.toBeInTheDocument());
        fireEvent.click(screen.getByText('Position'));
        const reopened = within(screen.getAllByRole('dialog').at(-1)!);
        expect(reopened.getAllByRole('textbox', {name: /^Wert/})[0]).toHaveValue('zweiter Wert');
        expect(reopened.getAllByRole('textbox', {name: /^Wert/})[1]).toHaveValue('dritter Wert');
    });

});

function renderPaymentConfigView(options?: {
    value?: PaymentConfigElementValue | null;
    setValue?: Mock;
    errorDetails?: Record<string, any> | null;
    isBusy?: boolean;
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
                    inputModeVariables: [{
                        source: InputVariableSource.ProcessData,
                        path: 'person.name',
                        label: 'Name der Person',
                    }],
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
    isBusy?: boolean;
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
        isBusy: options?.isBusy ?? false,
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
        documentationUrl: null,
        ...definition,
    };
}

function validPaymentConfig(): PaymentConfigElementValue {
    return {
        paymentProviderKey: 'live-provider', purpose: 'Verwendungszweck', description: 'Beschreibung',
        mapRequestor: false, requestorMapping: null,
        items: [createPaymentItem({description: 'Position', reference: 'REF'})], successMessage: null, failureMessage: null,
    };
}
