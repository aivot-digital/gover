import {Box, Button, Dialog, IconButton, Link, Stack, Typography, useTheme} from '@mui/material';
import CloseIcon from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import CodeIcon from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import HistoryIcon from '@aivot/mui-material-symbols-400-n25-outlined/History';
import ApiIcon from '@aivot/mui-material-symbols-400-n25-outlined/Api';
import Inventory2Icon from '@aivot/mui-material-symbols-400-n25-outlined/Inventory2';
import React from 'react';
import {createApiPath} from '../../../utils/url-path-utils';
import {DebugInformationDialog} from '../../../dialogs/debug-information-dialog/debug-information-dialog';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import {ProsunaLogo} from '../../../components/prosuna-logo/prosuna-logo';
import {
    SoftwareBillOfMaterialsDialog,
} from '../../../dialogs/software-bill-of-materials-dialog/software-bill-of-materials-dialog';
import {getAppVersionLabel} from '../../../utils/app-info-utils';

interface AboutProsunaDialogProps {
    open: boolean;
    onClose: () => void;
}

const dialogPaperElevation = 1;
// Cut the curved edge out of the hero image so the actual Dialog Paper remains visible underneath.
const heroImageMask = 'url("data:image/svg+xml,' +
    '%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 500 280%22 preserveAspectRatio=%22none%22%3E' +
    '%3Cpath fill=%22white%22 d=%22M0 0H500V260Q250 280 0 260Z%22/%3E' +
    '%3C/svg%3E")';
const resourceButtonSx = {
    width: {xs: '100%', sm: 'auto'},
    color: 'text.primary',
    borderColor: 'divider',
    '&:hover': {
        borderColor: 'text.secondary',
        backgroundColor: 'action.hover',
    },
};
const secondaryButtonSx = {
    width: {xs: '100%', sm: 'auto'},
    color: 'text.secondary',
    '&:hover': {
        color: 'text.primary',
        backgroundColor: 'action.hover',
    },
};

export function AboutProsunaDialog({ open, onClose }: AboutProsunaDialogProps): React.ReactElement {
    const theme = useTheme();
    const [
        isDebugInformationDialogOpen,
        setDebugInformationDialogOpen,
    ] = React.useState(false);
    const [
        isSoftwareBillOfMaterialsDialogOpen,
        setSoftwareBillOfMaterialsDialogOpen,
    ] = React.useState(false);

    const handleClose = (): void => {
        setDebugInformationDialogOpen(false);
        setSoftwareBillOfMaterialsDialogOpen(false);
        onClose();
    };

    return (
        <>
            <Dialog
                open={open}
                onClose={handleClose}
                maxWidth="sm"
                fullWidth
                slotProps={{
                    paper: {
                        elevation: dialogPaperElevation,
                        sx: {
                            borderRadius: 3,
                            overflow: 'hidden',
                            boxShadow: 6,
                        },
                    }
                }}
            >
                <Box
                    sx={{
                        position: 'relative',
                        height: 280,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                    }}
                >
                    <Box
                        aria-hidden="true"
                        sx={{
                            position: 'absolute',
                            inset: 0,
                            backgroundImage: 'url("/staff/assets/images/about-prosuna-bg.jpg")',
                            backgroundSize: 'cover',
                            backgroundPosition: 'center',
                            WebkitMaskImage: heroImageMask,
                            maskImage: heroImageMask,
                            WebkitMaskSize: '100% 100%',
                            maskSize: '100% 100%',
                            WebkitMaskRepeat: 'no-repeat',
                            maskRepeat: 'no-repeat',
                        }}
                    />

                    <ProsunaLogo
                        title="Prosuna"
                        colorVariant="monochrome"
                        color="#FFFFFF"
                        style={{
                            width: 'min(360px, 80%)',
                            height: 'auto',
                            zIndex: 2,
                            marginBottom: 28,
                            filter: 'drop-shadow(0 1.5px 11px rgba(0, 0, 0, 0.15))',
                        }}
                    />

                    <IconButton
                        onClick={handleClose}
                        sx={{
                            position: 'absolute',
                            top: 8,
                            right: 8,
                            color: 'white',
                            zIndex: 2,
                        }}
                    >
                        <CloseIcon />
                    </IconButton>
                </Box>

                <Box sx={{
                    p: 4, textAlign: 'center',
                }}>
                    <Typography
                        variant="h2"
                        sx={{
                            fontWeight: 600,
                            mb: 2
                        }}>
                        Über Prosuna Version {getAppVersionLabel()}
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
                        sx={{
                            justifyContent: "center",
                            mb: 3.5
                        }}>
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
                        sx={{
                            justifyContent: "center",
                            flexWrap: "wrap",
                            mb: 1.5
                        }}>
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

                    <Stack
                        direction={{xs: 'column', sm: 'row'}}
                        spacing={0.5}
                        sx={{
                            justifyContent: "center",
                            alignItems: "center"
                        }}>
                        <Button
                            variant="text"
                            color="inherit"
                            startIcon={<Inventory2Icon />}
                            onClick={() => {
                                setSoftwareBillOfMaterialsDialogOpen(true);
                            }}
                            sx={secondaryButtonSx}
                        >
                            Software Bill of Materials (SBOM)
                        </Button>
                        <Button
                            variant="text"
                            color="inherit"
                            startIcon={<BugReport />}
                            onClick={() => {
                                setDebugInformationDialogOpen(true);
                            }}
                            sx={secondaryButtonSx}
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
            <SoftwareBillOfMaterialsDialog
                open={isSoftwareBillOfMaterialsDialogOpen}
                onClose={() => {
                    setSoftwareBillOfMaterialsDialogOpen(false);
                }}
            />
        </>
    );
}
