import React from 'react';
import {Box, Button, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {Link} from 'react-router-dom';
import {faExternalLinkAlt, faFileText} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {type PublicListApplication} from '../../models/entities/public-list-application';
import {format, parseISO} from 'date-fns';


interface ApplicationListItemDisplayProps {
    application: PublicListApplication;
}

export function ApplicationListItemPublic(props: ApplicationListItemDisplayProps): JSX.Element {
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
                sx={{
                    ml: 2.5,
                    py: '8px',
                }}
            >
                <Typography
                    variant="h6"
                >
                    {props.application.headline}
                </Typography>

                <Typography variant="caption">
                    Stand vom {format(parseISO(props.application.updated), 'dd.MM.yyyy')}
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
                        target="_blank"
                        rel="noopener noreferrer"
                        to={`/${props.application.slug}/${props.application.version}`}
                    >
                        Öffnen
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}
