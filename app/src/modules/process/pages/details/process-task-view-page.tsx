import React, {type ReactElement, type ReactNode} from 'react';
import {Typography} from '@mui/material';
import FolderShared from '@aivot/mui-material-symbols-400-n25-outlined/FolderShared';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import Assignment from '@aivot/mui-material-symbols-400-n25-outlined/Assignment';
import Replay from '@aivot/mui-material-symbols-400-n25-outlined/Replay';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {Chip} from '../../../../components/chip/chip';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {type ProcessTaskDetails} from '../../entities/process-task-details';
import {ProcessTaskStatus, ProcessTaskStatusLabels, ProcessTaskStatusColors} from '../../enums/process-task-status';
import {createStaffPath} from '../../../../utils/url-path-utils';
import {KnownProviderIcons} from '../../data/known-provider-icons';
import {type SvgIconComponent} from '../../../../types/svg-icon-component';
import {useParams} from 'react-router-dom';
import {useRefreshPermissionSet} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';

export type ProcessTaskDetailsPageItem = ProcessTaskDetails;

export function createEmptyProcessTaskDetailsPageItem(): ProcessTaskDetailsPageItem {
    return {
        task: new ProcessInstanceTaskApiService().initialize(),
        instance: null,
        process: null,
        node: null,
        provider: null,
    };
}

export function getProcessTaskBasePath(instanceId: number | string, taskId: number | string): string {
    return `/tasks/${instanceId}/${taskId}`;
}

export function getProcessTaskEditPath(instanceId: number | string, taskId: number | string): string {
    return `${getProcessTaskBasePath(instanceId, taskId)}/edit`;
}

export function getProcessTaskCommunicationPath(instanceId: number | string, taskId: number | string): string {
    return `${getProcessTaskBasePath(instanceId, taskId)}/communication`;
}

export function getProcessTaskProcessPath(item?: ProcessTaskDetailsPageItem | null): string | null {
    if (item?.instance == null || item.process == null) {
        return null;
    }

    return createStaffPath(
        `/processes/${item.process.id}/versions/${item.task.processVersion}?instanceId=${item.instance.id}`,
    );
}

export function getProcessTaskName(item?: ProcessTaskDetailsPageItem | null): string {
    if (item?.node?.name != null && item.node.name.trim().length > 0) {
        return item.node.name;
    }

    return item?.provider?.name || 'Unbenannte Aufgabe';
}

export function getProcessTaskDescription(item?: ProcessTaskDetailsPageItem | null): string {
    if (item?.node?.description != null && item.node.description.trim().length > 0) {
        return item.node.description;
    }

    return item?.provider?.abstractDescription?.trim() || 'Keine Kurzbeschreibung hinterlegt.';
}

export function getProcessTaskStatusLabel(item?: ProcessTaskDetailsPageItem | null): string {
    return item?.task.statusOverride?.trim() || ProcessTaskStatusLabels[item?.task.status ?? ProcessTaskStatus.Running];
}

export function getProcessTaskStatusColor(
    item?: ProcessTaskDetailsPageItem | null,
): 'default' | 'info' | 'success' | 'warning' | 'error' {
    return ProcessTaskStatusColors[item?.task.status ?? ProcessTaskStatus.Running];
}

function getProcessTaskStatusIcon(item?: ProcessTaskDetailsPageItem | null): ReactElement | undefined {
    const status = item?.task.status;

    if (status === ProcessTaskStatus.Running) {
        return <AccountCircle fontSize="small" />;
    }

    if (status === ProcessTaskStatus.Restarted) {
        return <Replay fontSize="small" />;
    }

    return undefined;
}

export function getProcessTaskNodeIcon(item?: ProcessTaskDetailsPageItem | null): ReactElement {
    const ProviderIcon: SvgIconComponent =
        (item?.provider != null &&
            (KnownProviderIcons[item.provider.componentKey] || KnownProviderIcons[item.provider.key])) ||
        Assignment;

    return <ProviderIcon />;
}

function buildProcessTaskHeaderBadges(item?: ProcessTaskDetailsPageItem): ReactNode[] | undefined {
    if (item == null) {
        return undefined;
    }

    const badges: ReactNode[] = [
        <Chip
            key="task-status"
            label={getProcessTaskStatusLabel(item)}
            title={
                item.task.statusOverride?.trim() &&
                item.task.statusOverride.trim() !== ProcessTaskStatusLabels[item.task.status]
                    ? `Systemstatus: ${ProcessTaskStatusLabels[item.task.status]}`
                    : undefined
            }
            color={getProcessTaskStatusColor(item)}
            icon={getProcessTaskStatusIcon(item)}
            mode="soft"
            size="small"
        />,
    ];

    if (item.instance?.createdForTestClaimId != null) {
        badges.push(
            <Chip
                key="test-task"
                label="Test-Aufgabe"
                color="warning"
                mode="soft"
                size="small"
            />,
        );
    }

    return badges;
}

