import React, {type ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import Fuse from 'fuse.js';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Button,
    Chip,
    Dialog,
    Divider,
    Tab,
    Tabs,
    Typography,
} from '@mui/material';
import Assignment from '@aivot/mui-material-symbols-400-outlined/dist/assignment/Assignment';
import ExpandMore from '@mui/icons-material/ExpandMore';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import Add from '@mui/icons-material/Add';
import {SearchInput} from '../../../components/search-input/search-input';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {KnownProviderIcons} from '../data/known-provider-icons';
import {ProviderTypeStyles} from '../data/provider-type-styles';
import {type ProcessNodeProvider, ProcessNodeType} from '../services/process-node-provider-api-service';

const PROCESS_NODE_TYPE_ORDER = [
    ProcessNodeType.Trigger,
    ProcessNodeType.Action,
    ProcessNodeType.FlowControl,
    ProcessNodeType.Termination,
] as const;

const PROCESS_NODE_TYPE_PLURAL_LABELS: Record<ProcessNodeType, string> = {
    [ProcessNodeType.Trigger]: 'Auslöser',
    [ProcessNodeType.Action]: 'Aktionen',
    [ProcessNodeType.FlowControl]: 'Flusselemente',
    [ProcessNodeType.Termination]: 'Abschlüsse',
};

const DEFAULT_EXPANDED_GROUPS: Record<ProcessNodeType, boolean> = {
    [ProcessNodeType.Trigger]: true,
    [ProcessNodeType.Action]: true,
    [ProcessNodeType.FlowControl]: true,
    [ProcessNodeType.Termination]: true,
};

interface SelectNodeProviderDialogProps {
    open: boolean;
    nodeProviders: ProcessNodeProvider[];
    onClose: () => void;
    onSelect: (provider: ProcessNodeProvider) => void;
    filter?: (provider: ProcessNodeProvider) => boolean;
    title?: string;
    primaryActionLabel?: string;
    primaryActionIcon?: ReactNode;
}

function getProviderId(provider: ProcessNodeProvider): string {
    return `${provider.key}:${provider.majorVersion}`;
}

function getFilteredNodeProviders(
    nodeProviders: ProcessNodeProvider[],
    filter?: (provider: ProcessNodeProvider) => boolean,
): ProcessNodeProvider[] {
    return filter == null ? nodeProviders : nodeProviders.filter(filter);
}

function getSearchedNodeProviders(
    nodeProviders: ProcessNodeProvider[],
    search: string,
): ProcessNodeProvider[] {
    const trimmedSearch = search.trim();
    if (trimmedSearch.length === 0) {
        return [...nodeProviders].sort((left, right) => left.name.localeCompare(right.name, 'de'));
    }

    const fuse = new Fuse(nodeProviders, {
        threshold: 0.32,
        ignoreLocation: true,
        keys: [
            {name: 'name', weight: 0.45},
            {name: 'description', weight: 0.25},
            {name: 'key', weight: 0.1},
            {name: 'componentKey', weight: 0.1},
            {name: 'parentPluginKey', weight: 0.1},
        ],
    });

    return fuse.search(trimmedSearch).map((entry) => entry.item);
}

