import React, {type ReactNode, useEffect, useState} from 'react';
import {
    Alert,
    Box,
    Button,
    ButtonBase,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    Stack,
    type SxProps,
    type Theme,
    Typography
} from '@mui/material';
import {Chip as PsChip} from '../../../components/chip/chip';
import Assignment from '@aivot/mui-material-symbols-400-n25-outlined/Assignment';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';
import {type SvgIconComponent} from '../../../types/svg-icon-component';
import {MarkdownContent} from '../../../components/markdown-content/markdown-content';
import {KnownProviderIcons} from '../data/known-provider-icons';
import {ProviderTypeStyles} from '../data/provider-type-styles';
import {
    ProcessNodeExecutionTypeColors, ProcessNodeExecutionTypeIcons,
    ProcessNodeExecutionTypeLabels,
    type ProcessNodeProvider
} from '../services/process-node-provider-api-service';
import {ProcessNodeOutputCard} from './process-node-output-card';
import {DocumentationLink} from '../../../components/documentation-link/documentation-link';
import {PluginInfoDialog} from '../../../dialogs/plugin-info-dialog/plugin-info-dialog';
import {Permission} from '../../../data/permissions/permission';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {type PluginDTO, PluginsApiService} from '../../../services/plugins-api-service';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {ProcessNodeOutputTypeDialog} from './process-node-output-type-dialog';

const CORE_PLUGIN_KEY = 'de.aivot.core';

type PluginDetailsLoadState =
    | {status: 'idle'}
    | {status: 'loading'}
    | {status: 'loaded'; plugin: PluginDTO}
    | {status: 'error'};

interface ProcessNodeProviderDetailsDialogProps {
    open: boolean;
    provider: ProcessNodeProvider | null;
    onClose: () => void;
}

interface ProcessNodeProviderDetailsHeaderProps {
    provider: ProcessNodeProvider;
    sx?: SxProps<Theme>;
}

interface ProcessNodeProviderDetailsContentProps {
    provider: ProcessNodeProvider;
    showDescription?: boolean;
    sx?: SxProps<Theme>;
}

interface ProcessNodeProviderDetailsSectionProps {
    title: string;
    children: ReactNode;
}

interface ProcessNodeProviderDetailsRowProps {
    label: string;
    value: string;
    monospace?: boolean;
}

interface ProcessNodeProviderDetailsListRowProps {
    primary: string;
    secondary: string;
}

export function getProcessNodeProviderIcon(provider: ProcessNodeProvider): SvgIconComponent {
    return KnownProviderIcons[provider.componentKey] ?? KnownProviderIcons[provider.key] ?? Assignment;
}

export function ProcessNodeProviderDetailsDialog(props: ProcessNodeProviderDetailsDialogProps): ReactNode {
    const {
        open,
        provider,
        onClose,
    } = props;
    const renderProvider = useRetainedDialogValue(open, provider);

    return (
        <Dialog
            open={open && renderProvider != null}
            onClose={onClose}
            fullWidth
            maxWidth="sm"
        >
            <DialogTitleWithClose onClose={onClose}>
                Informationen zum Prozesselement
            </DialogTitleWithClose>

            {
                renderProvider != null &&
                <DialogContent
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2.5,
                    }}
                >
                    <ProcessNodeProviderDetailsHeader provider={renderProvider}/>

                    <ProcessNodeProviderDetailsContent
                        provider={renderProvider}
                        showDescription
                    />
                </DialogContent>
            }

            <DialogActions
                sx={{
                    justifyContent: 'flex-end',
                }}
            >
                <Button onClick={onClose}>
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

