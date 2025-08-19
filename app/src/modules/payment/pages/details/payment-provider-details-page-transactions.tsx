import React, {useContext} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {GridColDef, GridValueGetter} from '@mui/x-data-grid';
import {Box, Typography} from '@mui/material';
import {PaymentProviderResponseDTO} from '../../dtos/payment-provider-response-dto';
import {TransactionsApiService} from '../../transaction-api-service';
import {PaymentTransactionResponseDTO} from '../../dtos/payment-transaction-response-dto';
import {XBezahldienstePaymentStatus} from '../../../../data/xbezahldienste-payment-status';
import {formatNumToGermanNum} from '../../../../utils/format-german-numbers';
import {parseISO} from 'date-fns/parseISO';
import {formatDate} from 'date-fns/format';

const columns: GridColDef<PaymentTransactionResponseDTO>[] = [
    {
        field: 'created',
        headerName: 'Erstellt',
        flex: 1,
        renderCell: (params) => {
            const created = parseISO(params.value);
            return `${formatDate(created, 'dd.MM.yyyy')} ${formatDate(created, 'HH:mm')} Uhr`;
        },
    },
    {
        field: 'paymentInformation.transactionId',
        headerName: 'Schlüssel der Transaktion',
        flex: 1,
        valueGetter: (_, row) => {
            const value = row.paymentInformation?.transactionId;
            return value ? String(value) : 'Keine Transaktions-ID vorhanden';
        },
        sortable: false,
    },
    {
        field: 'paymentRequest.purpose',
        headerName: 'Verwendungszweck',
        flex: 1,
        valueGetter: (_, row) => {
            const value = row.paymentRequest?.purpose;
            return value ? String(value) : 'Kein Verwendungszweck angegeben';
        },
        sortable: false,
    },
    {
        field: 'paymentRequest.grosAmount',
        headerName: 'Gesamtbetrag in Euro',
        flex: 1,
        align: 'right',
        valueGetter: (_, row) => {
            const value = row.paymentRequest?.grosAmount;
            return value ? formatNumToGermanNum(Number(value), 2) + ' €' : 'Kein Gesamtbetrag vorhanden';
        },
        sortable: false,
    },
];

export function PaymentProviderDetailsPageTransactions() {
    const {
        item: paymentProvider,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<PaymentProviderResponseDTO, undefined>;

    if (paymentProvider == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Ausstehende Transaktionen
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Eine Liste aller ausstehenden Transaktionen, die für diesen Zahlungsdienstleister derzeit vorliegen.
            </Typography>

            <GenericList<PaymentTransactionResponseDTO>
                disableFullWidthToggle={true}
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columns}
                fetch={(options) => {
                    return new TransactionsApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {
                                paymentProviderKey: paymentProvider.key,
                                status: XBezahldienstePaymentStatus.Initial,
                                purpose: options.search,
                            },
                        );
                }}
                getRowIdentifier={(paymentProvider) => paymentProvider.key}
                defaultSortField="created"
                noDataPlaceholder="Keine Transaktionen vorhanden"
                loadingPlaceholder="Lade Transaktionen…"
                noSearchResultsPlaceholder="Keine Transaktionen gefunden"
            />
        </Box>
    );
}