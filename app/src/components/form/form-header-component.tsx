import React, {useEffect, useState} from 'react';
import {
    Box,
    Container,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import {showDialog} from '../../slices/app-slice';
import {Logo} from '../logo/logo';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import HelpOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import AccessibilityNewOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AccessibilityNew';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import Balancer from 'react-wrap-balancer';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {useConfirm} from '../../providers/confirm-provider';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {FormLayoutElement, resolveFormNodeName} from '../../models/elements/form-layout-element';
import {ProcessEntity} from '../../modules/process/entities/process-entity';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';
import {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import {resolveAccessibleForeground} from '../../theming/resolve-appearance-colors';
import {ColorModePicker} from '../color-mode-picker/color-mode-picker';

interface FormHeaderComponentProps {
    form: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    logoUrl: string | null;
    logoUrlDark: string | null;
    onDeleteFormData: () => void;
}

export function FormHeaderComponent(props: FormHeaderComponentProps) {
    const {
        form,
        node,
        process,
        version,
        logoUrl,
        logoUrlDark,
        onDeleteFormData,
    } = props;

    const dispatch = useAppDispatch();

    const theme = useTheme();

    const showConfirm = useConfirm();

    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();

    const formTitle = resolveFormNodeName(form, version);

    const hasManualLineBreaks = formTitle.includes('\n');

    const [logoStatus, setLogoStatus] = useState<'loading' | 'failed' | 'present'>('loading');

    const resolvedLogoUrl = theme.palette.mode === 'dark' ? logoUrlDark ?? logoUrl : logoUrl;

    useEffect(() => {
        setLogoStatus(resolvedLogoUrl == null ? 'failed' : 'loading');
    }, [resolvedLogoUrl]);

    const hasVisibleLogo = resolvedLogoUrl != null && logoStatus === 'present';

    return (
        <Box
            component="header"
            role="banner"
        >
            <Box
                sx={{
                    boxShadow: '0px 10px 20px rgba(0, 0, 0, 0.06)',
                    backgroundColor: 'background.paper',
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
                            {
                                resolvedLogoUrl != null &&
                                <Logo
                                    key={'logo-' + resolvedLogoUrl}
                                    updated={version.updated}
                                    src={logoUrl ?? undefined}
                                    srcDark={logoUrlDark ?? undefined}
                                    width={200}
                                    height={100}
                                    onStatusChange={setLogoStatus}
                                />
                            }

                            <Box
                                sx={{
                                    ml: hasVisibleLogo ? 4 : 0,
                                    pl: hasVisibleLogo ? 4 : 0,
                                    borderLeft: hasVisibleLogo ? `1px solid ${theme.palette.divider}` : 'none',
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
                                    sx={{
                                        color: resolveAccessibleForeground(
                                            theme.palette.primary.main,
                                            theme.palette.background.paper,
                                        ),
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
                                arrow
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
                                arrow
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

                            <ColorModePicker
                                color="primary"
                                iconFontSize="large"
                            />

                            <Tooltip
                                title="Weitere Optionen"
                                arrow
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
                                    Möchten Sie die Anlage eines neuen Formulars wirklich abbrechen? Bisher eingegebene
                                    Daten werden dabei verworfen.
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
                        <Delete/>
                    </ListItemIcon>
                    <ListItemText>
                        Alle Formulardaten löschen
                    </ListItemText>
                </MenuItem>
            </Menu>
        </Box>
    );
}
