import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Button,
    Checkbox,
    Chip,
    Dialog, DialogActions,
    DialogContent, Divider,
    FormControlLabel,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ClearIcon from '@mui/icons-material/Clear';
import SearchIcon from '@mui/icons-material/Search';
import UnfoldMoreIcon from '@mui/icons-material/UnfoldMore';
import UnfoldLessIcon from '@mui/icons-material/UnfoldLess';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import {CompareArrows} from '@mui/icons-material';
import Deselect from '@aivot/mui-material-symbols-400-outlined/dist/deselect/Deselect';
import SelectAll from '@aivot/mui-material-symbols-400-outlined/dist/select-all/SelectAll';

import {PermissionGroups} from '../../data/permissions/permission-groups';
import {PermissionLabelsDe} from '../../data/permissions/permission-labels';
import {PermissionApiService} from '../../modules/permissions/permission-api-service';

import {AlertComponent} from '../alert/alert-component';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {PermissionDetails, PermissionEditorProps} from './permission-editor-props';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';

function groupKey(label: string): string {
    return label.trim();
}

function normalizeSearch(value: string): string {
    return value
        .trim()
        .toLowerCase()
        .normalize('NFD')
        // remove accents/diacritics
        .replace(/[\u0300-\u036f]/g, '');
}

export function PermissionEditor(props: PermissionEditorProps): React.ReactElement {
    const {
        originalPermissions = [],
        value,
        onChange,
        isEditable = true,
        isBusy = false,
        title = 'Berechtigungen',
        scope = ['System', 'Domain'],
    } = props;

    const dispatch = useAppDispatch();

    const [permissions, setPermissions] = useState<PermissionDetails[]>([]);
    const [permissionQuery, setPermissionQuery] = useState('');
    const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
    const [bulkMenuAnchorEl, setBulkMenuAnchorEl] = useState<null | HTMLElement>(null);
    const [diffDialogOpen, setDiffDialogOpen] = useState(false);

    useEffect(() => {
        new PermissionApiService()
            .listPermissions()
            .then((apiPermissions: any[]) => {
                const allowedScopes = scope
                    ? Array.isArray(scope) ? scope : [scope]
                    : null;

                const scopedApiPermissions = allowedScopes
                    ? apiPermissions.filter((g) => allowedScopes.includes(g.scope))
                    : apiPermissions;

                const merged = [
                    ...PermissionGroups.map((group) => ({
                        contextLabel: group.label,
                        permissions: group.permissions.map((per) => ({
                            label: PermissionLabelsDe[per],
                            permission: per,
                            description: '',
                        })),
                    })),
                    ...scopedApiPermissions,
                ] as PermissionDetails[];

                setPermissions(merged);

                // Expand groups that have at least one selected permission initially
                const initialExpanded: Record<string, boolean> = {};
                for (const g of merged) {
                    const selected = (value ?? []).some((p) => g.permissions.some((gp) => gp.permission === p));
                    if (selected) {
                        initialExpanded[groupKey(g.contextLabel)] = true;
                    }
                }
                setExpandedGroups(initialExpanded);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der Berechtigungen ist ein Fehler aufgetreten.'));
            });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const selectedPermissions = value ?? [];
    const selectedCount = selectedPermissions.length;

    const bulkMenuOpen = Boolean(bulkMenuAnchorEl);
    const openBulkMenu = (event: React.MouseEvent<HTMLElement>): void => {
        setBulkMenuAnchorEl(event.currentTarget);
    };
    const closeBulkMenu = (): void => {
        setBulkMenuAnchorEl(null);
    };

    const diff = useMemo(() => {
        const original = new Set(originalPermissions ?? []);
        const current = new Set(selectedPermissions);

        const added: string[] = [];
        const removed: string[] = [];

        current.forEach((p) => {
            if (!original.has(p)) added.push(p);
        });
        original.forEach((p) => {
            if (!current.has(p)) removed.push(p);
        });

        added.sort();
        removed.sort();

        return {added, removed, hasChanges: added.length > 0 || removed.length > 0};
    }, [originalPermissions, selectedPermissions]);

    const filteredPermissionGroups = useMemo(() => {
        const q = normalizeSearch(permissionQuery);
        if (!q) {
            return permissions.map((g) => ({
                ...g,
                permissions: [...g.permissions].sort((a, b) => a.label.localeCompare(b.label)),
            }));
        }

        return permissions
            .map((g) => ({
                ...g,
                permissions: g.permissions
                    .filter((p) => {
                        const haystack = normalizeSearch(
                            `${p.label} ${p.permission} ${p.description ?? ''} ${g.contextLabel}`,
                        );
                        return haystack.includes(q);
                    })
                    .sort((a, b) => a.label.localeCompare(b.label)),
            }))
            .filter((g) => g.permissions.length > 0);
    }, [permissions, permissionQuery]);

    const visiblePermissions = useMemo(() => {
        const set = new Set<string>();
        for (const g of filteredPermissionGroups) {
            for (const p of g.permissions) set.add(p.permission);
        }
        return Array.from(set).sort();
    }, [filteredPermissionGroups]);

    const allKnownPermissions = useMemo(() => {
        const all = new Set<string>();
        for (const g of permissions) {
            for (const p of g.permissions) all.add(p.permission);
        }
        return Array.from(all).sort();
    }, [permissions]);

    const setPermissionsValue = (next: string[]): void => {
        const stable = [...next].filter(Boolean);
        stable.sort();
        onChange(stable);
    };

    const handleTogglePermission = (permission: string, checked: boolean): void => {
        const current = new Set(selectedPermissions);
        if (checked) current.add(permission);
        else current.delete(permission);
        setPermissionsValue(Array.from(current));
    };

    const handleToggleGroup = (group: PermissionDetails, checked: boolean): void => {
        const current = new Set(selectedPermissions);
        const groupPerms = group.permissions.map((p) => p.permission);

        if (checked) {
            for (const p of groupPerms) current.add(p);
        } else {
            for (const p of groupPerms) current.delete(p);
        }

        setPermissionsValue(Array.from(current));
    };

    const handleSelectAll = (checked: boolean, scope: 'all' | 'visible' = 'all'): void => {
        const base = scope === 'visible' ? visiblePermissions : allKnownPermissions;

        if (checked) {
            const current = new Set(selectedPermissions);
            for (const p of base) current.add(p);
            setPermissionsValue(Array.from(current));
            return;
        }

        if (scope === 'all') {
            setPermissionsValue([]);
            return;
        }

        const current = new Set(selectedPermissions);
        for (const p of base) current.delete(p);
        setPermissionsValue(Array.from(current));
    };

    const expandAll = (next: boolean, scope: 'all' | 'filtered' = 'all'): void => {
        const base = scope === 'filtered' ? filteredPermissionGroups : permissions;
        const nextExpanded: Record<string, boolean> = {};
        for (const g of base) nextExpanded[groupKey(g.contextLabel)] = next;

        setExpandedGroups((prev) => ({...prev, ...nextExpanded}));
    };

    const expandSmart = (): void => {
        const nextExpanded: Record<string, boolean> = {};

        for (const g of permissions) {
            const hasSelected = selectedPermissions.some((p) => g.permissions.some((gp) => gp.permission === p));
            if (hasSelected) nextExpanded[groupKey(g.contextLabel)] = true;
        }
        for (const g of filteredPermissionGroups) {
            nextExpanded[groupKey(g.contextLabel)] = true;
        }

        setExpandedGroups((prev) => ({...prev, ...nextExpanded}));
    };

    useEffect(() => {
        if (!permissionQuery.trim()) return;
        expandAll(true, 'filtered');
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [permissionQuery]);

    const permissionMeta = useMemo(() => {
        const map = new Map<string, { label?: string; description?: string }>();
        for (const g of permissions) {
            for (const p of g.permissions) map.set(p.permission, {label: p.label, description: p.description});
        }
        return map;
    }, [permissions]);

    const inferCrud = (permission: string): 'create' | 'read' | 'update' | 'delete' | null => {
        const p = permission.toLowerCase();
        if (p.includes('create') || p.includes('add') || p.includes('new')) return 'create';
        if (p.includes('read') || p.includes('view') || p.includes('list') || p.includes('get')) return 'read';
        if (p.includes('update') || p.includes('edit') || p.includes('write')) return 'update';
        if (p.includes('delete') || p.includes('destroy') || p.includes('remove')) return 'delete';
        return null;
    };

    const copyToClipboard = async (text: string): Promise<void> => {
        try {
            if (navigator?.clipboard?.writeText) {
                await navigator.clipboard.writeText(text);
                dispatch(showSuccessSnackbar('Permission-Key kopiert.'));
                return;
            }
        } catch {
            // ignore
        }

        try {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.focus();
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
            dispatch(showSuccessSnackbar('Permission-Key kopiert.'));
        } catch {
            dispatch(showErrorSnackbar('Kopieren nicht möglich.'));
        }
    };

    return (
        <Box sx={{mt: 3}}>
            <Stack
                direction="row"
                alignItems="center"
                justifyContent="space-between"
                spacing={2}
                sx={{mb: 1}}
            >
                <Box>
                    <Typography variant="h6">
                        {title}{' '}
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{display: 'inline'}}
                        >
                            ({selectedCount} von {allKnownPermissions.length} ausgewählt
                            {permissionQuery.trim() ? ` · ${visiblePermissions.length} sichtbar` : ''})
                        </Typography>
                    </Typography>
                </Box>

                <Stack
                    direction="row"
                    spacing={1}
                    alignItems="center"
                >
                    <Tooltip
                        title="Änderungen anzeigen"
                        arrow
                    >
                        <span>
                          <Button
                              variant='outlined'
                              size="small"
                              onClick={() => setDiffDialogOpen(true)}
                              disabled={!diff.hasChanges}
                              startIcon={<CompareArrows />}
                          >
                            <Stack
                                direction="row"
                                spacing={1}
                                alignItems="center"
                            >
                              <Typography variant="inherit">Änderungen</Typography>
                              <Chip
                                  size="small"
                                  label={`+${diff.added.length}`}
                                  variant={diff.added.length > 0 ? 'filled' : 'outlined'}
                              />
                              <Chip
                                  size="small"
                                  label={`-${diff.removed.length}`}
                                  variant={diff.removed.length > 0 ? 'filled' : 'outlined'}
                              />
                            </Stack>
                          </Button>
                        </span>
                    </Tooltip>

                    <Tooltip
                        title="Alle auswählen"
                        arrow
                    >
                        <span>
                          <IconButton
                              aria-label="Alle auswählen"
                              size="small"
                              onClick={() => handleSelectAll(true, 'all')}
                              disabled={isBusy || !isEditable || allKnownPermissions.length === 0}
                          >
                            <SelectAll fontSize="small" />
                          </IconButton>
                        </span>
                    </Tooltip>

                    <Tooltip
                        title="Alle abwählen"
                        arrow
                    >
                        <span>
                          <IconButton
                              aria-label="Alle abwählen"
                              size="small"
                              onClick={() => handleSelectAll(false, 'all')}
                              disabled={isBusy || !isEditable || selectedCount === 0}
                          >
                            <Deselect fontSize="small" />
                          </IconButton>
                        </span>
                    </Tooltip>

                    <Tooltip
                        title="Smartes ausklappen (Gruppen mit Auswahl und Suchtreffern öffnen)"
                        arrow
                    >
                        <span>
                          <IconButton
                              aria-label="Smartes ausklappen"
                              size="small"
                              onClick={expandSmart}
                              disabled={isBusy || permissions.length === 0}
                          >
                            <UnfoldMoreIcon fontSize="small" />
                          </IconButton>
                        </span>
                    </Tooltip>

                    <Tooltip
                        title="Alle einklappen"
                        arrow
                    >
                        <span>
                          <IconButton
                              aria-label="Alle einklappen"
                              size="small"
                              onClick={() => expandAll(false, 'all')}
                              disabled={isBusy || permissions.length === 0}
                          >
                            <UnfoldLessIcon fontSize="small" />
                          </IconButton>
                        </span>
                    </Tooltip>

                    {permissionQuery.trim() && (
                        <Button
                            variant="outlined"
                            size="small"
                            onClick={openBulkMenu}
                            disabled={isBusy || permissions.length === 0}
                            endIcon={<MoreVertIcon />}
                        >
                            Gefiltert
                        </Button>
                    )}

                    <Menu
                        anchorEl={bulkMenuAnchorEl}
                        open={bulkMenuOpen}
                        onClose={closeBulkMenu}
                        anchorOrigin={{vertical: 'bottom', horizontal: 'right'}}
                        transformOrigin={{vertical: 'top', horizontal: 'right'}}
                    >
                        <MenuItem
                            onClick={() => {
                                handleSelectAll(true, 'visible');
                                closeBulkMenu();
                            }}
                            disabled={isBusy || !isEditable || visiblePermissions.length === 0}
                        >
                            <ListItemIcon>
                                <SelectAll fontSize="small" />
                            </ListItemIcon>
                            <ListItemText primary="Sichtbare auswählen" />
                        </MenuItem>

                        <MenuItem
                            onClick={() => {
                                handleSelectAll(false, 'visible');
                                closeBulkMenu();
                            }}
                            disabled={isBusy || !isEditable || visiblePermissions.length === 0}
                        >
                            <ListItemIcon>
                                <Deselect fontSize="small" />
                            </ListItemIcon>
                            <ListItemText primary="Sichtbare abwählen" />
                        </MenuItem>
                    </Menu>
                </Stack>
            </Stack>

            <TextField
                fullWidth
                size="small"
                label="Berechtigungen suchen"
                value={permissionQuery}
                onChange={(e) => setPermissionQuery(e.target.value)}
                disabled={isBusy}
                InputProps={{
                    startAdornment: <SearchIcon
                        fontSize="small"
                        style={{marginRight: 8}}
                    />,
                    endAdornment: permissionQuery ? (
                        <IconButton
                            size="small"
                            onClick={() => setPermissionQuery('')}
                            aria-label="Suche löschen"
                        >
                            <ClearIcon fontSize="small" />
                        </IconButton>
                    ) : undefined,
                }}
                sx={{mt: 1.5, mb: 2}}
            />

            {filteredPermissionGroups.length === 0 && <AlertComponent color="info">Keine Berechtigungen gefunden.</AlertComponent>}

            <Stack spacing={2}>
                {filteredPermissionGroups.map((group) => {
                    const total = group.permissions.length;
                    const selectedInGroup = group.permissions.filter((p) => selectedPermissions.includes(p.permission)).length;
                    const allSelected = total > 0 && selectedInGroup === total;
                    const someSelected = selectedInGroup > 0 && selectedInGroup < total;

                    const isExpanded = expandedGroups[groupKey(group.contextLabel)] ?? false;

                    return (
                        <Accordion
                            key={group.contextLabel}
                            expanded={isExpanded}
                            onChange={(_, next) =>
                                setExpandedGroups((prev) => ({
                                    ...prev,
                                    [groupKey(group.contextLabel)]: next,
                                }))
                            }
                            disableGutters
                            sx={{
                                borderRadius: 2,
                                '&:before': {display: 'none'},
                                border: '1px solid',
                                borderColor: 'divider',
                                boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
                            }}
                        >
                            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                <Stack
                                    direction="row"
                                    alignItems="center"
                                    spacing={1.5}
                                    sx={{width: '100%', pr: 1}}
                                >
                                    <Typography
                                        sx={{flex: 1, minWidth: 0}}
                                        noWrap
                                    >
                                        {group.contextLabel}
                                    </Typography>

                                    <Chip
                                        size="small"
                                        variant={selectedInGroup > 0 ? 'filled' : 'outlined'}
                                        label={`${selectedInGroup} von ${total}`}
                                    />

                                    {/*
                                    // Alternative selection via Checkbox, does not work because it triggers the Accordion toggle
                                    // and thus cannot be used to select/deselect groups without expanding them first.
                                    // Maybe with some event.stopPropagation() magic, but the Button approach is more straightforward
                                    // and has a larger click target.
                                    <Tooltip
                                        title={allSelected ? 'Gruppe abwählen' : 'Gruppe auswählen'}
                                        arrow
                                    >
                                        <span>
                                          <Checkbox
                                              checked={allSelected}
                                              indeterminate={someSelected}
                                              onChange={(_, next) => handleToggleGroup(group, next)}
                                              disabled={isBusy || !isEditable || total === 0}
                                              size="small"
                                              sx={{p: 0.5}}
                                          />
                                        </span>
                                    </Tooltip>*/}

                                    <Divider orientation="vertical" flexItem/>

                                    <Tooltip title={allSelected ? 'Gruppe abwählen' : 'Gruppe auswählen'} arrow>
                                        <span>
                                            <Button
                                                component="span"
                                                size="small"
                                                variant="text"
                                                disabled={isBusy || !isEditable || total === 0}
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleToggleGroup(group, !allSelected);
                                                }}
                                            >
                                                {allSelected ? 'Abwählen' : 'Auswählen'}
                                            </Button>
                                        </span>
                                    </Tooltip>
                                </Stack>
                            </AccordionSummary>

                            <AccordionDetails>
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gridTemplateColumns: {xs: '1fr', md: '1fr 1fr'},
                                        columnGap: 2,
                                        rowGap: 2,
                                    }}
                                >
                                    {group.permissions.map(({permission, label, description}) => {
                                        const checked = selectedPermissions.includes(permission);
                                        const crud = inferCrud(permission);

                                        return (
                                            <Paper
                                                key={permission}
                                                variant="outlined"
                                                sx={{
                                                    px: 1.5,
                                                    py: 1,
                                                    borderRadius: 2,
                                                    display: 'flex',
                                                    alignItems: 'flex-start',
                                                    gap: 1,
                                                    '&:hover': {backgroundColor: 'action.hover'},
                                                }}
                                            >
                                                <FormControlLabel
                                                    sx={{m: 0, flex: 1, alignItems: 'flex-start'}}
                                                    control={(
                                                        <Checkbox
                                                            checked={checked}
                                                            onChange={(_, next) => handleTogglePermission(permission, next)}
                                                            disabled={isBusy || !isEditable}
                                                            size="small"
                                                            sx={{p: 0, pt: .25, pr: 1, m: 0}}
                                                        />
                                                    )}
                                                    label={(
                                                        <Box sx={{width: '100%'}}>
                                                            <Stack
                                                                direction="row"
                                                                spacing={1}
                                                                alignItems="center"
                                                                sx={{minWidth: 0}}
                                                            >
                                                                <Typography
                                                                    variant="body2"
                                                                    sx={{flex: 1, minWidth: 0}}
                                                                    noWrap
                                                                >
                                                                    {label}
                                                                </Typography>

                                                            </Stack>

                                                            <Typography
                                                                variant="caption"
                                                                color="text.secondary"
                                                                sx={{
                                                                    display: 'block',
                                                                    fontFamily: 'monospace',
                                                                    wordBreak: 'break-word',
                                                                }}
                                                            >
                                                                {permission}
                                                            </Typography>

                                                            {description && (
                                                                <Typography
                                                                    variant="caption"
                                                                    color="text.secondary"
                                                                    sx={{
                                                                        display: 'block',
                                                                        mt: 0.25,
                                                                        wordBreak: 'break-word',
                                                                    }}
                                                                >
                                                                    {description}
                                                                </Typography>
                                                            )}
                                                        </Box>
                                                    )}
                                                />

                                                <Stack
                                                    direction="row"
                                                    spacing={1}
                                                    alignItems="center"
                                                    sx={{pt: 0.25}}
                                                >
                                                    {crud && <Chip
                                                        size="small"
                                                        label={crud.toUpperCase()}
                                                        variant="outlined"
                                                    />}

                                                    <Tooltip title="Permission-Key kopieren">
                                                        <span>
                                                          <IconButton aria-label="Permission-Key kopieren" size="small" disabled={isBusy} onClick={() => copyToClipboard(permission)}>
                                                            <ContentCopy fontSize="inherit" />
                                                          </IconButton>
                                                        </span>
                                                    </Tooltip>
                                                </Stack>
                                            </Paper>
                                        );
                                    })}
                                </Box>
                            </AccordionDetails>
                        </Accordion>
                    );
                })}
            </Stack>

            <Dialog
                open={diffDialogOpen}
                onClose={() => setDiffDialogOpen(false)}
                fullWidth
                maxWidth="md"
            >
                <DialogTitleWithClose onClose={() => setDiffDialogOpen(false)}>Änderungen an Berechtigungen</DialogTitleWithClose>
                <DialogContent>
                    {!diff.hasChanges ? (
                        <Typography
                            variant="body2"
                            color="text.secondary"
                        >
                            Keine Änderungen gegenüber dem zuletzt gespeicherten Stand.
                        </Typography>
                    ) : (
                        <Stack
                            spacing={2.5}
                            sx={{mt: 0.5}}
                        >
                            {diff.added.length > 0 && (
                                <Box>
                                    <Typography
                                        variant="subtitle2"
                                        sx={{mb: 1}}
                                    >
                                        Hinzugefügt
                                    </Typography>
                                    <Stack
                                        direction="row"
                                        spacing={1}
                                        useFlexGap
                                        flexWrap="wrap"
                                    >
                                        {diff.added.map((p) => (
                                            <Chip
                                                key={p}
                                                size="small"
                                                label={(permissionMeta.get(p)?.label ?? p) + ` (${p})`}
                                            />
                                        ))}
                                    </Stack>
                                </Box>
                            )}

                            {diff.removed.length > 0 && (
                                <Box>
                                    <Typography
                                        variant="subtitle2"
                                        sx={{mb: 1}}
                                    >
                                        Entfernt
                                    </Typography>
                                    <Stack
                                        direction="row"
                                        spacing={1}
                                        useFlexGap
                                        flexWrap="wrap"
                                    >
                                        {diff.removed.map((p) => (
                                            <Chip
                                                key={p}
                                                size="small"
                                                variant="outlined"
                                                label={(permissionMeta.get(p)?.label ?? p) + ` (${p})`}
                                            />
                                        ))}
                                    </Stack>
                                </Box>
                            )}
                        </Stack>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button sx={{ml: 'auto'}} onClick={() => setDiffDialogOpen(false)}>Schließen</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}