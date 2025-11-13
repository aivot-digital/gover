import React from 'react';
import {Box, Button, Container, type SxProps, Typography, useTheme} from '@mui/material';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {Logo} from '../../components/logo/logo';
import {selectSetup} from '../../slices/shell-slice';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';

const buttonStyle: SxProps = {
    color: '#16191F',
    textTransform: 'none',
    ml: 1,
    fontSize: '1.225rem',
};

interface CustomerListPageFooterProps {
}

export function CustomerListPageFooter(props: CustomerListPageFooterProps) {
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const setup = useAppSelector(selectSetup);

    const imprintDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.imprintDepartmentId));
    const privacyDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.privacyDepartmentId));

    return (
        <Box
            component="footer"
            role="contentinfo"
            sx={{
                boxShadow: 'inset 0px 10px 20px rgba(0, 0, 0, 0.06)',
            }}
        >
            <Container>
                <Box
                    sx={{
                        display: 'flex',
                        pt: 8,
                        pb: 10,
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        [theme.breakpoints.down('md')]: {
                            flexDirection: 'column',
                            alignItems: 'flex-start',
                            pt: 4,
                            pb: 7,
                        },
                    }}
                >
                    <Logo
                        width={200}
                        height={100}
                    />

                    <Box
                        component="nav"
                        role="navigation"
                        aria-label="Rechtliche Informationen"
                        sx={{
                            [theme.breakpoints.down('md')]: {
                                ml: -2,
                            },
                        }}
                    >
                        <Box
                            sx={{
                                mb: 1,
                                display: 'flex',
                                justifyContent: 'flex-end',
                                flexWrap: 'wrap',
                                [theme.breakpoints.down('md')]: {
                                    mt: 2,
                                    justifyContent: 'flex-start',
                                },
                            }}
                        >
                            {
                                privacyDepartmentId != null &&
                                <Button
                                    sx={buttonStyle}
                                    size="medium"
                                    onClick={() => dispatch(showDialog(PrivacyDialogId))}
                                >
                                    Datenschutz
                                </Button>
                            }

                            {
                                imprintDepartmentId != null &&
                                <Button
                                    sx={buttonStyle}
                                    size="medium"
                                    onClick={() => dispatch(showDialog(ImprintDialogId))}
                                >
                                    Impressum
                                </Button>
                            }
                        </Box>

                        <Box>
                            <Typography
                                component={'p'}
                                variant="h6"
                                sx={{
                                    color: 'rgba(0, 0, 0, 0.55)',
                                    mt: 2,
                                    ml: 2.5,
                                    textAlign: 'left',
                                    [theme.breakpoints.up('md')]: {
                                        textAlign: 'right',
                                        mt: 0,
                                        mr: 1,
                                        ml: 0,
                                    },
                                }}
                            >
                                {setup?.providerName} &bull; Alle Rechte vorbehalten.
                            </Typography>
                        </Box>
                    </Box>
                </Box>
            </Container>

            <Box
                sx={{
                    backgroundColor: '#F2F2F2',
                    p: 0.5,
                    px: 2,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'center',
                    },
                }}
            >
                <Typography
                    variant="caption"
                    color="#444444"
                >
                    Dieses Angebot wurde umgesetzt mit Gover – dem Fundament für moderne digitale Verwaltungsleistungen von Aivot
                </Typography>
            </Box>
        </Box>
    );
}
