import React, {useEffect, useState} from 'react';
import {Box, List, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {ElementType} from '../../data/element-type/element-type';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faFileArrowUp, faFileLines, faUser} from '@fortawesome/pro-light-svg-icons';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {IntroductionStepElement} from '../../models/elements/step-elements/introduction-step-element';
import {BaseViewProps} from '../_lib/base-view-props';
import {FadingPaper} from '../static-components/fading-paper/fading-paper';
import {Preamble} from '../static-components/preamble/preamble';
import {Department} from '../../models/department';
import {DepartmentsService} from '../../services/departments.service';
import {selectLoadedApplication} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {isNullOrEmpty} from "../../utils/is-null-or-empty";

export const PrivacyUserInputKey = '__privacy__';

export function GeneralInformationComponentView({element}: BaseViewProps<IntroductionStepElement, void>) {
    const application = useAppSelector(selectLoadedApplication);
    const theme = useTheme();

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    useEffect(() => {
        if (element.responsibleDepartment != null && (element.responsibleDepartment as any) !== '') {
            DepartmentsService.retrieve(element.responsibleDepartment)
                .then(setResponsibleDepartment);
        } else {
            setResponsibleDepartment(undefined);
        }
    }, [element]);

    useEffect(() => {
        if (element.managingDepartment != null && (element.managingDepartment as any) !== '') {
            DepartmentsService.retrieve(element.managingDepartment)
                .then(setManagingDepartment);
        } else {
            setManagingDepartment(undefined);
        }
    }, [element]);

    return (
        <>
            <Preamble
                text={element.teaserText ?? ''}
            />

            {
                (
                    responsibleDepartment ||
                    managingDepartment ||
                    (element.eligiblePersons && element.eligiblePersons.length > 0) ||
                    (element.supportingDocuments && element.supportingDocuments.length > 0) ||
                    (element.documentsToAttach && element.documentsToAttach.length > 0) ||
                    !isNullOrEmpty(application?.root.expiring) ||
                    !isNullOrEmpty(element.expectedCosts)
                ) &&
                <FadingPaper>
                    {
                        responsibleDepartment &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                                {responsibleDepartment.name}<br/>
                                {responsibleDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        managingDepartment &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                        element.eligiblePersons && element.eligiblePersons.length > 0 &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                                            <ListItemIcon sx={{minWidth: '34px'}}>
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
                        element.supportingDocuments && element.supportingDocuments.length > 0 &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                                            <ListItemIcon sx={{minWidth: '34px'}}>
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
                        element.documentsToAttach && element.documentsToAttach.length > 0 &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                                            <ListItemIcon sx={{minWidth: '34px'}}>
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
                        !isNullOrEmpty(application?.root.expiring) &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                        !isNullOrEmpty(element.expectedCosts) &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
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
                sx={{mt: 4}}
            >
                Alle mit Stern (*) gekennzeichneten Felder sind Pflichtfelder.
            </Typography>

            <Typography
                variant="h6"
                color="primary"
                sx={{mt: 4}}
            >
                Hinweise zum Datenschutz
            </Typography>

            {
                application?.root.privacyText &&
                <Box
                    sx={{maxWidth: '600px', mt: 1}}
                >
                    <ViewDispatcherComponent
                        model={{
                            id: 'privacyText',
                            type: ElementType.Richtext,
                            content: application.root.privacyText,
                        }}
                    />
                </Box>
            }

            <Box>
                <ViewDispatcherComponent
                    model={{
                        type: ElementType.Checkbox,
                        label: 'Ich habe die Hinweise zum Datenschutz zur Kenntnis genommen.',
                        id: PrivacyUserInputKey,
                        required: true,
                    }}
                />
            </Box>
        </>
    );
}
