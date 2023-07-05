import React from 'react';
import {Box, Button, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {Link} from 'react-router-dom';
import {faExternalLinkAlt, faFileText} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {getColorPalette} from '../../theming/themes';
import {SimplePaletteColorOptions} from '@mui/material/styles/createPalette';
import {PublicListApplication} from "../../models/entities/public-list-application";


interface ApplicationListItemDisplayProps {
    application: PublicListApplication;
}

export function ApplicationListItemPublic({
                                               application,
                                           }: ApplicationListItemDisplayProps) {

    return (
        <Box className={styles.listItem}>
            <Box
                className={styles.listItemIcon}
            >
                <FontAwesomeIcon
                    icon={faFileText}
                    size="2x"
                    color={(getColorPalette(application.theme).primary as SimplePaletteColorOptions).main}
                />
            </Box>
            <Box
                className={styles.listItemInfo}
                sx={{ml: 2.5, py: '8px'}}
            >
                <Typography
                    variant="h6"
                >
                    {application.headline}
                </Typography>
            </Box>
            <Box className={styles.listItemActions}>
                <Box className={styles.listItemActionsContainer}>
                    <Button
                        startIcon={<FontAwesomeIcon
                            icon={faExternalLinkAlt}
                            style={{marginTop: '-2px'}}
                        />}
                        component={Link}
                        to={'/' + application.slug + '/' + application.version}
                    >
                        Öffnen
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}
