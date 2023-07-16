import React, {useEffect, useState} from 'react';
import {Box, Button, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {format, isToday} from 'date-fns';
import {ApplicationStatusNames} from '../../data/application-status/application-status-names';
import {ApplicationStatus} from '../../data/application-status/application-status';
import {Link} from 'react-router-dom';
import FolderSharedOutlinedIcon from '@mui/icons-material/FolderSharedOutlined';
import DriveFileRenameOutlineOutlinedIcon from '@mui/icons-material/DriveFileRenameOutlineOutlined';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
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
} from '@fortawesome/pro-regular-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {ApplicationService} from '../../services/application-service';
import {getColorPalette} from '../../theming/themes';
import {SimplePaletteColorOptions} from '@mui/material/styles/createPalette';
import {downloadConfigFile} from "../../utils/download-utils";
import {showSuccessSnackbar} from "../../slices/snackbar-slice";
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {Department} from "../../models/entities/department";
import {DepartmentsService} from "../../services/departments-service";
import {ApplicationListItemProps} from "./application-list-item-props";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectUser} from "../../slices/user-slice";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import FileCopyOutlinedIcon from '@mui/icons-material/FileCopyOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';

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
        if (application.title != null && application.title.length > 0) {
            const res = prompt(`Sind Sie sicher, dass sie den Antrag "${application.title}" löschen möchten? Bitte geben Sie den Titel des Antrages ("${application.title}") zur Bestätigung der Löschung ein.`);
            if (res != null && res.trim() === application.title) {
                onDelete(application);
            }
        } else {
            onDelete(application);
        }
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
                <DescriptionOutlinedIcon
                    sx={{ color: (getColorPalette(application.theme).primary as SimplePaletteColorOptions).main }}
                    fontSize="large"
                />
            </Box>
            <Box
                className={styles.listItemInfo}
                sx={{ml: 2.5, py: '8px'}}
            >
                <Typography
                    variant="h5"
                    sx={{mb: 0.5}}
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
                            startIcon={<FolderSharedOutlinedIcon sx={{marginTop: '-2px' }} />}
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
                            startIcon={<DriveFileRenameOutlineOutlinedIcon sx={{marginTop: '-2px' }} />}
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
                            startIcon={<DriveFolderUploadOutlinedIcon sx={{marginTop: '-2px' }} />}
                            onClick={handleNewVersion}
                        >
                            Neue Version
                        </Button>
                    </Box>
                }

                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<MenuOutlinedIcon sx={{marginTop: '-2px' }} />}
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
                                <FileCopyOutlinedIcon/>
                            </ListItemIcon>
                            <ListItemText>
                                Antrag duplizieren
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            component="a"
                            href={'/#/' + application.slug + '/' + application.version}
                            target="_blank"
                        >
                            <ListItemIcon>
                                <OpenInNewOutlinedIcon/>
                            </ListItemIcon>
                            <ListItemText>
                                Antrag als Antragsteller:in öffnen (in neuem Tab)
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            onClick={() => {
                                const link = `${window.location.protocol}//${window.location.host}/#/${application.slug}/${application.version}`;
                                navigator.clipboard.writeText(link);
                                dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                handleCloseOptions();
                            }}
                        >
                            <ListItemIcon>
                                <ContentPasteOutlinedIcon/>
                            </ListItemIcon>
                            <ListItemText>
                                Link in Zwischenablage kopieren
                            </ListItemText>
                        </MenuItem>

                        {
                            isDeveloper &&
                            <MenuItem onClick={handleDownloadConfig}>
                                <ListItemIcon>
                                    <ImportExportOutlinedIcon/>
                                </ListItemIcon>
                                <ListItemText>
                                    Antrag als .gov-Datei exportieren
                                </ListItemText>
                            </MenuItem>
                        }

                        {
                            isDeveloper &&
                            <MenuItem onClick={handleDelete}>
                                <ListItemIcon>
                                    <DeleteForeverOutlinedIcon/>
                                </ListItemIcon>
                                <ListItemText>
                                    Antrag löschen
                                </ListItemText>
                            </MenuItem>
                        }
                    </Menu>
                </Box>
            </Box>
        </Box>
    );
}
