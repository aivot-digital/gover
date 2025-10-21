import React from 'react';
import {Box, Container, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {Logo} from '../../components/logo/logo';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSetup} from '../../slices/shell-slice';
import Accessibility from '@aivot/mui-material-symbols-400-outlined/dist/accessibility/Accessibility';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showDialog} from '../../slices/app-slice';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';

interface CustomerListPageHeaderProps {
}

export function CustomerListPageHeader(props: CustomerListPageHeaderProps) {
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const setup = useAppSelector(selectSetup);
    const accessibilityDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId));

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
                                <Typography
                                    variant="h1"
                                    color="primary"
                                    sx={{
                                        display: 'block',
                                        maxWidth: '640px',
                                        margin: 0,
                                    }}
                                >
                                    Online-Antrags-Management <br />
                                    {setup?.providerName}
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
                        </Box>
                    </Box>
                </Container>
            </Box>
        </Box>
    );
}


