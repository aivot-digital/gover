import React, {useEffect, useState} from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {Preamble} from '../preamble/preamble';
import {Box, FormHelperText, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {FadingPaper} from '../fading-paper/fading-paper';
import {type Department} from '../../modules/departments/models/department';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {useApi} from '../../hooks/use-api';
import {AlertComponent} from '../alert/alert-component';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {FormCostCalculationResponseDTO} from '../../modules/forms/dtos/form-cost-calculation-response-dto';
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {ExpandableList} from '../expandable-list/expandable-list';
import {AltchaWidget} from '../altcha/altcha-widget';

export const SubmitPaymentDataKey = '__payment_data__';

export function SubmitComponentView(props: BaseViewProps<SubmitStepElement, string>): JSX.Element | null {
    const {
        element,
        value,
        setValue,
        errors,
        elementData,
    } = props;

    const api = useApi();
    const theme = useTheme();

    const initialDisplayCount = 4;

    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const form = useAppSelector(selectLoadedForm);

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    const [costs, setCosts] = useState<FormCostCalculationResponseDTO>();

    useEffect(() => {
        if (form == null) {
            return;
        }

        new FormsApiService(api)
            .calculateCosts(form.id, elementData)
            .then((data) => {
                setCosts(data);
            });
    }, [form]);

    useEffect(() => {
        if (form != null) {
            if (form.responsibleDepartmentId != null) {
                if (responsibleDepartment == null || responsibleDepartment.id !== form.responsibleDepartmentId) {
                    new DepartmentsApiService(api)
                        .retrievePublic(form.responsibleDepartmentId)
                        .then(setResponsibleDepartment);
                }
            } else {
                setResponsibleDepartment(undefined);
            }

            if (form.managingDepartmentId != null) {
                if (managingDepartment == null || managingDepartment.id !== form.managingDepartmentId) {
                    new DepartmentsApiService(api)
                        .retrievePublic(form.managingDepartmentId)
                        .then(setManagingDepartment);
                }
            } else {
                setManagingDepartment(undefined);
            }
        }
    }, [form]);

    const renderDocumentToReceive = (doc: string, index: number) => (
        <ListItem
            disableGutters
            key={String(index) + doc}
        >
            <ListItemIcon sx={{minWidth: '34px'}}>
                <UploadFileOutlinedIcon sx={{color: theme.palette.primary.main}} />
            </ListItemIcon>
            <ListItemText>{doc}</ListItemText>
        </ListItem>
    );

    if (form == null) {
        return null;
    }

    const sections: JSX.Element[] = [];

    if (responsibleDepartment != null) {
        sections.push(
            <Box key="responsible">
                <Typography
                    component={'h3'}
                    variant="h5"
                    color="primary"
                >
                    Zuständige Stelle
                </Typography>
                <Typography
                    component={'pre'}
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {[
                        isStringNotNullOrEmpty(providerName) ? providerName : null,
                        responsibleDepartment.name,
                        responsibleDepartment.address,
                    ].filter(Boolean).join('\n')}
                </Typography>
            </Box>,
        );
    }

    if (managingDepartment != null) {
        sections.push(
            <Box key="managing">
                <Typography
                    component={'h3'}
                    variant="h5"
                    color="primary"
                >
                    Bewirtschaftende Stelle
                </Typography>
                <Typography
                    component={'pre'}
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {[
                        isStringNotNullOrEmpty(providerName) ? providerName : null,
                        managingDepartment.name,
                        managingDepartment.address,
                    ].filter(Boolean).join('\n')}
                </Typography>
            </Box>,
        );
    }

    if (props.element.textProcessingTime) {
        sections.push(
            <Box key="processing-time">
                <Typography
                    component={'h3'}
                    variant="h5"
                    color="primary"
                >
                    Geschätzte Bearbeitungszeit
                </Typography>
                <Typography
                    component="pre"
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {props.element.textProcessingTime}
                </Typography>
            </Box>,
        );
    }

    if ((props.element.documentsToReceive != null)
        && props.element.documentsToReceive.length > 0) {
        sections.push(
            <ExpandableList
                key="documents-to-receive"
                title="Sie erhalten folgende Dokumente"
                items={props.element.documentsToReceive}
                initialVisible={initialDisplayCount}
                singularLabel="Dokument"
                pluralLabel="Dokumente"
                listId="documents-to-receive"
                renderItem={renderDocumentToReceive}
            />,
        );
    }

    return (
        <>
            {
                props.element.textPreSubmit != null &&
                !isStringNullOrEmpty(props.element.textPreSubmit) &&
                <Preamble
                    text={props.element.textPreSubmit}
                    logoLink={form.root.introductionStep?.initiativeLogoLink ?? undefined}
                    logoAlt={form.root.introductionStep?.initiativeName ?? undefined}
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
                    <Box
                        sx={{
                            columnCount: {xs: 1, md: 2},
                            columnGap: 7,
                        }}
                    >
                        {sections.map((section, index) => (
                            <Box
                                key={index}
                                sx={{
                                    breakInside: 'avoid',
                                    mb: 3,
                                    display: 'inline-block',
                                    width: '100%',
                                }}
                            >
                                {section}
                            </Box>
                        ))}
                    </Box>
                </FadingPaper>
            }

            {
                costs != null &&
                costs.totalCost != null &&
                costs.totalCost > 0 &&
                costs.paymentItems != null &&
                costs.paymentProviderName != null &&
                <Box sx={{mt: 4}}>
                    <Typography
                        component={'h3'}
                        variant="h5"
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
                        Dienstleister <strong>{costs.paymentProviderName}</strong> abgewickelt.
                        Bitte achten Sie darauf, dass Sie die Zahlungs&shy;informationen korrekt eingeben und den Vorgang abschließen.
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            maxWidth: '660px',
                            mt: 1,
                        }}
                    >
                        <strong>Wichtig:</strong>
                        &nbsp;Ihr Antrag wird erst nach erfolgter Zahlung bearbeitet.
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
                                costs.paymentItems.map((item, index) => (
                                    <li key={index}>
                                        {item.description}: {formatNumToGermanNum(item.totalPrice, 2)} Euro {
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

                        Insgesamt zu entrichtende Gebühr: {formatNumToGermanNum(costs.totalCost, 2)} Euro
                        inkl. Steuern
                    </AlertComponent>
                </Box>
            }

            <Box sx={{mt: 4}}>
                <Typography
                    id={element.id}
                    component={'h3'}
                    variant="h5"
                    color="primary"
                >
                    Schutz vor automatisierten Einreichungen
                </Typography>

                <Typography
                    sx={{
                        maxWidth: '600px',
                        mt: 1,
                    }}
                >
                    Bitte bestätigen Sie mit einem Klick, dass Sie ein Mensch sind.
                    Die Verifizierung erfolgt automatisch und kann einen kleinen Moment dauern. Vielen Dank!
                </Typography>

                <Box
                    sx={{
                        mt: 3,
                    }}
                >
                    <AltchaWidget
                        onChallengeSuccess={(solution) => {
                            setValue(JSON.stringify(solution));
                        }}
                    />
                    {
                        errors != null &&
                        <Box sx={{mt: 2}}>
                            <FormHelperText error={true}>
                                {errors.join(' ')}
                            </FormHelperText>
                        </Box>
                    }
                </Box>
            </Box>
        </>
    );
}
