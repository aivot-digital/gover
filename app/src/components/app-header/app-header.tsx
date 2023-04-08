import React, {useState} from 'react';
import {AppHeaderProps} from './app-header-props';
import {Box, Container, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import strings from './app-header-strings.json';
import {useAppSelector} from '../../hooks/use-app-selector';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AppMode} from '../../data/app-mode';
import {faCog, faQuestionCircle, faUniversalAccess} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {Localization} from '../../locale/localization';
import {selectLoadedApplication} from '../../slices/app-slice';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {AppHeaderMenu} from './components/app-header-menu/app-header-menu';
import {AccessibilityDialog} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {HelpDialog} from '../../dialogs/help-dialog/help.dialog';
import {Logo} from '../static-components/logo/logo';

const __ = Localization(strings);

export function AppHeader({mode}: AppHeaderProps) {
    const theme = useTheme();
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const app = useAppSelector(selectLoadedApplication);

    const [showAccessibility, setShowAccessibility] = useState(false);
    const [showHelp, setShowHelp] = useState(false);
    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();

    let titleLine1 = __.goverAppTitle;
    let titleLine2 = name;

    if (mode === AppMode.Customer) {
        if (app?.root.headline != null) {
            const appTitle = (app?.root.headline ?? '').split('\n');
            titleLine1 = appTitle[0] ?? '';
            titleLine2 = appTitle[1] ?? '';
        } else {
            titleLine1 = app?.root.title ?? '';
            titleLine2 = '';
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
                        <Box sx={{
                            [theme.breakpoints.down('md')]: {
                                ml: 'auto',
                                mt: 2,
                            },
                        }}>
                            {
                                mode === AppMode.Customer &&
                                <Tooltip
                                    title={__.tooltipAccessibility}
                                >
                                    <IconButton
                                        color="primary"
                                        onClick={() => setShowAccessibility(true)}
                                    >
                                        <FontAwesomeIcon
                                            icon={faUniversalAccess}
                                            size="lg"
                                        />
                                    </IconButton>
                                </Tooltip>
                            }

                            <Tooltip
                                title={__.tooltipHelp}
                            >
                                <IconButton
                                    color="primary"
                                    component={mode === AppMode.Staff ? 'a' : 'button'}
                                    href={mode === AppMode.Staff ? 'https://aivot.de/gover' : undefined}
                                    target={mode === AppMode.Staff ? '_blank' : undefined}
                                    onClick={mode === AppMode.Staff ? undefined : () => {
                                        setShowHelp(true);
                                    }}
                                >
                                    <FontAwesomeIcon
                                        icon={faQuestionCircle}
                                        size="lg"
                                    />
                                </IconButton>
                            </Tooltip>

                            <Tooltip
                                title={__.tooltipSettings}
                            >
                                <IconButton
                                    color="primary"
                                    onClick={handleOpenMenu}
                                >
                                    <FontAwesomeIcon
                                        icon={faCog}
                                        size="lg"
                                    />
                                </IconButton>
                            </Tooltip>
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

            <AccessibilityDialog
                onHide={() => setShowAccessibility(false)}
                open={showAccessibility}
            />
            <HelpDialog
                onHide={() => setShowHelp(false)}
                open={showHelp}
            />
        </>
    );
}
