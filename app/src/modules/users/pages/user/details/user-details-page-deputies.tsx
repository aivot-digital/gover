import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {EmptyDataListPlaceholder} from '../../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {type GridColDef} from '@mui/x-data-grid';
import {GenericList} from '../../../../../components/generic-list/generic-list';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {type User} from '../../../../../models/entities/user';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';
import {Button, Dialog, DialogActions, DialogContent} from "@mui/material";
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {useAppDispatch} from "../../../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar, showErrorSnackbar} from "../../../../../slices/snackbar-slice";
import {GenericListPropsFetchOptions, ListControlRef} from "../../../../../components/generic-list/generic-list-props";
import {setLoadingMessage} from "../../../../../slices/shell-slice";
import {isApiError} from "../../../../../models/api-error";
import {VUserDeputyWithDetailsEntity} from "../../../entities/v-user-deputy-with-details-entity";
import {VUserDeputyWithDetailsApiService} from "../../../services/v-user-deputy-with-details-api-service";
import {parseISO} from "date-fns/parseISO";
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {useConfirm} from "../../../../../providers/confirm-provider";
import {UserDeputyApiService} from "../../../services/user-deputy-api-service";
import {SelectUserDialog} from "../../../dialogs/select-user-dialog";
import {UserDeputyEntity} from "../../../entities/user-deputy-entity";
import {DialogTitleWithClose} from "../../../../../components/dialog-title-with-close/dialog-title-with-close";
import {DateFieldComponent} from "../../../../../components/date-field/date-field-component";
import {DateFieldComponentModelMode} from "../../../../../models/elements/form/input/date-field-element";
import {formatISODate} from "../../../../../utils/date-utils";
import {addDays} from "date-fns/addDays";
import {Permission} from '../../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../../permissions/utils/permission-utils';
import {useHasSystemPermission, useRefreshPermissionSet} from '../../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../../components/disabled-tooltip/disabled-tooltip';

const deletedUserDeputyTooltip = 'Für im Identity Provider gelöschte Mitarbeiter:innen können Stellvertretungen nicht mehr geändert werden.';

const columns: Array<GridColDef<VUserDeputyWithDetailsEntity>> = [
    {
        field: 'deputyUserFullName',
        headerName: 'Stellvertreter:in',
        flex: 1,
    },
    {
        field: 'fromTimestamp',
        headerName: 'Ab',
        flex: 1,
        renderCell: (params) => (
            <span>
                {formatISODate(params.row.fromTimestamp)}
            </span>
        ),
    },
    {
        field: 'untilTimestamp',
        headerName: 'Bis',
        flex: 1,
        renderCell: (params) => params.row.untilTimestamp != null ? (
            <span>
                {formatISODate(params.row.untilTimestamp)}
            </span>
        ) : (
            <em>Unbegrenzt</em>
        )
    },
    {
        field: 'active',
        headerName: 'Derzeit aktiv',
        flex: 1,
        type: 'boolean',
    },
];

function getDeputyPeriodLabel(item: VUserDeputyWithDetailsEntity): string {
    const fromLabel = formatISODate(item.fromTimestamp);

    if (item.untilTimestamp == null) {
        return `ab ${fromLabel}, unbegrenzt`;
    }

    return `${fromLabel} bis ${formatISODate(item.untilTimestamp)}`;
}

