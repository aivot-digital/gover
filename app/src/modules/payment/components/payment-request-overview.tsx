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
                Um Ihre Einreichung bearbeiten zu können, ist eine Zahlung von Gebühren erforderlich.
                Die Zahlung wird durch den
                Dienstleister <strong>{request.paymentProviderName}</strong> abgewickelt.
                Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben und den
                Vorgang abschließen.
            </Typography>

            <Typography
                variant="body2"
                sx={{
                    maxWidth: '660px',
                    mt: 1,
                }}
            >
                <strong>Wichtig:</strong>
                &nbsp;Ihre Einreichung wird erst nach erfolgter Zahlung bearbeitet.
            </Typography>

            <AlertComponent
                color="warning"
                sx={{
                    maxWidth: '660px',
                    mt: 3,
                }}
                title="Für Ihre Einreichung sind folgende Gebühren zu zahlen"
            >
                <ul style={{paddingLeft: '20px'}}>
                    {
                        request.paymentItems.map((item, index) => (
                            <li key={index}>
                                {item.description}: {formatNumToGermanNum((item.totalNetAmount ?? 0) + (item.totalTaxAmount ?? 0), 2)} Euro {
                                item.taxRate != null &&
                                item.taxRate > 0 &&
                                <>
                                    inkl. {item.taxRate}% Steuern
                                </>
                            }
                            </li>
                        ))
                    }
                </ul>

                Insgesamt zu entrichtende Gebühr: {formatNumToGermanNum(request.totalCost, 2)} Euro
                inkl. Steuern
            </AlertComponent>
        </>
    );
}