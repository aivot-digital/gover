import {
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Stack,
    Tooltip,
    Typography,
} from '@mui/material';
import React, {useCallback, useDeferredValue, useEffect, useMemo, useState} from 'react';
import UnfoldMoreIcon from '@aivot/mui-material-symbols-400-n25-outlined/UnfoldMore';
import UnfoldLessIcon from '@aivot/mui-material-symbols-400-n25-outlined/UnfoldLess';
import MoreVertIcon from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import CompareArrows from '@aivot/mui-material-symbols-400-n25-outlined/CompareArrows';
import Deselect from '@aivot/mui-material-symbols-400-n25-outlined/Deselect';
import SelectAll from '@aivot/mui-material-symbols-400-n25-outlined/SelectAll';
import {PermissionApiService} from '../permission-api-service';

import {AlertComponent} from '../../../components/alert/alert-component';
import {Actions} from '../../../components/actions/actions';
import {type Action} from '../../../components/actions/actions-props';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {SearchInput} from '../../../components/search-input/search-input';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {type PermissionProvider} from '../models/permission-provider';
import {PermissionGroupAccordion} from './permission-group-accordion';
import {
    buildAssignablePermissionGroups,
    buildPermissionDiff,
    buildPermissionMetaMap,
    buildPermissionSearchIndex,
    buildPermissionSet,
    buildRecoveryPermissionGroups,
    buildRemovedFromSystemPermissions,
    buildUnavailableForDomainRolePermissions,
    filterPermissionGroups,
    getPermissionKeys,
    getSelectedGroupKeys,
    groupKey,
    normalizeSearch,
    toSortedPermissionList,
    type PermissionMeta,
} from './permission-editor-utils';

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
    /** When true, only shows permissions assignable to domain roles. */
    onlyDomainRoleAssignable?: boolean;
}

function mergeExpandedGroups(
    previous: Record<string, boolean>,
    groupIds: readonly string[],
    expanded: boolean,
): Record<string, boolean> {
    let changed = false;
    const next = {...previous};

    for (const groupId of groupIds) {
        if (next[groupId] !== expanded) {
            next[groupId] = expanded;
            changed = true;
        }
    }

    return changed ? next : previous;
}

function formatPermissionChipLabel(
    permissionMeta: ReadonlyMap<string, PermissionMeta>,
    permission: string,
): string {
    return `${permissionMeta.get(permission)?.label ?? permission} (${permission})`;
}

