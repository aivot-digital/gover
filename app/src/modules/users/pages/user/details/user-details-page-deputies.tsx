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
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
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
import {Page} from '../../../../../models/dtos/page';
import {useRetainedDialogValue} from '../../../../../hooks/use-retained-dialog-value';

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
    const canUpdateDeputy = useHasSystemPermission(Permission.DEPUTY_UPDATE);
    const canDeleteDeputy = useHasSystemPermission(Permission.DEPUTY_DELETE);
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);
    const refreshPermissionSet = useRefreshPermissionSet();

    const listControlRef = useRef<ListControlRef | null>(null);

    const confirm = useConfirm();

    const [showSelectUserDialog, setShowSelectUserDialog] = useState(false);
    const [deputyDraft, setDeputyDraft] = useState<UserDeputyEntity | null>(null);
    const [deputyDraftDetails, setDeputyDraftDetails] = useState<VUserDeputyWithDetailsEntity | null>(null);
    const isDeputyDialogOpen = deputyDraft != null;
    // Keep the last selected deputy visible until the close transition has finished.
    const renderDeputyDraft = useRetainedDialogValue(isDeputyDialogOpen, deputyDraft);
    const renderDeputyDraftDetails = useRetainedDialogValue(isDeputyDialogOpen, deputyDraftDetails);

    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;
    const userId = user?.id;

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

    const isEditingDeputy = renderDeputyDraft != null && renderDeputyDraft.id !== 0;

    const deputyDateRangeError = useMemo(() => {
        if (renderDeputyDraft?.untilTimestamp == null) {
            return undefined;
        }

        const fromTimestamp = parseISO(renderDeputyDraft.fromTimestamp);
        const untilTimestamp = parseISO(renderDeputyDraft.untilTimestamp);

        if (Number.isNaN(fromTimestamp.getTime()) || Number.isNaN(untilTimestamp.getTime())) {
            return undefined;
        }

        if (untilTimestamp.getTime() <= fromTimestamp.getTime()) {
            return 'Das Ende der Vertretung muss nach dem Start der Vertretung liegen.';
        }

        return undefined;
    }, [renderDeputyDraft?.fromTimestamp, renderDeputyDraft?.untilTimestamp]);

    const saveDeputyDisabled = renderDeputyDraft == null ||
        deputyDateRangeError != null ||
        !canManageDeputies ||
        (isEditingDeputy ? !canUpdateDeputy : (!canCreateDeputy || !canReadUsers));
    const saveDeputyDisabledTooltip = !canManageDeputies
        ? deletedUserDeputyTooltip
        : isEditingDeputy && !canUpdateDeputy
            ? formatMissingPermissionTooltip(Permission.DEPUTY_UPDATE)
            : !isEditingDeputy && !canCreateDeputy
                ? formatMissingPermissionTooltip(Permission.DEPUTY_CREATE)
                : !isEditingDeputy && !canReadUsers
                    ? formatMissingPermissionTooltip(Permission.USER_READ)
                    : deputyDateRangeError;

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

    const fetchDeputies = useCallback((options: GenericListPropsFetchOptions<VUserDeputyWithDetailsEntity>): Promise<Page<VUserDeputyWithDetailsEntity>> => {
        if (userId == null) {
            return Promise.resolve({
                content: [],
                page: {
                    size: 0,
                    number: 0,
                    totalElements: 0,
                    totalPages: 0,
                },
            });
        }

        return new VUserDeputyWithDetailsApiService()
            .listAllOrdered(options.sort, options.order, {
                originalUserId: userId,
                deputyUserFullName: options.search,
            });
    }, [userId]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const closeDeputyDialog = () => {
        setDeputyDraft(null);
        setDeputyDraftDetails(null);
    };

    const handleSaveDeputy = () => {
        if (deputyDraft == null || !canManageDeputies) {
            return;
        }

        if (isEditingDeputy ? !canUpdateDeputy : (!canCreateDeputy || !canReadUsers)) {
            return;
        }

        if (deputyDateRangeError != null) {
            dispatch(showErrorSnackbar(deputyDateRangeError));
            return;
        }

        dispatch(setLoadingMessage({
            message: isEditingDeputy
                ? `Aktualisiere Stellvertretung für ${deputyDraft.deputyUserId}`
                : `Füge Stellvertretung für ${deputyDraft.deputyUserId} hinzu`,
            blocking: true,
            estimatedTime: 3000,
        }));

        const request = isEditingDeputy
            ? new UserDeputyApiService().update(deputyDraft.id, deputyDraft)
            : new UserDeputyApiService().create(deputyDraft);

        request
            .then(() => {
                // Refresh list
                listControlRef.current?.refresh();
                refreshPermissionsAfterDeputyChange();
                closeDeputyDialog();
            })
            .catch((error) => {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    console.error(error);
                    dispatch(showErrorSnackbar(isEditingDeputy
                        ? 'Fehler beim Aktualisieren der Stellvertretung'
                        : 'Fehler beim Hinzufügen der Stellvertretung'));
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
                    rowActions={(item) => {
                        const updateDisabled = item.originalUserDeletedInIdp || item.deputyUserDeletedInIdp || !canUpdateDeputy;
                        const updateDisabledTooltip = item.originalUserDeletedInIdp || item.deputyUserDeletedInIdp
                            ? deletedUserDeputyTooltip
                            : !canUpdateDeputy
                                ? formatMissingPermissionTooltip(Permission.DEPUTY_UPDATE)
                                : undefined;

                        return [
                            {
                                tooltip: 'Stellvertretung bearbeiten',
                                disabled: updateDisabled,
                                disabledTooltip: updateDisabledTooltip,
                                icon: <EditOutlined/>,
                                onClick: () => {
                                    setDeputyDraft({
                                        id: item.id,
                                        originalUserId: item.originalUserId,
                                        deputyUserId: item.deputyUserId,
                                        fromTimestamp: item.fromTimestamp ?? new Date().toISOString(),
                                        untilTimestamp: item.untilTimestamp,
                                    });
                                    setDeputyDraftDetails(item);
                                },
                            },
                            {
                                tooltip: 'Stellvertreter:in löschen',
                                disabled: !canDeleteDeputy,
                                disabledTooltip: formatMissingPermissionTooltip(Permission.DEPUTY_DELETE),
                                icon: <Delete/>,
                                onClick: () => {
                                    handleDeleteDeputy(item);
                                },
                            }
                        ];
                    }}
                    preSearchElements={preSearchElements}
                />
            </Box>

            <SelectUserDialog
                open={showSelectUserDialog}
                onClose={() => {
                    setShowSelectUserDialog(false);
                }}
                onSelect={(deputy) => {
                    setDeputyDraft({
                        id: 0,
                        deputyUserId: deputy.id,
                        originalUserId: user.id,
                        fromTimestamp: new Date().toISOString(),
                        untilTimestamp: null,
                    });
                    setDeputyDraftDetails(null);
                    setShowSelectUserDialog(false);
                }}
                idsToExclude={[
                    user.id,
                ]}
            />

            <Dialog
                open={isDeputyDialogOpen}
                onClose={closeDeputyDialog}
                maxWidth="sm"
                fullWidth
            >
                <DialogTitleWithClose
                    onClose={closeDeputyDialog}
                >
                    {isEditingDeputy ? 'Stellvertretung bearbeiten' : 'Stellvertreter:in hinzufügen'}
                </DialogTitleWithClose>

                <DialogContent>
                    {
                        isEditingDeputy && renderDeputyDraftDetails != null &&
                        <Box sx={{mb: 2}}>
                            <Typography variant="body2">
                                <Box component="span" sx={{fontWeight: 600}}>Mitarbeiter:in:</Box> {renderDeputyDraftDetails.originalUserFullName}
                            </Typography>
                            <Typography variant="body2">
                                <Box component="span" sx={{fontWeight: 600}}>Stellvertreter:in:</Box> {renderDeputyDraftDetails.deputyUserFullName}
                            </Typography>
                        </Box>
                    }

                    <DateFieldComponent
                        label="Start der Vertretung"
                        mode={DateFieldComponentModelMode.Day}
                        value={renderDeputyDraft?.fromTimestamp}
                        onChange={(val) => {
                            if (deputyDraft == null) {
                                return;
                            }
                            setDeputyDraft({
                                ...deputyDraft,
                                fromTimestamp: val != null ? val : new Date().toISOString(),
                            });
                        }}
                        required={true}
                    />

                    <DateFieldComponent
                        label="Ende der Vertretung"
                        mode={DateFieldComponentModelMode.Day}
                        value={renderDeputyDraft?.untilTimestamp ?? undefined}
                        minDate={renderDeputyDraft?.fromTimestamp != null ? addDays(parseISO(renderDeputyDraft.fromTimestamp), 1) : undefined}
                        error={deputyDateRangeError}
                        onChange={(val) => {
                            if (deputyDraft == null) {
                                return;
                            }
                            setDeputyDraft({
                                ...deputyDraft,
                                untilTimestamp: val != null ? val : null,
                            });
                        }}
                    />
                </DialogContent>

                <DialogActions>
                    <DisabledTooltip
                        title={saveDeputyDisabledTooltip}
                        disabled={saveDeputyDisabled}
                    >
                        <Button
                            variant="contained"
                            onClick={() => {
                                handleSaveDeputy();
                            }}
                            disabled={saveDeputyDisabled}
                        >
                            {isEditingDeputy ? 'Speichern' : 'Hinzufügen'}
                        </Button>
                    </DisabledTooltip>
                    <Button
                        onClick={closeDeputyDialog}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