export function ProcessNodeProviderDetailsHeader(props: ProcessNodeProviderDetailsHeaderProps): ReactNode {
    const {
        provider,
        sx,
    } = props;
    const typeStyle = ProviderTypeStyles[provider.type];
    const ProviderIcon = getProcessNodeProviderIcon(provider);
    const isDeprecated = isStringNotNullOrEmpty(provider.deprecationNotice);

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 2,
                minWidth: 0,
                ...sx,
            }}
        >
            <Box
                sx={{
                    width: 38,
                    height: 38,
                    minWidth: 38,
                    minHeight: 38,
                    aspectRatio: '1 / 1',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderRadius: '50%',
                    flexShrink: 0,
                    backgroundColor: typeStyle.bgColor,
                    color: typeStyle.textColor,
                }}
            >
                <ProviderIcon sx={{fontSize: 20}}/>
            </Box>

            <Box sx={{minWidth: 0, flex: 1}}>
                <Typography
                    variant="caption"
                    sx={{
                        display: 'block',
                        lineHeight: 1.2,
                        mt: 0.5,
                    }}
                >
                    {typeStyle.label}
                </Typography>

                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        minWidth: 0,
                        flexWrap: 'wrap',
                    }}
                >
                    <Typography
                        variant="h6"
                        title={provider.name}
                        sx={{
                            lineHeight: 1.2,
                            flex: 1,
                            minWidth: 0,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap'
                        }}>
                        {provider.name}
                    </Typography>
                    <Chip
                        size="small"
                        label={`Version ${provider.componentVersion}`}
                        sx={{flexShrink: 0}}
                    />
                    {
                        isDeprecated &&
                        <Chip
                            size="small"
                            label="Abgekündigt"
                            color="warning"
                            variant="outlined"
                            sx={{flexShrink: 0}}
                        />
                    }
                </Box>
            </Box>
        </Box>
    );
}

