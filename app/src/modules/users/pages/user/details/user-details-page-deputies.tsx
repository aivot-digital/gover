import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {RadioFieldComponent} from '../../../../../components/radio-field/radio-field-component';
import {EmptyDataListPlaceholder} from '../../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
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
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {useConfirm} from "../../../../../providers/confirm-provider";
import {UserDeputyApiService} from "../../../services/user-deputy-api-service";
import {SelectUserDialog} from "../../../dialogs/select-user-dialog";
import {UserDeputyEntity} from "../../../entities/user-deputy-entity";
import {DialogTitleWithClose} from "../../../../../components/dialog-title-with-close/dialog-title-with-close";
import {DateFieldComponent} from "../../../../../components/date-field/date-field-component";
import {DateFieldComponentModelMode} from "../../../../../models/elements/form/input/date-field-element";
import {formatLocalDate} from '../../../../../utils/date-utils';
import {stringOrDefault} from '../../../../../utils/string-utils';
import {Permission} from '../../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../../permissions/utils/permission-utils';
import {useHasSystemPermission, useRefreshPermissionSet} from '../../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../../components/disabled-tooltip/disabled-tooltip';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {createDeputyColumns} from '../../../components/deputy-columns';
import {RepresentedUsersList} from '../../../components/represented-users-list';
import {Page} from '../../../../../models/dtos/page';
import {useRetainedDialogValue} from '../../../../../hooks/use-retained-dialog-value';
import {
    dateValueToDateTime,
    getCurrentApplicationDate,
    parseLocalDateIso,
} from '../../../../../utils/temporal-utils';

const deputyColumns = createDeputyColumns('deputyUserFullName', 'Stellvertreter:in');
const deputyViews = [
    {value: 'deputies', label: 'Wird vertreten von'},
    {value: 'represented', label: 'Vertritt folgende Personen'},
];

const deletedUserDeputyTooltip = 'Für beim Identitätsanbieter gelöschte Mitarbeiter:innen können Stellvertretungen nicht mehr geändert werden.';

type DeputyNames = Pick<VUserDeputyWithDetailsEntity, 'originalUserFullName' | 'deputyUserFullName'>;

function DeputyParticipants({originalUserFullName, deputyUserFullName}: DeputyNames) {
    return (
        <Box component="dl" sx={{display: 'grid', gridTemplateColumns: 'auto 1fr', gap: 1, my: 0, overflowWrap: 'anywhere'}}>
            <Typography component="dt" variant="body2" sx={{fontWeight: 600}}>Vertretene Person:</Typography>
            <Typography component="dd" variant="body2" sx={{m: 0}}>
                {stringOrDefault(originalUserFullName, 'Kein Name hinterlegt')}
            </Typography>
            <Typography component="dt" variant="body2" sx={{fontWeight: 600}}>Stellvertreter:in:</Typography>
            <Typography component="dd" variant="body2" sx={{m: 0}}>
                {stringOrDefault(deputyUserFullName, 'Kein Name hinterlegt')}
            </Typography>
        </Box>
    );
}

function getDeputyPeriodLabel(item: VUserDeputyWithDetailsEntity): string {
    const fromLabel = formatLocalDate(item.fromDate);

    if (item.untilDate == null) {
        return `ab ${fromLabel}, unbegrenzt`;
    }

    return `${fromLabel} bis ${formatLocalDate(item.untilDate)}`;
}

