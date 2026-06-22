import {Box} from '@mui/material';
import {useProcessNodeEditorContext} from '../process-node-editor-context';
import {TextFieldComponent} from '../../../../../../../components/text-field/text-field-component';
import Typography from '@mui/material/Typography';
import {ProcessNodeOutputCard} from '../../../../../components/process-node-output-card';
import {ProcessDataKeyInputComponent} from '../../../../../../../views/process-data-key-input-field-view';
import {quoteString} from '../../../../../../../utils/string-utils';

export function ProcessNodeEditorOutputsTab() {
    const {
        node: localNode,
        setNode,
        provider,
        isEditable,
        problems,
    } = useProcessNodeEditorContext();

    if (provider.outputs.length === 0) {
        return (
            <Box>
                <Typography variant="h6">
                    Ausgangsdaten
                </Typography>
                <Typography
                    variant="body2"
                    color="textSecondary"
                >
                    Dieses Element erzeugt keine Ausgangsdaten oder bietet einen alternativen Weg, um Vorgangsdaten zu
                    schreiben.
                </Typography>
            </Box>
        );
    }

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
            <Typography variant="body1"
                        mt={1}
                        mb={2}
                        maxWidth={400}>
                Über den eindeutigen Datenschlüssel kann auf die erzeugten Elementdaten dieses Prozesselementes zugegriffen werden (siehe {quoteString('Datenstruktur der Ausgangsdaten')} weiter unten).
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
            <Typography variant="body1"
                        mt={1}
                        mb={2}
                        maxWidth={400}>
                Die Zuweisung von Datenvariablen für die Ausgangsdaten ist optional.
                Ohne Zuweisung sind Ergebnisse ausschließlich über die Elementdaten zugänglich.
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
                mt={4}
            >
                Datenstruktur der Ausgangsdaten
            </Typography>
            <Typography
                variant="body1"
                mt={1}
                mb={2}
                maxWidth={400}
            >
                Struktur und Übersicht der von diesem Element erzeugten und zur Verfügung gestellten Datenvariablen.
            </Typography>

            <Box mt={2}>
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
        </Box>
    );
}
