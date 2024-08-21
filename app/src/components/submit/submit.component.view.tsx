import React, {useEffect, useState} from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {Preamble} from '../preamble/preamble';
import {Box, Button, CircularProgress, FormHelperText, List, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {FadingPaper} from '../fading-paper/fading-paper';
import {type Department} from '../../models/entities/department';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectCustomerInputError, selectCustomerInputValue, selectLoadedForm, updateCustomerInput} from '../../slices/app-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import GppGoodTwoToneIcon from '@mui/icons-material/GppGoodTwoTone';
import SmartToyTwoToneIcon from '@mui/icons-material/SmartToyTwoTone';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {useApi} from '../../hooks/use-api';
import {useDepartmentsApi} from '../../hooks/use-departments-api';
import {AlertComponent} from '../alert/alert-component';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {XBezahldienstePaymentRequest} from '../../models/xbezahldienste/x-bezahldienste-payment-request';
import {PaymentProvider, PaymentProviderLabels} from '../../data/payment-provider';

export const SubmitHumanKey = '__human__';
export const SubmitPaymentDataKey = '__payment_data__';

export function SubmitComponentView(props: BaseViewProps<SubmitStepElement, void>): JSX.Element | null {
    const api = useApi();
    const theme = useTheme();
    const dispatch = useAppDispatch();

    const [isCalculating, setIsCalculating] = useState(false);
    const isHuman = useAppSelector(selectCustomerInputValue(SubmitHumanKey));
    const error = useAppSelector(selectCustomerInputError(SubmitHumanKey));
    const paymentData: XBezahldienstePaymentRequest = useAppSelector(selectCustomerInputValue(SubmitPaymentDataKey));
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const application = useAppSelector(selectLoadedForm);

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    useEffect(() => {
        if (application != null) {
            if (application.responsibleDepartmentId != null) {
                if (responsibleDepartment == null || responsibleDepartment.id !== application.responsibleDepartmentId) {
                    useDepartmentsApi(api)
                        .retrieve(application.responsibleDepartmentId)
                        .then(setResponsibleDepartment);
                }
            } else {
                setResponsibleDepartment(undefined);
            }

            if (application.managingDepartmentId != null) {
                if (managingDepartment == null || managingDepartment.id !== application.managingDepartmentId) {
                    useDepartmentsApi(api)
                        .retrieve(application.managingDepartmentId)
                        .then(setManagingDepartment);
                }
            } else {
                setManagingDepartment(undefined);
            }
        }
    }, [application]);

    if (application == null) {
        return null;
    }

    return (
        <>
            {
                props.element.textPreSubmit != null &&
                !isStringNullOrEmpty(props.element.textPreSubmit) &&
                <Preamble
                    text={props.element.textPreSubmit}
                    logoLink={application.root.introductionStep.initiativeLogoLink}
                    logoAlt={application.root.introductionStep.initiativeName}
                />
            }

            {
                (
                    (responsibleDepartment != null) ||
                    (managingDepartment != null) ||
                    !isStringNullOrEmpty(props.element.textProcessingTime) ||
                    ((props.element.documentsToReceive != null) && props.element.documentsToReceive.length > 0)
                ) &&
                <FadingPaper>
                    {
                        (responsibleDepartment != null) &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Zuständige Stelle
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {
                                    isStringNotNullOrEmpty(providerName) &&
                                    <>
                                        {providerName}<br />
                                    </>
                                }
                                {responsibleDepartment.name}<br />
                                {responsibleDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        (managingDepartment != null) &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Bewirtschaftende Stelle
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {managingDepartment.name}<br />
                                {managingDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        props.element.textProcessingTime &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Geschätzte Bearbeitungszeit
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {props.element.textProcessingTime}
                            </Typography>
                        </Box>
                    }

                    {
                        (props.element.documentsToReceive != null) && props.element.documentsToReceive.length > 0 &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Sie erhalten folgende Dokumente
                            </Typography>
                            <List
                                dense
                                disablePadding
                            >
                                {
                                    props.element.documentsToReceive.map((doc: string) => (
                                        <ListItem
                                            key={doc}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <UploadFileOutlinedIcon
                                                    sx={{color: theme.palette.primary.main}}
                                                />
                                            </ListItemIcon>
                                            <ListItemText>
                                                {doc}
                                            </ListItemText>
                                        </ListItem>
                                    ))
                                }
                            </List>
                        </Box>
                    }
                </FadingPaper>
            }

            {
                paymentData != null &&
                paymentData.items != null &&
                paymentData.items.length > 0 &&
                <Box sx={{mt: 4}}>
                    <Typography
                        component={'h3'}
                        variant="h6"
                        color="primary"
                    >
                        Gebührenübersicht
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            maxWidth: '660px',
                            mt: 1,
                        }}
                    >
                        Um Ihren Antrag bearbeiten zu können, ist eine Bezahlung von Gebühren erforderlich.
                        Die Zahlung wird durch den
                        Dienstleister <strong>{PaymentProviderLabels[application.paymentProvider ?? PaymentProvider.ePayBL]}</strong> abgewickelt.
                        Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben und den Vorgang abschließen.
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            maxWidth: '660px',
                            mt: 1,
                        }}
                    >
                        <strong>Wichtig:</strong> Ihr Antrag wird erst nach erfolgter Zahlung bearbeitet.
                    </Typography>

                    <AlertComponent
                        color="warning"
                        sx={{
                            maxWidth: '660px',
                            mt: 3,
                        }}
                        title="Für Ihren Antrag sind folgende Gebühren zu zahlen"
                    >
                        <ul style={{paddingLeft: '20px'}}>
                            {
                                paymentData.items.map((item, index) => (
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

                        Insgesamt zu entrichtende Gebühr: {formatNumToGermanNum(paymentData.grosAmount ?? 0, 2)} Euro
                        inkl. Steuern
                    </AlertComponent>
                </Box>
            }

            <Box sx={{mt: 4}}>
                <Typography
                    component={'h3'}
                    variant="h6"
                    color="primary"
                >
                    Schutz vor automatisierten Einreichungen
                </Typography>

                <Typography
                    variant="body2"
                    sx={{
                        maxWidth: '660px',
                        mt: 1,
                    }}
                >
                    Bitte bestätigen Sie mit einem Klick auf das nachfolgende Element, dass Sie ein Mensch sind.
                    Die Verifizierung kann einen kleinen Moment dauern. Vielen Dank!
                </Typography>

                <Box
                    sx={{
                        mt: 3,
                        minHeight: '61px',
                    }}
                >
                    {
                        !isCalculating && !isHuman &&
                        <>
                            <Box>
                                <Button
                                    startIcon={<SmartToyTwoToneIcon
                                    />}
                                    onClick={() => {
                                        setIsCalculating(true);
                                        setTimeout(() => {
                                            dispatch(updateCustomerInput({
                                                key: SubmitHumanKey,
                                                value: true,
                                            }));
                                            setIsCalculating(false);
                                        }, 1000);
                                    }}
                                    sx={{
                                        border: '1px solid #E0E0E0',
                                        px: 4,
                                        py: 2,
                                    }}
                                    size={'large'}
                                >
                                    Verifizierung starten *
                                </Button>
                            </Box>
                            {
                                error &&
                                <Box sx={{mt: 2}}>
                                    <FormHelperText error={true}>
                                        {error}
                                    </FormHelperText>
                                </Box>
                            }
                        </>
                    }
                    {
                        isCalculating &&
                        <CircularProgress />
                    }
                    {
                        isHuman &&
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                            }}
                        >
                            <GppGoodTwoToneIcon
                                fontSize={'large'}
                                sx={{color: theme.palette.primary.main}}
                            />
                            <Typography
                                sx={{
                                    ml: 2,
                                }}
                            >
                                Verifizierung erfolgreich. Sie sind ein Mensch.
                            </Typography>
                        </Box>
                    }
                </Box>
            </Box>
        </>
    );
}
