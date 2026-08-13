import React from 'react';
import {Box, Container, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {Logo} from '../../components/logo/logo';
import {useAppSelector} from '../../hooks/use-app-selector';
import Accessibility from '@aivot/mui-material-symbols-400-n25-outlined/Accessibility';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showDialog} from '../../slices/app-slice';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {resolveAccessibleForeground} from '../../theming/resolve-appearance-colors';
import {ColorModePicker} from '../../components/color-mode-picker/color-mode-picker';

interface CustomerListPageHeaderProps {
}

export function CustomerListPageHeader(props: CustomerListPageHeaderProps) {
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const accessibilityDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId));

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
                            <Logo
                                width={200}
                                height={100}
                            />

                            <Box
                                sx={{
                                    ml: 4,
                                    pl: 4,
                                    borderLeft: `1px solid ${theme.palette.divider}`,
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
                                    Formularverzeichnis <br />
                                    {AppConfig.providerName}
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
                            {
                                accessibilityDepartmentId != null &&
                                <Tooltip
                                    title="Informationen zur Barrierefreiheit"
                                    arrow
                                >
                                    <IconButton
                                        color="primary"
                                        onClick={() => dispatch(showDialog(AccessibilityDialogId))}
                                    >
                                        <Accessibility
                                            fontSize="large"
                                        />
                                    </IconButton>
                                </Tooltip>
                            }

                            <ColorModePicker
                                color="primary"
                                iconFontSize="large"
                            />
                        </Box>
                    </Box>
                </Container>
            </Box>
        </Box>
    );
}
