import {useEffect, useRef, useState} from 'react';
import {Alert, Box} from "@mui/material";
import {NumberFieldComponent} from "../../../../../../../components/number-field/number-field-component";
import {CheckboxFieldComponent} from '../../../../../../../components/checkbox-field/checkbox-field-component';
import {useProcessNodeEditorContext} from "../process-node-editor-context";
import {
    RichTextInputComponent
} from "../../../../../../../components/rich-text-input-component/rich-text-input-component";
import {type ProcessNodeDeadlineDefinition} from '../../../../../services/process-node-provider-api-service';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {ElementEditorSectionHeader} from '../../../../../../../components/element-editor-section-header/element-editor-section-header';

export function ProcessNodeEditorMoreTab() {
    const {
        node,
        setNode,
        isEditable,
    } = useProcessNodeEditorContext();

    const [definitions, setDefinitions] = useState<ProcessNodeDeadlineDefinition[]>([]);
    const [problems, setProblems] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const currentNode = useRef(node);
    currentNode.current = node;

    useEffect(() => {
        let active = true;
        setLoading(true);
        setProblems([]);
        new ProcessNodeApiService().resolvePorts(currentNode.current)
            .then((resolution) => {
                if (active) {
                    setDefinitions(resolution.deadlineDefinitions);
                    setProblems(resolution.problems);
                }
            })
            .catch(() => {
                if (active) {
                    setDefinitions([]);
                    setProblems(['Die verfügbaren Fristen konnten nicht geladen werden. Prüfen Sie die Konfiguration und öffnen Sie diesen Bereich erneut.']);
                }
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        return () => { active = false; };
    }, [node.id, node.processId, node.processVersion, node.processNodeDefinitionKey, node.processNodeDefinitionVersion, node.configuration]);

    return (
        <Box
            sx={{
                pt: 1,
                pb: 2,
            }}
        >
            <ElementEditorSectionHeader title="Weitere Eigenschaften des Elements" disableMarginTop>
                Konfigurieren Sie zusätzliche Eigenschaften dieses Prozesselementes.
            </ElementEditorSectionHeader>

            {loading && <Box role="status">Verfügbare Fristen werden geladen …</Box>}
            {problems.length > 0 && <Alert severity="warning">{problems.join(' ')}</Alert>}
            {!loading && definitions.map((definition) => {
                const setting = node.deadlines[definition.key] ?? {days: null, strict: false};
                return <Box key={definition.key} sx={{mb: 3}}>
                    <NumberFieldComponent
                        label={definition.label}
                        hint={`${definition.description} Ohne Angabe gilt keine Frist.`}
                        suffix="Tage"
                        decimalPlaces={0}
                        minValue={1}
                        maxValue={3652}
                        value={setting.days ?? undefined}
                        onChange={(value) => setNode({
                            ...node,
                            deadlines: {...node.deadlines, [definition.key]: {...setting, days: value ?? null}},
                        }, false)}
                        disabled={!isEditable}
                    />
                    <CheckboxFieldComponent
                        label={`Harte Frist: ${definition.label}`}
                        hint={`Stellt den Ausgang „${definition.port.label}“ bereit. Ohne diese Option kann die Frist ohne Prozessübergang verstreichen. Die automatische Weiterleitung bei Fristablauf ist noch nicht verfügbar.`}
                        value={setting.strict}
                        onChange={(value) => setNode({
                            ...node,
                            deadlines: {...node.deadlines, [definition.key]: {...setting, strict: value}},
                        }, false)}
                        disabled={!isEditable || setting.days == null}
                        sx={{mt: 2}}
                    />
                </Box>;
            })}

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
                    mt: definitions.length > 0 ? 2 : 0,
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
