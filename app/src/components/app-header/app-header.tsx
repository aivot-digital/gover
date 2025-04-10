import React, {useState} from 'react';
import {type AppHeaderProps} from './app-header-props';
import {Box, Container, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {useAppSelector} from '../../hooks/use-app-selector';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AppMode} from '../../data/app-mode';
import {selectLoadedForm, showDialog} from '../../slices/app-slice';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {AppHeaderMenu} from '../app-header-menu/app-header-menu';
import {Logo} from '../logo/logo';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import AccessibilityNewOutlinedIcon from '@mui/icons-material/AccessibilityNewOutlined';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import MenuOutlinedIcon from "@mui/icons-material/MenuOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";

export function AppHeader({mode}: AppHeaderProps): JSX.Element {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const app = useAppSelector(selectLoadedForm);
    const accessibilityDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId));

    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();

    const appTitleLine1 = 'Online-Antrags-Management';
    const appTitleLine2 = name;
    const formTitle = (app?.root.headline ?? '');

    const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setMenuAnchorEl(event.currentTarget);
    };

    const handleCloseMenu = () => {
        setMenuAnchorEl(undefined);
    };

    return (
        <header role="banner">
            <Box sx={{boxShadow: '0px 10px 20px rgba(0, 0, 0, 0.06)'}}>
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
                                width={200}
                                height={100}
                            />
                            <Box
                                sx={{
                                    ml: 4,
                                    pl: 4,
                                    borderLeft: '1px solid #E4E4E4',
                                    [theme.breakpoints.down('md')]: {
                                        borderLeft: 'none',
                                        pl: 0,
                                        ml: 0,
                                        mt: 2,
                                    },
                                }}
                            >
                                {mode === AppMode.Customer ?
                                    <>
                                        <h1 style={{margin: '0'}}>
                                            <Typography
                                                variant="h1"
                                                component={"span"}
                                                color="primary"
                                                sx={{display: 'block', maxWidth: '540px'}}
                                            >
                                                {formTitle}
                                            </Typography>
                                        </h1>
                                    </> :
                                    <>
                                        <h1 style={{margin: '0'}}>
                                            <Typography
                                                variant="h1"
                                                component={"span"}
                                                color="primary"
                                                sx={{display: 'block'}}
                                            >
                                                {appTitleLine1}
                                            </Typography>
                                            <Typography
                                                variant="h1"
                                                component={"span"}
                                                sx={{display: 'block'}}
                                            >
                                                {appTitleLine2}
                                            </Typography>
                                        </h1>
                                    </>
                                }

                            </Box>
                        </Box>
                        <Box
                            component={'nav'}
                            role="navigation"
                            sx={{
                                [theme.breakpoints.down('md')]: {
                                    mt: 2,
                                },
                            }}
                        >
                            {
                                (mode === AppMode.Customer || (mode === AppMode.CustomerDisplay && accessibilityDepartmentId)) &&
                                <Tooltip
                                    title="Informationen zur Barrierefreiheit"
                                >
                                    <IconButton
                                        color="primary"
                                        onClick={() => dispatch(showDialog(AccessibilityDialogId))}
                                    >
                                        <AccessibilityNewOutlinedIcon
                                            fontSize={'large'}
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
                                        onClick={mode === AppMode.Staff ?
                                            undefined :
                                            () => {
                                                dispatch(showDialog(HelpDialogId));
                                            }}
                                    >
                                        <HelpOutlineOutlinedIcon
                                            fontSize={'large'}
                                        />
                                    </IconButton>
                                </Tooltip>
                            }

                            {
                                mode !== AppMode.CustomerDisplay &&
                                <Tooltip
                                    title="Navigationsmenü anzeigen"
                                >
                                    <IconButton
                                        color="primary"
                                        onClick={handleOpenMenu}
                                    >
                                        {
                                            mode === AppMode.Staff ?
                                                <MenuOutlinedIcon
                                                    fontSize={'large'}
                                                /> : <SettingsOutlinedIcon
                                                    fontSize={'large'}
                                                />
                                        }
                                    </IconButton>
                                </Tooltip>
                            }
                        </Box>
                    </Box>
                </Container>
            </Box>

            {
                (menuAnchorEl != null) &&
                <AppHeaderMenu
                    mode={mode}
                    onClose={handleCloseMenu}
                    anchorElement={menuAnchorEl}
                />
            }
        </header>
    );
}
