import {Menu, MenuItem, Typography} from '@mui/material';
import {Link as RouterLink, useParams} from 'react-router-dom';
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import {useRef, useState} from 'react';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {type GenericDetailsPageControlRef} from '../../../../components/generic-details-page/generic-details-page-props';
import {Chip} from '../../../../components/chip/chip';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import FolderShared from '@aivot/mui-material-symbols-400-n25-outlined/FolderShared';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import {getProcessTaskBasePath} from './process-task-view-page';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {type ProcessInstanceDetails} from '../../entities/process-instance-details';
import {ProcessInstanceStatusLabels, ProcessInstanceStatusColor} from '../../enums/process-instance-status';
import {Permission} from '../../../../data/permissions/permission';
import {useRefreshPermissionSet} from '../../../permissions/hooks/use-permissions';

export function ProcessInstanceDetailsPage() {
    const {id} = useParams();
    const refreshPermissionSet = useRefreshPermissionSet();
    const controlRef = useRef<GenericDetailsPageControlRef>(null);
    const [taskMenu, setTaskMenu] = useState<{
        anchor: Element;
        item: ProcessInstanceDetails;
    } | null>(null);
    return (
        <PageWrapper
            title="Vorgang"
            fullWidth
            background
        >
            <GenericDetailsPage<ProcessInstanceDetails, string, undefined>
                key={id}
                controlRef={controlRef}
                entityType={ServerEntityType.ProcessInstances}
                header={(item) => ({
                    icon: <FolderShared />,
                    title: 'Vorgang',
                    badge:
                        item == null
                            ? undefined
                            : [
                                  <Chip
                                      key="status"
                                      label={
                                          item.instance.statusOverride?.trim() ||
                                          ProcessInstanceStatusLabels[item.instance.status]
                                      }
                                      title={
                                          item.instance.statusOverride?.trim()
                                              ? `Systemstatus: ${ProcessInstanceStatusLabels[item.instance.status]}`
                                              : undefined
                                      }
                                      color={ProcessInstanceStatusColor[item.instance.status]}
                                      mode="soft"
                                      size="small"
                                  />,
                                  ...(item.instance.createdForTestClaimId == null
                                      ? []
                                      : [
                                            <Chip
                                                key="test"
                                                label="Test-Vorgang"
                                                color="warning"
                                                mode="soft"
                                                size="small"
                                            />,
                                        ]),
                              ],
                    actions: [
                        {
                            tooltip: 'Vorgang aktualisieren',
                            icon: <Refresh />,
                            onClick: () => controlRef.current?.refresh(),
                        },
                        {
                            label: 'Aktive Aufgabe aufrufen',
                            icon: <Task />,
                            iconPosition: 'end',
                            variant: 'contained',
                            disabled: item == null || item.activeTasks.length === 0,
                            disabledTooltip: 'Dieser Vorgang hat keine aktive Aufgabe.',
                            ...(item?.activeTasks.length === 1
                                ? {
                                      to: getProcessTaskBasePath(item.instance.id, item.activeTasks[0].id),
                                  }
                                : {
                                      onClick: (event) => {
                                          if (item != null && item.activeTasks.length > 1) {
                                              setTaskMenu({
                                                  anchor: event.currentTarget,
                                                  item,
                                              });
                                          }
                                      },
                                  }),
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zum Vorgang',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Ein Vorgang ist die konkrete Ausführung eines Prozesses, zum Beispiel die
                                    Bearbeitung eines eingereichten Antrags. Der Prozess legt fest, welche Schritte
                                    erforderlich sind und wie sie zusammenhängen. In einem Vorgang können mehrere
                                    Aufgaben entstehen, die unterschiedlichen Personen zugewiesen sind.
                                </Typography>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Unter „Allgemeine Informationen“ sehen Sie seine Kennung, den aktuellen Stand und
                                    die aktiven Aufgaben. Über „Aktive Aufgabe aufrufen“ gelangen Sie zu den
                                    Aufgabendetails.
                                </Typography>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Im „Verlauf“ verfolgen Sie die bisherigen Schritte, unter „Kommunikation“ tauschen
                                    Sie Nachrichten aus und unter „Vorgangsdaten“ sehen Sie die erfassten Angaben. Über
                                    die Aktionen können Sie den Vorgang zuweisen, sperren oder beenden. Zuweisungen
                                    einzelner Aufgaben sind von der Vorgangszuweisung unabhängig.
                                </Typography>
                                <Typography component="p">
                                    Unter „Berechtigungen“ sehen Sie die aus dem Prozess übernommenen Zugriffsrechte und
                                    können sie mit entsprechender Berechtigung für diesen Vorgang anpassen. Der Prozess
                                    und andere Vorgänge bleiben unverändert. Berechtigungen aus Rollen und der
                                    verwaltenden Organisationseinheit gelten weiterhin.
                                </Typography>
                            </>
                        ),
                    },
                })}
                tabs={[
                    {
                        path: '/process-instances/:id',
                        label: 'Allgemeine Informationen',
                    },
                    ...[
                        ['history', 'Verlauf'],
                        ['communication', 'Kommunikation'],
                        ['data', 'Vorgangsdaten'],
                    ].map(([path, label]) => ({
                        path: `/process-instances/:id/${path}`,
                        label,
                        isDisabled: () => true,
                        disabledTooltip: 'Diese Funktion ist noch nicht verfügbar.',
                    })),
                    {
                        path: '/process-instances/:id/permissions',
                        label: 'Berechtigungen',
                    },
                ]}
                initializeItem={() => ({
                    instance: new ProcessInstanceApiService().initialize(),
                    processName: '',
                    departmentId: 0,
                    departmentName: null,
                    triggerName: '',
                    triggerType: null,
                    activeTasks: [],
                })}
                fetchData={async (_, instanceId) => {
                    // Grants may have changed in another user's session since the list was loaded.
                    // Wait for fresh permissions before GenericDetailsPage checks access and renders actions.
                    const [details] = await Promise.all([
                        new ProcessInstanceApiService().retrieveDetails(Number(instanceId)),
                        refreshPermissionSet(),
                    ]);
                    return details;
                }}
                getTabTitle={(item) => `Vorgang ${item.instance.caseNumber}`}
                getHeaderTitle={(item, _, notFound) =>
                    notFound
                        ? 'Vorgang nicht gefunden'
                        : item == null
                          ? 'Vorgang'
                          : `Vorgang: ${item.instance.caseNumber}`
                }
                parentLink={{
                    label: 'Liste der Vorgänge',
                    to: '/process-instances',
                }}
                permissionCheck={{
                    scope: {
                        type: 'processInstance',
                        getResourceId: (item) => item.instance.id,
                    },
                    read: Permission.PROCESS_INSTANCE_READ,
                    update: Permission.PROCESS_INSTANCE_UPDATE,
                }}
            />
            <Menu
                anchorEl={taskMenu?.anchor}
                open={taskMenu != null}
                onClose={() => setTaskMenu(null)}
                slotProps={{list: {'aria-label': 'Aktive Aufgabe auswählen'}}}
            >
                {taskMenu?.item.activeTasks.map((task) => (
                    <MenuItem
                        key={task.id}
                        component={RouterLink}
                        to={getProcessTaskBasePath(taskMenu.item.instance.id, task.id)}
                        onClick={() => setTaskMenu(null)}
                    >
                        {task.name}
                    </MenuItem>
                ))}
            </Menu>
        </PageWrapper>
    );
}