export function ProcessNodeProviderDetailsContent(props: ProcessNodeProviderDetailsContentProps): ReactNode {
    const {
        provider,
        showDescription = false,
        sx,
    } = props;
    const canReadPlugins = useHasSystemPermission(Permission.PLUGIN_READ);
    const [pluginDetails, setPluginDetails] = useState<PluginDetailsLoadState>({status: 'idle'});
    const [pluginReloadCounter, setPluginReloadCounter] = useState(0);
    const [pluginInfoDialogOpen, setPluginInfoDialogOpen] = useState(false);
    const [typeDialogOutput, setTypeDialogOutput] = useState<ProcessNodeProvider['outputs'][number] | null>(null);
    const typeStyle = ProviderTypeStyles[provider.type];
    const isDeprecated = isStringNotNullOrEmpty(provider.deprecationNotice);

    useEffect(() => {
        setPluginInfoDialogOpen(false);

        if (!canReadPlugins) {
            setPluginDetails({status: 'idle'});
            return;
        }

        const abortController = new AbortController();
        setPluginDetails({status: 'loading'});

        new PluginsApiService()
            .getPlugin(provider.parentPluginKey, {abort: abortController.signal})
            .then((plugin) => {
                if (!abortController.signal.aborted) {
                    setPluginDetails({status: 'loaded', plugin});
                }
            })
            .catch((error: unknown) => {
                if (!abortController.signal.aborted) {
                    console.error(error);
                    setPluginDetails({status: 'error'});
                }
            });

        return () => {
            abortController.abort();
        };
    }, [
        canReadPlugins,
        pluginReloadCounter,
        provider.parentPluginKey,
    ]);

    useEffect(() => {
        setTypeDialogOutput(null);
    }, [provider.componentVersion, provider.key, provider.majorVersion]);

    const loadedPlugin = pluginDetails.status === 'loaded' && pluginDetails.plugin.key === provider.parentPluginKey ?
        pluginDetails.plugin :
        null;

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                gap: 2.5,
                ...sx,
            }}
        >
            {
                showDescription &&
                <MarkdownContent
                    markdown={provider.description}
                    sx={{
                        typography: 'body2',
                        color: 'text.secondary',
                    }}
                />
            }

            <Stack
                direction="row"
                spacing={1}
                useFlexGap
                sx={{
                    justifyContent: 'flex-start',
                    flexWrap: 'wrap',
                }}
            >
                {
                    provider
                        .executionTypes
                        .map((executionType) => {
                            const Icon = ProcessNodeExecutionTypeIcons[executionType];
                            return (
                                <PsChip
                                    key={executionType}
                                    mode="soft"
                                    label={ProcessNodeExecutionTypeLabels[executionType]}
                                    color={ProcessNodeExecutionTypeColors[executionType] as any}
                                    icon={<Icon fontSize="small" />}
                                />
                            )
                        })
                }
            </Stack>

            <DocumentationLink
                url={provider.documentationUrl}
                sx={{alignSelf: 'flex-start'}}
            />

            {
                isDeprecated &&
                <Alert severity="warning">
                    <MarkdownContent
                        markdown={provider.deprecationNotice}
                        sx={{typography: 'body2'}}
                    />
                </Alert>
            }

            <ProcessNodeProviderDetailsSection title="Allgemein">
                <ProcessNodeProviderDetailsRow
                    label="Eindeutiger Schlüssel"
                    value={provider.key}
                    monospace
                />
                <ProcessNodeProviderDetailsRow label="Typ" value={typeStyle.label}/>
            </ProcessNodeProviderDetailsSection>

            <ProcessNodeProviderDetailsSection title="Herkunft">
                <ProcessNodePluginReference
                    pluginKey={provider.parentPluginKey}
                    canReadPlugins={canReadPlugins}
                    loadState={pluginDetails}
                    plugin={loadedPlugin}
                    onOpen={() => {
                        setPluginInfoDialogOpen(true);
                    }}
                    onRetry={() => {
                        setPluginReloadCounter((value) => value + 1);
                    }}
                />
            </ProcessNodeProviderDetailsSection>

            <ProcessNodeProviderDetailsSection title="Ausgänge">
                {
                    provider.ports.length > 0 ?
                        provider.ports.map((port) => (
                            <ProcessNodeProviderDetailsListRow
                                key={port.key}
                                primary={port.label}
                                secondary={port.description}
                            />
                        )) :
                        <Typography variant="body2" sx={{
                            color: "text.secondary"
                        }}>
                            Dieses Prozesselement besitzt keine Ausgangsports.
                        </Typography>
                }
            </ProcessNodeProviderDetailsSection>

            <ProcessNodeProviderDetailsSection title="Ausgangsdaten">
                {
                    provider.outputs.length > 0 ?
                        provider.outputs.map((output) => (
                            <ProcessNodeOutputCard
                                key={output.key}
                                label={output.label}
                                outputKey={output.key}
                                description={output.description}
                                onShowTypeDefinition={() => {
                                    setTypeDialogOutput(output);
                                }}
                            />
                        )) :
                        <Typography variant="body2" sx={{
                            color: "text.secondary"
                        }}>
                            Dieses Prozesselement erzeugt keine zusätzlichen Ausgangsdaten.
                        </Typography>
                }
            </ProcessNodeProviderDetailsSection>

            <PluginInfoDialog
                open={pluginInfoDialogOpen}
                plugin={loadedPlugin}
                onClose={() => {
                    setPluginInfoDialogOpen(false);
                }}
            />

            <ProcessNodeOutputTypeDialog
                open={typeDialogOutput != null}
                output={typeDialogOutput}
                onClose={() => {
                    setTypeDialogOutput(null);
                }}
            />
        </Box>
    );
}

function ProcessNodeProviderDetailsSection(props: ProcessNodeProviderDetailsSectionProps): ReactNode {
    return (
        <Box>
            <Typography
                variant="subtitle2"
                sx={{
                    mb: 1.25,
                    fontWeight: 700,
                }}
            >
                {props.title}
            </Typography>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1.25,
                }}
            >
                {props.children}
            </Box>
        </Box>
    );
}

