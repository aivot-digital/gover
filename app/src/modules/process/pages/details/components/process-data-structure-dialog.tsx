import {useEffect, useMemo, useState, type ReactNode} from 'react';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogContent,
    IconButton,
    Stack,
    Tab,
    Tabs,
    Tooltip,
    Typography,
} from '@mui/material';
import {DialogTitleWithClose} from '../../../../../components/dialog-title-with-close/dialog-title-with-close';
import {SearchInput} from '../../../../../components/search-input/search-input';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import {ProcessDataStructureTree} from './process-data-structure-tree';
import {
    ProcessDataStructureCategory,
    type ProcessDataStructureOrigin,
    type ProcessDataStructureEntry,
} from '../../../entities/process-data-structure';
import {CopyToClipboardButton} from '../../../../../components/copy-to-clipboard-button/copy-to-clipboard-button';
import {ProcessDefinitionVersionApiService} from '../../../services/process-definition-version-api-service';
import {ProcessDataStructureIssueKind, type ProcessDataStructure} from '../../../entities/process-data-structure';
import {humanizeNumber, pluralize} from '../../../../../utils/humanization-utils';
import {
    buildProcessDataStructureTree,
    filterProcessDataStructureTree,
    hasMixedStructure,
    getDataStructureTabEntries,
    matchesDataStructureTab,
    processDataStructureTabs,
    type ProcessDataStructureTab,
    type ProcessDataStructureTreeNode,
} from '../../../utils/process-data-structure';

interface ProcessDataStructureDialogProps {
    processId: number;
    processVersion: number;
    savedRevision: unknown;
    onClose: () => void;
    onSelectNode: (nodeId: number) => void;
}

type LoadState = { status: 'loading' } | { status: 'failed' } | { status: 'loaded'; data: ProcessDataStructure };

