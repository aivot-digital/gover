import {useEffect, useRef, useState} from 'react';
import {Alert, Box, Tooltip} from "@mui/material";
import {NumberFieldComponent} from "../../../../../../../components/number-field/number-field-component";
import {CheckboxFieldComponent} from '../../../../../../../components/checkbox-field/checkbox-field-component';
import {useProcessNodeEditorContext} from "../process-node-editor-context";
import {
    RichTextInputComponent
} from "../../../../../../../components/rich-text-input-component/rich-text-input-component";
import {type ProcessNodeDeadlineDefinition} from '../../../../../services/process-node-provider-api-service';
import {ProcessNodeApiService} from '../../../../../services/process-node-api-service';
import {ElementEditorSectionHeader} from '../../../../../../../components/element-editor-section-header/element-editor-section-header';
import {FormFieldGroup} from '../../../../../../../components/form-field';

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

            {!loading && definitions.length > 0 && (
                <ElementEditorSectionHeader title="Fristen" variant="h5"/>
            )}
            {loading && <Box role="status">Verfügbare Fristen werden geladen …</Box>}
            {problems.length > 0 && <Alert severity="warning">{problems.join(' ')}</Alert>}
            {!loading && definitions.map((definition) => {
                const setting = node.deadlines[definition.key] ?? {days: null, strict: false};
                const missingDeadlineHint = isEditable && setting.days == null
                    ? 'Legen Sie zunächst eine Frist fest, um diese Option zu aktivieren.'
                    : undefined;
                return <Box key={definition.key} sx={{mb: 2, p: 2, border: 1, borderColor: 'divider', borderRadius: 1}}>
                    <FormFieldGroup
                        label={<Box component="span" sx={{typography: 'subtitle1', fontWeight: 600}}>{definition.label}</Box>}
                        disabled={!isEditable}
                        margin="none"
                        showOptionalIndicator={false}
                    >
                        <NumberFieldComponent
                            label="Frist"
                            hint={`${definition.description} Ohne Angabe gilt keine Frist.`}
                            suffix={setting.days === 1 ? 'Tag' : 'Tage'}
                            decimalPlaces={0}
                            minValue={1}
                            maxValue={3652}
                            value={setting.days ?? undefined}
                            onChange={(value) => setNode({
                                ...node,
                                deadlines: {...node.deadlines, [definition.key]: {...setting, days: value ?? null}},
                            }, false)}
                            disabled={!isEditable}
                            margin="none"
                            sx={{mt: 1}}
                        />
                        <Tooltip title={missingDeadlineHint ?? ''} arrow>
                            <Box>
                                <CheckboxFieldComponent
                                    label="Aufgabe bei Fristüberschreitung beenden"
                                    hint={setting.days == null
                                        ? missingDeadlineHint ?? 'Keine Frist festgelegt.'
                                        : `Die Aufgabe wird beendet und der Prozess über den Ausgangsport „${definition.port.label}“ fortgesetzt. Ohne diese Option bleibt die Aufgabe aktiv; die Fristüberschreitung wird nur angezeigt.`}
                                    value={setting.strict}
                                    onChange={(value) => setNode({
                                        ...node,
                                        deadlines: {...node.deadlines, [definition.key]: {...setting, strict: value}},
                                    }, false)}
                                    disabled={!isEditable || setting.days == null}
                                    margin="none"
                                    sx={{mt: 2}}
                                />
                            </Box>
                        </Tooltip>
                    </FormFieldGroup>
                </Box>;
            })}

            <ElementEditorSectionHeader title="Anforderungen und Anmerkungen" variant="h5"/>
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
