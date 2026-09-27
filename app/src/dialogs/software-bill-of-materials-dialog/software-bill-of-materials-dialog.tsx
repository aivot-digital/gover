import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogContent,
    Divider,
    Link,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Stack,
    Typography,
} from '@mui/material';
import DownloadIcon from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import KeyboardArrowDownIcon from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import React from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {
    createSbomFileUrl,
    loadSbomManifest,
    type SbomManifest,
    SbomUnavailableError,
} from '../../services/sbom-service';

interface SoftwareBillOfMaterialsDialogProps {
    open: boolean;
    onClose: () => void;
}

type SbomDialogState =
    | {status: 'loading'}
    | {status: 'available', manifest: SbomManifest}
    | {status: 'unavailable'}
    | {status: 'error'};

interface SbomDownload {
    label: string;
    path: string;
}

interface MetadataEntryProps {
    label: string;
    children: React.ReactNode;
    wide?: boolean;
}

function formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    const formattedDate = new Intl.DateTimeFormat('de-DE', {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date);

    return `${formattedDate} Uhr`;
}

function getCommitUrl(commit: string): string | undefined {
    if (!/^[0-9a-f]{7,40}$/i.test(commit)) {
        return undefined;
    }

    return `https://github.com/aivot-digital/gover/commit/${commit}`;
}

function MetadataEntry(
    {
        label,
        children,
        wide = false,
    }: MetadataEntryProps,
): React.ReactElement {
    return (
        <Box sx={{
            gridColumn: wide ? '1 / -1' : undefined,
            minWidth: 0,
        }}>
            <Typography
                component="dt"
                variant="caption"
                sx={{
                    color: "text.secondary",
                    mb: 0.25,
                    fontWeight: 600
                }}>
                {label}
            </Typography>
            <Typography
                component="dd"
                variant="body1"
                sx={{
                    m: 0,
                    overflowWrap: 'anywhere',
                }}
            >
                {children}
            </Typography>
        </Box>
    );
}

export function SoftwareBillOfMaterialsDialog(
    {open, onClose}: SoftwareBillOfMaterialsDialogProps,
): React.ReactElement {
    const [
        state,
        setState,
    ] = React.useState<SbomDialogState>({status: 'loading'});
    const [
        retryCount,
        setRetryCount,
    ] = React.useState(0);
    const [
        moreFilesAnchor,
        setMoreFilesAnchor,
    ] = React.useState<HTMLElement | null>(null);

    React.useEffect(() => {
        if (!open) {
            setState({status: 'loading'});
            setMoreFilesAnchor(null);
            return;
        }

        const abortController = new AbortController();
        setState({status: 'loading'});

        loadSbomManifest(abortController.signal)
            .then((manifest) => {
                if (abortController.signal.aborted) {
                    return;
                }
                setState({
                    status: 'available',
                    manifest,
                });
            })
            .catch((error: unknown) => {
                if (abortController.signal.aborted) {
                    return;
                }
                if (error instanceof SbomUnavailableError) {
                    setState({status: 'unavailable'});
                } else {
                    console.error(error);
                    setState({status: 'error'});
                }
            });

        return () => {
            abortController.abort();
        };
    }, [
        open,
        retryCount,
    ]);

    const handleClose = (): void => {
        setMoreFilesAnchor(null);
        onClose();
    };

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            maxWidth="sm"
            fullWidth
            scroll="paper"
        >
            <DialogTitleWithClose onClose={handleClose}>
                Software Bill of Materials (SBOM)
            </DialogTitleWithClose>
            <DialogContent sx={{
                pt: 0.5,
                pb: 3,
            }}>
                {state.status === 'loading' &&
                    <Box sx={{
                        minHeight: 220, display: 'grid', placeItems: 'center',
                    }}>
                        <CircularProgress aria-label="SBOM wird geladen" size={32}/>
                    </Box>
                }

                {state.status === 'unavailable' &&
                    <Alert severity="info">
                        Für diesen Entwicklungsbuild wurde keine Software Bill of Materials erzeugt.
                    </Alert>
                }

                {state.status === 'error' &&
                    <Alert
                        severity="error"
                        action={
                            <Button
                                color="inherit"
                                size="small"
                                onClick={() => {
                                    setRetryCount((value) => value + 1);
                                }}
                            >
                                Erneut versuchen
                            </Button>
                        }
                    >
                        Die Software Bill of Materials konnte nicht geladen werden.
                    </Alert>
                }

                {state.status === 'available' &&
                    <SbomContent
                        manifest={state.manifest}
                        moreFilesAnchor={moreFilesAnchor}
                        onOpenMoreFiles={(anchor) => {
                            setMoreFilesAnchor(anchor);
                        }}
                        onCloseMoreFiles={() => {
                            setMoreFilesAnchor(null);
                        }}
                    />
                }
            </DialogContent>
        </Dialog>
    );
}

