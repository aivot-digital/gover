import React, {useEffect, useState} from 'react';
import {Box, Button, Grid, IconButton, Typography} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {Form as Application} from '../../models/entities/form';
import {TextFieldComponent} from '../text-field/text-field-component';
import {PaymentProduct, PaymentType} from '../../models/payment/payment-product';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {v4 as uuid4} from 'uuid';
import {RadioFieldComponent} from '../radio-field/radio-field-component';
import {AlertComponent} from '../alert/alert-component';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import ExpandLessOutlinedIcon from '@mui/icons-material/ExpandLessOutlined';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {CodeTabCodeEditor} from '../code-tab-code-editor';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {useApi} from '../../hooks/use-api';
import {OptionListInput} from '../option-list-input/option-list-input';
import {PaymentProvidersApiService} from '../../modules/payment/payment-providers-api-service';
import {Page} from '../../models/dtos/page';
import {PaymentProviderResponseDTO} from '../../modules/payment/dtos/payment-provider-response-dto';
import {Link} from 'react-router-dom';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';

interface PaymentPositionItemProps {
    index: number;
    product: PaymentProduct;
    onDelete: () => void;
    onPatch: (patch: Partial<PaymentProduct>) => void;
    disabled?: boolean;
}

function PaymentPositionItem(props: PaymentPositionItemProps) {
    const [expanded, setExpanded] = useState(true);

    return (
        <Box
            sx={{
                border: '1px solid rgba(0, 0, 0, 0.23)',
                borderRadius: 1,
                px: 4,
                py: 2,
                mb: 3,
            }}
        >
            <Box
                display="flex"
                alignItems="center"
            >
                <IconButton
                    onClick={() => {
                        setExpanded(!expanded);
                    }}
                >
                    {
                        expanded ? <ExpandMoreOutlinedIcon /> : <ExpandLessOutlinedIcon />
                    }
                </IconButton>

                <Typography
                    variant="h5"
                    sx={{ml: 2}}
                >
                   Position Nr. {props.index !== undefined && (props.index + 1)}: {isStringNotNullOrEmpty(props.product.reference) ? props.product.reference : props.product.id}
                </Typography>

                {
                    !props.disabled &&
                    <Button
                        sx={{ml: 'auto'}}
                        size="small"
                        color="error"
                        endIcon={<DeleteForeverOutlinedIcon />}
                        onClick={props.onDelete}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {
                expanded &&
                <>
                    <Grid
                        container
                        columnSpacing={4}
                        sx={{mt: 2}}
                    >
                        <Grid
                            item
                            xs={6}
                        >
                            <TextFieldComponent
                                label="Referenz"
                                value={props.product.reference}
                                onChange={val => {
                                    props.onPatch({
                                        reference: val ?? '',
                                    });
                                }}
                                maxCharacters={36}
                                required
                                disabled={props.disabled}
                                hint="Eine fachliche Referenz für die Zahlungsposition. Kann nur von Ihnen intern eingesehen werden."
                                pattern={{
                                    regex: '^[\\w\\d-]+$',
                                    message: 'Die Referenz darf nur aus Buchstaben (keine Umlaute), Zahlen und Bindestrichen (-) bestehen.',
                                }}
                            />
                        </Grid>

                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="ID"
                                value={props.product.id}
                                onChange={val => {
                                    props.onPatch({
                                        id: val ?? '',
                                    });
                                }}
                                maxCharacters={36}
                                minCharacters={36}
                                required
                                disabled
                                hint="Die automatisch erzeugte ID der Position zur technischen Identifikation."
                                pattern={{
                                    regex: '^[\\w\\d-]+$',
                                    message: 'Die Id darf nur aus Buchstaben (keine Umlaute), Zahlen und Bindestrichen (-) bestehen.',
                                }}
                            />
                        </Grid>

                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="Beschreibung"
                                value={props.product.description}
                                onChange={val => {
                                    props.onPatch({
                                        description: val ?? '',
                                    });
                                }}
                                multiline
                                disabled={props.disabled}
                                required
                                minCharacters={1}
                                maxCharacters={250}
                                hint="Name und/oder Beschreibung der Zahlungsposition. Wird der antragstellenden Person angezeigt."
                                pattern={{
                                    regex: '^[\\w\\d\\s-,\\.\\u00C0-\\u017F]+$',
                                    message: 'Die Beschreibung darf nur aus Buchstaben, Zahlen, Kommata, Punkten und Bindestrichen (-) bestehen.',
                                }}
                            />
                        </Grid>

                        <Grid
                            item
                            xs={12}
                            lg={6}
                        />

                        <Grid
                            item
                            xs={12}
                            lg={3}
                        >
                            <NumberFieldComponent
                                label="Einzelpreis (Netto)"
                                value={props.product.netPrice}
                                onChange={val => {
                                    props.onPatch({
                                        netPrice: val ?? 19,
                                    });
                                }}
                                required
                                disabled={props.disabled}
                                suffix="Euro"
                                decimalPlaces={2}
                                minValue={0}
                                maxValue={999999}
                                hint='Der Nettopreis des einzelnen "Artikels".'
                            />
                        </Grid>

                        <Grid
                            item
                            xs={12}
                            lg={3}
                        >
                            <NumberFieldComponent
                                label="Steuersatz"
                                value={props.product.taxRate}
                                onChange={val => {
                                    props.onPatch({
                                        taxRate: val ?? 19,
                                    });
                                }}
                                required
                                disabled={props.disabled}
                                suffix="Prozent"
                                decimalPlaces={2}
                                minValue={0}
                                maxValue={100}
                                hint="Der auf diese Zahlungsposition anzuwendende Steuersatz in Prozent."
                            />
                        </Grid>

                        <Grid
                            item
                            xs={12}
                            lg={3}
                        >
                            <NumberFieldComponent
                                label="Einzelpreis (Brutto)"
                                value={props.product.netPrice * (1 + props.product.taxRate / 100)}
                                onChange={() => {
                                }}
                                suffix="Euro"
                                decimalPlaces={2}
                                minValue={0}
                                maxValue={999999}
                                hint='Der Bruttopreis des einzelnen "Artikels".'
                                disabled={true}
                            />
                        </Grid>

                    {
                        props.product.taxRate === 0 &&
                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="Begründung des Steuersatzes"
                                value={props.product.taxInformation}
                                onChange={val => {
                                    props.onPatch({
                                        taxInformation: val ?? '',
                                    });
                                }}
                                multiline
                                disabled={props.disabled}
                                maxCharacters={250}
                                hint="Bitte begründen Sie den verwendeten Steuersatz. Beispiele: Umsatzsteuerbefreit, Kleinunternehmerregelung, Nicht steuerbar etc."
                                pattern={{
                                    regex: '^[\\w\\d\\s-,\\.\\u00C0-\\u017F]+$',
                                    message: 'Die Beschreibung darf nur aus Buchstaben, Zahlen, Kommata, Punkten und Bindestrichen (-) bestehen.',
                                }}
                            />
                        </Grid>
                    }
                    </Grid>

                    <Box>
                        <OptionListInput
                            label="Booking Data"
                            hint="In den Booking Data können technisch relevante Informationen für die weitergehende Verbuchung in Folgesystemen mitgegeben werden."
                            addLabel="Key/Value-Paar hinzufügen"
                            noItemsHint="Keine Booking Data hinterlegt"
                            value={(props.product.bookingData ?? [])
                                .map(item => ({
                                    value: item.value ?? '',
                                    label: item.key ?? '',
                                }))
                            }
                            onChange={val => {
                                props.onPatch({
                                    bookingData: (val ?? [])
                                        .map(item => ({
                                            key: item.label,
                                            value: item.value,
                                        })),
                                });
                            }}
                            disabled={props.disabled}
                            allowEmpty={true}
                            labelLabel="Schlüssel"
                            keyLabel="Wert"
                            variant="outlined"
                        />
                    </Box>

                    <RadioFieldComponent
                        label="Mengenangabe"
                        value={props.product.type}
                        onChange={type => {
                            props.onPatch({
                                type: (type ?? PaymentType.UPFRONT_FIXED) as PaymentType,
                                upfrontQuantityFunction: type === PaymentType.UPFRONT_CALCULATED ? {requirements: '', code: 'function main(data, element, id) {\n    console.log(data, element, id);\n    return 1;\n}'} : undefined,
                                upfrontFixedQuantity: type === PaymentType.UPFRONT_FIXED ? 1 : undefined,
                            });
                        }}
                        required
                        disabled={props.disabled}
                        options={[
                            {
                                label: 'Angabe einer festen Menge',
                                value: PaymentType.UPFRONT_FIXED,
                            },
                            {
                                label: 'Berechnung der Menge',
                                value: PaymentType.UPFRONT_CALCULATED,
                            },
                            /*{
                                label: 'Nachgelagert',
                                value: PaymentType.DOWNSTREAM,
                            },*/
                        ]}
                    />

                    {
                        props.product.type === PaymentType.UPFRONT_FIXED &&
                        <Grid
                            container
                            columnSpacing={4}
                            sx={{mt: 1}}
                        >
                            <Grid
                                item
                                xs={12}
                                lg={3}
                            >
                                <NumberFieldComponent
                                    label="Menge"
                                    value={props.product.upfrontFixedQuantity}
                                    onChange={val => {
                                        props.onPatch({
                                            upfrontFixedQuantity: val,
                                        });
                                    }}
                                    required
                                    disabled={props.disabled}
                                    suffix="Stück"
                                    decimalPlaces={0}
                                    minValue={1}
                                    maxValue={999999}
                                    hint="Feste Menge, die mit dem Einzelpreis multipliziert wird und die Endsumme bestimmt."
                                />
                            </Grid>

                            <Grid
                                item
                                xs={12}
                                lg={3}
                            >
                                <NumberFieldComponent
                                    label="Gesamtpreis (Netto)"
                                    value={props.product.netPrice * (props.product.upfrontFixedQuantity ?? 0)}
                                    onChange={() => {
                                    }}
                                    suffix="Euro"
                                    decimalPlaces={2}
                                    minValue={0}
                                    maxValue={999999}
                                    hint="Netto-Endbetrag der Zahlungsposition."
                                    disabled={true}
                                />
                            </Grid>

                            <Grid
                                item
                                xs={12}
                                lg={3}
                            >
                                <NumberFieldComponent
                                    label="Enthaltener Steuerbetrag"
                                    value={props.product.netPrice * (props.product.taxRate / 100) * (props.product.upfrontFixedQuantity ?? 0)}
                                    onChange={() => {
                                    }}
                                    suffix="Euro"
                                    decimalPlaces={2}
                                    minValue={0}
                                    maxValue={999999}
                                    hint="Gesamtsteuerbetrag innerhalb der Zahlungsposition."
                                    disabled={true}
                                />
                            </Grid>
                            <Grid
                                item
                                xs={12}
                                lg={3}
                            >
                                <NumberFieldComponent
                                    label="Gesamtpreis (Brutto)"
                                    value={props.product.netPrice * (1 + props.product.taxRate / 100) * (props.product.upfrontFixedQuantity ?? 0)}
                                    onChange={() => {
                                    }}
                                    suffix="Euro"
                                    decimalPlaces={2}
                                    minValue={0}
                                    maxValue={999999}
                                    hint="Brutto-Endbetrag der Zahlungsposition."
                                    disabled={true}
                                />
                            </Grid>
                        </Grid>
                    }

                    {
                        props.product.type === PaymentType.UPFRONT_CALCULATED &&
                        <CodeTabCodeEditor
                            editable={!props.disabled}
                            func={props.product.upfrontQuantityFunction ?? {requirements: ''}}
                            onChange={val => {
                                props.onPatch({
                                    upfrontQuantityFunction: {
                                        requirements: val.requirements,
                                        code: val.code ?? '',
                                    },
                                });
                            }}
                        />
                    }

                    {
                        props.product.type === PaymentType.DOWNSTREAM &&
                        <AlertComponent
                            color="warning"
                            sx={{m: 0}}
                        >
                            Bei nachgelagerten Zahlungspositionen wird die Menge im Bearbeitungsprozess durch eine Sachbearbeiter:in festgelegt.
                            Sie können dann einen Zahlungslink generieren und an die antragstellende Person versenden.
                        </AlertComponent>
                    }
                </>
            }
        </Box>
    );
}

export function RootComponentEditorTabPayment(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const api = useApi();

    const [availablePaymentProviders, setAvailablePaymentProviders] = useState<Page<PaymentProviderResponseDTO>>();
    const selectedPaymentProvider = availablePaymentProviders?.content.find(
        provider => provider.key === props.entity.paymentProvider,
    );
    const isTestPaymentProvider = selectedPaymentProvider?.isTestProvider ?? false;

    useEffect(() => {
        new PaymentProvidersApiService(api)
            .listAllOrdered('name', 'ASC', {
                isEnabled: true,
            })
            .then(setAvailablePaymentProviders)
            .catch(error => {
                console.error('Failed to load payment providers', error);
            });
    }, [api]);

    const handleProductPatch = (index: number, patch: Partial<PaymentProduct>) => {
        if (props.entity.products != null) {
            const products = [...props.entity.products];
            products[index] = {
                ...products[index],
                ...patch,
            };
            props.onPatchEntity({
                ...props.entity,
                products,
            });
        }
    };

    if (availablePaymentProviders == null || availablePaymentProviders.size === 0) {
        return (
            <>
                <ElementEditorSectionHeader
                    title="E-Payment konfigurieren"
                    disableMarginTop
                >
                    Wählen Sie einen Zahlungsdienstleister aus und hinterlegen Sie Zahlungspositionen, um Online-Zahlungen über dieses Formular zu ermöglichen.
                </ElementEditorSectionHeader>

                <AlertComponent
                    color="info"
                    sx={{
                        m: 0,
                    }}
                    title="Kein Zahlungsdienstleister hinterlegt"
                >
                    Sie haben noch keinen Zahlungsdienstleister hinterlegt. Wenn Sie eine Administrator:in sind, können Sie einen <Link
                    to={'/payment-providers/new'}
                    target="_blank"
                    style={{color: 'inherit'}}
                >
                    neuen Zahlungsdienstleister
                </Link> zur Verwendung in Formularen anlegen.
                </AlertComponent>
            </>

        );
    }

    return (
        <>
            <ElementEditorSectionHeader
                title="E-Payment konfigurieren"
                disableMarginTop
            >
                Wählen Sie einen Zahlungsdienstleister aus und hinterlegen Sie Zahlungspositionen, um Online-Zahlungen über dieses Formular zu ermöglichen.
            </ElementEditorSectionHeader>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={6}
                >

                    <SelectFieldComponent
                        label="Zahlungsdienstleister"
                        value={props.entity.paymentProvider}
                        onChange={val => {
                            props.onPatchEntity({
                                ...props.entity,
                                paymentProvider: val,
                            });
                        }}
                        disabled={!props.editable}
                        options={availablePaymentProviders?.content.map(pm => ({
                            value: pm.key,
                            label: pm.name,
                        })) ?? []}
                        hint="Wählen Sie einen Zahlungsdienstleister aus. Beachten Sie ggf. dienspezifische Hinweise zur Konfiguration."
                    />

                    {
                        isTestPaymentProvider && (
                            <AlertComponent
                                color="warning"
                                sx={{mt: 2}}
                                title="Es handelt sich um eine vorproduktive Konfiguration"
                            >
                                Über diese vorproduktive Konfiguration werden keine echten Zahlungen durchgeführt.
                                Hinterlegen Sie eine Produktivkonfiguration, um das Formular veröffentlichen zu können.
                            </AlertComponent>
                        )
                    }

                </Grid>
            </Grid>

            {
                isStringNotNullOrEmpty(props.entity.paymentProvider) &&
                <>
                    <Grid
                        container
                        columnSpacing={4}
                    >
                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="Buchungstext"
                                value={props.entity.paymentPurpose}
                                onChange={val => {
                                    props.onPatchEntity({
                                        ...props.entity,
                                        paymentPurpose: val,
                                    });
                                }}
                                disabled={!props.editable}
                                required
                                minCharacters={1}
                                maxCharacters={27}
                                pattern={{
                                    regex: '^[\\w\\d\\ \\-]+$',
                                    message: 'Der Buchungstext darf nur aus Buchstaben (keine Umlaute), Zahlen, Leerzeichen und Bindestrichen (-) bestehen.',
                                }}
                                hint="Der Buchungstext erscheint auf der Abrechnung der antragstellenden Person (z. B. Bank oder Kreditkarte)."
                            />

                            <TextFieldComponent
                                label="Beschreibung"
                                value={props.entity.paymentDescription}
                                onChange={val => {
                                    props.onPatchEntity({
                                        ...props.entity,
                                        paymentDescription: val,
                                    });
                                }}
                                disabled={!props.editable}
                                multiline
                                maxCharacters={250}
                                pattern={{
                                    regex: '^[\\w\\d\\s-,\\.\u00C0-\u017F]+$',
                                    message: 'Die Beschreibung darf nur aus Buchstaben, Zahlen, Kommata, Punkten und Bindestrichen (-) bestehen.',
                                }}
                                hint="Diese Beschreibung wird im Bezahlvorgang angezeigt und erläutert die anfallenden Gebühren."
                            />
                        </Grid>
                    </Grid>

                    <Box
                        display="flex"
                        alignItems="center"
                        sx={{
                            mb: 2,
                            mt: 4,
                        }}
                    >
                        <ElementEditorSectionHeader title={"Zahlungspositionen"} variant={"h4"}/>

                        {
                            props.editable &&
                            <Button
                                sx={{
                                    ml: 'auto',
                                }}
                                variant="outlined"
                                startIcon={<AddCircleOutlineOutlinedIcon />}
                                onClick={() => {
                                    props.onPatchEntity({
                                        ...props.entity,
                                        products: [
                                            ...props.entity.products ?? [],
                                            {
                                                id: uuid4(),
                                                reference: '',
                                                description: '',
                                                type: PaymentType.UPFRONT_FIXED,
                                                netPrice: 1,
                                                taxRate: 19,
                                                upfrontFixedQuantity: 1,
                                                bookingData: [],
                                                taxInformation: '',
                                            },
                                        ],
                                    });
                                }}
                            >
                                Position hinzufügen
                            </Button>
                        }
                    </Box>

                    <Box>
                        {
                            (props.entity.products == null ||
                                props.entity.products.length === 0) &&
                            <AlertComponent color="info">
                                Es sind noch keine Zahlungspositionen konfiguriert.
                                Fügen Sie mindestens eine Zahlungsposition hinzu.
                            </AlertComponent>
                        }
                        {
                            props.entity.products != null &&
                            props.entity.products.map((product, index) => (
                                <PaymentPositionItem
                                    key={index}
                                    index={index}
                                    product={product}
                                    onDelete={() => {
                                        const products = [...props.entity.products ?? []];
                                        products.splice(index, 1);
                                        props.onPatchEntity({
                                            ...props.entity,
                                            products,
                                        });
                                    }}
                                    onPatch={patch => {
                                        handleProductPatch(index, patch);
                                    }}
                                    disabled={!props.editable}
                                />
                            ))
                        }
                    </Box>
                </>
            }
        </>
    );
}
