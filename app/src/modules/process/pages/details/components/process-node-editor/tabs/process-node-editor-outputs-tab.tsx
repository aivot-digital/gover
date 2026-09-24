import {Box} from '@mui/material';
import {useProcessNodeEditorContext} from '../process-node-editor-context';
import {TextFieldComponent} from '../../../../../../../components/text-field/text-field-component';
import Typography from '@mui/material/Typography';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {ProcessNodeOutputCard} from '../../../../../components/process-node-output-card';
import {ProcessDataKeyInputComponent} from '../../../../../../../views/process-data-key-input-field-view';

export function ProcessNodeEditorOutputsTab() {
    const {
        node: localNode,
        setNode,
        provider,
        isEditable,
        problems,
    } = useProcessNodeEditorContext();
    const hasOutputs = provider.outputs.length > 0;

    return (
        <Box
            sx={{
                pt: 1,
                pb: 2,
            }}
        >
            <Typography variant="h4">
                Datenschlüssel
            </Typography>
            <Typography
                variant="body1"
                sx={{
                    mt: 1,
                    mb: 2,
                    maxWidth: 400
                }}>
                Mit dem Datenschlüssel greifen Sie auf die Elementdaten und Ausführungsmetadaten dieses Prozesselements
                zu.
            </Typography>

            <TextFieldComponent
                label="Datenschlüssel"
                hint="Eindeutiger Schlüssel zur Identifikation dieses Elementes im Vorgang."
                value={localNode.dataKey}
                onChange={(val) => {
                    setNode({
                        ...localNode,
                        dataKey: val ?? '',
                    }, false);
                }}
                required={true}
                maxCharacters={32}
                error={problems?.commonErrors.dataKey}
                disabled={!isEditable}
            />

            <Typography
                variant="h4"
                sx={{
                    mt: 2,
                }}
            >
                Ausgangsdaten
            </Typography>
            {hasOutputs ? <>
                <Typography
                    variant="body1"
                    sx={{
                        mt: 1,
                        mb: 2,
                        maxWidth: 400
                    }}>
                    Sie können die Ausgangsdaten dieses Prozesselements optional in die Vorgangsdaten übernehmen.
                    Ohne Zuordnung bleiben die Werte über die Elementdaten zugänglich.
                </Typography>

                {
                    provider.outputs.map((output) => (
                        <ProcessDataKeyInputComponent
                            key={output.key}
                            label={output.label}
                            hint={output.description}
                            value={localNode.outputMappings?.[output.key] ?? ''}
                            onChange={(val) => {
                                setNode({
                                    ...localNode,
                                    outputMappings: {
                                        ...localNode.outputMappings,
                                        [output.key]: val,
                                    },
                                }, false);
                            }}
                            disabled={!isEditable}
                            disableWildCards={true}
                        />
                    ))
                }

                <Typography
                    variant="h4"
                    sx={{
                        mt: 4
                    }}
                >
                    Datenstruktur der Ausgangsdaten
                </Typography>
                <Typography
                    variant="body1"
                    sx={{
                        mt: 1,
                        mb: 2,
                        maxWidth: 400
                    }}>
                    Hier sehen Sie die verfügbaren Ausgangsdaten mit ihren Datenpfaden in den Elementdaten und einer
                    Beschreibung der Werte.
                </Typography>

                <Box sx={{
                    mt: 2
                }}>
                    {
                        provider.outputs.map((output) => (
                            <ProcessNodeOutputCard
                                key={output.key}
                                label={output.label}
                                outputKey={`_.${localNode.dataKey}.${output.key}`}
                                description={output.description}
                                sx={{
                                    mb: 1,
                                    '&:last-child': {
                                        mb: 0,
                                    },
                                }}
                            />
                        ))
                    }
                </Box>
            </> :
                <Box
                    sx={{
                        mt: 2,
                        p: 2,
                        width: '100%',
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 2.5,
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1.5,
                        bgcolor: 'rgba(15, 23, 42, 0.035)',
                    }}
                >
                    <Box
                        aria-hidden="true"
                        sx={{
                            display: 'grid',
                            placeItems: 'center',
                            width: 40,
                            height: 40,
                            flexShrink: 0,
                            borderRadius: '50%',
                            bgcolor: 'action.selected',
                            color: 'text.secondary',
                        }}
                    >
                        <DataObject sx={{fontSize: 24}}/>
                    </Box>
                    <Box sx={{minWidth: 0}}>
                        <Typography variant="body1" component="h5" sx={{fontWeight: 600}}>
                            Keine zuweisbaren Ausgangsdaten
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{mt: 0.75}}>
                            Dieses Prozesselement erzeugt keine Ausgangsdaten oder schreibt Vorgangsdaten auf anderem Weg.
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{mt: 1}}>
                            Über den Datenschlüssel können Sie dennoch auf Metadaten zu seiner Ausführung zugreifen.
                        </Typography>
                    </Box>
                </Box>
            }
        </Box>
    );
}
