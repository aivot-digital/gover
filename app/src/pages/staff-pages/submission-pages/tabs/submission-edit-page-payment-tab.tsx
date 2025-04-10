import React, {useEffect, useMemo, useState} from 'react';
import {Form} from '../../../../models/entities/form';
import {Box, IconButton, Typography} from '@mui/material';
import {StatusTable} from '../../../../components/status-table/status-table';
import {formatNumToGermanNum} from '../../../../utils/format-german-numbers';
import {XBezahldienstePaymentStatus, XBezahldienstePaymentStatusLabels} from '../../../../data/xbezahldienste-payment-status';
import {StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import SubdirectoryArrowRightOutlinedIcon from '@mui/icons-material/SubdirectoryArrowRightOutlined';
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined';
import StoreOutlinedIcon from '@mui/icons-material/StoreOutlined';
import CreditCardOutlinedIcon from '@mui/icons-material/CreditCardOutlined';
import LinkOutlinedIcon from '@mui/icons-material/LinkOutlined';
import {XBezahldienstPaymentMethodLabels} from '../../../../data/xbezahldienste-payment-method';
import ReportProblemOutlinedIcon from '@mui/icons-material/ReportProblemOutlined';
import AssignmentLateOutlinedIcon from '@mui/icons-material/AssignmentLateOutlined';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {SubmissionStatus} from '../../../../modules/submissions/enums/submission-status';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {Link} from 'react-router-dom';
import {PaymentTransactionResponseDTO} from '../../../../modules/payment/dtos/payment-transaction-response-dto';
import {PaymentProviderResponseDTO} from '../../../../modules/payment/dtos/payment-provider-response-dto';
import {SubmissionDetailsResponseDTO} from '../../../../modules/submissions/dtos/submission-details-response-dto';
import {useApi} from "../../../../hooks/use-api";
import {PaymentProvidersApiService} from "../../../../modules/payment/payment-providers-api-service";
import {
    PaymentProviderDefinitionResponseDTO
} from "../../../../modules/payment/dtos/payment-provider-definition-response-dto";
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';

interface SubmissionEditPageSummaryTabProps {
    form: Form;
    submission: SubmissionDetailsResponseDTO;
    transaction: PaymentTransactionResponseDTO;
    paymentProvider: PaymentProviderResponseDTO;
}

export function SubmissionEditPagePaymentTab(props: SubmissionEditPageSummaryTabProps): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();
    const apiService = useMemo(() => new PaymentProvidersApiService(api), [api]);

    const [definitions, setDefinitions] = useState<PaymentProviderDefinitionResponseDTO[]>([]);

    useEffect(() => {
        apiService
            .listDefinitions()
            .then(setDefinitions)
            .catch(console.error);
    }, [apiService]);

    const providerName = useMemo(() => {
        return definitions.find(def => def.key === props.paymentProvider.providerKey)?.name ?? props.paymentProvider.providerKey;
    }, [definitions, props.paymentProvider.providerKey]);

    const items: StatusTablePropsItem[] = useMemo(() => {
        if (props.transaction.paymentRequest == null || props.transaction.paymentInformation == null || props.transaction.paymentInformation.status == null || props.submission.status === SubmissionStatus.HasPaymentError) {
            return [
                {
                    label: 'Zahlungsstatus',
                    icon: <ReportProblemOutlinedIcon />,
                    children: 'Zahlung fehlgeschlagen',
                },
                {
                    label: 'Schnittstellenfehler',
                    icon: <AssignmentLateOutlinedIcon />,
                    children: isStringNotNullOrEmpty(props.transaction.paymentError) ? props.transaction.paymentError : 'Unbekannter Fehler',
                },
                {
                    label: 'Schnittstelle',
                    icon: <DataObjectOutlinedIcon />,
                    alignTop: true,
                    children: (
                        <Link to={`/payment-providers/${props.paymentProvider.key}`} color="inherit">
                            {providerName} ({props.paymentProvider.name})
                        </Link>
                    ),
                },
            ];
        }

        const items: StatusTablePropsItem[] = [
            {
                label: 'Zahlungsdienstleister',
                icon: <StoreOutlinedIcon />,
                children: (`${providerName} (${props.paymentProvider.name})`),
            },
            {
                label: 'Zahlungsstatus',
                icon: <LabelOutlinedIcon />,
                children: XBezahldienstePaymentStatusLabels[props.transaction.paymentInformation.status],
            },
            {
                label: 'Gebühr',
                icon: <ShoppingCartOutlinedIcon />,
                children: (
                    <Typography>
                        Insgesamt zu entrichtende
                        Gebühr: {formatNumToGermanNum(props.transaction.paymentRequest.grosAmount ?? 0, 2)} Euro inkl.
                        Steuern
                    </Typography>
                ),
                subItems: props.transaction.paymentRequest.items?.map((item, index) => ({
                    label: '',
                    icon: <SubdirectoryArrowRightOutlinedIcon sx={{mt: 1}} />,
                    children: (
                        <Box sx={{mt: 1}}>
                            <Typography variant="body1">
                                {item.description}: {formatNumToGermanNum((item.totalNetAmount ?? 0) + (item.totalTaxAmount ?? 0), 2)} Euro {item.taxRate != null && item.taxRate > 0 ? `inkl. ${item.taxRate}% Steuern` : ''}
                            </Typography>
                            <Box
                                sx={{pl: 2}}
                            >
                                <Typography variant="body2">
                                    Menge: {formatNumToGermanNum(item.quantity ?? 0, 0)} | Einzelpreis
                                    Netto {formatNumToGermanNum(item.singleNetAmount ?? 0, 2)} Euro | Einzelpreis
                                    Brutto {formatNumToGermanNum((item.singleNetAmount ?? 0) + (item.singleTaxAmount ?? 0), 2)} Euro
                                </Typography>
                            </Box>
                        </Box>
                    ),
                })),
            },
        ];

        if (
            props.transaction.paymentInformation.status === XBezahldienstePaymentStatus.Payed &&
            props.transaction.paymentInformation.paymentMethod != null
        ) {
            items.push({
                label: 'Zahlungsmittel',
                icon: <CreditCardOutlinedIcon />,
                children: XBezahldienstPaymentMethodLabels[props.transaction.paymentInformation.paymentMethod],
            });
        }

        if (props.transaction.paymentInformation.transactionRedirectUrl != null) {
            items.push({
                label: 'Link zur Bezahlung',
                icon: <LinkOutlinedIcon />,
                children: <>
                    {props.transaction.paymentInformation.transactionRedirectUrl}
                    <IconButton
                        size="small"
                        title={'Kicken zum Kopieren'}
                        sx={{ml: 1, transform: 'translateY(-2px)'}}
                        onClick={() => {
                            navigator
                                .clipboard
                                .writeText(props.transaction.paymentInformation?.transactionRedirectUrl ?? '')
                                .then(() => {
                                    dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                })
                                .catch((err) => {
                                    console.error(err);
                                    dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                });
                        }}
                    >
                        <ContentPasteOutlinedIcon
                            fontSize="small"
                        />
                    </IconButton>
                </>,
            });
        }

        if (props.transaction.paymentError != null) {
            items.push({
                label: 'Schnittstellenfehler',
                icon: <LinkOutlinedIcon />,
                children: isStringNotNullOrEmpty(props.transaction.paymentError) ? props.transaction.paymentError : 'Unbekannter Fehler',
            });
        }

        return items;
    }, [props.submission, providerName]);

    return (
        <StatusTable
            label="Zahlungsinformationen"
            items={items}
        />
    );
}
