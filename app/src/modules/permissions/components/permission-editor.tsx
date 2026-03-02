import {
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import ClearIcon from '@mui/icons-material/Clear';
import SearchIcon from '@mui/icons-material/Search';
import UnfoldMoreIcon from '@mui/icons-material/UnfoldMore';
import UnfoldLessIcon from '@mui/icons-material/UnfoldLess';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import {CompareArrows} from '@mui/icons-material';
import Deselect from '@aivot/mui-material-symbols-400-outlined/dist/deselect/Deselect';
import SelectAll from '@aivot/mui-material-symbols-400-outlined/dist/select-all/SelectAll';

import {PermissionGroups} from '../../../data/permissions/permission-groups';
import {PermissionLabelsDe} from '../../../data/permissions/permission-labels';
import {PermissionApiService} from '../permission-api-service';

import {AlertComponent} from '../../../components/alert/alert-component';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {PermissionScope} from '../enums/permission-scope';
import {type PermissionEntry, type PermissionProvider} from '../models/permission-provider';
import {PermissionGroupAccordion} from './permission-group-accordion';

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

export type PermissionGroup = Pick<PermissionProvider, 'contextLabel' | 'permissions'>;

interface PermissionEditorProps {
    /** Persisted permissions (used for diff view). */
    originalPermissions?: string[];
    /** Current permissions (controlled). */
    value: string[];
    /** Controlled change callback. */
    onChange: (next: string[]) => void;
    /** When false, disables selection and bulk actions. */
    isEditable?: boolean;
    /** When true, disables inputs and copy action. */
    isBusy?: boolean;
    /** Optional label above editor. */
    title?: string;
    /** Scope of the permissions. */
    scope?: PermissionScope | PermissionScope[];
}

export function PermissionEditor(props: PermissionEditorProps): React.ReactElement {
    const {
        originalPermissions = [],
        value,
        onChange,
        isEditable = true,
        isBusy = false,
        title = 'Berechtigungen',
        scope = [PermissionScope.System, PermissionScope.Domain],
    } = props;

    const dispatch = useAppDispatch();

    const [apiPermissions, setApiPermissions] = useState<PermissionProvider[]>([]);
    const [permissionQuery, setPermissionQuery] = useState('');
    const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
    const [bulkMenuAnchorEl, setBulkMenuAnchorEl] = useState<null | HTMLElement>(null);
    const [diffDialogOpen, setDiffDialogOpen] = useState(false);

    useEffect(() => {
        new PermissionApiService()
            .listPermissions()
            .then((permissions: PermissionProvider[]) => {
                setApiPermissions(permissions);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der Berechtigungen ist ein Fehler aufgetreten.'));
            });
    }, [dispatch]);

    const permissions = useMemo(() => {
        const allowedScopes = scope
            ? Array.isArray(scope) ? scope : [scope]
            : null;

        const scopedApiPermissions = allowedScopes
            ? apiPermissions.filter((g) => allowedScopes.includes(g.scope))
            : apiPermissions;

        return [
            ...PermissionGroups.map((group) => ({
                contextLabel: group.label,
                permissions: group.permissions.map((per) => ({
                    label: PermissionLabelsDe[per],
                    permission: per,
                    description: '',
                })),
            })),
            ...scopedApiPermissions,
        ] as PermissionGroup[];
    }, [apiPermissions, scope]);

    const selectedPermissions = value ?? [];
    const selectedCount = selectedPermissions.length;

    useEffect(() => {
        const initialExpanded: Record<string, boolean> = {};
        for (const g of permissions) {
            const selected = selectedPermissions.some((p) => g.permissions.some((gp) => gp.permission === p));
            if (selected) {
                initialExpanded[groupKey(g.contextLabel)] = true;
            }
        }
        setExpandedGroups((prev) => ({...prev, ...initialExpanded}));
    }, [permissions, selectedPermissions]);

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

    const permissionSearchIndex = useMemo(() => {
        type IndexedPermission = {
            groupLabel: string;
            permission: PermissionEntry;
            searchLabel: string;
            searchPermission: string;
            searchDescription: string;
            searchGroupLabel: string;
        };

        const indexedPermissions: IndexedPermission[] = permissions.flatMap((group) =>
            group.permissions.map((permission) => ({
                groupLabel: group.contextLabel,
                permission,
                searchLabel: normalizeSearch(permission.label),
                searchPermission: normalizeSearch(permission.permission),
                searchDescription: normalizeSearch(permission.description ?? ''),
                searchGroupLabel: normalizeSearch(group.contextLabel),
            })),
        );

        return new Fuse(indexedPermissions, {
            keys: ['searchLabel', 'searchPermission', 'searchDescription', 'searchGroupLabel'],
            threshold: 0.35,
            ignoreLocation: true,
        });
    }, [permissions]);

    const filteredPermissionGroups = useMemo(() => {
        const q = normalizeSearch(permissionQuery);
        if (!q) {
            return permissions.map((g) => ({
                ...g,
                permissions: [...g.permissions].sort((a, b) => a.label.localeCompare(b.label)),
            }));
        }

        const matchesByGroup = new Map<string, PermissionEntry[]>();
        const seen = new Set<string>();

        permissionSearchIndex.search(q).forEach(({item}) => {
            const key = `${groupKey(item.groupLabel)}::${item.permission.permission}`;
            if (seen.has(key)) return;
            seen.add(key);

            const existing = matchesByGroup.get(item.groupLabel) ?? [];
            existing.push(item.permission);
            matchesByGroup.set(item.groupLabel, existing);
        });

        return permissions
            .map((group) => ({
                ...group,
                permissions: [...(matchesByGroup.get(group.contextLabel) ?? [])]
                    .sort((a, b) => a.label.localeCompare(b.label)),
            }))
            .filter((group) => group.permissions.length > 0);
    }, [permissions, permissionQuery, permissionSearchIndex]);

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

    const handleToggleGroup = (group: PermissionGroup, checked: boolean): void => {
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

    const expandSelectedAndSearchMatchGroups = (): void => {
        const nextExpanded: Record<string, boolean> = {};

        // Expand groups that currently matter: those with selected permissions and,
        // only when a query is active, groups with search matches.
        for (const g of permissions) {
            const hasSelected = selectedPermissions.some((p) => g.permissions.some((gp) => gp.permission === p));
            if (hasSelected) nextExpanded[groupKey(g.contextLabel)] = true;
        }
        if (permissionQuery.trim()) {
            for (const g of filteredPermissionGroups) {
                nextExpanded[groupKey(g.contextLabel)] = true;
            }
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
                              variant="outlined"
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
                        title="Gruppen mit Auswahl und Suchtreffern ausklappen"
                        arrow
                    >
                        <span>
                          <IconButton
                              aria-label="Gruppen mit Auswahl und Suchtreffern ausklappen"
                              size="small"
                              onClick={expandSelectedAndSearchMatchGroups}
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
                    const isExpanded = expandedGroups[groupKey(group.contextLabel)] ?? false;

                    return (
                        <PermissionGroupAccordion
                            key={group.contextLabel}
                            group={group}
                            selectedPermissions={selectedPermissions}
                            isExpanded={isExpanded}
                            isBusy={isBusy}
                            isEditable={isEditable}
                            onExpandedChange={(next) =>
                                setExpandedGroups((prev) => ({
                                    ...prev,
                                    [groupKey(group.contextLabel)]: next,
                                }))
                            }
                            onToggleGroup={handleToggleGroup}
                            onTogglePermission={handleTogglePermission}
                            inferCrud={inferCrud}
                            onCopyPermission={copyToClipboard}
                        />
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
                    <Button
                        sx={{ml: 'auto'}}
                        onClick={() => setDiffDialogOpen(false)}
                    >Schließen</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
