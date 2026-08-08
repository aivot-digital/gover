import {Box, Button, Dialog, IconButton, Link, Stack, Typography, useTheme} from '@mui/material';
import CloseIcon from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import CodeIcon from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import HistoryIcon from '@aivot/mui-material-symbols-400-n25-outlined/History';
import ApiIcon from '@aivot/mui-material-symbols-400-n25-outlined/Api';
import React from 'react';
import {AppInfo} from '../../../app-info';
import {createApiPath} from '../../../utils/url-path-utils';
import {DebugInformationDialog} from '../../../dialogs/debug-information-dialog/debug-information-dialog';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import {getOverlayAlpha, lighten} from '@mui/material/styles';

interface AboutProsunaDialogProps {
    open: boolean;
    onClose: () => void;
}

const dialogPaperElevation = 1;
const resourceButtonSx = {
    width: {xs: '100%', sm: 'auto'},
    color: 'text.primary',
    borderColor: 'divider',
    '&:hover': {
        borderColor: 'text.secondary',
        backgroundColor: 'action.hover',
    },
};

export function AboutProsunaDialog({ open, onClose }: AboutProsunaDialogProps): React.ReactElement {
    const theme = useTheme();
    // Match the SVG edge to MUI's elevation overlay on dark paper surfaces.
    const dialogSurfaceColor = theme.palette.mode === 'dark'
        ? lighten(theme.palette.background.paper, getOverlayAlpha(dialogPaperElevation))
        : theme.palette.background.paper;
    const [
        isDebugInformationDialogOpen,
        setDebugInformationDialogOpen,
    ] = React.useState(false);

    const handleClose = (): void => {
        setDebugInformationDialogOpen(false);
        onClose();
    };

    return (
        <>
            <Dialog
                open={open}
                onClose={handleClose}
                maxWidth="sm"
                fullWidth
                PaperProps={{
                    elevation: dialogPaperElevation,
                    sx: {
                        borderRadius: 3,
                        overflow: 'hidden',
                        boxShadow: 6,
                    },
                }}
            >
                <Box
                    sx={{
                        position: 'relative',
                        height: 280,
                        backgroundImage: 'url("/staff/assets/images/about-prosuna-bg.jpg")',
                        backgroundSize: 'cover',
                        backgroundPosition: 'center',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                    }}
                >
                    <Box
                        component="img"
                        src="/staff/assets/images/about-prosuna-logo.svg"
                        alt="Prosuna Logo"
                        sx={{
                            width: 360, height: 'auto', zIndex: 2, mb: 3.5,
                        }}
                    />

                    <IconButton
                        onClick={handleClose}
                        sx={{
                            position: 'absolute',
                            top: 8,
                            right: 8,
                            color: 'white',
                        }}
                    >
                        <CloseIcon />
                    </IconButton>

                    <Box
                        component="svg"
                        viewBox="0 0 500 40"
                        xmlns="http://www.w3.org/2000/svg"
                        preserveAspectRatio="none"
                        sx={{
                            position: 'absolute',
                            bottom: -1,
                            left: 0,
                            width: '100%',
                            height: 40,
                        }}
                    >
                        <path
                            d="M0,20 Q250,40 500,20 L500,40 L0,40 Z"
                            fill={dialogSurfaceColor}
                        />
                    </Box>

                </Box>

                <Box sx={{
                    p: 4, textAlign: 'center',
                }}>
                    <Typography variant="h2" fontWeight={600} sx={{ mb: 2 }}>
                        Über Prosuna Version {AppInfo.version === '@buildVersion' ? '5.x (DEV)' : AppInfo.version}
                    </Typography>

                    <Typography
                        variant="body1"
                        sx={{
                            color: theme.palette.text.secondary, mb: 2,
                        }}
                    >
                        Prosuna ist die Plattform für Organisationen, die Verwaltungsprozesse durchgängig und nachvollziehbar digital abbilden möchten. Sie verbindet Formulare, Prozessmodellierung, automatisierte und manuelle Bearbeitungsschritte, die Einbeziehung von externen Beteiligten und Anschlussfähigkeit an bestehende IT-Landschaften. So wird Prosuna zum Dreh- und Angelpunkt für digitale Vorgänge innerhalb der Organisation.
                    </Typography>
                    <Typography
                        variant="body1"
                        sx={{
                            color: theme.palette.text.secondary, mb: 4,
                        }}
                    >
                        Entwickelt von{' '}
                        <Link
                            href="https://aivot.de/"
                            target="_blank"
                            rel="noopener noreferrer"
                            color="inherit"
                        >
                            Aivot
                        </Link>{' '}
                        und weiteren{' '}
                        <abbr
                            title={[
                                'Kontributoren im Kontext von Open Source sind Einzelpersonen und Organisationen,',
                                'die an frei verfügbaren Softwareprojekten mitwirken, indem sie Code schreiben,',
                                'Fehler melden, die Dokumentation verbessern oder die Community unterstützen.',
                            ].join(' ')}
                        >
                            Kontributoren
                        </abbr>{' '}
                        in Deutschland.
                    </Typography>

                    <Stack
                        direction="row"
                        justifyContent="center"
                        sx={{mb: 3.5}}
                    >
                        <Button
                            variant="contained"
                            color="primary"
                            href="https://prosuna.de"
                            target="_blank"
                            rel="noopener noreferrer"
                            endIcon={<OpenInNewIcon />}
                        >
                            Mehr über Prosuna
                        </Button>
                    </Stack>

                    <Typography
                        component="h3"
                        variant="caption"
                        sx={{
                            display: 'block',
                            mb: 1.25,
                            color: 'text.secondary',
                            fontWeight: 600,
                        }}
                    >
                        Weiterführende Links
                    </Typography>

                    <Stack
                        direction={{xs: 'column', sm: 'row'}}
                        spacing={2}
                        useFlexGap
                        justifyContent="center"
                        flexWrap="wrap"
                        sx={{mb: 1.5}}
                    >
                        <Button
                            variant="outlined"
                            color="inherit"
                            href="https://github.com/aivot-digital/gover"
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<CodeIcon />}
                            sx={resourceButtonSx}
                        >
                            Quellcode
                        </Button>
                        <Button
                            variant="outlined"
                            color="inherit"
                            href="https://github.com/aivot-digital/gover/releases"
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<HistoryIcon />}
                            sx={resourceButtonSx}
                        >
                            Versionshinweise
                        </Button>
                        <Button
                            variant="outlined"
                            color="inherit"
                            href={createApiPath('/api/public/docs/swagger.html')}
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<ApiIcon />}
                            sx={resourceButtonSx}
                        >
                            API-Dokumentation
                        </Button>
                    </Stack>

                    <Stack direction="row" justifyContent="center">
                        <Button
                            variant="text"
                            color="inherit"
                            startIcon={<BugReport />}
                            onClick={() => {
                                setDebugInformationDialogOpen(true);
                            }}
                            sx={{
                                color: 'text.secondary',
                                '&:hover': {
                                    color: 'text.primary',
                                    backgroundColor: 'action.hover',
                                },
                            }}
                        >
                            Debug-Informationen
                        </Button>
                    </Stack>
                </Box>
            </Dialog>
            <DebugInformationDialog
                open={isDebugInformationDialogOpen}
                onClose={() => {
                    setDebugInformationDialogOpen(false);
                }}
            />
        </>
    );
}
