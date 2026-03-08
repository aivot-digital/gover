import {Box} from "@mui/material";
import {useProcessNodeEditorContext} from "../process-node-editor-context";
import {TextFieldComponent} from "../../../../../../../components/text-field/text-field-component";
import Typography from "@mui/material/Typography";

export function ProcessNodeEditorOutputsTab() {
    const {
        node,
        setNode,
        provider,
    } = useProcessNodeEditorContext();

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
                        mt={1} mb={2} maxWidth={400}>
                Die Zuweisung von Datenvariablen für die Ausgangsdaten ist optional.
                Ohne Zuweisung sind Ergebnisse ausschließlich über die Metadaten-Ebene zugänglich.
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
                    />
                ))
            }

            <Typography variant="h4" mt={4}>
                Datenstruktur der Ausgangsdaten
            </Typography>
            <Typography variant="body1"
                        mt={1} mb={2} maxWidth={400}>
                Struktur und Übersicht der von diesem Element erzeugten und zur Verfügung gestellten Datenvariablen.
            </Typography>

            <Box mt={2}>
                {
                    provider.outputs.map((output, index, all) => (
                        <Box key={output.key}
                             sx={{
                                 borderTopLeftRadius: index === 0 ? '0.5rem' : 0,
                                 borderTopRightRadius: index === 0 ? '0.5rem' : 0,
                                 borderBottomLeftRadius: index === all.length - 1 ? '0.5rem' : 0,
                                 borderBottomRightRadius: index === all.length - 1 ? '0.5rem' : 0,
                                 border: '1px solid #ccc',
                                 p: 1,
                             }}
                        >
                            <Box>
                            <span style={{
                                fontWeight: 'bold',
                            }}>{output.key}</span>
                            </Box>

                            <Typography variant="body2">
                                {output.description}
                            </Typography>
                        </Box>
                    ))
                }
            </Box>
        </Box>
    );
}