import {Box} from "@mui/material";
import {TextFieldComponent} from "../../../../../../../components/text-field/text-field-component";
import {useProcessNodeEditorContext} from "../process-node-editor-context";
import {ElementDerivationContext} from "../../../../../../elements/components/element-derivation-context";
import Typography from "@mui/material/Typography";
import {useEffect, useState} from "react";

export function ProcessNodeEditorConfigurationTab() {
    const {
        layout,
        node: localNode,
        setNode: setLocalNode,
    } = useProcessNodeEditorContext();

    const [firstDerivationDone, setFirstDerivationDone] = useState(false);

    useEffect(() => {
        setFirstDerivationDone(false);
    }, [layout]);

    return (
        <Box
            sx={{
                position: 'relative',
            }}
        >
            <Typography variant="h6">
                Allgemeine Eigenschaften des Elements
            </Typography>
            <Typography variant="body1"
                        mb={2}>
                Konfigurieren Sie dieses Prozesselement gemäß Ihrer fachlichen Anforderungen.
            </Typography>

            <TextFieldComponent
                label="Datenschlüssel"
                hint="Eindeutiger Schlüssel zur Identifikation der Ausgangsdaten dieses Elementes im Vorgang"
                value={localNode.dataKey}
                onChange={(val) => {
                    setLocalNode({
                        ...localNode,
                        dataKey: val ?? '',
                    });
                }}
                required={true}
                maxCharacters={32}
            />

            <TextFieldComponent
                label="Name des Elementes"
                hint="Wird im Vorgang und Prozess als Titel angezeigt"
                value={localNode.name}
                onChange={(val) => {
                    setLocalNode({
                        ...localNode,
                        name: val ?? null,
                    });
                }}
                maxCharacters={96}
            />

            <TextFieldComponent
                label="Kurzbeschreibung des Elements"
                hint="Wird im Vorgang und Prozess als Beschreibung angezeigt"
                value={localNode.description}
                onChange={(val) => {
                    setLocalNode({
                        ...localNode,
                        description: val ?? null,
                    });
                }}
                multiline={true}
                maxCharacters={512}
            />

            <Typography variant="h6"
                        mt={4}>
                Spezifische Eigenschaften des Elements
            </Typography>

            <ElementDerivationContext
                element={layout}
                elementData={localNode.configuration}
                onElementDataChange={(elementData) => {
                    setLocalNode({
                        ...localNode,
                        configuration: elementData,
                    });
                }}
                onDerivationFinished={() => {
                    if (!firstDerivationDone) {
                        setTimeout(() => {
                            setFirstDerivationDone(true);
                        }, 5000);
                    }
                }}
            />
        </Box>
    );
}