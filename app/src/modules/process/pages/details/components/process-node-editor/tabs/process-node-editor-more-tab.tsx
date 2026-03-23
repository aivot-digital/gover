import {Box} from "@mui/material";
import {NumberFieldComponent} from "../../../../../../../components/number-field/number-field-component";
import {useProcessNodeEditorContext} from "../process-node-editor-context";
import {
    RichTextInputComponent
} from "../../../../../../../components/rich-text-input-component/rich-text-input-component";
import Typography from "@mui/material/Typography";

export function ProcessNodeEditorMoreTab() {
    const {
        node,
        setNode,
    } = useProcessNodeEditorContext();

    return (
        <Box>
            <Typography variant="h6">
                Weitere Eigenschaften des Elements
            </Typography>
            <Typography variant="body1" mb={2}>
                Konfigurieren Sie zusätzliche Eigenschaften dieses Prozesselementes.
            </Typography>

            <NumberFieldComponent
                label="Maximale Laufzeit"
                hint="Maximale Laufzeit dieses Elementes in Tagen. Nach Ablauf der Zeit wird das Element automatisch als „Abgelaufen“ markiert. Nach 70% Ablauf der Zeit wird eine Benachrichtigung an den Verantwortlichen gesendet."
                suffix="Tage"
                decimalPlaces={0}
                value={node.timeLimitDays ?? undefined}
                onChange={(val) => {
                    setNode({
                        ...node,
                        timeLimitDays: val ?? null,
                    });
                }}
            />

            <RichTextInputComponent
                label="Fachliche Anforderungen"
                hint="Beschreiben Sie die fachlichen Anforderungen oder Voraussetzungen für die Ausführung dieses Elemente."
                value={node.requirements}
                onChange={(val) => {
                    setNode({
                        ...node,
                        requirements: val,
                    });
                }}
                sx={{
                    mt: 2,
                }}
            />



            <RichTextInputComponent
                label="Notizen"
                hint="Fügen Sie zusätzliche Notizen oder Kommentare zu diesem Elemente hinzu."
                value={node.notes}
                onChange={(val) => {
                    setNode({
                        ...node,
                        notes: val,
                    });
                }}
                sx={{
                    mt: 2,
                }}
            />
        </Box>
    );
}