import React, {useState} from 'react';
import {AppHeaderProps} from './app-header-props';
import {Box, Container, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {useAppSelector} from '../../hooks/use-app-selector';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AppMode} from '../../data/app-mode';
import {MetaDialog, selectLoadedApplication, showMetaDialog} from '../../slices/app-slice';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {AppHeaderMenu} from './app-header-menu/app-header-menu';
import {Logo} from '../static-components/logo/logo';
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import AccessibilityNewOutlinedIcon from '@mui/icons-material/AccessibilityNewOutlined';

export function AppHeader({mode}: AppHeaderProps) {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const app = useAppSelector(selectLoadedApplication);

    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();

    let titleLine1 = 'Online-Antrags-Management';
    let titleLine2 = name;

    if (mode === AppMode.Customer) {
        if (app?.root.headline != null) {
            const appTitle = (app?.root.headline ?? '').split('\n');
            titleLine1 = appTitle[0] ?? '';
            titleLine2 = appTitle[1] ?? '';
        } else {
            titleLine1 = 'Keine Überschrift angegeben';
            titleLine2 = 'Füge eine Überschrift hinzu';
        }
    }

    const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setMenuAnchorEl(event.currentTarget);
    };

    const handleCloseMenu = () => {
        setMenuAnchorEl(undefined);
    };

    return (
        <>
            <Box sx={{boxShadow: '0px 10px 20px rgba(0, 0, 0, 0.06)'}}>
                <Container>
                    <Box
                        sx={{
                            py: 4,
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
                            <Box>
                                <Logo
                                    width={200}
                                    height={100}
                                />
                            </Box>
                            <Box
                                sx={{
                                    ml: 4,
                                    pl: 4,
                                    borderLeft: '1px solid #E4E4E4',
                                    [theme.breakpoints.down('md')]: {
                                        borderLeft: 'none',
                                        pl: 0,
                                        ml: 0,
                                    },
                                }}
                            >
                                <div>
                                    <Typography
                                        variant="h1"
                                        color="primary"
                                        sx={{fontWeight: 'bold'}}
                                    >
                                        {titleLine1}
                                    </Typography>
                                </div>
                                <div>

                                    <Typography
                                        variant="h1"
                                        sx={{fontWeight: 'bold'}}
                                    >
                                        {titleLine2}
                                    </Typography>
                                </div>
                            </Box>
                        </Box>
                        <Box
                            sx={{
                                [theme.breakpoints.down('md')]: {
                                    ml: 'auto',
                                    mt: 2,
                                },
                            }}
                        >
                            {
                                mode === AppMode.Customer &&
                                <Tooltip
                                    title="Informationen zur Barrierefreiheit"
                                >
                                    <IconButton
                                        color="primary"
                                        onClick={() => dispatch(showMetaDialog(MetaDialog.Accessibility))}
                                    >
                                        <AccessibilityNewOutlinedIcon
                                            fontSize={"large"}
                                        />
                                    </IconButton>
                                </Tooltip>
                            }

                            {
                                mode !== AppMode.CustomerDisplay &&
                                <Tooltip
                                    title="Hilfe & FAQs"
                                >
                                    <IconButton
                                        color="primary"
                                        component={mode === AppMode.Staff ? 'a' : 'button'}
                                        href={mode === AppMode.Staff ? 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' : undefined}
                                        target={mode === AppMode.Staff ? '_blank' : undefined}
                                        onClick={mode === AppMode.Staff ? undefined : () => {
                                            dispatch(showMetaDialog(MetaDialog.Help));
                                        }}
                                    >
                                        <HelpOutlineOutlinedIcon
                                            fontSize={"large"}
                                        />
                                    </IconButton>
                                </Tooltip>
                            }

                            {
                                mode !== AppMode.CustomerDisplay &&
                                <Tooltip
                                    title="Einstellungen"
                                >
                                    <IconButton
                                        color="primary"
                                        onClick={handleOpenMenu}
                                    >
                                        <SettingsOutlinedIcon
                                            fontSize={"large"}
                                        />
                                    </IconButton>
                                </Tooltip>
                            }
                        </Box>
                    </Box>
                </Container>
            </Box>

            {
                menuAnchorEl &&
                <AppHeaderMenu
                    mode={mode}
                    onClose={handleCloseMenu}
                    anchorElement={menuAnchorEl}
                />
            }
        </>
    );
}
