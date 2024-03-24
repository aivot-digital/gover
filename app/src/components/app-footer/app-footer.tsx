import React from 'react';
import {Box, Button, Container, type SxProps, Typography, useTheme} from '@mui/material';
import {type AppFooterProps} from './app-footer-props';
import {AppMode} from '../../data/app-mode';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {Logo} from '../logo/logo';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';

const buttonStyle: SxProps = {
    color: '#16191F',
    textTransform: 'none',
    ml: 1,
    fontSize: '1.3125rem',
};

export function AppFooter({mode}: AppFooterProps): JSX.Element {
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const name = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    return (
        <Box
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
                        alignItems: 'flex-start',
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

                    <Box sx={{
                        [theme.breakpoints.down('md')]: {
                            ml: -2
                        },
                    }}>
                        {
                            mode === AppMode.Customer &&
                            <Box sx={{
                                mb: 1,
                                [theme.breakpoints.down('md')]: {
                                    mt: 2,
                                },
                            }}>
                                <Button
                                    startIcon={
                                        <HelpOutlineOutlinedIcon/>
                                    }
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showDialog(HelpDialogId))}
                                >
                                    Hilfe
                                </Button>

                                <Button
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showDialog(PrivacyDialogId))}
                                >
                                    Datenschutz
                                </Button>

                                <Button
                                    sx={buttonStyle}
                                    size="large"
                                    onClick={() => dispatch(showDialog(ImprintDialogId))}
                                >
                                    Impressum
                                </Button>
                            </Box>
                        }
                        <Box>
                            <Typography
                                variant="h6"
                                sx={{
                                    opacity: 0.5,
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
                                {name}
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
                    {
                        mode === AppMode.Staff ?
                            'Das Online-Antrags-Management wird umgesetzt mit Gover – der intelligenten Software-Plattform zur Ende-zu-Ende Digitalisierung von Antragsprozessen.' :
                            (
                                mode === AppMode.Customer ?
                                    'Dieses Formular wurde umgesetzt mit Gover – dem Fundament für moderne digitale Verwaltungsleistungen von Aivot' :
                                    'Dieses Angebot wurde umgesetzt mit Gover – dem Fundament für moderne digitale Verwaltungsleistungen von Aivot'
                            )
                    }
                </Typography>
            </Box>
        </Box>
    );
}