export function PermissionEditor(props: PermissionEditorProps): React.ReactElement {
    const {
        originalPermissions = [],
        value,
        onChange,
        isEditable = true,
        isBusy = false,
        title = 'Berechtigungen',
        onlyDomainRoleAssignable = false,
    } = props;

    const dispatch = useAppDispatch();

    const [apiPermissions, setApiPermissions] = useState<PermissionProvider[]>([]);
    const [permissionQuery, setPermissionQuery] = useState('');
    const deferredPermissionQuery = useDeferredValue(permissionQuery);
    const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});
    const [bulkMenuAnchorEl, setBulkMenuAnchorEl] = useState<null | HTMLElement>(null);
    const [diffDialogOpen, setDiffDialogOpen] = useState(false);

    useEffect(() => {
        let isActive = true;

        new PermissionApiService()
            .listPermissions()
            .then((permissions: PermissionProvider[]) => {
                if (isActive) {
                    setApiPermissions(permissions);
                }
            })
            .catch((err) => {
                if (isActive) {
                    dispatch(showApiErrorSnackbar(err, 'Beim Laden der Berechtigungen ist ein Fehler aufgetreten.'));
                }
            });

        return () => {
            isActive = false;
        };
    }, [dispatch]);

    const selectedPermissions = value ?? [];
    const selectedPermissionsSet = useMemo(() => new Set(selectedPermissions), [selectedPermissions]);
    const selectedCount = selectedPermissions.length;
    const hasLoadedApiPermissions = apiPermissions.length > 0;

    const assignablePermissions = useMemo(
        () => buildAssignablePermissionGroups(apiPermissions, onlyDomainRoleAssignable),
        [apiPermissions, onlyDomainRoleAssignable],
    );

    const allApiPermissionMeta = useMemo(
        () => buildPermissionMetaMap(apiPermissions),
        [apiPermissions],
    );

    const assignablePermissionSet = useMemo(
        () => buildPermissionSet(assignablePermissions),
        [assignablePermissions],
    );

    const allApiPermissionSet = useMemo(
        () => new Set(allApiPermissionMeta.keys()),
        [allApiPermissionMeta],
    );

    const removedFromSystemPermissions = useMemo(
        () => buildRemovedFromSystemPermissions(
            selectedPermissions,
            allApiPermissionSet,
            hasLoadedApiPermissions,
        ),
        [allApiPermissionSet, hasLoadedApiPermissions, selectedPermissions],
    );

    const unavailableForDomainRolePermissions = useMemo(
        () => buildUnavailableForDomainRolePermissions(
            selectedPermissions,
            allApiPermissionSet,
            assignablePermissionSet,
            allApiPermissionMeta,
            onlyDomainRoleAssignable,
            hasLoadedApiPermissions,
        ),
        [
            allApiPermissionMeta,
            allApiPermissionSet,
            assignablePermissionSet,
            hasLoadedApiPermissions,
            onlyDomainRoleAssignable,
            selectedPermissions,
        ],
    );

    const recoveryPermissionCount = removedFromSystemPermissions.length + unavailableForDomainRolePermissions.length;

    const permissionGroups = useMemo(() => [
        // Recovery permissions are shown only while selected. This keeps them removable without making them selectable again.
        ...buildRecoveryPermissionGroups(removedFromSystemPermissions, unavailableForDomainRolePermissions),
        ...assignablePermissions,
    ], [assignablePermissions, removedFromSystemPermissions, unavailableForDomainRolePermissions]);

    const allKnownPermissions = useMemo(
        () => getPermissionKeys(assignablePermissions),
        [assignablePermissions],
    );

    const normalizedPermissionQuery = useMemo(
        () => normalizeSearch(deferredPermissionQuery),
        [deferredPermissionQuery],
    );

    const permissionSearchIndex = useMemo(
        () => buildPermissionSearchIndex(permissionGroups),
        [permissionGroups],
    );

    const filteredPermissionGroups = useMemo(
        () => filterPermissionGroups(permissionGroups, permissionSearchIndex, normalizedPermissionQuery),
        [normalizedPermissionQuery, permissionGroups, permissionSearchIndex],
    );

    const visiblePermissions = useMemo(
        () => getPermissionKeys(filteredPermissionGroups),
        [filteredPermissionGroups],
    );

    const selectedGroupKeys = useMemo(
        () => getSelectedGroupKeys(permissionGroups, selectedPermissionsSet),
        [permissionGroups, selectedPermissionsSet],
    );

    const selectedGroupKeySignature = selectedGroupKeys.join('\u0000');
    useEffect(() => {
        if (selectedGroupKeySignature.length === 0) {
            return;
        }

        setExpandedGroups((prev) => mergeExpandedGroups(prev, selectedGroupKeys, true));
    }, [selectedGroupKeySignature, selectedGroupKeys]);

    const filteredGroupKeys = useMemo(
        () => filteredPermissionGroups.map((group) => groupKey(group.contextLabel)),
        [filteredPermissionGroups],
    );

    const filteredGroupKeySignature = filteredGroupKeys.join('\u0000');
    useEffect(() => {
        if (!normalizedPermissionQuery) {
            return;
        }

        setExpandedGroups((prev) => mergeExpandedGroups(prev, filteredGroupKeys, true));
    }, [filteredGroupKeySignature, filteredGroupKeys, normalizedPermissionQuery]);

    const diff = useMemo(
        () => buildPermissionDiff(originalPermissions, selectedPermissions),
        [originalPermissions, selectedPermissions],
    );

    const permissionMeta = useMemo(() => {
        const map = new Map(allApiPermissionMeta);

        for (const group of permissionGroups) {
            for (const permission of group.permissions) {
                map.set(permission.permission, {
                    label: permission.label,
                    description: permission.description,
                });
            }
        }

        return map;
    }, [allApiPermissionMeta, permissionGroups]);

    const setPermissionsValue = useCallback((next: Iterable<string>): void => {
        onChange(toSortedPermissionList(next));
    }, [onChange]);

    const handleTogglePermission = useCallback((permission: string, checked: boolean): void => {
        const current = new Set(selectedPermissions);

        if (checked) {
            current.add(permission);
        } else {
            current.delete(permission);
        }

        setPermissionsValue(current);
    }, [selectedPermissions, setPermissionsValue]);

    const handleToggleGroup = useCallback((group: typeof permissionGroups[number], checked: boolean): void => {
        const current = new Set(selectedPermissions);

        for (const permission of group.permissions) {
            if (checked) {
                current.add(permission.permission);
            } else {
                current.delete(permission.permission);
            }
        }

        setPermissionsValue(current);
    }, [selectedPermissions, setPermissionsValue]);

    const handleSelectAll = useCallback((checked: boolean, scope: 'all' | 'visible' = 'all'): void => {
        const base = scope === 'visible' ? visiblePermissions : allKnownPermissions;

        if (checked) {
            setPermissionsValue([...selectedPermissions, ...base]);
            return;
        }

        if (scope === 'all') {
            setPermissionsValue([]);
            return;
        }

        const visiblePermissionSet = new Set(base);
        setPermissionsValue(selectedPermissions.filter((permission) => !visiblePermissionSet.has(permission)));
    }, [allKnownPermissions, selectedPermissions, setPermissionsValue, visiblePermissions]);

    const expandAll = useCallback((next: boolean, scope: 'all' | 'filtered' = 'all'): void => {
        const base = scope === 'filtered' ? filteredGroupKeys : permissionGroups.map((group) => groupKey(group.contextLabel));
        setExpandedGroups((prev) => mergeExpandedGroups(prev, base, next));
    }, [filteredGroupKeys, permissionGroups]);

    const expandSelectedAndSearchMatchGroups = useCallback((): void => {
        const nextExpanded = new Set(selectedGroupKeys);

        if (normalizedPermissionQuery) {
            for (const groupId of filteredGroupKeys) {
                nextExpanded.add(groupId);
            }
        }

        setExpandedGroups((prev) => mergeExpandedGroups(prev, Array.from(nextExpanded), true));
    }, [filteredGroupKeys, normalizedPermissionQuery, selectedGroupKeys]);

    const handleExpandedChange = useCallback((groupId: string, next: boolean): void => {
        setExpandedGroups((prev) => {
            if (prev[groupId] === next) {
                return prev;
            }

            return {
                ...prev,
                [groupId]: next,
            };
        });
    }, []);

    const bulkMenuOpen = Boolean(bulkMenuAnchorEl);
    const openBulkMenu = useCallback((event: React.MouseEvent<HTMLElement>): void => {
        setBulkMenuAnchorEl(event.currentTarget);
    }, []);
    const closeBulkMenu = useCallback((): void => {
        setBulkMenuAnchorEl(null);
    }, []);

    const toolbarActions = useMemo<Action[]>(() => [
        {
            tooltip: 'Alle auswählen',
            ariaLabel: 'Alle auswählen',
            icon: <SelectAll fontSize="small"/>,
            onClick: () => handleSelectAll(true, 'all'),
            disabled: !isEditable || allKnownPermissions.length === 0,
        },
        {
            tooltip: 'Alle abwählen',
            ariaLabel: 'Alle abwählen',
            icon: <Deselect fontSize="small"/>,
            onClick: () => handleSelectAll(false, 'all'),
            disabled: !isEditable || selectedCount === 0,
        },
        {
            tooltip: 'Gruppen mit Auswahl und Suchtreffern ausklappen',
            ariaLabel: 'Gruppen mit Auswahl und Suchtreffern ausklappen',
            icon: <UnfoldMoreIcon fontSize="small"/>,
            onClick: expandSelectedAndSearchMatchGroups,
            disabled: permissionGroups.length === 0,
        },
        {
            tooltip: 'Alle einklappen',
            ariaLabel: 'Alle einklappen',
            icon: <UnfoldLessIcon fontSize="small"/>,
            onClick: () => expandAll(false, 'all'),
            disabled: permissionGroups.length === 0,
        },
    ], [
        allKnownPermissions.length,
        expandAll,
        expandSelectedAndSearchMatchGroups,
        handleSelectAll,
        isEditable,
        permissionGroups.length,
        selectedCount,
    ]);

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
                            ({selectedCount} ausgewählt · {allKnownPermissions.length} zuweisbar
                            {recoveryPermissionCount > 0 ? ` · ${recoveryPermissionCount} zu prüfen` : ''}
                            {normalizedPermissionQuery ? ` · ${visiblePermissions.length} sichtbar` : ''})
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
                                startIcon={<CompareArrows/>}
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

                    <Actions
                        actions={toolbarActions}
                        isBusy={isBusy}
                        dense
                        tooltipPlacement="bottom"
                        size="small"
                    />

                    {normalizedPermissionQuery && (
                        <Button
                            variant="outlined"
                            size="small"
                            onClick={openBulkMenu}
                            disabled={isBusy || permissionGroups.length === 0}
                            endIcon={<MoreVertIcon/>}
                        >
                            Gefiltert
                        </Button>
                    )}

                    <Menu
                        anchorEl={bulkMenuAnchorEl}
                        open={bulkMenuOpen}
                        onClose={closeBulkMenu}
                        anchorOrigin={{
                            vertical: 'bottom',
                            horizontal: 'right',
                        }}
                        transformOrigin={{
                            vertical: 'top',
                            horizontal: 'right',
                        }}
                    >
                        <MenuItem
                            onClick={() => {
                                handleSelectAll(true, 'visible');
                                closeBulkMenu();
                            }}
                            disabled={isBusy || !isEditable || visiblePermissions.length === 0}
                        >
                            <ListItemIcon>
                                <SelectAll fontSize="small"/>
                            </ListItemIcon>
                            <ListItemText primary="Sichtbare auswählen"/>
                        </MenuItem>

                        <MenuItem
                            onClick={() => {
                                handleSelectAll(false, 'visible');
                                closeBulkMenu();
                            }}
                            disabled={isBusy || !isEditable || visiblePermissions.length === 0}
                        >
                            <ListItemIcon>
                                <Deselect fontSize="small"/>
                            </ListItemIcon>
                            <ListItemText primary="Sichtbare abwählen"/>
                        </MenuItem>
                    </Menu>
                </Stack>
            </Stack>

            <SearchInput
                value={permissionQuery}
                onChange={setPermissionQuery}
                label="Berechtigungen suchen"
                placeholder="Name, Key oder Beschreibung eingeben"
                ariaLabel="Berechtigungen suchen"
                clearable
                disabled={isBusy}
                size="small"
                debounce={120}
                sx={{
                    mt: 1.5,
                    mb: 2,
                }}
            />

            {filteredPermissionGroups.length === 0 &&
                <AlertComponent color="info">Keine Berechtigungen gefunden.</AlertComponent>}

            {removedFromSystemPermissions.length > 0 && (
                <AlertComponent
                    color="warning"
                    sx={{mb: 2}}
                >
                    Diese Rolle enthält Berechtigungen, die aktuell nicht mehr vom System bereitgestellt werden.
                    Das kann zum Beispiel durch entfernte Funktionen oder Erweiterungen (Plugins) entstehen.
                    Die Berechtigungen werden beim Speichern unverändert übernommen, solange sie ausgewählt bleiben,
                    sollten aber entfernt werden, um die Rolle zu bereinigen.
                </AlertComponent>
            )}

            {unavailableForDomainRolePermissions.length > 0 && (
                <AlertComponent
                    color="warning"
                    sx={{mb: 2}}
                >
                    Diese Domänenrolle enthält Berechtigungen, die aktuell nicht für Domänenrollen verfügbar sind.
                    Das kann zum Beispiel durch geänderte Funktionen oder Erweiterungen (Plugins) entstehen.
                    Die Berechtigungen werden beim Speichern unverändert übernommen, solange sie ausgewählt bleiben,
                    sollten aber entfernt werden, um die Rolle zu bereinigen.
                </AlertComponent>
            )}

            <Stack spacing={2}>
                {filteredPermissionGroups.map((group) => {
                    const currentGroupKey = groupKey(group.contextLabel);

                    return (
                        <PermissionGroupAccordion
                            key={currentGroupKey}
                            group={group}
                            groupId={currentGroupKey}
                            selectedPermissionsSet={selectedPermissionsSet}
                            isExpanded={expandedGroups[currentGroupKey] ?? false}
                            isBusy={isBusy}
                            isEditable={isEditable}
                            onExpandedChange={handleExpandedChange}
                            onToggleGroup={handleToggleGroup}
                            onTogglePermission={handleTogglePermission}
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
                <DialogTitleWithClose onClose={() => setDiffDialogOpen(false)}>
                    Änderungen an Berechtigungen
                </DialogTitleWithClose>
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
                                        {diff.added.map((permission) => (
                                            <Chip
                                                key={permission}
                                                size="small"
                                                label={formatPermissionChipLabel(permissionMeta, permission)}
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
                                        {diff.removed.map((permission) => (
                                            <Chip
                                                key={permission}
                                                size="small"
                                                variant="outlined"
                                                label={formatPermissionChipLabel(permissionMeta, permission)}
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
                    >
                        Schließen
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