export function SelectNodeProviderDialog(props: SelectNodeProviderDialogProps): ReactNode {
    const {
        open,
        nodeProviders,
        onClose,
        onSelect,
        filter,
        title = 'Prozesselement hinzufügen',
        primaryActionLabel = 'Hinzufügen',
        primaryActionIcon = <Add sx={{fontSize: 18}}/>,
    } = props;

    const retainedFilterRef = useRef<SelectNodeProviderDialogProps['filter']>(filter);
    const [retainedNodeProviders, setRetainedNodeProviders] = useState<ProcessNodeProvider[]>(nodeProviders);
    const [currentTab, setCurrentTab] = useState(0);
    const [search, setSearch] = useState('');
    const [selectedProviderId, setSelectedProviderId] = useState<string | null>(null);
    const [expandedGroups, setExpandedGroups] = useState<Record<ProcessNodeType, boolean>>(DEFAULT_EXPANDED_GROUPS);

    useEffect(() => {
        if (!open) {
            return;
        }

        setRetainedNodeProviders(nodeProviders);
        retainedFilterRef.current = filter;
    }, [filter, nodeProviders, open]);

    const renderNodeProviders = open ? nodeProviders : retainedNodeProviders;
    const renderFilter = open ? filter : retainedFilterRef.current;

    const filteredNodeProviders = useMemo(() => (
        getFilteredNodeProviders(renderNodeProviders, renderFilter)
    ), [renderFilter, renderNodeProviders]);

    const searchedNodeProviders = useMemo(() => (
        getSearchedNodeProviders(filteredNodeProviders, search)
    ), [filteredNodeProviders, search]);

    const groupedNodeProviders = useMemo(() => {
        const groupedProviders = new Map<ProcessNodeType, ProcessNodeProvider[]>();

        for (const type of PROCESS_NODE_TYPE_ORDER) {
            groupedProviders.set(type, []);
        }

        for (const provider of searchedNodeProviders) {
            groupedProviders.get(provider.type)?.push(provider);
        }

        return groupedProviders;
    }, [searchedNodeProviders]);
    const visibleGroupTypes = useMemo(() => (
        PROCESS_NODE_TYPE_ORDER.filter((type) => (groupedNodeProviders.get(type)?.length ?? 0) > 0)
    ), [groupedNodeProviders]);

    const selectedProvider = useMemo(() => {
        if (selectedProviderId == null) {
            return null;
        }

        return filteredNodeProviders.find((provider) => getProviderId(provider) === selectedProviderId) ?? null;
    }, [filteredNodeProviders, selectedProviderId]);

    const handleAddProvider = (provider: ProcessNodeProvider): void => {
        onSelect(provider);
        onClose();
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth={selectedProvider != null ? 'lg' : 'md'}
            TransitionProps={{
                onExited: () => {
                    setCurrentTab(0);
                    setSearch('');
                    setSelectedProviderId(null);
                    setExpandedGroups(DEFAULT_EXPANDED_GROUPS);
                },
            }}
        >
            <DialogTitleWithClose
                onClose={onClose}
            >
                {title}
            </DialogTitleWithClose>

            <Tabs
                value={currentTab}
                onChange={(_, value) => {
                    setCurrentTab(value);
                }}
                sx={{
                    px: 2,
                    mt: -1.5,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <Tab label="Elemente" value={0}/>
                <Tab label="Vorlagen" value={1} disabled/>
                <Tab label="Gover Marktplatz" value={2} disabled/>
            </Tabs>

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: selectedProvider != null ? 'minmax(0, 1.2fr) minmax(320px, 0.8fr)' : 'minmax(0, 1fr)',
                    height: 'min(74vh, 820px)',
                }}
            >
                <Box
                    sx={{
                        minWidth: 0,
                        minHeight: 0,
                        display: 'flex',
                        flexDirection: 'column',
                    }}
                >
                    <Box
                        sx={{
                            p: 2,
                            borderBottom: '1px solid',
                            borderColor: 'divider',
                        }}
                    >
                        <SearchInput
                            label="Prozesselement suchen"
                            ariaLabel="Prozesselement suchen"
                            placeholder="Name, Beschreibung oder Plugin durchsuchen"
                            value={search}
                            onChange={setSearch}
                            debounce={120}
                        />
                    </Box>

                    <Box
                        sx={{
                            flex: 1,
                            overflowY: 'auto',
                            px: 0,
                            pb: 1.5,
                        }}
                    >
                        {
                            filteredNodeProviders.length === 0 &&
                            <Box sx={{mt: 2, px: 2}}>
                                <Alert severity="info">
                                    Für diese Aktion stehen aktuell keine kompatiblen Prozesselemente zur Verfügung.
                                </Alert>
                            </Box>
                        }

                        {
                            filteredNodeProviders.length > 0 && searchedNodeProviders.length === 0 &&
                            <Box sx={{mt: 2, px: 2}}>
                                <Alert severity="info">
                                    Es wurden keine Prozesselemente gefunden, die zu Ihrer Suche passen.
                                </Alert>
                            </Box>
                        }

                        {
                            visibleGroupTypes.map((type, groupIndex) => {
                                const providersForType = groupedNodeProviders.get(type) ?? [];
                                const typeStyle = ProviderTypeStyles[type];
                                const isExpanded = search.trim().length > 0 ? true : expandedGroups[type];
                                const shouldShowExpandIcon = search.trim().length === 0;

                                return (
                                    <Accordion
                                        key={type}
                                        disableGutters
                                        expanded={isExpanded}
                                        onChange={(_, expanded) => {
                                            if (search.trim().length > 0) {
                                                return;
                                            }

                                            setExpandedGroups((previousState) => ({
                                                ...previousState,
                                                [type]: expanded,
                                            }));
                                        }}
                                        sx={{
                                            mx: 0,
                                            mb: 0,
                                            boxShadow: 'none',
                                            bgcolor: 'transparent',
                                            '&::before': {
                                                display: 'none',
                                            },
                                            '&.Mui-expanded': {
                                                mt: 0,
                                                mb: 0,
                                            },
                                        }}
                                    >
                                        <AccordionSummary
                                            expandIcon={shouldShowExpandIcon ? <ExpandMore/> : undefined}
                                            sx={{
                                                px: 2,
                                                minHeight: 56,
                                                bgcolor: 'rgba(15, 23, 42, 0.035)',
                                                borderTop: groupIndex === 0 ? 'none' : '1px solid',
                                                borderBottom: '1px solid',
                                                borderColor: 'divider',
                                                '& .MuiAccordionSummary-content': {
                                                    my: 1.5,
                                                },
                                                '& .MuiAccordionSummary-expandIconWrapper': {
                                                    mr: 0.25,
                                                },
                                            }}
                                        >
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: 1,
                                                    minWidth: 0,
                                                    pl: 1,
                                                }}
                                            >
                                                <typeStyle.Icon
                                                        sx={{
                                                            color: 'text.secondary',
                                                        }}
                                                    />
                                                <Typography fontWeight={700}>
                                                    {PROCESS_NODE_TYPE_PLURAL_LABELS[type]}
                                                </Typography>
                                                <Chip
                                                    size="small"
                                                    label={`${providersForType.length} ${providersForType.length === 1 ? 'Element' : 'Elemente'}`}
                                                    sx={{
                                                        ml: 0.5,
                                                        bgcolor: typeStyle.bgColor,
                                                        color: typeStyle.textColor,
                                                    }}
                                                />
                                            </Box>
                                        </AccordionSummary>
                                        <AccordionDetails sx={{p: 0}}>
                                            {
                                                providersForType.map((provider, index) => (
                                                    <React.Fragment key={getProviderId(provider)}>
                                                        <SelectNodeProviderDialogRow
                                                            provider={provider}
                                                            isSelected={selectedProviderId === getProviderId(provider)}
                                                            primaryActionLabel={primaryActionLabel}
                                                            primaryActionIcon={primaryActionIcon}
                                                            onShowDetails={() => {
                                                                setSelectedProviderId(getProviderId(provider));
                                                            }}
                                                            onAdd={() => {
                                                                handleAddProvider(provider);
                                                            }}
                                                        />
                                                        {
                                                            index < providersForType.length - 1 &&
                                                            <Divider/>
                                                        }
                                                    </React.Fragment>
                                                ))
                                            }
                                        </AccordionDetails>
                                    </Accordion>
                                );
                            })
                        }
                    </Box>
                </Box>

                {
                    selectedProvider != null &&
                    <Box
                        sx={{
                            minWidth: 0,
                            minHeight: 0,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            display: 'flex',
                            flexDirection: 'column',
                            overflow: 'hidden',
                        }}
                    >
                        <SelectNodeProviderDetails
                            provider={selectedProvider}
                            primaryActionLabel={primaryActionLabel}
                            primaryActionIcon={primaryActionIcon}
                            onAdd={() => {
                                handleAddProvider(selectedProvider);
                            }}
                            onClose={() => {
                                setSelectedProviderId(null);
                            }}
                        />
                    </Box>
                }
            </Box>
        </Dialog>
    );
}

