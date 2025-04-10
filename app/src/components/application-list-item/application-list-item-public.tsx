import React from 'react';
import {Box, Button, Typography} from '@mui/material';
import styles from './application-list-item.module.scss';
import {Link} from 'react-router-dom';
import {format, parseISO} from 'date-fns';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';

interface ApplicationListItemDisplayProps {
    form: FormCitizenListResponseDTO;
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
                    component={'h4'}
                    variant="h6"
                >
                    {props.form.title.replace(/\n/g, ' ')}
                </Typography>
                <Typography
                    variant="body2"
                    className={styles.metaText}
                    sx={{
                        mt: -0.6,
                        fontSize: '0.875rem',
                        lineHeight: '1.5rem',
                        color: 'text.secondary',
                    }}
                >
                    Stand vom: {format(parseISO(props.form.updated), 'dd.MM.yyyy')} • Version: {props.form.version}
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
                        to={`/${props.form.slug}/${props.form.version}`}
                    >
                        Formular öffnen
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}
