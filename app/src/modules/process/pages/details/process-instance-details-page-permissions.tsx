import {useEffect, useRef, useState} from 'react';
import {Alert, Box, Button, Link, Typography} from '@mui/material';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {type ProcessInstanceAccessControlEntity} from '../../entities/process-instance-access-control-entity';
import {ProcessInstanceAccessControlApiService} from '../../services/process-instance-access-control-api-service';
import {ProcessSettingsDialogAccessControlMatrix} from '../../dialogs/process-settings-dialog/process-settings-dialog-access-control-matrix';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {type PermissionEntry} from '../../../permissions/models/permission-provider';
import {VDepartmentShadowedApiService} from '../../../departments/services/v-department-shadowed-api-service';
import {TeamsApiService} from '../../../teams/services/teams-api-service';
import {type VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {type TeamEntity} from '../../../teams/entities/team-entity';
import {useHasProcessInstancePermission, useRefreshPermissionSet} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker-2';
import {isApiError} from '../../../../models/api-error';
import {createStaffPath} from '../../../../utils/url-path-utils';

interface AccessConflict {
    message: string;
    tasks: {id: number; name: string; assignedUserName: string}[];
}
interface AccessDraft extends ProcessInstanceAccessControlEntity {
    clientId: string;
}
const toDraft = (entry: ProcessInstanceAccessControlEntity): AccessDraft => ({
    ...entry,
    clientId: `server-${entry.id}`,
});
const toEntity = ({clientId: _, ...entry}: AccessDraft): ProcessInstanceAccessControlEntity => entry;

export function ProcessInstanceDetailsPagePermissions() {
    const {item} = useGenericDetailsPageContext<ProcessInstanceDetails, undefined>();
    return item == null ? (
        <GenericDetailsSkeleton />
    ) : (
        <InstancePermissions
            key={item.instance.id}
            item={item}
        />
    );
}

function InstancePermissions({item}: {item: ProcessInstanceDetails}) {
    const instanceId = item.instance.id;
    const canRead = useHasProcessInstancePermission(instanceId, Permission.PROCESS_INSTANCE_READ);
    const canEdit = useHasProcessInstancePermission(instanceId, Permission.PROCESS_INSTANCE_UPDATE);
    const refreshPermissions = useRefreshPermissionSet();
    const dispatch = useAppDispatch();
    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[]>([]);
    const [teams, setTeams] = useState<TeamEntity[]>([]);
    const [persisted, setPersisted] = useState<ProcessInstanceAccessControlEntity[]>([]);
    const [draft, setDraft] = useState<AccessDraft[]>([]);
    const [loading, setLoading] = useState(true);
    const [failed, setFailed] = useState(false);
    const [lookupFailed, setLookupFailed] = useState(false);
    const [saving, setSaving] = useState(false);
    const [conflict, setConflict] = useState<AccessConflict | null>(null);
    const [reload, setReload] = useState(0);
    const nextId = useRef(0);
    const {hasChanged, dialog} = useChangeBlocker({
        original: persisted,
        edited: draft.map(toEntity),
        isActive: canEdit && !loading,
    });

    useEffect(() => {
        if (!canRead) return;
        let cancelled = false;
        setLoading(true);
        setFailed(false);
        setLookupFailed(false);
        Promise.all([
            new ProcessInstanceAccessControlApiService().listAll({targetProcessInstanceId: instanceId}),
            new PermissionApiService().listPermissions(),
            Promise.allSettled([new VDepartmentShadowedApiService().listAll(), new TeamsApiService().listAll()]),
        ])
            .then(([access, providers, domains]) => {
                if (cancelled) return;
                setPersisted(access.content);
                setDraft(access.content.map(toDraft));
                setPermissions(
                    providers
                        .flatMap((provider) => provider.permissions)
                        .filter((permission) => permission.permission.startsWith('process_instance.')),
                );
                setDepartments(domains[0].status === 'fulfilled' ? domains[0].value.content : []);
                setTeams(domains[1].status === 'fulfilled' ? domains[1].value.content : []);
                setLookupFailed(domains.some((result) => result.status === 'rejected'));
            })
            .catch(() => {
                if (!cancelled) setFailed(true);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [instanceId, canRead, reload]);

    const save = async () => {
        if (!canEdit || saving || !hasChanged) return;
        setSaving(true);
        setConflict(null);
        try {
            const saved = await new ProcessInstanceAccessControlApiService().replace(instanceId, draft.map(toEntity));
            setPersisted(saved);
            setDraft(saved.map(toDraft));
            dispatch(showSuccessSnackbar('Die Berechtigungen des Vorgangs wurden gespeichert.'));
            try {
                await refreshPermissions({broadcast: true});
            } catch (error) {
                dispatch(
                    showApiErrorSnackbar(
                        error,
                        'Die eigenen Berechtigungen konnten nicht aktualisiert werden. Laden Sie die Seite neu.',
                    ),
                );
            }
        } catch (error) {
            if (
                isApiError(error) &&
                error.status === 409 &&
                error.details?.reason === 'assigned_tasks_lose_access' &&
                Array.isArray(error.details.tasks)
            ) {
                setConflict({
                    message: error.message,
                    tasks: error.details.tasks,
                });
                dispatch(
                    showErrorSnackbar(
                        'Berechtigungen nicht gespeichert. Bitte weisen Sie die betroffenen Aufgaben zuerst neu zu.',
                    ),
                );
            } else {
                dispatch(showApiErrorSnackbar(error, 'Die Berechtigungen konnten nicht gespeichert werden.'));
            }
        } finally {
            setSaving(false);
        }
    };

    if (!canRead) return <Alert severity="info">Sie haben keine Berechtigung mehr, diesen Vorgang einzusehen.</Alert>;
    if (loading) return <GenericDetailsSkeleton />;
    if (failed)
        return (
            <Alert
                severity="error"
                action={<Button onClick={() => setReload((value) => value + 1)}>Erneut laden</Button>}
            >
                Die Berechtigungen konnten nicht geladen werden.
            </Alert>
        );
    return (
        <Box sx={{pt: 1}}>
            {dialog}
            <Typography variant="h5">Berechtigungen des Vorgangs</Typography>
            <Box
                sx={{
                    mt: 1,
                    mb: 3,
                    maxWidth: 850,
                }}
            >
                <Typography>
                    Hier sehen Sie, welche Organisationseinheiten und Teams auf diesen Vorgang zugreifen dürfen. Die
                    Berechtigungen wurden beim Start aus den Prozesseinstellungen übernommen. Sie können sie hier für
                    diesen Vorgang anpassen. Der Prozess und andere Vorgänge bleiben dabei unverändert.
                </Typography>
                <Typography sx={{mt: 1.5}}>
                    Die verwaltende Organisationseinheit hat weiterhin Zugriff. Was einzelne Personen tun dürfen, hängt
                    auch von ihren Domänenrollen ab. Zusätzliche Berechtigungen aus Systemrollen gelten unabhängig von
                    den Einstellungen auf dieser Seite.
                </Typography>
            </Box>
            {!canEdit && (
                <Alert
                    severity="info"
                    sx={{mb: 2}}
                >
                    Sie können die Berechtigungen einsehen. Zum Bearbeiten benötigen Sie die Berechtigung „Vorgang
                    bearbeiten“.
                </Alert>
            )}
            {lookupFailed && (
                <Alert
                    severity="warning"
                    sx={{mb: 2}}
                >
                    Einige Organisationseinheiten oder Teams konnten nicht geladen werden. Bestehende Zuordnungen werden
                    gegebenenfalls mit ihrer Kennung angezeigt.
                </Alert>
            )}
            {conflict && (
                <Alert
                    aria-label="Betroffene Aufgaben"
                    severity="error"
                    sx={{mb: 2}}
                >
                    {conflict.message}
                    <Box
                        component="ul"
                        sx={{
                            mb: 0,
                            pl: 2,
                        }}
                    >
                        {conflict.tasks.map((task) => (
                            <li key={task.id}>
                                <Link
                                    color="inherit"
                                    underline="always"
                                    sx={{fontWeight: 600}}
                                    href={createStaffPath(`/tasks/${instanceId}/${task.id}`)}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    {task.name}
                                </Link>{' '}
                                – {task.assignedUserName}
                            </li>
                        ))}
                    </Box>
                </Alert>
            )}
            <ProcessSettingsDialogAccessControlMatrix
                permissions={permissions}
                accessControls={draft}
                owningDepartmentId={item.departmentId}
                departments={departments}
                teams={teams}
                isBusy={saving}
                readOnly={!canEdit}
                onAccessControlsChange={setDraft}
                onAddAccessControl={(option) => {
                    if (!canEdit || saving) return;
                    setDraft((current) => [
                        ...current,
                        {
                            ...new ProcessInstanceAccessControlApiService().initialize(),
                            clientId: `new-${++nextId.current}`,
                            targetProcessInstanceId: instanceId,
                            sourceDepartmentId: option.type === 'department' ? option.value : null,
                            sourceTeamId: option.type === 'team' ? option.value : null,
                        },
                    ]);
                }}
                onDeleteAccessControl={(entry) => {
                    if (canEdit && !saving)
                        setDraft((current) => current.filter((value) => value.clientId !== entry.clientId));
                }}
            />
            {canEdit && (
                <Box
                    sx={{
                        mt: 3,
                        display: 'flex',
                        gap: 2,
                    }}
                >
                    <Button
                        variant="contained"
                        startIcon={<Save />}
                        disabled={!hasChanged || saving}
                        onClick={() => void save()}
                    >
                        Berechtigungen speichern
                    </Button>
                    <Button
                        disabled={!hasChanged || saving}
                        onClick={() => {
                            setDraft(persisted.map(toDraft));
                            setConflict(null);
                        }}
                    >
                        Änderungen verwerfen
                    </Button>
                </Box>
            )}
        </Box>
    );
}