interface SelectNodeProviderDialogRowProps {
    provider: ProcessNodeProvider;
    isSelected: boolean;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    onShowDetails: () => void;
    onAdd: () => void;
}

function SelectNodeProviderDialogRow(props: SelectNodeProviderDialogRowProps): ReactNode {
    const {
        provider,
        isSelected,
        primaryActionLabel,
        primaryActionIcon,
        onShowDetails,
        onAdd,
    } = props;

    const ProviderIcon = KnownProviderIcons[provider.componentKey] ?? KnownProviderIcons[provider.key] ?? Assignment;

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 1.75,
                px: 2.25,
                py: 1.9,
                bgcolor: isSelected ? 'action.hover' : 'transparent',
            }}
        >
            <Box
                sx={{
                    width: 40,
                    height: 40,
                    borderRadius: '50%',
                    bgcolor: 'grey.100',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                }}
            >
                <ProviderIcon sx={{fontSize: 20, color: 'text.secondary'}}/>
            </Box>

            <Box
                sx={{
                    minWidth: 0,
                    flex: 1,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        minWidth: 0,
                    }}
                >
                    <Typography
                        fontWeight={700}
                        sx={{
                            minWidth: 0,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {provider.name}
                    </Typography>
                    <Chip
                        size="small"
                        label={`Version ${provider.majorVersion}`}
                        sx={{flexShrink: 0}}
                    />
                </Box>

                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{
                        mt: 0.5,
                    }}
                >
                    {provider.description}
                </Typography>
            </Box>

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    flexShrink: 0,
                    pl: 1.5,
                }}
            >
                <Button
                    variant="text"
                    size="small"
                    startIcon={<InfoOutlined sx={{fontSize: 18}}/>}
                    onClick={onShowDetails}
                >
                    Details
                </Button>
                <Button
                    variant="contained"
                    size="small"
                    startIcon={primaryActionIcon}
                    onClick={onAdd}
                >
                    {primaryActionLabel}
                </Button>
            </Box>
        </Box>
    );
}

