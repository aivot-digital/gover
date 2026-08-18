import React from 'react';
import {Box, Button, Typography} from '@mui/material';
import styles from './public-form-list-item.module.scss';
import {Link} from 'react-router-dom';
import OpenInNewOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import {FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';
import {formatInstantInApplicationTimeZone} from '../../utils/temporal-utils';

interface PublicFormListItemProps {
    form: FormCitizenListResponseDTO;
}

export function PublicFormListItem(props: PublicFormListItemProps) {
    return (
        <Box className={styles.listItem}>
            <Box
                className={styles.listItemIcon}
            >
                <DescriptionOutlinedIcon
                    fontSize="large"
                    sx={{color: 'primary.dark'}}
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
                    Stand vom: {formatInstantInApplicationTimeZone(
                        props.form.updated,
                        'dd.MM.yyyy',
                    ) ?? 'Unbekannt'} • Version: {props.form.version}
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
                        to={`/${props.form.slug}`}
                    >
                        Formular öffnen
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}
