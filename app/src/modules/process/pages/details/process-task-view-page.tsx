import React, {type ReactElement, type ReactNode} from 'react';
import {Typography} from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import AccountCircle from '@aivot/mui-material-symbols-400-outlined/dist/account-circle/AccountCircle';
import Assignment from '@aivot/mui-material-symbols-400-outlined/dist/assignment/Assignment';
import Replay from '@aivot/mui-material-symbols-400-outlined/dist/replay/Replay';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {Chip} from '../../../../components/chip/chip';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {type ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {type ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {type ProcessEntity} from '../../entities/process-entity';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {
    type ProcessNodeProvider,
    ProcessNodeProviderApiService,
} from '../../services/process-node-provider-api-service';
import {ProcessInstanceStatus, ProcessInstanceStatusLabels} from '../../enums/process-instance-status';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../enums/process-task-status';
import {getNodeDescription, getNodeName} from './components/process-flow-editor/utils/node-utils';
import {createStaffPath} from '../../../../utils/url-path-utils';
import {KnownProviderIcons} from '../../data/known-provider-icons';

export interface ProcessTaskDetailsPageItem {
    task: ProcessInstanceTaskEntity;
    instance: ProcessInstanceEntity | null;
    process: ProcessEntity | null;
    node: ProcessNodeEntity | null;
    provider: ProcessNodeProvider | null;
}

const PROCESS_INSTANCE_STATUS_COLORS: Record<ProcessInstanceStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    [ProcessInstanceStatus.Created]: 'default',
    [ProcessInstanceStatus.Running]: 'info',
    [ProcessInstanceStatus.Paused]: 'warning',
    [ProcessInstanceStatus.Completed]: 'success',
    [ProcessInstanceStatus.Aborted]: 'error',
    [ProcessInstanceStatus.Failed]: 'error',
};

const PROCESS_TASK_STATUS_COLORS: Record<ProcessTaskStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    [ProcessTaskStatus.Running]: 'info',
    [ProcessTaskStatus.Paused]: 'warning',
    [ProcessTaskStatus.Completed]: 'success',
    [ProcessTaskStatus.Aborted]: 'error',
    [ProcessTaskStatus.Failed]: 'error',
    [ProcessTaskStatus.Restarted]: 'warning',
};

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

    return createStaffPath(`/processes/${item.process.id}/versions/${item.task.processVersion}?instanceId=${item.instance.id}`);
}

export function getProcessTaskName(item?: ProcessTaskDetailsPageItem | null): string {
    if (item?.node != null && item.provider != null) {
        return getNodeName(item.node, item.provider);
    }

    if (item?.node?.name != null && item.node.name.trim().length > 0) {
        return item.node.name;
    }

    return 'Unbenannte Aufgabe';
}

export function getProcessTaskDescription(item?: ProcessTaskDetailsPageItem | null): string {
    if (item?.node != null && item.provider != null) {
        const description = getNodeDescription(item.node, item.provider);
        if (description.trim().length > 0) {
            return description;
        }
    }

    if (item?.node?.description != null && item.node.description.trim().length > 0) {
        return item.node.description;
    }

    return 'Keine Kurzbeschreibung hinterlegt.';
}

export function getProcessTaskStatusLabel(item?: ProcessTaskDetailsPageItem | null): string {
    if (item?.instance?.statusOverride != null && item.instance.statusOverride.trim().length > 0) {
        return item.instance.statusOverride;
    }

    if (item?.instance != null) {
        return ProcessInstanceStatusLabels[item.instance.status];
    }

    if (item?.task.statusOverride != null && item.task.statusOverride.trim().length > 0) {
        return item.task.statusOverride;
    }

    return ProcessTaskStatusLabels[item?.task.status ?? ProcessTaskStatus.Running];
}

export function getProcessTaskStatusColor(item?: ProcessTaskDetailsPageItem | null): 'default' | 'info' | 'success' | 'warning' | 'error' {
    if (item?.instance != null) {
        return PROCESS_INSTANCE_STATUS_COLORS[item.instance.status];
    }

    return PROCESS_TASK_STATUS_COLORS[item?.task.status ?? ProcessTaskStatus.Running];
}

function getProcessTaskStatusIcon(item?: ProcessTaskDetailsPageItem | null): ReactElement | undefined {
    const status = item?.instance?.status ?? item?.task.status;

    if (status === ProcessInstanceStatus.Running || status === ProcessTaskStatus.Running) {
        return <AccountCircle fontSize="small" />;
    }

    if (status === ProcessTaskStatus.Restarted) {
        return <Replay fontSize="small" />;
    }

    return undefined;
}

export function getProcessTaskNodeIcon(item?: ProcessTaskDetailsPageItem | null): ReactElement {
    const ProviderIcon = (
        (item?.provider != null && (
            KnownProviderIcons[item.provider.componentKey] ||
            KnownProviderIcons[item.provider.key]
        )) ||
        Assignment
    );

    return <ProviderIcon />;
}

async function fetchProcessTaskDetails(taskId: string): Promise<ProcessTaskDetailsPageItem> {
    const task = await new ProcessInstanceTaskApiService().retrieve(Number(taskId));
    const node = await new ProcessNodeApiService().retrieve(task.processNodeId);

    const [instance, process, provider] = await Promise.all([
        new ProcessInstanceApiService().retrieve(task.processInstanceId),
        new ProcessDefinitionApiService().retrieve(task.processId),
        new ProcessNodeProviderApiService().getNodeProvider(node.processNodeDefinitionKey, node.processNodeDefinitionVersion),
    ]);

    return {
        task,
        instance,
        process,
        node,
        provider,
    };
}

function buildProcessTaskHeaderBadges(item?: ProcessTaskDetailsPageItem): ReactNode[] | undefined {
    if (item == null) {
        return undefined;
    }

    const badges: ReactNode[] = [
        <Chip
            key="task-status"
            label={getProcessTaskStatusLabel(item)}
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
    return (
        <PageWrapper
            title="Aufgabe"
            fullWidth
            background
        >
            <GenericDetailsPage<ProcessTaskDetailsPageItem, string, undefined>
                header={(item) => {
                    const processPath = getProcessTaskProcessPath(item);

                    return {
                        icon: ModuleIcons.tasks,
                        title: 'Aufgabe',
                        badge: buildProcessTaskHeaderBadges(item),
                        actions: [
                            {
                                label: 'Vorgang aufrufen',
                                icon: <OpenInNewIcon />,
                                href: processPath ?? '#',
                                variant: 'contained',
                                disabled: processPath == null,
                                disabledTooltip: 'Die Vorgangsdetails stehen noch nicht zur Verfügung.',
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Aufgaben',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography
                                        variant="body1"
                                        component="p"
                                    >
                                        In der Aufgabenansicht sehen Sie die wichtigsten Metadaten der aktuell zugewiesenen Aufgabe
                                        und können diese im Bearbeitungs-Tab fachlich abarbeiten.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        component="p"
                                    >
                                        Noch nicht implementierte Funktionen wie Kommunikation, Wiedervorlage oder Neu-Zuweisung
                                        sind bereits vorgesehen, bleiben aber vorerst deaktiviert.
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
                    },
                    {
                        path: '/tasks/:instanceId/:taskId/communication',
                        label: 'Kommunikation',
                        isDisabled: () => true,
                    },
                ]}
                initializeItem={() => createEmptyProcessTaskDetailsPageItem()}
                fetchData={(_, taskId) => fetchProcessTaskDetails(taskId)}
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
            />
        </PageWrapper>
    );
}