interface SbomContentProps {
    manifest: SbomManifest;
    moreFilesAnchor: HTMLElement | null;
    onOpenMoreFiles: (anchor: HTMLElement) => void;
    onCloseMoreFiles: () => void;
}

function SbomContent(props: SbomContentProps): React.ReactElement {
    const {manifest} = props;
    const commitUrl = getCommitUrl(manifest.commit);
    const licenseCsvPath = manifest.reports.find((path) => path.toLowerCase().endsWith('.csv'));
    const otherDownloads: SbomDownload[] = [
        {
            label: 'Frontend-SBOM (.json)', path: manifest.sboms.frontend,
        },
        {
            label: 'Backend-SBOM (.json)', path: manifest.sboms.backend,
        },
        {
            label: 'E-Mail-Templates-SBOM (.json)', path: manifest.sboms.mails,
        },
        ...manifest.reports
            .filter((path) => path !== licenseCsvPath)
            .map((path) => ({
                label: path.toLowerCase().endsWith('.txt') ? 'Lizenzliste (.txt)' : path,
                path,
            })),
    ];

    return (
        <>
            <Typography sx={{
                color: "text.secondary"
            }}>
                Die SBOM dokumentiert die in diesem Prosuna-Build enthaltenen Softwarekomponenten und deren Lizenzen.
            </Typography>

            <Box
                component="dl"
                sx={{
                    display: 'grid',
                    gridTemplateColumns: {
                        xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))',
                    },
                    columnGap: 4,
                    rowGap: 2,
                    my: 2.5,
                }}
            >
                <MetadataEntry label="Prosuna-Version">{manifest.version}</MetadataEntry>
                <MetadataEntry label="Build-Datum">{formatDate(manifest.buildDate)}</MetadataEntry>
                <MetadataEntry label="Format">
                    {manifest.sbomFormat} {manifest.sbomSpecVersion}
                </MetadataEntry>
                {manifest.componentCount != null &&
                    <MetadataEntry label="Komponenten">
                        {manifest.componentCount.toLocaleString('de-DE')}
                    </MetadataEntry>
                }
                <MetadataEntry label="Bereiche" wide>Frontend, Backend und E-Mail-Templates</MetadataEntry>
                <MetadataEntry label="Commit" wide>
                    {commitUrl == null ?
                        <Box component="span" sx={{fontFamily: 'monospace'}}>{manifest.commit}</Box> :
                        <Link
                            href={commitUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            sx={{
                                display: 'inline-flex', alignItems: 'center', gap: 0.5, fontFamily: 'monospace',
                            }}
                        >
                            {manifest.commit.slice(0, 12)}
                            <OpenInNewIcon sx={{fontSize: 16}}/>
                        </Link>
                    }
                </MetadataEntry>
            </Box>

            <Divider sx={{mb: 2.25}}/>

            <Typography
                variant="subtitle2"
                component="h3"
                sx={{
                    color: "text.secondary",
                    mb: 1
                }}>
                Downloads
            </Typography>
            <Stack direction={{
                xs: 'column', sm: 'row',
            }} spacing={1.25} useFlexGap>
                <Button
                    variant="contained"
                    href={createSbomFileUrl(manifest.sboms.merged)}
                    download
                    startIcon={<DownloadIcon/>}
                    sx={{
                        width: {
                            xs: '100%', sm: 'auto',
                        },
                    }}
                >
                    SBOM (JSON)
                </Button>
                {licenseCsvPath != null &&
                    <Button
                        variant="outlined"
                        href={createSbomFileUrl(licenseCsvPath)}
                        download
                        startIcon={<DownloadIcon/>}
                        sx={{
                            width: {
                                xs: '100%', sm: 'auto',
                            },
                        }}
                    >
                        Lizenzliste (CSV)
                    </Button>
                }
                <Button
                    variant="outlined"
                    endIcon={<KeyboardArrowDownIcon/>}
                    aria-haspopup="menu"
                    aria-expanded={props.moreFilesAnchor != null ? 'true' : undefined}
                    sx={{
                        width: {
                            xs: '100%', sm: 'auto',
                        },
                    }}
                    onClick={(event) => {
                        props.onOpenMoreFiles(event.currentTarget);
                    }}
                >
                    Weitere
                </Button>
            </Stack>
            <Menu
                anchorEl={props.moreFilesAnchor}
                open={props.moreFilesAnchor != null}
                onClose={props.onCloseMoreFiles}
            >
                {otherDownloads.map((download) =>
                    <MenuItem
                        key={download.path}
                        component="a"
                        href={createSbomFileUrl(download.path)}
                        download
                        onClick={props.onCloseMoreFiles}
                    >
                        <ListItemIcon><DownloadIcon/></ListItemIcon>
                        <ListItemText>{download.label}</ListItemText>
                    </MenuItem>,
                )}
            </Menu>
        </>
    );
}
