import {useConfirm} from '../providers/confirm-provider';
import React, {useCallback} from 'react';
import {Typography} from '@mui/material';
import {AlertComponent} from '../components/alert/alert-component';
import {ProcessDefinitionApiService} from '../modules/process/services/process-definition-api-service';
import {downloadObjectFile} from '../utils/download-utils';
import {showApiErrorSnackbar} from '../slices/snackbar-slice';
import {useAppDispatch} from './use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../slices/shell-slice';

export function useProcessExport() {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    return useCallback((processId: number, processVersion: number) => {
        confirm({
            title: 'Prozess exportieren',
            children: (
                <>
                    <Typography>
                        Sie können den Prozess exportieren, um ihn z. B. in einem anderen System weiterzuverwenden oder
                        zu archivieren.
                        Der Export erfolgt im offenen .json-Format.
                    </Typography>

                    <AlertComponent
                        color="info"
                        title="Wichtig"
                        sx={{
                            mt: 2,
                        }}
                    >
                        <p>
                            Zum Schutz Ihrer Daten werden bestimmte Informationen aus dem Export ausgeschlossen und sind
                            für die importierende Person nicht sichtbar.
                            Dazu zählen u. a. Personenkreis-Definitionen, Referenzen auf lokale Dateien und Medien und
                            Referenzen auf Organisationseinheiten.
                        </p>
                        <p>
                            Bei Bedarf müssen Sie diese Informationen nach einem Import im Zielsystem neu konfigurieren.
                        </p>
                    </AlertComponent>
                </>
            ),
            confirmButtonText: 'Prozess als .json-Datei herunterladen',
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return null;
                }

                dispatch(setLoadingMessage({
                    message: 'Prozess wird exportiert',
                    blocking: true,
                    estimatedTime: 1000,
                }));

                return new ProcessDefinitionApiService()
                    .export(processId, processVersion);
            })
            .then((exp) => {
                if (exp == null) {
                    return null;
                }
                downloadObjectFile(`${exp.process.internalTitle ?? exp.version.publicTitle}.process.prosuna.json`, exp);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Der Prozess konnte nicht exportiert werden.'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    }, []);
}