export function UserDetailsPageDeputies() {
    const dispatch = useAppDispatch();
    const [searchParams, setSearchParams] = useSearchParams();
    const view = searchParams.get('view') === 'represented' ? 'represented' : 'deputies';
    const canCreateDeputy = useHasSystemPermission(Permission.DEPUTY_CREATE);
    const canUpdateDeputy = useHasSystemPermission(Permission.DEPUTY_UPDATE);
    const canDeleteDeputy = useHasSystemPermission(Permission.DEPUTY_DELETE);
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);
    const refreshPermissionSet = useRefreshPermissionSet();

    const listControlRef = useRef<ListControlRef | null>(null);

    const confirm = useConfirm();

    const [showSelectUserDialog, setShowSelectUserDialog] = useState(false);
    const [deputyDraft, setDeputyDraft] = useState<UserDeputyEntity | null>(null);
    const [deputyDraftDetails, setDeputyDraftDetails] = useState<DeputyNames | null>(null);
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
        refreshPermissionSet({broadcast: true}).catch((error) => {
            const message = 'Die Berechtigungen konnten nach der Änderung der Stellvertretung nicht aktualisiert werden.';
            if (isApiError(error) && error.displayableToUser) {
                dispatch(showErrorSnackbar(`${message} ${error.message}`));
            } else {
                dispatch(showApiErrorSnackbar(error, message));
            }
        });
    }, [dispatch, refreshPermissionSet]);

    const isEditingDeputy = renderDeputyDraft != null && renderDeputyDraft.id !== 0;

    const deputyDateRangeError = useMemo(() => {
        if (renderDeputyDraft?.untilDate == null) {
            return undefined;
        }

        const fromDate = dateValueToDateTime(renderDeputyDraft.fromDate, 'day');
        const untilDate = dateValueToDateTime(renderDeputyDraft.untilDate, 'day');

        if (fromDate == null || untilDate == null) {
            return undefined;
        }

        if (untilDate.toMillis() < fromDate.toMillis()) {
            return 'Das Ende der Stellvertretung darf nicht vor ihrem Beginn liegen.';
        }

        return undefined;
    }, [renderDeputyDraft?.fromDate, renderDeputyDraft?.untilDate]);

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
                    Stellvertretung hinzufügen
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
            .list(options.page, options.size, options.sort, options.order, {
                originalUserId: userId,
                deputyUserFullName: options.search,
            });
    }, [userId]);

    if (user == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const representedPersonName = stringOrDefault(user.fullName, 'diese Person');
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
                ? `Aktualisiere Stellvertretung für ${representedPersonName}`
                : `Füge Stellvertretung für ${representedPersonName} hinzu`,
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
            .catch((error) => dispatch(showApiErrorSnackbar(error, isEditingDeputy
                ? 'Die Änderungen an der Stellvertretung konnten nicht gespeichert werden.'
                : 'Die Stellvertretung konnte nicht hinzugefügt werden.')))
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    };

    const handleDeleteDeputy = (item: VUserDeputyWithDetailsEntity) => {
        if (!canDeleteDeputy) {
            return;
        }

        confirm({
            isDestructive: true,
            title: 'Stellvertretung löschen?',
            confirmButtonText: 'Stellvertretung löschen',
            children: (
                <>
                    <DeputyParticipants {...item}/>
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
                    message: item.deputyUserFullName?.trim()
                        ? `Lösche Stellvertretung für ${representedPersonName} durch ${item.deputyUserFullName}`
                        : `Lösche Stellvertretung für ${representedPersonName}`,
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
                    .catch((error) => dispatch(showApiErrorSnackbar(error, 'Die Stellvertretung konnte nicht gelöscht werden.')))
                    .finally(() => {
                        dispatch(setLoadingMessage(undefined));
                    });
            })
    };

    return (
        <>
            <Box sx={{pt: 1.5}}>
                <RadioFieldComponent
                    label="Ansicht"
                    options={deputyViews}
                    value={view}
                    displayInline
                    toggleButtons
                    showOptionalIndicator={false}
                    onChange={(nextView) => {
                        if (nextView == null) {
                            return;
                        }
                        const next = new URLSearchParams(searchParams);
                        if (nextView === 'represented') {
                            next.set('view', nextView);
                        } else {
                            next.delete('view');
                        }
                        // Each direction searches and sorts by a different person's name.
                        for (const key of ['search', 'page', 'sort', 'order']) {
                            next.delete(key);
                        }
                        setSearchParams(next);
                    }}
                />
            </Box>
            {view === 'deputies' ? (
                <GenericList<VUserDeputyWithDetailsEntity>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={deputyColumns}
                    controlRef={listControlRef}
                    fetch={fetchDeputies}
                    getRowIdentifier={(item) => item.id.toString()}
                    searchLabel="Stellvertreter:innen suchen"
                    searchPlaceholder="Name eingeben…"
                    defaultSortField="untilDate"
                    rowMenuItems={[]}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Keine Stellvertreter:innen eingetragen"
                            description="Stellvertretungen legen fest, wer Aufgaben dieser Person für einen Zeitraum übernehmen kann."
                            addText="Stellvertretung hinzufügen"
                            onAdd={() => setShowSelectUserDialog(true)}
                            addDisabled={addDeputyDisabled}
                            addDisabledTooltip={addDeputyDisabledTooltip}
                        />
                    }
                    loadingPlaceholder="Stellvertreter:innen werden geladen…"
                    noSearchResultsPlaceholder="Keine Stellvertreter:innen für diesen Suchbegriff gefunden"
                    rowActionsCount={2}
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
                                        fromDate: item.fromDate ?? getCurrentApplicationDate(),
                                        untilDate: item.untilDate,
                                        lockVersion: item.lockVersion,
                                    });
                                    setDeputyDraftDetails(item);
                                },
                            },
                            {
                                color: 'error',
                                tooltip: 'Stellvertretung löschen',
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
            ) : (
                <RepresentedUsersList
                    deputyUserId={user.id}
                    emptyDescription="Stellvertretungen werden im Profil der vertretenen Person eingerichtet."
                />
            )}

            <SelectUserDialog
                open={showSelectUserDialog}
                title={`Stellvertreter:in für ${representedPersonName} auswählen`}
                onClose={() => {
                    setShowSelectUserDialog(false);
                }}
                onSelect={(deputy) => {
                    setDeputyDraft({
                        id: 0,
                        deputyUserId: deputy.id,
                        originalUserId: user.id,
                        fromDate: getCurrentApplicationDate(),
                        untilDate: null,
                    });
                    setDeputyDraftDetails({originalUserFullName: user.fullName, deputyUserFullName: deputy.fullName});
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
                    {isEditingDeputy ? 'Stellvertretung bearbeiten' : 'Stellvertretung hinzufügen'}
                </DialogTitleWithClose>

                <DialogContent>
                    {/* The concept requires this warning: deputies inherit every permission of the represented person. */}
                    <AlertComponent
                        color="warning"
                        title="Alle Berechtigungen werden übernommen"
                        sx={{mt: 0, mb: 2}}
                    >
                        Während der Stellvertretung erhält die stellvertretende Person alle Berechtigungen der vertretenen
                        Person, auch die aus deren Systemrolle und Domänenrollen.
                        {!isEditingDeputy && ' Wählen Sie deshalb eine Person mit vergleichbaren Berechtigungen und passender Funktion.'}
                    </AlertComponent>

                    {
                        renderDeputyDraftDetails != null &&
                        <Box sx={{mb: 2}}>
                            <DeputyParticipants {...renderDeputyDraftDetails}/>
                        </Box>
                    }

                    <DateFieldComponent
                        label="Beginn der Stellvertretung"
                        mode={DateFieldComponentModelMode.Day}
                        value={renderDeputyDraft?.fromDate}
                        onChange={(val) => {
                            if (deputyDraft == null) {
                                return;
                            }
                            setDeputyDraft({
                                ...deputyDraft,
                                fromDate: val != null
                                    ? parseLocalDateIso(val)
                                    : getCurrentApplicationDate(),
                            });
                        }}
                        required={true}
                    />

                    <DateFieldComponent
                        label="Ende der Stellvertretung"
                        mode={DateFieldComponentModelMode.Day}
                        value={renderDeputyDraft?.untilDate ?? undefined}
                        minDate={renderDeputyDraft?.fromDate}
                        error={deputyDateRangeError}
                        onChange={(val) => {
                            if (deputyDraft == null) {
                                return;
                            }
                            setDeputyDraft({
                                ...deputyDraft,
                                untilDate: val != null ? parseLocalDateIso(val) : null,
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