export function ProcessTaskViewPage() {
    const {taskId} = useParams();
    const refreshPermissionSet = useRefreshPermissionSet();
    return (
        <PageWrapper
            title="Aufgabe"
            fullWidth
            background
        >
            <GenericDetailsPage<ProcessTaskDetailsPageItem, string, undefined>
                key={taskId}
                header={(item) => {
                    return {
                        icon: <Task />,
                        title: 'Aufgabe',
                        badge: buildProcessTaskHeaderBadges(item),
                        actions: [
                            {
                                label: 'Vorgang aufrufen',
                                icon: <FolderShared />,
                                iconPosition: 'end',
                                to: item?.instance == null ? '#' : `/process-instances/${item.instance.id}`,
                                variant: 'contained',
                                disabled: item?.instance == null,
                                disabledTooltip: 'Die Vorgangsdetails stehen noch nicht zur Verfügung.',
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Aufgaben',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography
                                        component="p"
                                        sx={{mb: 2}}
                                    >
                                        Eine Aufgabe ist ein einzelner Bearbeitungsschritt innerhalb eines Vorgangs. Der
                                        zugehörige Prozess legt fest, welche Schritte erforderlich sind und wie sie
                                        zusammenhängen. In einem Vorgang können dadurch mehrere Aufgaben entstehen, die
                                        von unterschiedlichen Personen bearbeitet werden.
                                    </Typography>
                                    <Typography
                                        component="p"
                                        sx={{mb: 2}}
                                    >
                                        Unter „Allgemeine Informationen“ sehen Sie, was zu tun ist, wer die Aufgabe
                                        bearbeitet und wann sie fällig ist. „Vorgang aufrufen“ führt zum gesamten
                                        Vorgang.
                                    </Typography>
                                    <Typography
                                        component="p"
                                        sx={{mb: 2}}
                                    >
                                        Unter „Aufgabe bearbeiten“ führen Sie den vorgesehenen Schritt aus. Im Bereich
                                        „Kommunikation“ tauschen Sie Nachrichten zur Aufgabe aus. Mit „Wiedervorlage
                                        einrichten“ merken Sie die Aufgabe für eine spätere Bearbeitung vor.
                                    </Typography>
                                    <Typography component="p">
                                        Über „Aufgabe zuweisen“ geben Sie eine aktive Aufgabe mit entsprechender
                                        Berechtigung an eine andere Person weiter. Die Zuweisung des gesamten Vorgangs
                                        bleibt dabei unverändert.
                                    </Typography>
                                </>
                            ),
                        },
                    };
                }}
                tabs={[
                    {
                        path: '/tasks/:instanceId/:taskId',
                        label: 'Allgemeine Informationen',
                    },
                    {
                        path: '/tasks/:instanceId/:taskId/edit',
                        label: 'Aufgabe bearbeiten',
                        requiredPermission: {
                            permission: Permission.PROCESS_INSTANCE_EDIT_TASK,
                        },
                    },
                    {
                        path: '/tasks/:instanceId/:taskId/communication',
                        label: 'Kommunikation',
                        isDisabled: () => true,
                        disabledTooltip: 'Diese Funktion ist noch nicht verfügbar.',
                    },
                ]}
                initializeItem={() => createEmptyProcessTaskDetailsPageItem()}
                fetchData={async (_, taskId) => {
                    // Task actions depend on instance permissions, which may have changed since list loading.
                    const [details] = await Promise.all([
                        new ProcessInstanceTaskApiService().retrieveDetails(Number(taskId)),
                        refreshPermissionSet(),
                    ]);
                    return details;
                }}
                getTabTitle={(item) => getProcessTaskName(item)}
                getHeaderTitle={(item, _, notFound) => {
                    if (notFound) {
                        return 'Aufgabe nicht gefunden';
                    }

                    return `Aufgabe: ${getProcessTaskName(item)}`;
                }}
                parentLink={{
                    label: 'Liste der Aufgaben',
                    to: '/tasks',
                }}
                idParam="taskId"
                permissionCheck={{
                    scope: {
                        type: 'processInstance',
                        getResourceId: (item) => item.task.processInstanceId,
                    },
                    read: Permission.PROCESS_INSTANCE_READ,
                    update: Permission.PROCESS_INSTANCE_EDIT_TASK,
                }}
            />
        </PageWrapper>
    );
}
