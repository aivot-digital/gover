import React, {useCallback, useState} from 'react';
import {Box, Button, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {format, isToday} from 'date-fns';
import {ApplicationStatusNames} from '../../../../../data/application-status/application-status-names';
import {ApplicationStatus} from '../../../../../data/application-status/application-status';
import {Link} from 'react-router-dom';
import {
    faArrowUpRightFromSquare,
    faBars,
    faClone,
    faEdit,
    faFileExport,
    faFilePdf,
    faFileText,
    faTrashCanXmark
} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {showNotImplementedMessage} from '../../../../../utils/show-not-implemented-message';
import {ApplicationService} from '../../../../../services/application.service';
import {Application} from '../../../../../models/entities/application';
import {getColorPalette} from '../../../../../theming/themes';
import {SimplePaletteColorOptions} from '@mui/material/styles/createPalette';
import strings from './application-list-item-strings.json';
import {Localization} from '../../../../../locale/localization';
import {downloadCodeFile, downloadConfigFile} from "../../../../../utils/download-utils";

const __ = Localization(strings);

interface ApplicationListItemComponentViewProps {
    application: Application;
    onClone: (application: Application) => void;
    onDelete: (application: Application) => void;
}

export function ApplicationListItem({
                                                     application,
                                                     onClone,
                                                     onDelete
                                                 }: ApplicationListItemComponentViewProps) {
    const [optionAnchorEl, setOptionAnchorEl] = useState<null | HTMLElement>(null);
    const showOptions = Boolean(optionAnchorEl);

    const handleOptionsClick = useCallback((event: React.MouseEvent<HTMLButtonElement>) => {
        setOptionAnchorEl(event.currentTarget);
    }, []);

    const handleCloseOptions = useCallback(() => {
        setOptionAnchorEl(null);
    }, []);

    const handleClone = useCallback(() => {
        onClone(application);
    }, [application, onClone]);

    const handleDelete = useCallback(() => {
        if (application.root.title != null && application.root.title.length > 0) {
            const res = prompt(`Sind Sie sicher, dass sie den Antrag "${application.root.title}" löschen möchten? Bitte geben Sie den Titel des Antrages ("${application.root.title}") zur Bestätigung der Löschung ein.`);
            if (res != null && res.trim() === application.root.title) {
                onDelete(application);
            }
        } else {
            onDelete(application);
        }
        handleCloseOptions();
    }, [application, onDelete, handleCloseOptions]);

    const handleDownloadConfig = useCallback(() => {
        if (application.slug) {
            ApplicationService.retrieve(application.id)
                .then(model => {
                    downloadConfigFile(model);
                    downloadCodeFile(model);
                });
        }
        handleCloseOptions();
    }, [application, handleCloseOptions]);

    const lastUpdate = application.updated ? new Date(application.updated) : new Date();

    return (
        <Box className={styles.listItem}>
            <Box
                className={styles.listItemIcon}
            >
                <FontAwesomeIcon
                    icon={faFileText}
                    size="2x"
                    color={(getColorPalette(application.root.theme).primary as SimplePaletteColorOptions).main}
                />
            </Box>
            <Box
                className={styles.listItemInfo}
                sx={{ml: 2.5, py: '8px'}}
            >
                <Typography
                    variant="h6"
                >
                    {application.root.title ?? __.defaultTitle}
                    <Typography
                        variant="caption"
                        sx={{ml: 1}}
                    >{application.version}</Typography>
                </Typography>
                <Typography
                    variant="body2"
                    className={styles.metaText}
                    sx={{mt: -0.6, fontSize: '0.875rem', lineHeight: '1.5rem'}}
                >
                    {ApplicationStatusNames[application.status ?? ApplicationStatus.Drafted]} • {__.lastUpdatedLabel}: {isToday(lastUpdate) ? __.today : format(lastUpdate, 'dd.MM.yyyy')} – {format(lastUpdate, 'HH:mm')} {__.oClock}
                </Typography>
            </Box>
            <Box className={styles.listItemActions}>
                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<FontAwesomeIcon
                            icon={faEdit}
                            style={{marginTop: '-2px'}}
                        />}
                        component={Link}
                        to={'/edit/' + application.id}
                    >
                        {__.editLabel}
                    </Button>
                </Box>

                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<FontAwesomeIcon
                            icon={faClone}
                            style={{marginTop: '-2px'}}
                        />}
                        onClick={handleClone}
                    >
                        {__.cloneLabel}
                    </Button>
                </Box>

                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<FontAwesomeIcon
                            icon={faBars}
                            style={{marginTop: '-2px'}}
                        />}
                        onClick={handleOptionsClick}
                    >
                        {__.optionsLabel}
                    </Button>

                    <Menu
                        anchorEl={optionAnchorEl}
                        open={showOptions}
                        onClose={handleCloseOptions}
                    >
                        <MenuItem
                            component="a"
                            href={'/#/' + application.slug + '/' + application.version}
                            target="_blank"
                        >
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faArrowUpRightFromSquare}/>
                            </ListItemIcon>
                            <ListItemText>
                                {__.openAsCustomerLabel}
                            </ListItemText>
                        </MenuItem>
                        <MenuItem onClick={showNotImplementedMessage}>
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faFilePdf}/>
                            </ListItemIcon>
                            <ListItemText>
                                {__.downloadDocumentationLabel}
                            </ListItemText>
                        </MenuItem>
                        <MenuItem onClick={handleDownloadConfig}>
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faFileExport}/>
                            </ListItemIcon>
                            <ListItemText>
                                {__.downloadGovFileLabel}
                            </ListItemText>
                        </MenuItem>
                        <MenuItem onClick={handleDelete}>
                            <ListItemIcon>
                                <FontAwesomeIcon icon={faTrashCanXmark}/>
                            </ListItemIcon>
                            <ListItemText>
                                {__.deleteLabel}
                            </ListItemText>
                        </MenuItem>
                    </Menu>
                </Box>
            </Box>
        </Box>
    );
}
