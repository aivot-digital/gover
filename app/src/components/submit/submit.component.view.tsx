import React, {useEffect, useState} from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {Preamble} from '../preamble/preamble';
import {
    Box,
    Button,
    CircularProgress,
    Divider,
    FormHelperText,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Typography,
    useTheme
} from '@mui/material';
import {FadingPaper} from '../fading-paper/fading-paper';
import {type Department} from '../../modules/departments/models/department';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectCustomerInputError, selectCustomerInputValue, selectLoadedForm, updateCustomerInput} from '../../slices/app-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
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
import ExpandableList from "../expandable-list/expandable-list";
import {AltchaWidget} from "../altcha/altcha-widget";

export const SubmitHumanKey = '__human__';
export const SubmitPaymentDataKey = '__payment_data__';

export function SubmitComponentView(props: BaseViewProps<SubmitStepElement, void>): JSX.Element | null {
    const api = useApi();
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const initialDisplayCount = 4;

    const [isCalculating, setIsCalculating] = useState(false);
    const customerInputs = useAppSelector(state => state.app.inputs);
    const isHuman = useAppSelector(selectCustomerInputValue(SubmitHumanKey));
    const error = useAppSelector(selectCustomerInputError(SubmitHumanKey));
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const [showAllDocumentsToReceive, setShowAllDocumentsToReceive] = useState(false);


    const form = useAppSelector(selectLoadedForm);

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    const [costs, setCosts] = useState<FormCostCalculationResponseDTO>();

    const handleToggleShowAllDocumentsToReceive = () => {
        setShowAllDocumentsToReceive(!showAllDocumentsToReceive);
    };

    useEffect(() => {
        if (form == null) {
            return;
        }

        new FormsApiService(api)
            .calculateCosts(form.id, customerInputs)
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
        <ListItem disableGutters key={String(index) + doc}>
            <ListItemIcon sx={{ minWidth: '34px' }}>
                <UploadFileOutlinedIcon sx={{ color: theme.palette.primary.main }} />
            </ListItemIcon>
            <ListItemText>{doc}</ListItemText>
        </ListItem>
    );

    if (form == null) {
        return null;
    }

    return (
        <>
            {
                props.element.textPreSubmit != null &&
                !isStringNullOrEmpty(props.element.textPreSubmit) &&
                <Preamble
                    text={props.element.textPreSubmit}
                    logoLink={form.root.introductionStep.initiativeLogoLink}
                    logoAlt={form.root.introductionStep.initiativeName}
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
                                sx={{textTransform: 'uppercase'}}
                            >
                                Zuständige Stelle
                            </Typography>
                            <Typography
                                component={"pre"}
                                variant="body2"
                            >
                                {[
                                    isStringNotNullOrEmpty(providerName) ? providerName : null,
                                    responsibleDepartment.name,
                                    responsibleDepartment.address
                                ].filter(Boolean).join("\n")}
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
                                sx={{textTransform: 'uppercase'}}
                            >
                                Bewirtschaftende Stelle
                            </Typography>
                            <Typography
                                component={"pre"}
                                variant="body2"
                            >
                                {[
                                    isStringNotNullOrEmpty(providerName) ? providerName : null,
                                    managingDepartment.name,
                                    managingDepartment.address
                                ].filter(Boolean).join("\n")}
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
                                sx={{textTransform: 'uppercase'}}
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
                        <ExpandableList
                            title="Sie erhalten folgende Dokumente"
                            items={props.element.documentsToReceive}
                            initialVisible={initialDisplayCount}
                            singularLabel="Dokument"
                            pluralLabel="Dokumente"
                            listId="documents-to-receive"
                            renderItem={renderDocumentToReceive}
                        />
                    }
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
                    id={SubmitHumanKey}
                    component={'h3'}
                    variant="h6"
                    color="primary"
                >
                    Schutz vor automatisierten Einreichungen
                </Typography>

                <Box
                    sx={{
                        mt: 3,
                    }}
                >
                    <AltchaWidget
                        onChallengeSuccess={(solution) => {
                            dispatch(updateCustomerInput({
                                key: SubmitHumanKey,
                                value: JSON.stringify(solution),
                            }));
                        }}
                    />
                    {
                        error &&
                        <Box sx={{mt: 2}}>
                            <FormHelperText error={true}>
                                {error}
                            </FormHelperText>
                        </Box>
                    }
                </Box>

            </Box>
        </>
    );
}
