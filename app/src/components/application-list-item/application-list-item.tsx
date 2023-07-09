import React, {useEffect, useState} from 'react';
import {Box, Button, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {format, isToday} from 'date-fns';
import {ApplicationStatusNames} from '../../data/application-status/application-status-names';
import {ApplicationStatus} from '../../data/application-status/application-status';
import {Link} from 'react-router-dom';
import {
    faArrowUpRightFromSquare,
    faBars,
    faClipboard,
    faClone,
    faEdit,
    faFileExport,
    faFileText,
    faTrashCanXmark,
    faUpFromDottedLine,
    faFiles,
} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {ApplicationService} from '../../services/application-service';
import {SimplePaletteColorOptions} from '@mui/material/styles/createPalette';
import {downloadConfigFile} from "../../utils/download-utils";
import {showSuccessSnackbar} from "../../slices/snackbar-slice";
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {Department} from "../../models/entities/department";
import {DepartmentsService} from "../../services/departments-service";
import {ApplicationListItemProps} from "./application-list-item-props";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectUser} from "../../slices/user-slice";


export function ApplicationListItem({
                                        application,
                                        onClone,
                                        onDelete,
                                        onNewVersion,
                                        memberships,
                                    }: ApplicationListItemProps) {
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const [department, setDepartment] = useState<Department>();
    const [optionAnchorEl, setOptionAnchorEl] = useState<null | HTMLElement>(null);
    const showOptions = Boolean(optionAnchorEl);

    useEffect(() => {
        DepartmentsService
            .retrieve(application.developingDepartment)
            .then(setDepartment);
    }, [application]);

    const handleOptionsClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setOptionAnchorEl(event.currentTarget);
    };

    const handleCloseOptions = () => {
        setOptionAnchorEl(null);
    };

    const handleNewVersion = () => {
        onNewVersion(application);
    };

    const handleClone = () => {
        onClone(application);
        handleCloseOptions();
    };

    const handleDelete = () => {
        onDelete(application);
        handleCloseOptions();
    };

    const handleDownloadConfig = () => {
        ApplicationService
            .retrieve(application.id)
            .then(downloadConfigFile);
        handleCloseOptions();
    };

    const isDeveloper = user?.admin || (memberships ?? []).some(mem => mem.department === application.developingDepartment);
    const isEditor = user?.admin || (memberships ?? []).some(mem => mem.department === application.managingDepartment || mem.department === application.responsibleDepartment);
    const lastUpdate = application.updated ? new Date(application.updated) : new Date();

    return (
        <Box className={styles.listItem}>
            <Box
                className={styles.listItemIcon}
            >
                <FontAwesomeIcon
                    icon={faFileText}
                    size="2x"
                />
            </Box>
            <Box
                className={styles.listItemInfo}
                sx={{ml: 2.5, py: '8px'}}
            >
                <Typography
                    variant="h6"
                >
                    {application.title}

                    <Typography
                        variant="caption"
                        sx={{ml: 1}}
                    >
                        {application.version}
                    </Typography>
                </Typography>

                <Typography
                    sx={{
                        mt: -0.75,
                        fontSize: '0.875rem',
                        lineHeight: '1.5rem',
                    }}
                >
                    Entwickelt durch: {department?.name}
                </Typography>

                <Typography
                    variant="body2"
                    className={styles.metaText}
                    sx={{
                        mt: -0.6,
                        fontSize: '0.875rem',
                        lineHeight: '1.5rem',
                    }}
                >
                    {ApplicationStatusNames[application.status ?? ApplicationStatus.Drafted]} • Zuletzt
                                                                                              bearbeitet: {isToday(lastUpdate) ? 'Heute' : format(lastUpdate, 'dd.MM.yyyy')} – {format(lastUpdate, 'HH:mm')} Uhr
                </Typography>
            </Box>
            <Box className={styles.listItemActions}>
                {
                    isEditor &&
                    <Box className={styles.listItemActionsContainer}>
                        <Button
                            startIcon={<FontAwesomeIcon
                                icon={faFiles}
                                style={{marginTop: '-2px'}}
                            />}
                            component={Link}
                            to={'/submissions/' + application.id}
                        >
                            Anträge einsehen
                        </Button>
                    </Box>
                }

                {
                    isDeveloper &&
                    <Box className={styles.listItemActionsContainer}>
                        <Button
                            startIcon={<FontAwesomeIcon
                                icon={faEdit}
                                style={{marginTop: '-2px'}}
                            />}
                            component={Link}
                            to={'/edit/' + application.id}
                        >
                            Bearbeiten
                        </Button>
                    </Box>
                }

                {
                    isDeveloper &&
                    <Box className={styles.listItemActionsContainer}>
                        <Button
                            startIcon={<FontAwesomeIcon
                                icon={faUpFromDottedLine}
                                style={{marginTop: '-2px'}}
                            />}
                            onClick={handleNewVersion}
                        >
                            Neue Version
                        </Button>
                    </Box>
                }

                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<FontAwesomeIcon
                            icon={faBars}
                            style={{marginTop: '-2px'}}
                        />}
                        onClick={handleOptionsClick}
                    >
                        Optionen
                    </Button>

                    <Menu
                        anchorEl={optionAnchorEl}
                        open={showOptions}
                        onClose={handleCloseOptions}
                    >
                        <MenuItem onClick={handleClone}>
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faClone}/>
                            </ListItemIcon>
                            <ListItemText>
                                Formular duplizieren
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            component="a"
                            href={'/#/' + application.slug + '/' + application.version}
                            target="_blank"
                        >
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faArrowUpRightFromSquare}/>
                            </ListItemIcon>
                            <ListItemText>
                                Formular als Antragsteller:in öffnen (in neuem Tab)
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            onClick={() => {
                                const link = `${window.location.protocol}//${window.location.host}/#/${application.slug}/${application.version}`;
                                navigator.clipboard.writeText(link);
                                dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert!'));
                                handleCloseOptions();
                            }}
                        >
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faClipboard}/>
                            </ListItemIcon>
                            <ListItemText>
                                Formularlink in Zwischenablage kopieren
                            </ListItemText>
                        </MenuItem>

                        {
                            isDeveloper &&
                            <MenuItem onClick={handleDownloadConfig}>
                                <ListItemIcon>
                                    <FontAwesomeIcon icon={faFileExport}/>
                                </ListItemIcon>
                                <ListItemText>
                                    Formular exportieren
                                </ListItemText>
                            </MenuItem>
                        }

                        {
                            isDeveloper &&
                            <MenuItem onClick={handleDelete}>
                                <ListItemIcon>
                                    <FontAwesomeIcon icon={faTrashCanXmark}/>
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
