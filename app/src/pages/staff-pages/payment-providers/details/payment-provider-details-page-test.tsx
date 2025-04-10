import {Box, Button, Grid, Typography} from '@mui/material';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useContext, useMemo, useState} from 'react';
import {NumberFieldComponent} from '../../../../components/number-field/number-field-component';
import {useApi} from '../../../../hooks/use-api';
import {PaymentProvidersApiService} from '../../../../modules/payment/payment-providers-api-service';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {isStringNullOrEmpty} from '../../../../utils/string-utils';
import {AlertComponent} from '../../../../components/alert/alert-component';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {PaymentProviderTestDataResponseDTO} from '../../../../modules/payment/dtos/payment-provider-test-data-response-dto';
import {PaymentProviderTestDataRequestDTO} from '../../../../modules/payment/dtos/payment-provider-test-data-request-dto';
import {useValidationErrors} from '../../../../hooks/use-validation-errors';
import {useChangeBlocker} from "../../../../hooks/use-change-blocker";

export function PaymentProviderDetailsPageTest() {
    const api = useApi();
    const apiService = useMemo(() => new PaymentProvidersApiService(api), [api]);

    const {
        item,
        isBusy,
        setIsBusy,
    } = useContext(GenericDetailsPageContext);

    const initialData = {
        amount: 0,
        purpose: '',
        description: '',
    }

    const [data, setData] = useState<PaymentProviderTestDataRequestDTO>(initialData);
    const changeBlocker = useChangeBlocker(
        initialData,
        data,
        "Eingaben verwerfen?",
        "Ihre eingegebenen Testdaten werden nicht gespeichert. Möchten Sie die Seite wirklich verlassen und die Eingaben verwerfen? Wenn Sie zurückkehren, müssen Sie die Testdaten erneut eingeben.",
    );

    const {
        validationErrors,
        hasValidationErrors,
        setValidationError,
        clearValidationErrors,
    } = useValidationErrors<PaymentProviderTestDataRequestDTO>();

    const [result, setResult] = useState<PaymentProviderTestDataResponseDTO>();

    const handleTest = () => {
        clearValidationErrors();

        if (isStringNullOrEmpty(data.purpose)) {
            setValidationError('purpose', 'Buchungstext ist erforderlich');
        }

        if (isStringNullOrEmpty(data.description)) {
            setValidationError('description', 'Beschreibung ist erforderlich');
        }

        if (data.amount === 0) {
            setValidationError('amount', 'Einzelpreis (netto) ist erforderlich');
        }

        if (hasValidationErrors()) {
            return;
        }

        setIsBusy(true);

        apiService
            .test(item.key, data.purpose, data.description, data.amount)
            .then(setResult)
            .catch(error => {
                console.error(error);
                setResult({
                    ok: false,
                    errorMessage: 'Ein Fehler ist aufgetreten: ' + error,
                });
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    return (
        <Box>
            <Grid
                container
                spacing={2}
            >
                <Grid
                    item
                    xs={6}
                >
                    <TextFieldComponent
                        label="Buchungstext"
                        value={data.purpose}
                        onChange={value => setData({
                            ...data,
                            purpose: value ?? '',
                        })}
                        disabled={isBusy}
                        hint="Der Buchungstext (oder auch Verwendungszweck) wird bei der antragstellenden Person auf der Abrechnung (z.B. Bank, Kreditkarte etc.) erscheinen."
                        error={validationErrors.purpose}
                    />
                </Grid>

                <Grid
                    item
                    xs={6}
                >
                    <TextFieldComponent
                        label="Beschreibung"
                        value={data.description}
                        onChange={value => setData({
                            ...data,
                            description: value ?? '',
                        })}
                        disabled={isBusy}
                        hint="Name und/oder Beschreibung der Zahlungsposition. Wird der antragstellenden Person angezeigt."
                        error={validationErrors.description}
                    />
                </Grid>

                <Grid
                    item
                    xs={6}
                >
                    <NumberFieldComponent
                        label="Einzelpreis (netto)"
                        value={data.amount}
                        onChange={value => setData({
                            ...data,
                            amount: value ?? 0,
                        })}
                        disabled={isBusy}
                        error={validationErrors.amount}
                        suffix="€"
                        decimalPlaces={2}
                        hint='Der Nettopreis des einzelnen "Artikels".'
                    />
                </Grid>
            </Grid>

            {
                result != null &&
                <AlertComponent
                    title={result.ok ? 'Erfolgreich' : 'Fehler'}
                    color={result.ok ? 'success' : 'error'}
                >
                    {
                        result.errorMessage != null &&
                        result.request != null &&
                        <Typography sx={{mb: 1}}>
                            Request-ID: {result.request.requestId}
                            <br />
                            {result.request.items?.map((item, index) => (
                                <div key={index}>
                                    Zahlungsposition: {item.quantity} x {item.description} (Einzelpreis (netto): {item.singleNetAmount} {result.request!.currency}) – Nettobetrag: {item.totalNetAmount} {result.request!.currency} – Steuerbetrag: {item.totalTaxAmount} {result.request!.currency} ({item.taxRate} %)
                                </div>
                            ))}
                            Gesamtbetrag: {result.request.grosAmount} {result.request.currency}
                        </Typography>
                    }

                    {
                        result.errorMessage != null &&
                        <Typography sx={{mb: 1}}>
                            Fehlermeldung:<br/>
                            <code style={{fontFamily: 'monospace', fontSize: '0.875rem', marginTop: '4px', padding: '0.5rem', backgroundColor: 'rgba(255, 255, 255, 0.6)', borderRadius: '4px', wordBreak: 'break-word', display: 'block'}}>
                                {result.errorMessage}
                            </code>
                        </Typography>
                    }

                    {
                        result.transaction != null &&
                        result.transaction.paymentInformation != null &&
                        <Typography>
                            ID der Transaktion: {result.transaction.paymentInformation.transactionId}
                            <br />
                            Bezahl-URL: <a style={{color: "inherit"}} target="_blank" href={result.transaction.paymentInformation.transactionRedirectUrl}>{result.transaction.paymentInformation.transactionRedirectUrl}</a>
                            <br />
                        </Typography>
                    }
                </AlertComponent>
            }
            <Box
                sx={{
                    display: 'flex',
                    marginTop: 2,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleTest}
                    disabled={isBusy}
                    variant="contained"
                    startIcon={<ScienceOutlinedIcon />}
                >
                    Zahlung testen
                </Button>
            </Box>

            {changeBlocker.dialog}
        </Box>
    );
}