interface SelectNodeProviderDetailsProps {
    provider: ProcessNodeProvider;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    onAdd: () => void;
    onClose: () => void;
}

function SelectNodeProviderDetails(props: SelectNodeProviderDetailsProps): ReactNode {
    const {
        provider,
        primaryActionLabel,
        primaryActionIcon,
        onAdd,
        onClose,
    } = props;

    const typeStyle = ProviderTypeStyles[provider.type];
    const {
        label: typeLabel,
        bgColor: typeBgColor,
        textColor: typeTextColor,
    } = typeStyle;
    const ProviderIcon = KnownProviderIcons[provider.componentKey] ?? KnownProviderIcons[provider.key] ?? Assignment;

    return (
        <>
            <Box
                sx={{
                    p: 2,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            width: 38,
                            height: 38,
                            minWidth: 38,
                            minHeight: 38,
                            aspectRatio: '1 / 1',
                            flexShrink: 0,
                            borderRadius: '50%',
                            backgroundColor: typeBgColor,
                            color: typeTextColor,
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
                            {typeLabel}
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
                                lineHeight={1.2}
                                sx={{
                                    flex: 1,
                                    minWidth: 0,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                }}
                            >
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
            </Box>

            <Box
                sx={{
                    flex: 1,
                    overflowY: 'auto',
                    px: 2.25,
                    pt: 2.25,
                    pb: 3.75,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2.5,
                }}
            >
                <Typography variant="body2" color="text.secondary">
                    {provider.description}
                </Typography>

                {
                    provider.deprecationNotice != null &&
                    <Alert severity="warning">
                        {provider.deprecationNotice}
                    </Alert>
                }

                <SelectNodeProviderDetailsSection title="Allgemein">
                    <SelectNodeProviderDetailsRow label="Plugin" value={provider.parentPluginKey}/>
                    <SelectNodeProviderDetailsRow label="Elementschlüssel" value={provider.key}/>
                    <SelectNodeProviderDetailsRow label="Komponente" value={provider.componentKey}/>
                    <SelectNodeProviderDetailsRow label="Komponententyp" value={provider.componentType}/>
                    <SelectNodeProviderDetailsRow label="Komponentenversion" value={provider.componentVersion}/>
                </SelectNodeProviderDetailsSection>

                <SelectNodeProviderDetailsSection title="Ausgänge">
                    {
                        provider.ports.length > 0 ?
                            provider.ports.map((port) => (
                                <SelectNodeProviderDetailsListRow
                                    key={port.key}
                                    primary={port.label}
                                    secondary={port.description}
                                />
                            )) :
                            <Typography variant="body2" color="text.secondary">
                                Dieses Prozesselement besitzt keine Ausgangsports.
                            </Typography>
                    }
                </SelectNodeProviderDetailsSection>

                <SelectNodeProviderDetailsSection title="Ausgangsdaten">
                    {
                        provider.outputs.length > 0 ?
                            provider.outputs.map((output) => (
                                <SelectNodeProviderDetailsListRow
                                    key={output.key}
                                    primary={output.label}
                                    secondary={output.description}
                                />
                            )) :
                            <Typography variant="body2" color="text.secondary">
                                Dieses Prozesselement erzeugt keine zusätzlichen Ausgangsdaten.
                            </Typography>
                    }
                </SelectNodeProviderDetailsSection>
            </Box>

            <Box
                sx={{
                    px: 2,
                    pt: 2,
                    pb: 2.5,
                    borderTop: '1px solid',
                    borderColor: 'divider',
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 1,
                }}
            >
                <Button
                    variant="contained"
                    startIcon={primaryActionIcon}
                    onClick={onAdd}
                >
                    {primaryActionLabel}
                </Button>
                <Button
                    variant="text"
                    onClick={onClose}
                >
                    Details schließen
                </Button>
            </Box>
        </>
    );
}

interface SelectNodeProviderDetailsSectionProps {
    title: string;
    children: ReactNode;
}

function SelectNodeProviderDetailsSection(props: SelectNodeProviderDetailsSectionProps): ReactNode {
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

interface SelectNodeProviderDetailsRowProps {
    label: string;
    value: string;
}

function SelectNodeProviderDetailsRow(props: SelectNodeProviderDetailsRowProps): ReactNode {
    return (
        <Box
            sx={{
                py: 0.25,
            }}
        >
            <Typography variant="caption" color="text.secondary">
                {props.label}
            </Typography>
            <Typography variant="body2" sx={{mt: 0.25}}>
                {props.value}
            </Typography>
        </Box>
    );
}

interface SelectNodeProviderDetailsListRowProps {
    primary: string;
    secondary: string;
}

function SelectNodeProviderDetailsListRow(props: SelectNodeProviderDetailsListRowProps): ReactNode {
    return (
        <Box
            sx={{
                p: 1.5,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1.5,
            }}
        >
            <Typography variant="body2" fontWeight={600}>
                {props.primary}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>
                {props.secondary}
            </Typography>
        </Box>
    );
}
