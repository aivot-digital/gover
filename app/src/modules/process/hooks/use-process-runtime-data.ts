import {useCallback, useEffect, useState} from 'react';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {useRefreshPermissionSet} from '../../permissions/hooks/use-permissions';
import {type ProcessInstanceEntity} from '../entities/process-instance-entity';
import {type ProcessInstanceTaskEntity} from '../entities/process-instance-task-entity';
import {type ProcessInstanceEventEntity} from '../entities/process-instance-event-entity';
import {type ProcessInstanceAttachmentEntity} from '../entities/process-instance-attachment-entity';
import {type ProcessInstanceAttachmentSetEntity} from '../entities/process-instance-attachment-set-entity';
import {ProcessInstanceApiService} from '../services/process-instance-api-service';
import {ProcessInstanceTaskApiService} from '../services/process-instance-task-api-service';
import {ProcessInstanceAttachmentApiService} from '../services/process-instance-attachment-api-service';
import {ProcessInstanceAttachmentSetApiService} from '../services/process-instance-attachment-set-api-service';

export interface ProcessRuntimeData {
    instance: ProcessInstanceEntity;
    tasks: ProcessInstanceTaskEntity[];
    events: ProcessInstanceEventEntity[];
    attachments: ProcessInstanceAttachmentEntity[];
    attachmentSets: ProcessInstanceAttachmentSetEntity[];
}

export function useProcessRuntimeData(instanceId: number | null) {
    const dispatch = useAppDispatch();
    const refreshPermissionSet = useRefreshPermissionSet();

    const [runtimeData, setRuntimeData] = useState<ProcessRuntimeData | null>(null);
    const [isRefreshingRuntimeData, setIsRefreshingRuntimeData] = useState(false);

    const loadRuntimeData = useCallback(() => {
        if (instanceId == null) {
            setRuntimeData(null);
            return Promise.resolve();
        }

        dispatch(setLoadingMessage({
            message: 'Lade Laufzeitdaten',
            blocking: false,
            estimatedTime: 1000,
        }));

        setIsRefreshingRuntimeData(true);
        return Promise.all([
            new ProcessInstanceApiService().retrieve(instanceId),
            // The permission set lists department-derived grants per instance, so it misses instances created
            // since it was loaded, such as test instances. Wait for it before the runtime actions are rendered.
            refreshPermissionSet(),
        ])
            .then(([instance]) => {
                return Promise.all([
                    Promise.resolve(instance),
                    new ProcessInstanceTaskApiService().listAll({
                        processInstanceId: instanceId,
                    }),
                    new ProcessInstanceAttachmentApiService().listAll({
                        processInstanceId: instanceId,
                    }),
                    new ProcessInstanceAttachmentSetApiService().listAll({
                        processInstanceId: instanceId,
                    }),
                    /* new ProcessInstanceEventApiService().listAll({
                        processInstanceId: instanceId,
                    })*/Promise.resolve([]),
                ]);
            })
            .then(([instance, tasks, attachments, attachmentSets, events]) => {
                setRuntimeData({
                    instance,
                    tasks: tasks.content,
                    attachments: attachments.content,
                    attachmentSets: attachmentSets.content,
                    events,
                });
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Vorgang konnte nicht geladen werden.'));
            })
            .finally(() => {
                setIsRefreshingRuntimeData(false);
                dispatch(clearLoadingMessage());
            });
    }, [instanceId, dispatch, refreshPermissionSet]);

    const clearRuntimeData = useCallback(() => {
        setRuntimeData(null);
    }, []);

    useEffect(() => {
        void loadRuntimeData();
    }, [loadRuntimeData]);

    return {
        runtimeData,
        isRefreshingRuntimeData,
        loadRuntimeData,
        clearRuntimeData,
    };
}
