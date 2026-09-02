import {Box, Button, Typography} from '@mui/material';
import {Link} from 'react-router-dom';
import OpenInNewOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import {type FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';
import {formatInstantInApplicationTimeZone} from '../../utils/temporal-utils';

interface PublicFormListItemProps {
    form: FormCitizenListResponseDTO;
}

export function PublicFormListItem(props: PublicFormListItemProps) {
    return (
        <Box
            sx={{
                mb: 2,
                display: 'flex',
                alignItems: 'stretch',
                minHeight: 76,
                overflow: 'hidden',
                backgroundColor: 'background.paper',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                boxShadow: 1,
            }}
        >
            <Box
                sx={{
                    px: 2.25,
                    py: 1.75,
                    display: 'flex',
                    alignItems: 'center',
                    borderRight: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <DescriptionOutlinedIcon
                    fontSize="large"
                    sx={{color: 'primary.main'}}
                />
            </Box>
            <Box
                sx={{
                    flex: 1,
                    minWidth: 0,
                    ml: 2.5,
                    py: '8px',
                }}
            >
                <Typography
                    component={'h4'}
                    variant="h6"
                    sx={{overflowWrap: 'anywhere'}}
                >
                    {props.form.title.replace(/\n/g, ' ')}
                </Typography>
                <Typography
                    variant="body2"
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
            <Box
                sx={{
                    ml: 'auto',
                    px: 3,
                    display: 'flex',
                    alignItems: 'center',
                    borderLeft: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <Button
                    startIcon={<OpenInNewOutlinedIcon sx={{mt: '-2px'}}/>}
                    component={Link}
                    target="_blank"
                    rel="noopener noreferrer"
                    to={`/${props.form.slug}`}
                    sx={{textTransform: 'none', whiteSpace: 'nowrap'}}
                >
                    Formular öffnen
                </Button>
            </Box>
        </Box>
    );
}