// Mount only while open: each visit starts with the current saved version and fresh UI state.
export function ProcessDataStructureDialog(props: ProcessDataStructureDialogProps): ReactNode {
    const {
        processId,
        processVersion,
        savedRevision,
        onClose,
        onSelectNode,
    } = props;
    const [state, setState] = useState<LoadState>({status: 'loading'});
    const [attempt, setAttempt] = useState(0);
    const [search, setSearch] = useState('');
    const [selectedPath, setSelectedPath] = useState('');
    const [category, setCategory] = useState<ProcessDataStructureTab>(ProcessDataStructureCategory.ProcessData);
    const {
        label: categoryLabel,
        prefix,
    } = processDataStructureTabs[category];

    useEffect(() => {
        let cancelled = false;
        setState({status: 'loading'});
        new ProcessDefinitionVersionApiService().getDataStructure({
            processDefinitionId: processId,
            processDefinitionVersion: processVersion,
        }).then(data => {
            if (!cancelled) setState({
                status: 'loaded',
                data,
            });
        }).catch(() => {
            if (!cancelled) setState({status: 'failed'});
        });
        return () => {
            cancelled = true;
        };
    }, [processId, processVersion, savedRevision, attempt]);

    const entries = useMemo(() => state.status === 'loaded' ? getDataStructureTabEntries(state.data.entries, category) : [], [state, category]);
    const issues = state.status === 'loaded' ? state.data.issues.filter(issue => matchesDataStructureTab(issue.category, issue.path, category)) : [];
    const tree = useMemo(() => buildProcessDataStructureTree(entries), [entries]);
    const filteredTree = useMemo(() => filterProcessDataStructureTree(tree, search, prefix), [tree, search, prefix]);
    const selected = findNode(tree, selectedPath);
    const originGroups = groupOrigins(selected?.entries ?? []);
    const originCount = new Set(selected?.entries.flatMap(entry => entry.origin == null ? [] : [entry.origin.nodeId]) ?? []).size;
    const pathCount = new Set(entries.map(entry => entry.path)).size;
    // Only element namespaces identify a container with a particular node. A process-data parent may combine origins.
    const containerOrigin = selected?.entries.length === 0 ? entries.find(entry => entry.origin != null && (
        (category === ProcessDataStructureCategory.ElementData && selected.path === entry.origin.dataKey) ||
        (category === 'ElementMetadata' && selected.path === entry.origin.dataKey)
    ))?.origin : null;
    const childCountText = selected == null ? '' : `Enthält ${humanizeNumber(selected.children.size, {1: 'einen'})} direkt ${pluralize(selected.children.size, 'untergeordneten Pfad', 'untergeordnete Pfade')}.`;
    const structureNotes: string[] = [];
    const notes: string[] = [];
    if (selected != null) {
        if (selected.segment === '*') {
            structureNotes.push('[*] ist ein Platzhalter für die einzelnen Einträge dieser Liste.');
        }
        if (selected.children.has('*') && !hasMixedStructure(selected)) {
            structureNotes.push('Dieser Datenpfad bezeichnet die gesamte Liste. Unter [*] sehen Sie den Aufbau der einzelnen Listeneinträge.');
        }
        if (originGroups.length > 0) notes.push(...structureNotes);
        if (selected.entries.length > 0 && selected.children.size > 0) notes.push(childCountText);
        if (selected.entries.some(entry => entry.unexpandedStructure)) {
            notes.push('Unterstruktur nicht aufgeschlüsselt: Mögliche Felder oder Listeneinträge innerhalb dieses Werts sind hier nicht aufgeführt.');
        }
        if (category === ProcessDataStructureCategory.ProcessData && originCount > 1) {
            notes.push(`Dieser Pfad ist in ${humanizeNumber(originCount)} Prozesselementen definiert. Diese können aufeinanderfolgen oder zu unterschiedlichen Prozesspfaden gehören.`);
        }
        if (originGroups.length > 1) notes.push('Die Angaben sind nach Bezeichnung und Beschreibung gruppiert.');
        if (category === 'ElementMetadata') notes.push('Vom System bei der Ausführung des jeweiligen Prozesselements bereitgestellt.');
        if (category === ProcessDataStructureCategory.ProtectedProcessData) notes.push('Vom System bereitgestellt.');
    }

    return <Dialog open
                   onClose={onClose}
                   maxWidth="lg"
                   fullWidth
                   aria-labelledby="process-data-structure-title"
                   slotProps={{paper: {sx: {height: 'min(800px, calc(100vh - 64px))'}}}}>
        <DialogTitleWithClose onClose={onClose}
                              id="process-data-structure-title">
            Datenstruktur
        </DialogTitleWithClose>
        <DialogContent sx={{
            display: 'flex',
            flexDirection: 'column',
            minHeight: 0,
            overflow: 'hidden',
            pb: 0,
        }}>
            <Typography variant="body2"
                        color="textSecondary"
                        sx={{
                            mb: 2.5,
                            flexShrink: 0,
                        }}>
                Die Übersicht zeigt bekannte Datenpfade der gespeicherten Prozessversion.
                Welche Daten tatsächlich vorhanden sind, hängt vom ausgeführten Prozesspfad ab.
                Dynamisch erzeugte Pfade können fehlen.
                Ungespeicherte Änderungen werden nicht berücksichtigt.
            </Typography>
            <Tabs value={category}
                  onChange={(_, value: ProcessDataStructureTab) => {
                      setCategory(value);
                      setSearch('');
                      setSelectedPath('');
                  }}
                  variant="scrollable"
                  scrollButtons="auto"
                  aria-label="Datenebene"
                  sx={{
                      mx: -3,
                      borderBottom: 1,
                      borderColor: 'divider',
                      flexShrink: 0,
                  }}>
                {(Object.keys(processDataStructureTabs) as ProcessDataStructureTab[]).map(value => <Tab key={value}
                                                                                                        value={value}
                                                                                                        id={`data-structure-tab-${value}`}
                                                                                                        aria-controls={`data-structure-panel-${value}`}
                                                                                                        sx={{px: 3}}
                                                                                                        label={`${processDataStructureTabs[value].label} (${processDataStructureTabs[value].prefix})`}/>)}
            </Tabs>
            <Box role="tabpanel"
                 id={`data-structure-panel-${category}`}
                 aria-labelledby={`data-structure-tab-${category}`}
                 sx={{
                     display: 'flex',
                     flexDirection: 'column',
                     flex: 1,
                     minHeight: 0,
                 }}>
                {state.status === 'loading' && <Box role="status"
                                                    sx={{
                                                        p: 4,
                                                        textAlign: 'center',
                                                        m: 'auto',
                                                    }}>
                    <CircularProgress size={28}
                                      aria-label="Datenstruktur wird geladen"/>
                    <Typography>Datenstruktur wird geladen …</Typography>
                </Box>}
                {state.status === 'failed' && <Alert severity="error"
                                                     action={<Button color="inherit"
                                                                     onClick={() => setAttempt(value => value + 1)}>Erneut
                                                                                                                    versuchen</Button>}>
                    Die Datenstruktur konnte nicht geladen werden.
                </Alert>}
                {state.status === 'loaded' && <>
                    {issues.length > 0 && <Alert severity="warning"
                                                 sx={{
                                                     maxHeight: 140,
                                                     overflow: 'auto',
                                                     flexShrink: 0,
                                                     my: 2,
                                                 }}>
                        <Typography variant="body2"
                                    sx={{fontWeight: 'bold'}}>Die Übersicht ist unvollständig.</Typography>
                        <Box component="ul"
                             sx={{
                                 m: 0,
                                 pl: 2,
                             }}>
                            {issues.map((issue, index) => <li key={index}>
                                {issue.kind === ProcessDataStructureIssueKind.InvalidPath
                                    ? `Ungültiger Datenpfad: ${issue.path ?? '(leer)'}.`
                                    : 'Die Datenschlüssel dieses Prozesselements konnten nicht ermittelt werden.'}
                                {' '}<Button size="small"
                                             onClick={() => onSelectNode(issue.origin.nodeId)}>
                                {issue.origin.name || issue.origin.dataKey}
                            </Button>
                            </li>)}
                        </Box>
                    </Alert>}
                    {tree.children.size === 0 ? <Typography sx={{p: 3}}>
                        Keine Datenpfade in dieser Kategorie ermittelt.
                    </Typography> : <Box sx={{
                        display: 'grid',
                        gridTemplateColumns: 'minmax(300px, 38%) minmax(0, 1fr)',
                        flex: 1,
                        minHeight: 0,
                    }}>
                        <Box component="nav"
                             aria-label={`${categoryLabel} durchsuchen`}
                             sx={{
                                 display: 'flex',
                                 flexDirection: 'column',
                                 minHeight: 0,
                                 pt: 2,
                                 pr: 3,
                             }}>
                            <Box>
                                <SearchInput label={`${categoryLabel} durchsuchen`}
                                             value={search}
                                             onChange={setSearch}
                                             placeholder="Pfad, Bezeichnung oder Prozesselement"/>
                            </Box>
                            <ProcessDataStructureTree key={category}
                                                      label={categoryLabel}
                                                      prefix={prefix}
                                                      node={filteredTree}
                                                      search={search}
                                                      selectedPath={selectedPath}
                                                      onSelect={setSelectedPath}
                                                      pathCount={pathCount}/>
                        </Box>
                        <Box component="section"
                             aria-label="Pfaddetails"
                             sx={{
                                 overflow: 'auto',
                                 py: 2,
                                 pl: 3,
                                 pr: 1,
                                 borderLeft: 1,
                                 borderColor: 'divider',
                                 overflowWrap: 'anywhere',
                             }}>
                            {selected == null || selected.path === '' ? <Stack spacing={1.5}
                                                                               sx={{
                                                                                   height: '100%',
                                                                                   alignItems: 'center',
                                                                                   justifyContent: 'center',
                                                                                   textAlign: 'center',
                                                                                   color: 'text.secondary',
                                                                               }}>
                                <DataObject sx={{
                                    fontSize: 36,
                                    color: 'text.disabled',
                                }}/>
                                <Typography variant="h6"
                                            color="textPrimary">Pfad auswählen</Typography>
                                <Typography variant="body2"
                                            sx={{maxWidth: 320}}>
                                    Wählen Sie links einen Pfad aus, um seine Bezeichnung und Herkunft anzuzeigen.
                                </Typography>
                            </Stack> : <Stack spacing={2.5}>
                                <Box>
                                    <Typography variant="caption"
                                                color="textSecondary">Datenpfad</Typography>
                                    <Box sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 1,
                                    }}>
                                        <Typography component="h3"
                                                    variant="h4"
                                                    sx={{
                                                        fontFamily: 'monospace',
                                                        flex: 1,
                                                        minWidth: 0,
                                                    }}>{prefix}.{selected.path}</Typography>
                                        <CopyToClipboardButton
                                            key={`${prefix}.${selected.path}`}
                                            text={`${prefix}.${selected.path}`}
                                            tooltip="Pfad kopieren"
                                            copiedTooltip="Pfad kopiert"
                                            ariaLabel="Pfad kopieren"
                                            errorMessage="Der Datenpfad konnte nicht kopiert werden."
                                        />
                                    </Box>
                                </Box>
                                {originGroups.length === 0 && <Box>
                                    <Typography component="h4"
                                                variant="h5">
                                        {containerOrigin == null ? 'Untergeordnete Pfade'
                                            : category === ProcessDataStructureCategory.ElementData ? 'Elementdaten' : 'Elementmetadaten'}
                                    </Typography>
                                    {structureNotes.map(note => <Typography key={note}
                                                                           variant="body2"
                                                                           sx={{mt: 1}}>{note}</Typography>)}
                                    <Typography variant="body2"
                                                sx={{mt: 1}}>{childCountText}</Typography>
                                    {containerOrigin != null && <DataStructureOrigins label="Prozesselement"
                                                                                      origins={[containerOrigin]}
                                                                                      onSelectNode={onSelectNode}/>}
                                </Box>}
                                {originGroups.map((group, index) => <Box key={index}>
                                    <Typography variant="caption"
                                                color="textSecondary">Bezeichnung</Typography>
                                    <Typography component="h4"
                                                variant="h5">{group.label}</Typography>
                                    {group.description && <Box sx={{mt: 1.5}}>
                                        <Typography variant="caption"
                                                    color="textSecondary">Beschreibung</Typography>
                                        <Typography variant="body2"
                                                    sx={{whiteSpace: 'pre-wrap'}}>{group.description}</Typography>
                                    </Box>}
                                    {group.origins.length > 0 && <DataStructureOrigins
                                        label={category === 'ElementMetadata' ? 'Prozesselement'
                                            : category === ProcessDataStructureCategory.ElementData ? 'Erzeugt von' : 'Definiert in'}
                                        origins={group.origins}
                                        onSelectNode={onSelectNode}/>}
                                </Box>)}
                                {hasMixedStructure(selected) && <Alert severity="warning">
                                    Mögliche Strukturabweichung: Für diesen Pfad sind sowohl Objektfelder als auch
                                    Listeneinträge definiert.
                                    Diese können aus unterschiedlichen Prozesspfaden stammen.
                                </Alert>}
                                {notes.length > 0 && <Stack role="note"
                                                            aria-label="Hinweise zum Datenpfad"
                                                            spacing={1}>
                                    {notes.map(note => <Typography key={note}
                                                                   component="p"
                                                                   variant="caption"
                                                                   color="textSecondary">
                                        {note}
                                    </Typography>)}
                                </Stack>}
                            </Stack>}
                        </Box>
                    </Box>}

                </>}
            </Box>
        </DialogContent>
    </Dialog>;
}

