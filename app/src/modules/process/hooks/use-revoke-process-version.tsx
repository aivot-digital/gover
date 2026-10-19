import React, {useCallback} from 'react';
import {Stack, Typography} from '@mui/material';
import {AlertComponent} from '../../../components/alert/alert-component';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useConfirm} from '../../../providers/confirm-provider';
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {type ProcessEntity} from '../entities/process-entity';
import {type ProcessVersionEntity} from '../entities/process-version-entity';
import {ProcessDefinitionVersionApiService} from '../services/process-definition-version-api-service';

export function useRevokeProcessVersion(): (process: ProcessEntity, version: ProcessVersionEntity) => Promise<ProcessVersionEntity | null> {
    const confirm = useConfirm();
    const dispatch = useAppDispatch();

    return useCallback(async (process, version) => {
        let loadingStarted = false;

        try {
            const confirmed = await confirm({
                title: 'Prozessversion zurückziehen',
                children: (
                    <Stack spacing={2}>
                        <Typography>
                            Möchten Sie die veröffentlichte Prozessversion {version.processVersion} wirklich zurückziehen?
                            Neue Vorgänge können danach nicht mehr auf Basis dieser Version gestartet werden.
                            Bestehende Vorgänge bleiben erhalten und behalten die Prozessversion, mit der sie angelegt wurden.
                        </Typography>

                        <AlertComponent
                            color="warning"
                            title="Diese Prozessversion ist aktuell veröffentlicht"
                        >
                            Durch das Zurückziehen ist der Prozess für neue Vorgänge nicht mehr öffentlich verfügbar,
                            solange keine andere Version veröffentlicht wird.
                        </AlertComponent>
                    </Stack>
                ),
                confirmButtonText: 'Prozessversion zurückziehen',
            });

            if (!confirmed) {
                return null;
            }

            loadingStarted = true;
            dispatch(setLoadingMessage({
                message: 'Prozessversion wird zurückgezogen',
                blocking: true,
                estimatedTime: 1200,
            }));

            const revokedVersion = await new ProcessDefinitionVersionApiService()
                .revoke({
                    processDefinitionId: process.id,
                    processDefinitionVersion: version.processVersion,
                });

            dispatch(showSuccessSnackbar('Die Prozessversion wurde zurückgezogen.'));
            return revokedVersion;
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Die Prozessversion konnte nicht zurückgezogen werden.'));
            return null;
        } finally {
            if (loadingStarted) {
                dispatch(clearLoadingMessage());
            }
        }
    }, [confirm, dispatch]);
}