function ProcessNodeProviderDetailsRow(props: ProcessNodeProviderDetailsRowProps): ReactNode {
    return (
        <Box
            sx={{
                py: 0.25,
            }}
        >
            <Typography variant="caption" sx={{
                color: "text.secondary"
            }}>
                {props.label}
            </Typography>
            <Typography
                variant="body2"
                sx={{
                    mt: 0.25,
                    fontFamily: props.monospace ? 'monospace' : undefined,
                    overflowWrap: 'anywhere',
                }}
            >
                {props.value}
            </Typography>
        </Box>
    );
}

interface ProcessNodePluginReferenceProps {
    pluginKey: string;
    canReadPlugins: boolean;
    loadState: PluginDetailsLoadState;
    plugin: PluginDTO | null;
    onOpen: () => void;
    onRetry: () => void;
}

function ProcessNodePluginReference(props: ProcessNodePluginReferenceProps): ReactNode {
    const originLabel = props.pluginKey === CORE_PLUGIN_KEY ? 'Standardumfang' : 'Plugin';

    return (
        <Box
            sx={{
                p: 1.5,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1.5,
            }}
        >
            <Stack
                direction="row"
                spacing={1}
                useFlexGap
                sx={{
                    alignItems: 'center',
                    flexWrap: 'wrap',
                }}
            >
                <Chip
                    size="small"
                    label={originLabel}
                    color={props.pluginKey === CORE_PLUGIN_KEY ? 'default' : 'primary'}
                    variant="outlined"
                />
                <Typography
                    variant="body2"
                    sx={{
                        color: 'text.secondary',
                        fontFamily: 'monospace',
                        overflowWrap: 'anywhere',
                    }}
                >
                    {props.pluginKey}
                </Typography>
            </Stack>

            {
                props.plugin != null &&
                <ButtonBase
                    focusRipple
                    onClick={props.onOpen}
                    aria-label={`Plugin-Informationen zu ${props.plugin.name} anzeigen`}
                    aria-haspopup="dialog"
                    sx={{
                        display: 'block',
                        width: '100%',
                        mt: 1.25,
                        borderRadius: 1,
                        textAlign: 'left',
                        p: 0.75,
                        mx: -0.75,
                        '&:hover': {
                            backgroundColor: 'action.hover',
                        },
                    }}
                >
                    <Typography variant="body2" sx={{fontWeight: 600, color: 'primary.main'}}>
                        {props.plugin.name}
                    </Typography>
                    <Typography variant="caption" sx={{display: 'block', color: 'text.secondary'}}>
                        {props.plugin.vendorName}
                    </Typography>
                </ButtonBase>
            }

            {
                props.canReadPlugins && props.loadState.status === 'loading' &&
                <Stack
                    direction="row"
                    spacing={1}
                    sx={{
                        alignItems: 'center',
                        mt: 1.25,
                        color: 'text.secondary',
                    }}
                >
                    <CircularProgress size={16} aria-label="Plugin-Informationen werden geladen"/>
                    <Typography variant="caption">
                        Plugin-Informationen werden geladen …
                    </Typography>
                </Stack>
            }

            {
                props.canReadPlugins && props.loadState.status === 'error' &&
                <Box sx={{mt: 1.25}}>
                    <Typography variant="caption" sx={{color: 'text.secondary'}}>
                        Die Plugin-Informationen konnten nicht geladen werden.
                    </Typography>
                    <Button
                        size="small"
                        onClick={props.onRetry}
                        sx={{
                            display: 'block',
                            mt: 0.5,
                            ml: -1,
                        }}
                    >
                        Erneut versuchen
                    </Button>
                </Box>
            }
        </Box>
    );
}

function ProcessNodeProviderDetailsListRow(props: ProcessNodeProviderDetailsListRowProps): ReactNode {
    return (
        <Box
            sx={{
                p: 1.5,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1.5,
            }}
        >
            <Typography variant="body2" sx={{
                fontWeight: 600
            }}>
                {props.primary}
            </Typography>
            <Typography
                variant="body2"
                sx={{
                    color: "text.secondary",
                    mt: 0.5
                }}>
                {props.secondary}
            </Typography>
        </Box>
    );
}
