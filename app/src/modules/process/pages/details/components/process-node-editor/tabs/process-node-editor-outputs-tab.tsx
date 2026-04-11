import {Box} from '@mui/material';
import {useProcessNodeEditorContext} from '../process-node-editor-context';
import {TextFieldComponent} from '../../../../../../../components/text-field/text-field-component';
import Typography from '@mui/material/Typography';
import {ProcessNodeOutputCard} from '../../../../../components/process-node-output-card';

export function ProcessNodeEditorOutputsTab() {
    const {
        node,
        setNode,
        provider,
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
                    <TextFieldComponent
                        key={output.key}
                        label={output.label}
                        hint={output.description}
                        value={node.outputMappings?.[output.key] ?? ''}
                        onChange={(val) => {
                            setNode({
                                ...node,
                                outputMappings: {
                                    ...node.outputMappings,
                                    [output.key]: val,
                                },
                            }, false);
                        }}
                        muiPassTroughProps={{
                            margin: 'none',
                        }}
                        sx={{
                            mb: 2,
                        }}
                        startIcon="$."
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
                            outputKey={output.key}
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