export function UserDetailsPageDeputies() {
    const dispatch = useAppDispatch();
    const canCreateDeputy = useHasSystemPermission(Permission.DEPUTY_CREATE);
    const canDeleteDeputy = useHasSystemPermission(Permission.DEPUTY_DELETE);
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);
    const refreshPermissionSet = useRefreshPermissionSet();

    const listControlRef = useRef<ListControlRef | null>(null);

    const confirm = useConfirm();

    const [showSelectUserDialog, setShowSelectUserDialog] = useState(false);
    const [deputyToAdd, setDeputyToAdd] = useState<UserDeputyEntity | null>(null);

    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const canManageDeputies = user != null && !user.deletedInIdp;
    const addDeputyDisabled = !canManageDeputies || !canCreateDeputy || !canReadUsers;
    const addDeputyDisabledTooltip = !canManageDeputies
        ? deletedUserDeputyTooltip
        : !canCreateDeputy
            ? formatMissingPermissionTooltip(Permission.DEPUTY_CREATE)
            : !canReadUsers
                ? formatMissingPermissionTooltip(Permission.USER_READ)
                : '';

    const refreshPermissionsAfterDeputyChange = useCallback(() => {
        // Deputy changes may alter effective permissions for the active user or another open tab.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Stellvertretung nicht aktualisiert werden.',
            )));
    }, [dispatch, refreshPermissionSet]);

    const deputyDateRangeError = useMemo(() => {
        if (deputyToAdd?.untilTimestamp == null) {
            return undefined;
        }

        const fromTimestamp = parseISO(deputyToAdd.fromTimestamp);
        const untilTimestamp = parseISO(deputyToAdd.untilTimestamp);

        if (Number.isNaN(fromTimestamp.getTime()) || Number.isNaN(untilTimestamp.getTime())) {
            return undefined;
        }

        if (untilTimestamp.getTime() <= fromTimestamp.getTime()) {
            return 'Das Ende der Vertretung muss nach dem Start der Vertretung liegen.';
        }

        return undefined;
    }, [deputyToAdd?.fromTimestamp, deputyToAdd?.untilTimestamp]);

    const preSearchElements = useMemo(() => {
        return [
            <DisabledTooltip
                key="add-deputy"
                title={addDeputyDisabledTooltip}
                disabled={addDeputyDisabled}
            >
                <Button
                    variant="contained"
                    startIcon={<Add/>}
                    disabled={addDeputyDisabled}
                    onClick={() => {
                        setShowSelectUserDialog(true);
                    }}
                >
                    Stellvertreter:in hinzufügen
                </Button>
            </DisabledTooltip>,
        ];
    }, [addDeputyDisabled, addDeputyDisabledTooltip]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const fetchDeputies = useCallback((options: GenericListPropsFetchOptions<VUserDeputyWithDetailsEntity>) => {
        return new VUserDeputyWithDetailsApiService()
            .listAllOrdered(options.sort, options.order, {
                originalUserId: user.id,
                deputyUserFullName: options.search,
            });
    }, [user.id]);

    const handleAddDeputy = () => {
        if (!canManageDeputies || !canCreateDeputy || !canReadUsers || deputyToAdd == null) {
            return;
        }

        if (deputyDateRangeError != null) {
            dispatch(showErrorSnackbar(deputyDateRangeError));
            return;
        }

        dispatch(setLoadingMessage({
            message: `Füge Stellvertretung für ${deputyToAdd.deputyUserId} hinzu`,
            blocking: true,
            estimatedTime: 3000,
        }));

        new UserDeputyApiService()
            .create(deputyToAdd)
            .then(() => {
                // Refresh list
                listControlRef.current?.refresh();
                refreshPermissionsAfterDeputyChange();
                setDeputyToAdd(null);
            })
            .catch((error) => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    console.error(error);
                    dispatch(showErrorSnackbar('Fehler beim Hinzufügen der Stellvertretung'));
                }
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    };

    const handleDeleteDeputy = (item: VUserDeputyWithDetailsEntity) => {
        if (!canDeleteDeputy) {
            return;
        }

        confirm({
            title: 'Stellvertreter:in löschen',
            children: (
                <>
                    <Typography>
                        Sind Sie sicher, dass Sie die Stellvertretung von <strong>{item.originalUserFullName}</strong> durch <strong>{item.deputyUserFullName}</strong> löschen möchten?
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Zeitraum: <strong>{getDeputyPeriodLabel(item)}</strong>
                    </Typography>
                </>
            ),
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                dispatch(setLoadingMessage({
                    message: `Lösche Stellvertretung von ${item.originalUserFullName} durch ${item.deputyUserFullName}`,
                    blocking: true,
                    estimatedTime: 3000,
                }));

                new UserDeputyApiService()
                    .destroy(item.id)
                    .then(() => {
                        // Refresh list
                        listControlRef.current?.refresh();
                        refreshPermissionsAfterDeputyChange();
                    })
                    .catch((error) => {
                        if (isApiError(error) && error.displayableToUser) {
                            dispatch(showErrorSnackbar(error.message));
                        } else {
                            console.error(error);
                            dispatch(showErrorSnackbar('Fehler beim Löschen der Stellvertretung'));
                        }
                    })
                    .finally(() => {
                        dispatch(setLoadingMessage(undefined));
                    });
            })
    };

    return (
        <>
            <Box sx={{pt: 1.5}}>
                <Typography
                    variant="h5"
                    sx={{mb: 1}}
                >
                    Stellvertreter:innen
                </Typography>

                <Typography sx={{mb: 3, maxWidth: 900}}>
                    Eine Übersicht der Stellvertreter:innen, die für diese Nutzer:in eingerichtet sind.
                </Typography>

                <GenericList<VUserDeputyWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    controlRef={listControlRef}
                    fetch={fetchDeputies}
                    getRowIdentifier={(item) => item.id.toString()}
                    searchLabel="Stellvertreter:in suchen"
                    searchPlaceholder="Name der Stellvertreter:in eingeben…"
                    defaultSortField="untilTimestamp"
                    rowMenuItems={[]}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Keine Stellvertretungen eingerichtet"
                            description="Stellvertretungen legen fest, wer Aufgaben dieser Person für einen Zeitraum übernehmen kann."
                            addText="Stellvertretung hinzufügen"
                            onAdd={() => setShowSelectUserDialog(true)}
                            addDisabled={addDeputyDisabled}
                            addDisabledTooltip={addDeputyDisabledTooltip}
                        />
                    }
                    loadingPlaceholder="Lade Stellvertreter:innen…"
                    noSearchResultsPlaceholder="Keine Stellvertreter:innen gefunden"
                    rowActions={(item) => [
                        {
                            tooltip: 'Stellvertreter:in löschen',
                            disabled: !canDeleteDeputy,
                            disabledTooltip: formatMissingPermissionTooltip(Permission.DEPUTY_DELETE),
                            icon: <Delete/>,
                            onClick: () => {
                                handleDeleteDeputy(item);
                            },
                        }
                    ]}
                    preSearchElements={preSearchElements}
                />
            </Box>

            <SelectUserDialog
                open={showSelectUserDialog}
                onClose={() => {
                    setShowSelectUserDialog(false);
                }}
                onSelect={(deputy) => {
                    setDeputyToAdd({
                        id: 0,
                        deputyUserId: deputy.id,
                        originalUserId: user.id,
                        fromTimestamp: new Date().toISOString(),
                        untilTimestamp: null,
                    });
                    setShowSelectUserDialog(false);
                }}
                idsToExclude={[
                    user.id,
                ]}
            />

            <Dialog
                open={deputyToAdd != null}
                onClose={() => setDeputyToAdd(null)}
                maxWidth="sm"
                fullWidth
            >
                <DialogTitleWithClose
                    onClose={() => {
                        setDeputyToAdd(null);
                    }}
                >
                    Stellvertreter:in hinzufügen
                </DialogTitleWithClose>

                <DialogContent>
                    <DateFieldComponent
                        label="Start der Vertretung"
                        mode={DateFieldComponentModelMode.Day}
                        value={deputyToAdd?.fromTimestamp}
                        onChange={(val) => {
                            if (deputyToAdd == null) {
                                return;
                            }
                            setDeputyToAdd({
                                ...deputyToAdd,
                                fromTimestamp: val != null ? val : new Date().toISOString(),
                            });
                        }}
                        required={true}
                    />

                    <DateFieldComponent
                        label="Ende der Vertretung"
                        mode={DateFieldComponentModelMode.Day}
                        value={deputyToAdd?.untilTimestamp ?? undefined}
                        minDate={deputyToAdd?.fromTimestamp != null ? addDays(parseISO(deputyToAdd.fromTimestamp), 1) : undefined}
                        error={deputyDateRangeError}
                        onChange={(val) => {
                            if (deputyToAdd == null) {
                                return;
                            }
                            setDeputyToAdd({
                                ...deputyToAdd,
                                untilTimestamp: val != null ? val : null,
                            });
                        }}
                    />
                </DialogContent>

                <DialogActions>
                    <Button
                        variant="contained"
                        onClick={() => {
                            handleAddDeputy();
                        }}
                        disabled={deputyToAdd == null || deputyDateRangeError != null}
                    >
                        Hinzufügen
                    </Button>
                    <Button
                        onClick={() => {
                            setDeputyToAdd(null);
                        }}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
