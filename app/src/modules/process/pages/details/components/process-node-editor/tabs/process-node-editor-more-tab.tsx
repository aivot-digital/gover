import {Box} from "@mui/material";
import {NumberFieldComponent} from "../../../../../../../components/number-field/number-field-component";
import {useProcessNodeEditorContext} from "../process-node-editor-context";
import {
    RichTextInputComponent
} from "../../../../../../../components/rich-text-input-component/rich-text-input-component";
import Typography from "@mui/material/Typography";
import {ProcessNodeType} from '../../../../../services/process-node-provider-api-service';

export function ProcessNodeEditorMoreTab() {
    const {
        node,
        provider,
        setNode,
        isEditable,
    } = useProcessNodeEditorContext();

    // TODO: Narrow this to manual and semi-automated actions once providers expose execution-mode metadata.
    const showTimeLimit = provider.type === ProcessNodeType.Action;

    return (
        <Box
            sx={{
                pt: 1,
                pb: 2,
            }}
        >
            <Typography variant="h4">
                Weitere Eigenschaften des Elements
            </Typography>
            <Typography
                variant="body1"
                sx={{
                    mt: 1,
                    mb: 2,
                    maxWidth: 400
                }}>
                Konfigurieren Sie zusätzliche Eigenschaften dieses Prozesselementes.
            </Typography>

            {
                showTimeLimit &&
                <NumberFieldComponent
                    label="Frist (Maximale Laufzeit)"
                    hint="Definiert die Fälligkeit einer Aufgabe in Tagen ab deren Erstellung. Die Frist kann für Auswertungen, Hinweise oder spätere Ablaufsteuerung genutzt werden."
                    suffix="Tage"
                    decimalPlaces={0}
                    minValue={1}
                    maxValue={3652}
                    value={node.timeLimitDays ?? undefined}
                    onChange={(val) => {
                        setNode({
                            ...node,
                            timeLimitDays: val ?? null,
                        }, false);
                    }}
                    disabled={!isEditable}
                />
            }

            <RichTextInputComponent
                label="Fachliche Anforderungen"
                hint="Beschreiben Sie die fachliche Logik oder Vorgaben, die mit diesem Prozesselement umgesetzt werden."
                value={node.requirements}
                onChange={(val) => {
                    setNode({
                        ...node,
                        requirements: val,
                    }, false);
                }}
                sx={{
                    mt: showTimeLimit ? 2 : 0,
                }}
                disabled={!isEditable}
            />

            <RichTextInputComponent
                label="Notizen"
                hint="Erfassen Sie interne Hinweise zur Modellierung dieses Elements. Notizen erscheinen zusätzlich in der zentralen Notizübersicht."
                value={node.notes}
                onChange={(val) => {
                    setNode({
                        ...node,
                        notes: val,
                    }, false);
                }}
                sx={{
                    mt: 2,
                }}
                disabled={!isEditable}
            />
        </Box>
    );
}
