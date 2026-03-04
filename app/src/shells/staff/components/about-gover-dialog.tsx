import {Box, Button, Dialog, IconButton, Stack, Typography, useTheme} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import CodeIcon from '@mui/icons-material/Code';
import ListAltIcon from '@mui/icons-material/ListAlt';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';
import React from 'react';
import {AppInfo} from '../../../app-info';
import {createApiPath} from '../../../utils/url-path-utils';
import {DebugInformationDialog} from '../../../dialogs/debug-information-dialog/debug-information-dialog';
import BugReport from '@aivot/mui-material-symbols-400-outlined/dist/bug-report/BugReport';

interface AboutGoverDialogProps {
    open: boolean;
    onClose: () => void;
}

export function AboutGoverDialog({ open, onClose }: AboutGoverDialogProps) {
    const theme = useTheme();
    const [isDebugInformationDialogOpen, setDebugInformationDialogOpen] = React.useState(false);

    const handleClose = () => {
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
                    sx: {
                        borderRadius: 3,
                        overflow: "hidden",
                        boxShadow: 6,
                    },
                }}
            >
                <Box
                    sx={{
                        position: "relative",
                        height: 280,
                        backgroundImage: 'url("/staff/assets/images/about-gover-bg.jpg")',
                        backgroundSize: "cover",
                        backgroundPosition: "center",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        color: "#fff",
                    }}
                >
                    <Box
                        component="img"
                        src="/staff/assets/images/about-gover-logo.svg"
                        alt="Gover Logo"
                        sx={{ width: 250, height: "auto", zIndex: 2, mb: 3.5 }}
                    />

                    <IconButton
                        onClick={handleClose}
                        sx={{
                            position: "absolute",
                            top: 8,
                            right: 8,
                            color: "white",
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
                            fill="#fff"
                        />
                    </Box>

                </Box>

                <Box sx={{ p: 4, textAlign: "center" }}>
                    <Typography variant="h2" fontWeight={600} sx={{ mb: 2 }}>
                        Über Gover Version {AppInfo.version === '@buildVersion' ? '5.x (DEV)' : AppInfo.version}
                    </Typography>

                    <Typography
                        variant="body1"
                        sx={{ color: theme.palette.text.secondary, mb: 4 }}
                    >
                        Gover ist die quelloffene Software-Plattform für Ende-zu-Ende digitalisierte Antragsprozesse.
                        Einfach bedienbar und flexibel einsetzbar für Verwaltungen jeder Größe.
                        Entwickelt von Aivot und{' '}
                        <abbr title={'Kontributoren im Kontext von Open Source sind Einzelpersonen und Organisationen, die an frei verfügbaren Softwareprojekten mitwirken, indem sie Code schreiben, Fehler melden, die Dokumentation verbessern oder die Community unterstützen.'}>
                            Kontributoren
                        </abbr>{' '}
                        in Deutschland für die deutsche Verwaltung.
                    </Typography>

                    <Stack
                        direction="row"
                        spacing={2}
                        useFlexGap
                        justifyContent="center"
                        flexWrap="wrap"
                        sx={{ mb: 2 }}
                    >
                        <Button
                            variant="contained"
                            color="primary"
                            href="https://www.gover.digital"
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<OpenInNewIcon />}
                        >
                            Mehr erfahren
                        </Button>
                    </Stack>

                    <Stack
                        direction="row"
                        spacing={2}
                        useFlexGap
                        justifyContent="center"
                        flexWrap="wrap"
                        sx={{mb: 2}}
                    >
                        <Button
                            variant="outlined"
                            href="https://github.com/aivot-digital/gover"
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<CodeIcon />}
                        >
                            Quellcode einsehen
                        </Button>
                        <Button
                            variant="outlined"
                            href="https://github.com/aivot-digital/gover/releases"
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<ListAltIcon />}
                        >
                            Changelog ansehen
                        </Button>
                        <Button
                            variant="outlined"
                            href={createApiPath('/api/public/docs/swagger.html')}
                            target="_blank"
                            rel="noopener noreferrer"
                            startIcon={<ListAltIcon />}
                        >
                            API-Dokumentation ansehen
                        </Button>
                        <Button
                            variant="outlined"
                            startIcon={<BugReport />}
                            onClick={() => {
                                setDebugInformationDialogOpen(true);
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
