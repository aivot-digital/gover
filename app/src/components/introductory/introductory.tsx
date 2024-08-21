import React, {useState} from 'react';
import {Box, Container, Divider, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {useAppSelector} from '../../hooks/use-app-selector';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {AppMode} from '../../data/app-mode';
import {StorageScope, StorageService} from '../../services/storage-service';
import {StorageKey} from '../../data/storage-key';
import HighlightOffOutlinedIcon from '@mui/icons-material/HighlightOffOutlined';
import NorthWestOutlinedIcon from '@mui/icons-material/NorthWestOutlined';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';

interface IntroductoryProps {
    mode: AppMode;
}

export function Introductory({mode}: IntroductoryProps) {
    const theme = useTheme();
    const systemConfig = useAppSelector((state) => state.systemConfig);

    const [show, setShow] = useState(!StorageService.loadFlag(StorageKey.IntroDismissed));


    const onDismiss = (): void => {
        setShow(false);
        StorageService
            .storeFlag(
                StorageKey.IntroDismissed,
                true,
                StorageScope.Local,
            );
    };

    if (!show) {
        return null;
    }

    return (
        <Container>
            <Box>
                <Box
                    sx={{
                        position: 'relative',
                        px: 7,
                        py: 6,
                        mt: 5,
                        mb: 5,
                        backgroundColor: theme.palette.primary.dark,
                        borderRadius: theme.shape.borderRadius / 2,
                    }}
                >
                    {
                        mode === AppMode.Staff &&
                        <Box
                            sx={{
                                position: 'absolute',
                                right: theme.spacing(7),
                            }}
                        >
                            <Tooltip
                                title="Diese Meldung ausblenden"
                            >
                                <IconButton
                                    sx={{color: theme.palette.secondary.main}}
                                    onClick={onDismiss}
                                >
                                    <HighlightOffOutlinedIcon sx={{fontSize: '1.75rem'}} />
                                </IconButton>
                            </Tooltip>
                        </Box>
                    }

                    <Box>
                        <Typography
                            variant="h2"
                            fontSize="1.75rem"
                            color="white"
                        >
                            Herzlich willkommen!
                        </Typography>
                        <Typography
                            component={'p'}
                            variant="h2"
                            fontSize="2.5rem"
                            fontWeight={800}
                            lineHeight="2.625rem"
                            color="white"
                            sx={{
                                mt: 2,
                            }}
                        >
                            <span style={{color: theme.palette.secondary.main}}>
                                Online-Antrags-Management
                            </span><br />
                            {systemConfig[SystemConfigKeys.provider.name]}
                        </Typography>

                        {
                            mode === AppMode.Staff &&
                            <>

                                <Divider
                                    sx={{
                                        mt: 4,
                                        mb: 4,
                                        borderColor: 'var(--hw-secondary)',
                                    }}
                                />
                                <Typography
                                    variant="h4"
                                    fontSize="1.125rem"
                                    lineHeight="1.25rem"
                                    color="white"
                                    fontWeight="normal"
                                    component="a"
                                    href="https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/erste-schritte/home"
                                    target="_blank"
                                    sx={[
                                        {
                                            maxWidth: '480px',
                                            display: 'flex',
                                            justifyContent: 'flex-general-information',
                                            mt: 2,
                                            mb: 2,
                                            transition: '200ms all ease-in-out',
                                            cursor: 'pointer',
                                            textDecoration: 'none',
                                        },
                                        {
                                            '&:hover': {
                                                color: 'var(--hw-secondary)',
                                            },
                                        }]}
                                >
                                    <ArrowForwardOutlinedIcon
                                        sx={{
                                            marginRight: '6px',
                                            flexShrink: 0,
                                            fontSize: '0.9em',
                                        }}
                                    />
                                    <span>Zur Benutzereinführung</span>
                                </Typography>

                                <Typography
                                    variant="h4"
                                    fontSize="1.125rem"
                                    lineHeight="1.25rem"
                                    color="white"
                                    fontWeight="normal"
                                    component="a"
                                    href="https://wiki.teamaivot.de/dokumentation/gover/benutzerhandbuch/home"
                                    target="_blank"
                                    sx={[
                                        {
                                            maxWidth: '480px',
                                            display: 'flex',
                                            justifyContent: 'flex-general-information',
                                            transition: '200ms all ease-in-out',
                                            cursor: 'pointer',
                                            textDecoration: 'none',
                                        },
                                        {
                                            '&:hover': {
                                                color: 'var(--hw-secondary)',
                                            },
                                        }]}
                                >
                                    <NorthWestOutlinedIcon
                                        style={{
                                            marginRight: '6px',
                                            flexShrink: 0,
                                            fontSize: '0.9em',
                                        }}
                                    />
                                    <span>Alles neu – was jetzt? Die neuen Arbeitsabläufe und Prozessschritte für Online-Formulare einfach erklärt</span>
                                </Typography>
                            </>
                        }
                    </Box>
                </Box>
            </Box>
        </Container>
    );
}
