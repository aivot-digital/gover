import React, { useEffect, useState } from 'react';
import { Box, List, ListItem, ListItemIcon, ListItemText, Typography, useTheme } from '@mui/material';
import { ElementType } from '../../data/element-type/element-type';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFileArrowUp, faFileLines, faUser } from '@fortawesome/pro-light-svg-icons';
import { ViewDispatcherComponent } from '../view-dispatcher.component';
import { type IntroductionStepElement } from '../../models/elements/steps/introduction-step-element';
import { FadingPaper } from '../static-components/fading-paper/fading-paper';
import { Preamble } from '../static-components/preamble/preamble';
import { type Department } from '../../models/entities/department';
import { DepartmentsService } from '../../services/departments-service';
import { MetaDialog, selectLoadedApplication } from '../../slices/app-slice';
import { useAppSelector } from '../../hooks/use-app-selector';
import { isStringNotNullOrEmpty, isStringNullOrEmpty } from '../../utils/string-utils';
import ProjectPackage from '../../../package.json';
import { type BaseViewProps } from '../../views/base-view';
import { useLocation, useNavigate } from 'react-router-dom';
import { selectSystemConfigValue } from '../../slices/system-config-slice';
import { SystemConfigKeys } from '../../data/system-config-keys';

export const PrivacyUserInputKey = '__privacy__';

export function GeneralInformationComponentView({ allElements, element }: BaseViewProps<IntroductionStepElement, void>): JSX.Element {
    const application = useAppSelector(selectLoadedApplication);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const theme = useTheme();
    const location = useLocation();

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.responsibleDepartment != null &&
            (responsibleDepartment == null || responsibleDepartment.id !== application.responsibleDepartment)
        ) {
            DepartmentsService
                .retrieve(application.responsibleDepartment)
                .then(setResponsibleDepartment);
        } else {
            setResponsibleDepartment(undefined);
        }
    }, [element]);

    useEffect(() => {
        if (
            application?.managingDepartment != null &&
            (managingDepartment == null || managingDepartment.id !== application.managingDepartment)
        ) {
            DepartmentsService
                .retrieve(application.managingDepartment)
                .then(setManagingDepartment);
        } else {
            setManagingDepartment(undefined);
        }
    }, [element]);

    return (
        <>
            <Preamble
                allElements={allElements}
                text={element.teaserText ?? ''}
            />

            {
                (
                    (responsibleDepartment != null) ||
                    (managingDepartment != null) ||
                    ((element.eligiblePersons != null) && element.eligiblePersons.length > 0) ||
                    ((element.supportingDocuments != null) && element.supportingDocuments.length > 0) ||
                    ((element.documentsToAttach != null) && element.documentsToAttach.length > 0) ||
                    !isStringNullOrEmpty(application?.root.expiring) ||
                    !isStringNullOrEmpty(element.expectedCosts)
                ) &&
                <FadingPaper>
                    {
                        (responsibleDepartment != null) &&
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
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
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
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
                        (element.eligiblePersons != null) && element.eligiblePersons.length > 0 &&
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
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
                                    element.eligiblePersons.map((person: string) => (
                                        <ListItem
                                            key={person}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{ minWidth: '34px' }}>
                                                <FontAwesomeIcon
                                                    icon={faUser}
                                                    fixedWidth
                                                    size={'lg'}
                                                    color={theme.palette.primary.main}
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
                        (element.supportingDocuments != null) && element.supportingDocuments.length > 0 &&
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
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
                                    element.supportingDocuments.map((document: string) => (
                                        <ListItem
                                            key={document}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{ minWidth: '34px' }}>
                                                <FontAwesomeIcon
                                                    icon={faFileLines}
                                                    fixedWidth
                                                    size={'lg'}
                                                    color={theme.palette.primary.main}
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
                        (element.documentsToAttach != null) && element.documentsToAttach.length > 0 &&
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
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
                                    element.documentsToAttach.map((document: string) => (
                                        <ListItem
                                            key={document}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{ minWidth: '34px' }}>
                                                <FontAwesomeIcon
                                                    icon={faFileArrowUp}
                                                    fixedWidth
                                                    size={'lg'}
                                                    color={theme.palette.primary.main}
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
                        !isStringNullOrEmpty(application?.root.expiring) &&
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
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
                                {application!.root.expiring}
                            </Typography>
                        </Box>
                    }

                    {
                        !isStringNullOrEmpty(element.expectedCosts) &&
                        <Box sx={{ mb: 3, position: 'relative', zIndex: 1 }}>
                            <Typography
                                variant="subtitle1"
                                color="primary"
                            >
                                GEBÜHREN DIESES ANTRAGES
                            </Typography>
                            <Typography
                                variant="body2"
                            >
                                {element.expectedCosts}
                            </Typography>
                        </Box>
                    }
                </FadingPaper>
            }

            <Typography
                variant={'body2'}
                sx={{ mt: 4 }}
            >
                Alle mit Stern (*) gekennzeichneten Felder sind Pflichtfelder.
            </Typography>

            <Typography
                variant="h6"
                color="primary"
                sx={{ mt: 4 }}
            >
                Hinweise zum Datenschutz
            </Typography>

            {
                application?.root.privacyText &&
                <Box
                    sx={{ maxWidth: '600px', mt: 1 }}
                >
                    <Typography
                        variant="body2"
                        dangerouslySetInnerHTML={{
                            __html: application.root.privacyText
                                .replace('{privacy}', `<a href="/#${ location.pathname }?dialog=${ MetaDialog.Privacy }">`)
                                .replace('{/privacy}', '</a>'),
                        }}
                    />
                </Box>
            }

            <Box>
                <ViewDispatcherComponent
                    allElements={allElements}
                    element={{
                        type: ElementType.Checkbox,
                        appVersion: ProjectPackage.version,
                        label: 'Ich habe die Hinweise zum Datenschutz zur Kenntnis genommen.',
                        id: PrivacyUserInputKey,
                        required: true,
                    }}
                />
            </Box>
        </>
    );
}
