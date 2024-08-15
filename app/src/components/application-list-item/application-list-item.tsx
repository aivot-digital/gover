import React, {useEffect, useState} from 'react';
import {Box, Button, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {format, isToday, parseISO} from 'date-fns';
import {ApplicationStatus, ApplicationStatusNames} from '../../data/application-status';
import {Link} from 'react-router-dom';
import FolderSharedOutlinedIcon from '@mui/icons-material/FolderSharedOutlined';
import DriveFileRenameOutlineOutlinedIcon from '@mui/icons-material/DriveFileRenameOutlineOutlined';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import {downloadConfigFile} from '../../utils/download-utils';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {type Department} from '../../models/entities/department';
import {type ApplicationListItemProps} from './application-list-item-props';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import FileCopyOutlinedIcon from '@mui/icons-material/FileCopyOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {isAdmin} from '../../utils/is-admin';
import {useApi} from '../../hooks/use-api';
import {useDepartmentsApi} from '../../hooks/use-departments-api';
import {useFormsApi} from '../../hooks/use-forms-api';


export function ApplicationListItem(props: ApplicationListItemProps): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [department, setDepartment] = useState<Department>();
    const [optionAnchorEl, setOptionAnchorEl] = useState<null | HTMLElement>(null);
    const showOptions = Boolean(optionAnchorEl);

    useEffect(() => {
        if (department == null) {
            useDepartmentsApi(api)
                .retrieve(props.application.developingDepartmentId)
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
        useFormsApi(api)
            .retrieve(props.application.id)
            .then(downloadConfigFile);
        handleCloseOptions();
    };

    const isDeveloper =
        isAdmin(props.user) ||
        props.memberships
            .some((mem) => {
                return mem.departmentId === props.application.developingDepartmentId;
            });

    const isEditor =
        isAdmin(props.user) ||
        props.memberships
            .some((mem) => {
                return (
                    mem.departmentId === props.application.managingDepartmentId ||
                    mem.departmentId === props.application.responsibleDepartmentId
                );
            });
    const lastUpdate = parseISO(props.application.updated);

    return (
        <Box className={styles.listItem}>
            <Box
                className={styles.listItemIcon}
            >
                <DescriptionOutlinedIcon
                    sx={{color: 'primary'}}
                    fontSize="large"
                />
            </Box>
            <Box
                className={styles.listItemInfo}
                sx={{
                    ml: 2.5,
                    py: '8px',
                }}
            >
                <Typography
                    variant="h5"
                    sx={{mb: 0.5}}
                >
                    {props.application.title}

                    <Typography
                        variant="caption"
                        sx={{ml: 1}}
                    >
                        {props.application.version}
                    </Typography>
                </Typography>

                <Typography
                    sx={{
                        mt: -0.75,
                        fontSize: '0.875rem',
                        lineHeight: '1.5rem',
                    }}
                    color={'text.secondary'}
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
                    color={'text.secondary'}
                >
                    {ApplicationStatusNames[props.application.status ?? ApplicationStatus.Drafted]} • Zuletzt
                    bearbeitet: {isToday(lastUpdate) ? 'Heute' : format(lastUpdate, 'dd.MM.yyyy')} – {format(lastUpdate, 'HH:mm')} Uhr
                </Typography>
            </Box>
            <Box className={styles.listItemActions}>
                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<DriveFileRenameOutlineOutlinedIcon
                            sx={{
                                marginTop: '-2px',
                            }}
                        />}
                        component={Link}
                        to={`/forms/${props.application.id}`}
                    >
                        Bearbeiten
                    </Button>
                </Box>

                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<DriveFolderUploadOutlinedIcon
                            sx={{
                                marginTop: '-2px',
                            }}
                        />}
                        onClick={handleNewVersion}
                    >
                        Neue Version
                    </Button>
                </Box>

                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<MenuOutlinedIcon
                            sx={{
                                marginTop: '-2px',
                            }}
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
                                <FileCopyOutlinedIcon />
                            </ListItemIcon>
                            <ListItemText>
                                Formular duplizieren
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            component="a"
                            href={`/${props.application.slug}/${props.application.version}`}
                            target="_blank"
                        >
                            <ListItemIcon>
                                <OpenInNewOutlinedIcon />
                            </ListItemIcon>
                            <ListItemText>
                                Formular als antragstellende Person öffnen (in neuem Tab)
                            </ListItemText>
                        </MenuItem>

                        <MenuItem
                            onClick={() => {
                                const link = `${window.location.protocol}//${window.location.host}/${props.application.slug}/${props.application.version}`;
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
                            }}
                        >
                            <ListItemIcon>
                                <ContentPasteOutlinedIcon />
                            </ListItemIcon>
                            <ListItemText>
                                Formularlink in Zwischenablage kopieren
                            </ListItemText>
                        </MenuItem>

                        {
                            isDeveloper &&
                            <MenuItem onClick={handleDownloadConfig}>
                                <ListItemIcon>
                                    <ImportExportOutlinedIcon />
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
                                    <DeleteForeverOutlinedIcon />
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
