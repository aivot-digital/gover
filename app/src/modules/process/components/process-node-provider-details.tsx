import React, {type ReactNode} from 'react';
import {Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, type SxProps, type Theme, Typography} from '@mui/material';
import Assignment from '@aivot/mui-material-symbols-400-n25-outlined/Assignment';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';
import {type SvgIconComponent} from '../../../types/svg-icon-component';
import {KnownProviderIcons} from '../data/known-provider-icons';
import {ProviderTypeStyles} from '../data/provider-type-styles';
import {type ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {ProcessNodeOutputCard} from './process-node-output-card';

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
                        label={`Version ${provider.majorVersion}`}
                        sx={{flexShrink: 0}}
                    />
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
                <Typography variant="body2" sx={{
                    color: "text.secondary"
                }}>
                    {provider.description}
                </Typography>
            }

            {
                provider.deprecationNotice != null &&
                <Alert severity="warning">
                    {provider.deprecationNotice}
                </Alert>
            }

            <ProcessNodeProviderDetailsSection title="Allgemein">
                <ProcessNodeProviderDetailsRow label="Plugin" value={provider.parentPluginKey}/>
                <ProcessNodeProviderDetailsRow label="Elementschlüssel" value={provider.key}/>
                <ProcessNodeProviderDetailsRow label="Komponente" value={provider.componentKey}/>
                <ProcessNodeProviderDetailsRow label="Komponententyp" value={provider.componentType}/>
                <ProcessNodeProviderDetailsRow label="Komponentenversion" value={provider.componentVersion}/>
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
                            />
                        )) :
                        <Typography variant="body2" sx={{
                            color: "text.secondary"
                        }}>
                            Dieses Prozesselement erzeugt keine zusätzlichen Ausgangsdaten.
                        </Typography>
                }
            </ProcessNodeProviderDetailsSection>
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
            <Typography variant="body2" sx={{mt: 0.25}}>
                {props.value}
            </Typography>
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
