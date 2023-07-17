import React, {useEffect, useState} from 'react';
import {Box, List, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FadingPaper} from '../static-components/fading-paper/fading-paper';
import {Preamble} from '../static-components/preamble/preamble';
import {type Department} from '../../models/entities/department';
import {DepartmentsService} from '../../services/departments-service';
import {selectLoadedApplication} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {useLocation} from 'react-router-dom';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {selectCustomerInputValue, updateUserInput} from '../../slices/customer-input-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {formatMetaDialog} from '../../utils/format-meta-dialog';
import {selectCustomerInputErrorValue} from '../../slices/customer-input-errors-slice';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';

export const PrivacyUserInputKey = '__privacy__';

export function GeneralInformationComponentView(props: BaseViewProps<IntroductionStepElement, void>): JSX.Element {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const location = useLocation();

    const application = useAppSelector(selectLoadedApplication);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const value = useAppSelector(selectCustomerInputValue(PrivacyUserInputKey));
    const error = useAppSelector(selectCustomerInputErrorValue(PrivacyUserInputKey));


    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    useEffect(() => {
        if (application != null) {
            if (application.responsibleDepartment != null) {
                if (responsibleDepartment == null || responsibleDepartment.id !== application.responsibleDepartment) {
                    DepartmentsService
                        .retrieve(application.responsibleDepartment)
                        .then(setResponsibleDepartment)
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fehler beim Laden der zuständigen Stelle'));
                        });
                }
            }

            if (application.managingDepartment != null) {
                if (managingDepartment == null || managingDepartment.id !== application.managingDepartment) {
                    DepartmentsService
                        .retrieve(application.managingDepartment)
                        .then(setManagingDepartment)
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fehler beim Laden der bewirtschaftenden Stelle'));
                        });
                }
            }
        }
    }, [props.element]);


    return (
        <>
            {
                props.element.teaserText != null &&
                isStringNotNullOrEmpty(props.element.teaserText) &&
                <Preamble
                    text={props.element.teaserText}
                    logoLink={props.element.initiativeLogoLink}
                    logoAlt={props.element.initiativeName}
                />
            }

            {
                (
                    responsibleDepartment != null ||
                    managingDepartment != null ||
                    (props.element.eligiblePersons ?? []).length > 0 ||
                    (props.element.supportingDocuments ?? []).length > 0 ||
                    (props.element.documentsToAttach ?? []).length > 0 ||
                    !isStringNullOrEmpty(application?.root.expiring) ||
                    !isStringNullOrEmpty(props.element.expectedCosts)
                ) &&
                <FadingPaper>
                    {
                        responsibleDepartment != null &&
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
                        managingDepartment != null &&
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
                                {
                                    isStringNotNullOrEmpty(providerName) &&
                                    <>
                                        {providerName}<br/>
                                    </>
                                }
                                {managingDepartment.name}<br/>
                                {managingDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        props.element.eligiblePersons != null &&
                        props.element.eligiblePersons.length > 0 &&
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
                                ANTRAGSBERECHTIGTE
                            </Typography>
                            <List
                                dense
                                disablePadding
                            >
                                {
                                    props.element.eligiblePersons.map((person: string) => (
                                        <ListItem
                                            key={person}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <PersonOutlineOutlinedIcon
                                                    sx={{color: theme.palette.primary.main}}
                                                />
                                            </ListItemIcon>
                                            <ListItemText>
                                                {person}
                                            </ListItemText>
                                        </ListItem>
                                    ))
                                }
                            </List>
                        </Box>
                    }

                    {
                        props.element.supportingDocuments != null &&
                        props.element.supportingDocuments.length > 0 &&
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
                                RELEVANTE DOKUMENTE
                            </Typography>
                            <List
                                dense
                                disablePadding
                            >
                                {
                                    props.element.supportingDocuments.map((document: string) => (
                                        <ListItem
                                            key={document}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <DescriptionOutlinedIcon
                                                    sx={{color: theme.palette.primary.main}}
                                                />
                                            </ListItemIcon>
                                            <ListItemText>
                                                {document}
                                            </ListItemText>
                                        </ListItem>
                                    ))
                                }
                            </List>
                        </Box>
                    }

                    {
                        props.element.documentsToAttach != null &&
                        props.element.documentsToAttach.length > 0 &&
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
                                EINZUREICHENDE DOKUMENTE
                            </Typography>
                            <List
                                dense
                                disablePadding
                            >
                                {
                                    props.element.documentsToAttach.map((document: string) => (
                                        <ListItem
                                            key={document}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <UploadFileOutlinedIcon
                                                    sx={{color: theme.palette.primary.main}}
                                                />
                                            </ListItemIcon>
                                            <ListItemText>
                                                {document}
                                            </ListItemText>
                                        </ListItem>
                                    ))
                                }
                            </List>
                        </Box>
                    }

                    {
                        application != null &&
                        !isStringNullOrEmpty(application?.root.expiring) &&
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
                                ANTRAGSFRISTEN
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {application.root.expiring}
                            </Typography>
                        </Box>
                    }

                    {
                        props.element.expectedCosts != null &&
                        !isStringNullOrEmpty(props.element.expectedCosts) &&
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
                                GEBÜHREN DIESES ANTRAGES
                            </Typography>
                            <Typography
                                variant="body2"
                            >
                                {props.element.expectedCosts}
                            </Typography>
                        </Box>
                    }
                </FadingPaper>
            }

            <Typography
                variant={'body2'}
                sx={{
                    mt: 4,
                }}
            >
                Alle mit Stern (*) gekennzeichneten Felder sind Pflichtfelder.
            </Typography>

            <Typography
                variant="h6"
                color="primary"
                sx={{
                    mt: 4,
                }}
            >
                Hinweise zum Datenschutz
            </Typography>

            {
                application?.root.privacyText != null &&
                <Box
                    sx={{
                        maxWidth: '600px',
                        mt: 1,
                    }}
                >
                    <Typography
                        variant="body2"
                        dangerouslySetInnerHTML={{
                            __html: formatMetaDialog(application.root.privacyText, location),
                        }}
                    />
                </Box>
            }

            <Box>
                <CheckboxFieldComponent
                    label="Ich habe die Hinweise zum Datenschutz zur Kenntnis genommen."
                    value={value}
                    onChange={(checked) => {
                        dispatch(updateUserInput({
                            key: PrivacyUserInputKey,
                            value: checked,
                        }));
                    }}
                    required={true}
                    error={error}
                />
            </Box>
        </>
    );
}
