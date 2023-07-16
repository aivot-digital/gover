import React, { useEffect, useState } from 'react';
import { Box, Button, ListItemIcon, ListItemText, Menu, MenuItem, Typography } from '@mui/material';
import styles from './application-list-item.module.scss';
import { format, isToday, parseISO } from 'date-fns';
import { ApplicationStatusNames } from '../../data/application-status/application-status-names';
import { ApplicationStatus } from '../../data/application-status/application-status';
import { Link } from 'react-router-dom';
import {
    faArrowUpRightFromSquare,
    faBars,
    faClipboard,
    faClone,
    faEdit,
    faFileExport,
    faFiles,
    faFileText,
    faTrashCanXmark,
    faUpFromDottedLine,
} from '@fortawesome/pro-light-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { ApplicationService } from '../../services/application-service';
import { downloadConfigFile } from '../../utils/download-utils';
import { showSuccessSnackbar } from '../../slices/snackbar-slice';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { type Department } from '../../models/entities/department';
import { DepartmentsService } from '../../services/departments-service';
import { type ApplicationListItemProps } from './application-list-item-props';


export function ApplicationListItem(props: ApplicationListItemProps): JSX.Element {
    const dispatch = useAppDispatch();
    const [department, setDepartment] = useState<Department>();
    const [optionAnchorEl, setOptionAnchorEl] = useState<null | HTMLElement>(null);
    const showOptions = Boolean(optionAnchorEl);

    useEffect(() => {
        if (department == null) {
            DepartmentsService
                .retrieve(props.application.developingDepartment)
                .then(setDepartment);
        }
    }, [props.application]);

    const handleOptionsClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
        setOptionAnchorEl(event.currentTarget);
    };

    const handleCloseOptions = (): void => {
        setOptionAnchorEl(null);
    };

    const handleNewVersion = (): void => {
        props.onNewVersion(props.application);
    };

    const handleClone = (): void => {
        props.onClone(props.application);
        handleCloseOptions();
    };

    const handleDelete = (): void => {
        props.onDelete(props.application);
        handleCloseOptions();
    };

    const handleDownloadConfig = (): void => {
        ApplicationService
            .retrieve(props.application.id)
            .then(downloadConfigFile);
        handleCloseOptions();
    };

    const isDeveloper =
        props.user.admin ||
        props.memberships
            .some((mem) => {
                return mem.department === props.application.developingDepartment;
            });

    const isEditor =
        props.user.admin ||
        props.memberships
            .some((mem) => {
                return (
                    mem.department === props.application.managingDepartment ||
                    mem.department === props.application.responsibleDepartment
                );
            });
    const lastUpdate = parseISO(props.application.updated);

    return (
        <Box className={ styles.listItem }>
            <Box
                className={ styles.listItemIcon }
            >
                <FontAwesomeIcon
                    icon={ faFileText }
                    size="2x"
                />
            </Box>
            <Box
                className={ styles.listItemInfo }
                sx={ {
                    ml: 2.5,
                    py: '8px',
                } }
            >
                <Typography
                    variant="h6"
                >
                    { props.application.title }

                    <Typography
                        variant="caption"
                        sx={ {
                            ml: 1,
                        } }
                    >
                        { props.application.version }
                    </Typography>
                </Typography>

                <Typography
                    sx={ {
                        mt: -0.75,
                        fontSize: '0.875rem',
                        lineHeight: '1.5rem',
                    } }
                >
                    Entwickelt durch: { department?.name }
                </Typography>

                <Typography
                    variant="body2"
                    className={ styles.metaText }
                    sx={ {
                        mt: -0.6,
                        fontSize: '0.875rem',
                        lineHeight: '1.5rem',
                    } }
                >
                    { ApplicationStatusNames[props.application.status ?? ApplicationStatus.Drafted] } • Zuletzt
                                                                                                      bearbeitet: { isToday(lastUpdate) ? 'Heute' : format(lastUpdate, 'dd.MM.yyyy') } – { format(lastUpdate, 'HH:mm') } Uhr
                </Typography>


                <Typography
                    variant="caption"
                >
                    Anträge: Offen { props.application.openSubmissions } | In Bearbeitung { props.application.inProgressSubmissions } | Gesamt { props.application.totalSubmissions }
                </Typography>
            </Box>
            <Box className={ styles.listItemActions }>
                {
                    isEditor &&
                    <Box className={ styles.listItemActionsContainer }>
                        <Button
                            startIcon={ <FontAwesomeIcon
                                icon={ faFiles }
                                style={ {
                                    marginTop: '-2px',
                                } }
                            /> }
                            component={ Link }
                            to={ `/submissions/${ props.application.id }` }
                        >
                            Anträge einsehen
                        </Button>
                    </Box>
                }

                {
                    isDeveloper &&
                    <Box className={ styles.listItemActionsContainer }>
                        <Button
                            startIcon={ <FontAwesomeIcon
                                icon={ faEdit }
                                style={ {
                                    marginTop: '-2px',
                                } }
                            /> }
                            component={ Link }
                            to={ `/edit/${ props.application.id }` }
                        >
                            Bearbeiten
                        </Button>
                    </Box>
                }

                {
                    isDeveloper &&
                    <Box className={ styles.listItemActionsContainer }>
                        <Button
                            startIcon={ <FontAwesomeIcon
                                icon={ faUpFromDottedLine }
                                style={ {
                                    marginTop: '-2px',
                                } }
                            /> }
                            onClick={ handleNewVersion }
                        >
                            Neue Version
                        </Button>
                    </Box>
                }

                <Box className={ styles.listItemActionsContainer }>
                    <Button
                        startIcon={ <FontAwesomeIcon
                            icon={ faBars }
                            style={ {
                                marginTop: '-2px',
                            } }
                        /> }
                        onClick={ handleOptionsClick }
                    >
                        Optionen
                    </Button>

                    <Menu
                        anchorEl={ optionAnchorEl }
                        open={ showOptions }
                        onClose={ handleCloseOptions }
                    >
                        <MenuItem onClick={ handleClone }>
                            <ListItemIcon>
                                <FontAwesomeIcon icon={ faClone }/>
                            </ListItemIcon>
                            <ListItemText>
                                Formular duplizieren
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            component="a"
                            href={ `/#/ ${ props.application.slug }/${ props.application.version }` }
                            target="_blank"
                        >
                            <ListItemIcon>
                                <FontAwesomeIcon icon={ faArrowUpRightFromSquare }/>
                            </ListItemIcon>
                            <ListItemText>
                                Formular als Antragsteller:in öffnen (in neuem Tab)
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            onClick={ () => {
                                const link = `${ window.location.protocol }//${ window.location.host }/#/${ props.application.slug }/${ props.application.version }`;
                                navigator
                                    .clipboard
                                    .writeText(link)
                                    .then(() => {
                                        dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
                                    })
                                    .catch((err) => {
                                        console.error(err);
                                        dispatch(showSuccessSnackbar('Formularlink konnte nicht kopiert werden'));
                                    });
                                handleCloseOptions();
                            } }
                        >
                            <ListItemIcon>
                                <FontAwesomeIcon icon={ faClipboard }/>
                            </ListItemIcon>
                            <ListItemText>
                                Formularlink in Zwischenablage kopieren
                            </ListItemText>
                        </MenuItem>

                        {
                            isDeveloper &&
                            <MenuItem onClick={ handleDownloadConfig }>
                                <ListItemIcon>
                                    <FontAwesomeIcon icon={ faFileExport }/>
                                </ListItemIcon>
                                <ListItemText>
                                    Formular exportieren
                                </ListItemText>
                            </MenuItem>
                        }

                        {
                            isDeveloper &&
                            <MenuItem onClick={ handleDelete }>
                                <ListItemIcon>
                                    <FontAwesomeIcon icon={ faTrashCanXmark }/>
                                </ListItemIcon>
                                <ListItemText>
                                    Formular löschen
                                </ListItemText>
                            </MenuItem>
                        }
                    </Menu>
                </Box>
            </Box>
        </Box>
    );
}
