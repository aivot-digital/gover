import {Typography} from '@mui/material';
import {AlertComponent} from '../../../components/alert/alert-component';
import {formatNumToGermanNum} from '../../../utils/format-german-numbers';
import React from 'react';
import {FormTriggerCostCalculationResponseV1} from '../../forms/services/form-trigger-api-service';

interface PaymentRequestOverviewProps {
    request: FormTriggerCostCalculationResponseV1;
}

export function PaymentRequestOverview(props: PaymentRequestOverviewProps) {
    const {
        request,
    } = props;

    return (
        <>
            <Typography
                variant="body2"
                sx={{
                    maxWidth: '660px',
                    mt: 1,
                }}
            >
                Für diesen Vorgang ist eine Zahlung erforderlich.
                Die Zahlung wird durch den Dienstleister <strong>{request.paymentProviderName}</strong> abgewickelt.
                Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben und den Zahlungsvorgang abschließen.
            </Typography>

            <Typography
                variant="body2"
                sx={{
                    maxWidth: '660px',
                    mt: 1,
                }}
            >
                <strong>Wichtig:</strong>
                &nbsp;Die weitere Verarbeitung wartet auf die Bestätigung der Zahlung.
            </Typography>

            <AlertComponent
                color="warning"
                sx={{
                    maxWidth: '660px',
                    mt: 3,
                }}
                title="Zahlungsübersicht"
            >
                <ul style={{paddingLeft: '20px'}}>
                    {
                        request.paymentItems.map((item, index) => (
                            <li key={index}>
                                {item.description}: {formatNumToGermanNum(item.totalPrice, 2)} Euro {
                                item.taxRate != null &&
                                item.taxRate > 0 &&
                                <>
                                    inkl. {String(item.taxRate).replace('.', ',')}% Steuern
                                </>
                            }
                            </li>
                        ))
                    }
                </ul>

                Gesamtbetrag: {formatNumToGermanNum(request.totalCost, 2)} Euro
                { request.hasTaxes ? ' inkl. Steuern' : '' }
            </AlertComponent>
        </>
    );
}