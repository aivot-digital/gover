import React, {useMemo} from 'react';
import {Form} from '../../../../models/entities/form';
import {Submission} from '../../../../models/entities/submission';
import {Box, IconButton, Typography} from '@mui/material';
import {StatusTable} from '../../../../components/status-table/status-table';
import {formatNumToGermanNum} from '../../../../utils/format-german-numbers';
import {
    XBezahldienstePaymentStatus,
    XBezahldienstePaymentStatusLabels
} from '../../../../data/xbezahldienste-payment-status';
import {StatusTablePropsItem} from '../../../../components/status-table/status-table-props';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import SubdirectoryArrowRightOutlinedIcon from '@mui/icons-material/SubdirectoryArrowRightOutlined';
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined';
import StoreOutlinedIcon from '@mui/icons-material/StoreOutlined';
import CreditCardOutlinedIcon from '@mui/icons-material/CreditCardOutlined';
import LinkOutlinedIcon from '@mui/icons-material/LinkOutlined';
import {PaymentProvider, PaymentProviderLabels} from '../../../../data/payment-provider';
import {XBezahldienstPaymentMethodLabels} from '../../../../data/xbezahldienste-payment-method';
import ReportProblemOutlinedIcon from '@mui/icons-material/ReportProblemOutlined';
import AssignmentLateOutlinedIcon from '@mui/icons-material/AssignmentLateOutlined';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {SubmissionStatus} from '../../../../data/submission-status';
import {showErrorSnackbar, showSuccessSnackbar} from "../../../../slices/snackbar-slice";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import ContentPasteOutlinedIcon from "@mui/icons-material/ContentPasteOutlined";

interface SubmissionEditPageSummaryTabProps {
    form: Form;
    submission: Submission;
}

export function SubmissionEditPagePaymentTab(props: SubmissionEditPageSummaryTabProps): JSX.Element {
    const dispatch = useAppDispatch();

    const items: StatusTablePropsItem[] = useMemo(() => {
        if (props.submission.paymentRequest == null || props.submission.paymentInformation == null || props.submission.paymentInformation.status == null || props.submission.status === SubmissionStatus.HasPaymentError) {
            return [
                {
                    label: 'Zahlungsstatus',
                    icon: <ReportProblemOutlinedIcon/>,
                    children: 'Zahlung fehlgeschlagen',
                },
                {
                    label: 'Schnittstellenfehler',
                    icon: <AssignmentLateOutlinedIcon/>,
                    children: props.submission.paymentError,
                },
                {
                    label: 'Schnittstellendaten',
                    icon: <DataObjectOutlinedIcon/>,
                    alignTop: true,
                    children: (
                        <Box>
                            Provider: {props.submission.paymentProvider}<br/>
                            Originator ID: {props.submission.paymentOriginatorId}<br/>
                            Endpoint ID: {props.submission.paymentEndpointId}<br/>
                            Request: <br/>
                            <code style={{
                                fontSize: '90%',
                                whiteSpace: 'break-spaces'
                            }}>{JSON.stringify(props.submission.paymentRequest, null, 4)}</code>
                        </Box>
                    ),
                },
            ];
        }

        const items: StatusTablePropsItem[] = [
            {
                label: 'Zahlungsdienstleister',
                icon: <StoreOutlinedIcon/>,
                children: PaymentProviderLabels[props.submission.paymentProvider as PaymentProvider ?? PaymentProvider.ePayBL],
            },
            {
                label: 'Zahlungsstatus',
                icon: <LabelOutlinedIcon/>,
                children: XBezahldienstePaymentStatusLabels[props.submission.paymentInformation.status ?? XBezahldienstePaymentStatus.Initial],
            },
            {
                label: 'Gebühr',
                icon: <ShoppingCartOutlinedIcon/>,
                children: (
                    <Typography>
                        Insgesamt zu entrichtende
                        Gebühr: {formatNumToGermanNum(props.submission.paymentRequest.grosAmount ?? 0, 2)} Euro inkl.
                        Steuern
                    </Typography>
                ),
                subItems: props.submission.paymentRequest.items?.map((item, index) => ({
                    label: '',
                    icon: <SubdirectoryArrowRightOutlinedIcon sx={{mt: 1}}/>,
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
            props.submission.paymentInformation.status === XBezahldienstePaymentStatus.Payed &&
            props.submission.paymentInformation.paymentMethod != null
        ) {
            items.push({
                label: 'Zahlungsmittel',
                icon: <CreditCardOutlinedIcon/>,
                children: XBezahldienstPaymentMethodLabels[props.submission.paymentInformation.paymentMethod],
            });
        }

        if (props.submission.paymentInformation.transactionRedirectUrl != null) {
            items.push({
                label: 'Link zur Bezahlung',
                icon: <LinkOutlinedIcon/>,
                children: <>
                            {props.submission.paymentInformation.transactionRedirectUrl}
                            <IconButton
                                size="small"
                                title={"Kicken zum Kopieren"}
                                sx={{ml: 1, transform: 'translateY(-2px)'}}
                                onClick={() => {
                                    navigator
                                        .clipboard
                                        .writeText(props.submission.paymentInformation?.transactionRedirectUrl ?? '')
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

        if (props.submission.paymentError != null) {
            items.push({
                label: 'Schnittstellenfehler',
                icon: <LinkOutlinedIcon/>,
                children: props.submission.paymentError,
            });
        }

        return items;
    }, [props.submission]);

    return (
        <StatusTable
            label="Zahlungsinformationen"
            items={items}
        />
    );
}
