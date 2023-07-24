import React from 'react';
import {Box, Button, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {Link} from 'react-router-dom';
import {type PublicListApplication} from '../../models/entities/public-list-application';
import {format, parseISO} from 'date-fns';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';

interface ApplicationListItemDisplayProps {
    application: PublicListApplication;
}

export function ApplicationListItemPublic(props: ApplicationListItemDisplayProps): JSX.Element {
    return (
        <Box className={styles.listItem}>
            <Box
                className={styles.listItemIcon}
            >
                <DescriptionOutlinedIcon
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
                        startIcon={<OpenInNewOutlinedIcon
                            sx={{marginTop: '-2px'}}
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