function DataStructureOrigins({
                                  label,
                                  origins,
                                  onSelectNode,
                              }: {
    label: string;
    origins: ProcessDataStructureOrigin[];
    onSelectNode: (nodeId: number) => void;
}) {
    return <Box sx={{mt: 2}}>
        <Typography variant="caption"
                    color="textSecondary">{label}</Typography>
        <Box sx={{
            border: 1,
            borderColor: 'divider',
            borderRadius: 1,
            overflow: 'hidden',
            mt: 1,
        }}>
            {origins.map((origin, index) => <Box key={origin.nodeId}
                                                 sx={{
                                                     display: 'flex',
                                                     alignItems: 'center',
                                                     gap: 2,
                                                     px: 2,
                                                     py: 1.25,
                                                     borderTop: index === 0 ? 0 : 1,
                                                     borderColor: 'divider',
                                                 }}>
                <Box sx={{
                    flex: 1,
                    minWidth: 0,
                }}>
                    <Typography variant="body2">{origin.name || origin.dataKey}</Typography>
                    <Typography variant="caption"
                                color="textSecondary">Datenschlüssel: {origin.dataKey}</Typography>
                </Box>
                <Tooltip title="Zum Prozesselement"
                         arrow>
                    <IconButton size="small"
                                aria-label={`Zum Prozesselement ${origin.name || origin.dataKey}`}
                                onClick={() => onSelectNode(origin.nodeId)}><ArrowForward fontSize="small"/></IconButton>
                </Tooltip>
            </Box>)}
        </Box>
    </Box>;
}

function findNode(node: ProcessDataStructureTreeNode, path: string): ProcessDataStructureTreeNode | null {
    if (node.path === path) return node;
    for (const child of node.children.values()) {
        const found = findNode(child, path);
        if (found != null) return found;
    }
    return null;
}

interface OriginGroup {
    label: string;
    description: string | null;
    origins: ProcessDataStructureOrigin[];
}

function groupOrigins(entries: ProcessDataStructureEntry[]): OriginGroup[] {
    const groups: OriginGroup[] = [];
    for (const entry of entries) {
        let group = groups.find(candidate => candidate.label === entry.label && candidate.description === entry.description);
        if (group == null) {
            group = {
                label: entry.label,
                description: entry.description,
                origins: [],
            };
            groups.push(group);
        }
        if (entry.origin != null && !group.origins.some(origin => origin.nodeId === entry.origin?.nodeId)) group.origins.push(entry.origin);
    }
    return groups;
}
