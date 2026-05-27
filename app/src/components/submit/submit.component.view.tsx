import React, {useEffect, useState} from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {Preamble} from '../preamble/preamble';
import {Box, FormHelperText, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {FadingPaper} from '../fading-paper/fading-paper';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {AlertComponent} from '../alert/alert-component';
import {formatNumToGermanNum} from '../../utils/format-german-numbers';
import {FormCostCalculationResponseDTO} from '../../modules/forms/dtos/form-cost-calculation-response-dto';
import {ExpandableList} from '../expandable-list/expandable-list';
import {AltchaWidget} from '../altcha/altcha-widget';
import {VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {FormApiService} from '../../modules/forms/services/form-api-service';
import {ElementType} from '../../data/element-type/element-type';
import type {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import {isRootElement} from '../../models/elements/form-layout-element';
import {getDepartmentDisplayAddress} from '../../modules/departments/utils/department-utils';

export function SubmitComponentView(props: BaseViewProps<SubmitStepElement, any>): React.ReactNode | null {
    const {
        element,
        setValue,
        errors,
        authoredElementValues,
    } = props;

    const {
        rootElement,
    } = useViewDispatcherContext();

    const theme = useTheme();

    const initialDisplayCount = 4;

    const [responsibleDepartment, setResponsibleDepartment] = useState<VDepartmentShadowedEntity>();
    const [managingDepartment, setManagingDepartment] = useState<VDepartmentShadowedEntity>();

    const [costs, setCosts] = useState<FormCostCalculationResponseDTO>();

    useEffect(() => {
        setValue(undefined);
    }, []);

    /* TODO: calculate costs
    useEffect(() => {
        if (form == null) {
            return;
        }

        new FormApiService()
            .calculateCosts(form.form.slug, form.version.version, authoredElementValues)
            .then((data) => {
                setCosts(data);
            });
    }, [form]);
     */

    useEffect(() => {
        if (!isRootElement(rootElement)) {
            return;
        }

            if (rootElement.responsibleDepartmentId != null) {
                if (responsibleDepartment == null || responsibleDepartment.id !== rootElement.responsibleDepartmentId) {
                    new DepartmentApiService()
                        .retrievePublic(rootElement.responsibleDepartmentId)
                        .then(setResponsibleDepartment);
                }
            } else {
                setResponsibleDepartment(undefined);
            }

            if (rootElement.managingDepartmentId != null) {
                if (managingDepartment == null || managingDepartment.id !== rootElement.managingDepartmentId) {
                    new DepartmentApiService()
                        .retrievePublic(rootElement.managingDepartmentId)
                        .then(setManagingDepartment);
                }
            } else {
                setManagingDepartment(undefined);
            }
    }, [rootElement]);

    const renderDocumentToReceive = (doc: string, index: number) => (
        <ListItem
            disableGutters
            key={String(index) + doc}
        >
            <ListItemIcon sx={{minWidth: '34px'}}>
                <UploadFileOutlinedIcon sx={{color: theme.palette.primary.main}}/>
            </ListItemIcon>
            <ListItemText>{doc}</ListItemText>
        </ListItem>
    );

    const sections: React.ReactNode[] = [];
    const responsibleDepartmentAddress = getDepartmentDisplayAddress(responsibleDepartment);
    const managingDepartmentAddress = getDepartmentDisplayAddress(managingDepartment);

    if (responsibleDepartmentAddress != null) {
        sections.push(
            <Box key="responsible">
                <Typography
                    component={'h3'}
                    variant="h5"
                >
                    Zuständige Stelle
                </Typography>
                <Typography
                    component={'pre'}
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {responsibleDepartmentAddress}
                </Typography>
            </Box>,
        );
    }

    if (managingDepartmentAddress != null) {
        sections.push(
            <Box key="managing">
                <Typography
                    component={'h3'}
                    variant="h5"
                >
                    Bewirtschaftende Stelle
                </Typography>
                <Typography
                    component={'pre'}
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {managingDepartmentAddress}
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

    if (!isRootElement(rootElement)) {
        return null;
    }

    return (
        <>
            {
                props.element.textPreSubmit != null &&
                !isStringNullOrEmpty(props.element.textPreSubmit) &&
                <Preamble
                    text={props.element.textPreSubmit}
                    logoLink={(rootElement.children?.find((c: any) => c.type === ElementType.IntroductionStep) as IntroductionStepElement)?.initiativeLogoLink ?? undefined}
                    logoAlt={(rootElement.children?.find((c: any) => c.type === ElementType.IntroductionStep) as IntroductionStepElement)?.initiativeName ?? undefined}
                />
            }

            {
                sections.length > 0 &&
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
                        onChallengeSuccess={setValue}
                    />

                    {
                        errors != null &&
                        <Box sx={{mt: 1}}>
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
