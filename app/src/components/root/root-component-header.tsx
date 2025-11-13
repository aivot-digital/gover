import React, {useState} from 'react';
import {Box, Container, IconButton, ListItemIcon, ListItemText, Menu, MenuItem, Tooltip, Typography, useTheme} from '@mui/material';
import {showDialog} from '../../slices/app-slice';
import {Logo} from '../logo/logo';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import AccessibilityNewOutlinedIcon from '@mui/icons-material/AccessibilityNewOutlined';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import Balancer from 'react-wrap-balancer';
import {FormDetailsResponseDTO} from '../../modules/forms/dtos/form-details-response-dto';
import {FormsApiService} from '../../modules/forms/forms-api-service-v2';
import DeleteForever from '@aivot/mui-material-symbols-400-outlined/dist/delete-forever/DeleteForever';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import {useConfirm} from '../../providers/confirm-provider';

interface RootComponentHeaderProps {
    form: FormDetailsResponseDTO;
    onDeleteFormData: () => void;
}

export function RootComponentHeader(props: RootComponentHeaderProps) {
    const {
        form,
        onDeleteFormData,
    } = props;

    const dispatch = useAppDispatch();

    const theme = useTheme();

    const showConfirm = useConfirm();

    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();

    const formTitle = (form?.publicTitle ?? '');

    const hasManualLineBreaks = formTitle.includes('\n');

    const [logoStatus, setLogoStatus] = useState<'loading' | 'failed' | 'present'>('loading');

    return (
        <Box
            component="header"
            role="banner"
        >
            <Box
                sx={{
                    boxShadow: '0px 10px 20px rgba(0, 0, 0, 0.06)',
                }}
            >
                <Container>
                    <Box
                        sx={{
                            py: 5,
                            display: 'flex',
                            alignItems: 'center',
                            [theme.breakpoints.down('md')]: {
                                flexDirection: 'column',
                                alignItems: 'flex-start',
                            },
                        }}
                    >
                        <Box
                            sx={{
                                display: 'flex',
                                flex: 1,
                                alignItems: 'center',
                                [theme.breakpoints.down('md')]: {
                                    flexDirection: 'column',
                                    alignItems: 'flex-start',
                                },
                            }}
                        >
                            <Logo
                                key={'logo-' + (form.themeId ?? 'default')}
                                updated={form.updated}
                                src={new FormsApiService().getFormLogoLink(form.slug, form.version)}
                                width={200}
                                height={100}
                                onStatusChange={setLogoStatus}
                            />

                            <Box
                                sx={{
                                    ml: logoStatus !== 'failed' ? 4 : 0,
                                    pl: logoStatus !== 'failed' ? 4 : 0,
                                    borderLeft: logoStatus !== 'failed' ? '1px solid #E4E4E4' : 'none',
                                    [theme.breakpoints.down('md')]: {
                                        borderLeft: 'none',
                                        pl: 0,
                                        ml: 0,
                                        mt: 2,
                                    },
                                }}
                            >
                                <Typography
                                    variant="h1"
                                    color="primary"
                                    sx={{
                                        display: 'block',
                                        maxWidth: '640px',
                                        margin: 0,
                                    }}
                                >
                                    {hasManualLineBreaks ? formTitle : <Balancer>{formTitle}</Balancer>}
                                </Typography>
                            </Box>
                        </Box>
                        <Box
                            component="nav"
                            role="navigation"
                            sx={{
                                [theme.breakpoints.down('md')]: {
                                    mt: 2,
                                },
                            }}
                        >
                            <Tooltip
                                title="Informationen zur Barrierefreiheit"
                            >
                                <IconButton
                                    color="primary"
                                    onClick={() => dispatch(showDialog(AccessibilityDialogId))}
                                >
                                    <AccessibilityNewOutlinedIcon
                                        fontSize="large"
                                    />
                                </IconButton>
                            </Tooltip>

                            <Tooltip
                                title="Hilfe & FAQs"
                            >
                                <IconButton
                                    color="primary"
                                    onClick={() => {
                                        dispatch(showDialog(HelpDialogId));
                                    }}
                                >
                                    <HelpOutlineOutlinedIcon
                                        fontSize="large"
                                    />
                                </IconButton>
                            </Tooltip>

                            <Tooltip
                                title="Formular zurücksetzen"
                            >
                                <IconButton
                                    color="primary"
                                    onClick={(event) => {
                                        setMenuAnchorEl(event.currentTarget);
                                    }}
                                >
                                    <MoreVert
                                        fontSize="large"
                                    />
                                </IconButton>
                            </Tooltip>
                        </Box>
                    </Box>
                </Container>
            </Box>

            <Menu
                anchorEl={menuAnchorEl}
                open={menuAnchorEl != null}
                onClose={() => {
                    setMenuAnchorEl(undefined);
                }}
            >
                <MenuItem
                    onClick={() => {
                        setMenuAnchorEl(undefined);
                        showConfirm({
                            title: 'Anlage abbrechen?',
                            children: (
                                <Typography>
                                    Möchten Sie die Anlage eines neuen Formulars wirklich abbrechen? Bisher eingegebene Daten werden dabei verworfen.
                                </Typography>
                            ),
                            confirmButtonText: 'Ja, Eingaben verwerfen',
                            isDestructive: false,
                            theme: theme,
                        })
                            .then((confirmed) => {
                                if (confirmed) {
                                    onDeleteFormData();
                                }
                            });
                    }}
                >
                    <ListItemIcon>
                        <DeleteForever />
                    </ListItemIcon>
                    <ListItemText>
                        Alle Antragsdaten löschen
                    </ListItemText>
                </MenuItem>
            </Menu>
        </Box>
    );
}


