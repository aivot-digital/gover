import React, {useCallback} from 'react';
import {Stack, Typography} from '@mui/material';
import {AlertComponent} from '../../../components/alert/alert-component';
import {useConfirm} from '../../../providers/confirm-provider';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {type ProcessEntity} from '../entities/process-entity';
import {ProcessDefinitionApiService} from '../services/process-definition-api-service';

interface DeleteProcessOptions {
    onDeleted?: (process: ProcessEntity) => void;
}

export function useDeleteProcess(): (process: ProcessEntity, options?: DeleteProcessOptions) => Promise<boolean> {
    const confirm = useConfirm();
    const dispatch = useAppDispatch();

    return useCallback(async (process, options = {}) => {
        let loadingStarted = false;

        try {
            const confirmed = await confirm({
                title: 'Prozess löschen',
                children: (
                    <Stack spacing={2}>
                        <Typography>
                            Möchten Sie den Prozess wirklich löschen?
                            Alle zugehörigen Versionen, Modellierungen und Vorgänge werden dabei entfernt.
                            Dieser Vorgang kann nicht rückgängig gemacht werden.
                        </Typography>

                        {
                            process.publishedVersion != null &&
                            <AlertComponent
                                color="warning"
                                title="Dieser Prozess ist aktuell veröffentlicht"
                            >
                                Durch das Löschen wird auch die veröffentlichte Version entfernt.
                                Öffentliche Einstiegspunkte für diesen Prozess sind danach nicht mehr verfügbar.
                            </AlertComponent>
                        }
                    </Stack>
                ),
                confirmationText: process.internalTitle,
                inputLabel: 'Interner Titel zur Bestätigung',
                inputPlaceholder: process.internalTitle,
                confirmButtonText: 'Prozess endgültig löschen',
                isDestructive: true,
            });

            if (!confirmed) {
                return false;
            }

            loadingStarted = true;
            dispatch(setLoadingMessage({
                message: 'Lösche Prozess',
                blocking: false,
                estimatedTime: 1200,
            }));

            await new ProcessDefinitionApiService()
                .destroy(process.id);

            dispatch(showSuccessSnackbar('Der Prozess wurde erfolgreich gelöscht.'));
            options.onDeleted?.(process);
            return true;
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Der Prozess konnte nicht gelöscht werden.'));
            return false;
        } finally {
            if (loadingStarted) {
                dispatch(clearLoadingMessage());
            }
        }
    }, [confirm, dispatch]);
}
