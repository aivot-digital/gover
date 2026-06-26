import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useEffect, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {AlertComponent} from '../../../../components/alert/alert-component';
import type {StorageProviderEntity} from '../../entities/storage-provider-entity';
import {StorageProvidersApiService} from '../../storage-providers-api-service';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';

export function StorageProviderDetailsPageTest() {
    const {
        item: storageProvider,
    } = useContext<GenericDetailsPageContextType<StorageProviderEntity, void>>(GenericDetailsPageContext);

    const [testResult, setTestResult] = useState<{ success: boolean; error?: string } | null>(null);
    const [isTesting, setIsTesting] = useState(false);
    const [writable, setWritable] = useState(false);

    useEffect(() => {
        setTestResult(null);
    }, [writable]);

    const handleTest = async () => {
        if (!storageProvider) return;
        setIsTesting(true);
        setTestResult(null);
        try {
            const api = new StorageProvidersApiService();
            const result = await api.testStorageProvider(storageProvider.id, writable);
            setTestResult(result);
        } catch (e) {
            setTestResult({ success: false, error: 'Unbekannter Fehler beim Testen.' });
        } finally {
            setIsTesting(false);
        }
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Test des Speicheranbieters
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Um die korrekte Funktion eines Speicheranbieters sicherzustellen, können Sie hier einen Test durchführen.
                Technisch wird ein Health-Check durchgeführt, bei welchem Gover eine Anfrage an den Speicheranbieter sendet und prüft, ob eine erfolgreiche Verbindung aufgebaut werden kann.
            </Typography>
            <Typography sx={{mb: 1, maxWidth: 900}}>
                Optional kann auch die Beschreibbarkeit des Anbieters getestet werden. Je nach Speicheranbieter kann es sein, dass zu diesem Zweck eine Testdatei hochgeladen und wieder gelöscht wird. Aktivieren Sie diese Option nur, wenn Sie sicher sind, dass dies bei Ihrem Speicheranbieter keine Probleme verursacht.
            </Typography>

            <CheckboxFieldComponent
                label="Beschreibbarkeit testen (optional)"
                value={writable}
                onChange={setWritable}
                disabled={!!storageProvider?.readOnlyStorage}
                hint={storageProvider?.readOnlyStorage ? 'Diese Option ist deaktiviert, da der Speicheranbieter als read-only (nur lesend) konfiguriert ist.' : undefined}
            />

            <Box
                sx={{
                    mt: 2,
                    mb: 2,
                }}
            >
                <Button
                    onClick={handleTest}
                    variant="contained"
                    startIcon={<ScienceOutlinedIcon />}
                    disabled={storageProvider == null || isTesting}
                >
                    Speicheranbieter testen
                </Button>
            </Box>

            {testResult && (
                <AlertComponent
                    color={testResult.success ? 'success' : 'error'}
                    title={testResult.success ? (writable ? 'Verbindung und Beschreibbarkeit erfolgreich getestet' : 'Verbindung erfolgreich getestet') : (writable ? 'Test von Verbindung und Beschreibbarkeit fehlgeschlagen' : 'Test der Verbindung fehlgeschlagen')}
                >
                    {testResult.success
                        ? writable
                            ? 'Die Verbindung zum Speicheranbieter war erfolgreich und der Anbieter ist beschreibbar.'
                            : 'Die Verbindung zum Speicheranbieter war erfolgreich.'
                        : (
                            <>
                                {writable
                                    ? 'Die Verbindung zum Speicheranbieter und dessen Beschreibbarkeit konnte nicht bestätigt werden.'
                                    : 'Die Verbindung zum Speicheranbieter konnte nicht bestätigt werden.'}
                                {testResult.error && (
                                    <ExpandableCodeBlock value={testResult.error} sx={{ mt: 2 }} />
                                )}
                            </>
                        )
                    }
                </AlertComponent>
            )}
        </Box>
    );
}