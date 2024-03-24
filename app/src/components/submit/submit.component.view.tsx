import React, {useEffect, useState} from 'react';
import {type SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {Preamble} from '../preamble/preamble';
import {Box, Button, CircularProgress, FormHelperText, List, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {FadingPaper} from '../fading-paper/fading-paper';
import {type Department} from '../../models/entities/department';
import {useAppSelector} from '../../hooks/use-app-selector';
import {
    selectCustomerInputError,
    selectCustomerInputValue,
    selectLoadedForm,
    updateCustomerInput
} from '../../slices/app-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import GppGoodTwoToneIcon from '@mui/icons-material/GppGoodTwoTone';
import SmartToyTwoToneIcon from '@mui/icons-material/SmartToyTwoTone';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {useApi} from "../../hooks/use-api";
import {useDepartmentsApi} from "../../hooks/use-departments-api";

export const SubmitHumanKey = '__human__';

export function SubmitComponentView(props: BaseViewProps<SubmitStepElement, void>): JSX.Element | null {
    const api = useApi();
    const theme = useTheme();
    const dispatch = useAppDispatch();

    const [isCalculating, setIsCalculating] = useState(false);
    const isHuman = useAppSelector(selectCustomerInputValue(SubmitHumanKey));
    const error = useAppSelector(selectCustomerInputError(SubmitHumanKey));
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
                                variant="subtitle1"
                                color="primary"
                            >
                                ZUSTÄNDIGE STELLE
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {
                                    isStringNotNullOrEmpty(providerName) &&
                                    <>
                                        {providerName}<br/>
                                    </>
                                }
                                {responsibleDepartment.name}<br/>
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
                                variant="subtitle1"
                                color="primary"
                            >
                                BEWIRTSCHAFTENDE STELLE
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {managingDepartment.name}<br/>
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
                                variant="subtitle1"
                                color="primary"
                            >
                                GESCHÄTZTE BEARBEITUNGSZEIT
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
                                variant="subtitle1"
                                color="primary"
                            >
                                SIE ERHALTEN FOLGENDE DOKUMENTE
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

            <Box sx={{mt: 4}}>
                <Typography
                    variant="h6"
                    color="primary"
                >
                    Schutz vor automatisierten Einreichungen
                </Typography>

                <Typography
                    variant={'body2'}
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
                                    Verifizierung starten
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
                        <CircularProgress/>
                    }
                    {
                        isHuman &&
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                            }}
                        >
                            <GppGoodTwoToneIcon fontSize={"large"}
                                                sx={{color: theme.palette.primary.main}}/>
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
