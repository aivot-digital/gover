import {useEffect, useMemo, useRef, useState} from 'react';
import {Box, Button, Typography} from '@mui/material';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {useApi} from '../../../hooks/use-api';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {createDerivedRuntimeElementData, EffectiveElementValues} from '../../../models/element-data';
import {isApiError} from '../../../models/api-error';
import {NoCodeDryRunResultDTO} from '../../../models/dtos/no-code-dry-run-dto';
import {NoCodeApiService} from '../../../services/no-code-api-service';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {AlertComponent} from '../../../components/alert/alert-component';
import {NoCodeDataTypeMap} from '../data/no-code-data-type-map';
import {PillNoCodeNode, toNoCodeOperand} from '../utils/pill-no-code-store';

type JsonSource = 'processData' | 'instanceData' | 'nodeData';

const JSON_SOURCES: Record<JsonSource, {type: PillNoCodeNode['type']; label: string; hint: string}> = {
    processData: {
        type: 'NoCodeProcessDataReference',
        label: 'Vorgangsdaten (JSON)',
        hint: 'Zum Beispiel {"antrag": {"betrag": 120}}.',
    },
    instanceData: {
        type: 'NoCodeInstanceDataReference',
        label: 'Geschützte Vorgangsdaten (JSON)',
        hint: 'Zum Beispiel {"status": "neu"}.',
    },
    nodeData: {
        type: 'NoCodeNodeDataReference',
        label: 'Elementdaten (JSON)',
        hint: 'Ergebnisse je Datenschlüssel, zum Beispiel {"pruefung": {"ok": true}}.',
    },
};

// Waits for a pause in typing, so the backend evaluates once per change instead of once per keystroke.
const DRY_RUN_DELAY_MS = 400;

interface PillNoCodeDryRunPanelProps {
    root: PillNoCodeNode | null;
    allElements: ElementWithParents[];
    /** Receives the result of every node by node ID, or null when no results are shown. */
    onResults: (results: Map<string, NoCodeDryRunResultDTO> | null) => void;
}

/**
 * Lets authors enter test data and evaluates the expression with it on every change, without saving anything.
 */
export function PillNoCodeDryRunPanel(props: PillNoCodeDryRunPanelProps) {
    const {root, allElements, onResults} = props;
    const api = useApi();
    const panelId = `${useNormalizedReactId()}-dry-run`;
    const [open, setOpen] = useState(false);
    const [elementValues, setElementValues] = useState<Record<string, string>>({});
    const [jsonValues, setJsonValues] = useState<Partial<Record<JsonSource, string>>>({});
    const [busy, setBusy] = useState(false);
    const [requestError, setRequestError] = useState<string | null>(null);
    const requestIdRef = useRef(0);

    const {elementIds, nodeTypes} = useMemo(() => collectReferences(root), [root]);
    const referencedElements = useMemo(() => elementIds
        .map((id) => allElements.find((entry) => entry.element.id === id)?.element)
        .filter((element) => element != null), [elementIds, allElements]);
    const jsonSources = (Object.keys(JSON_SOURCES) as JsonSource[]).filter((source) => nodeTypes.has(JSON_SOURCES[source].type));
    const parsedJson = Object.fromEntries(jsonSources.map((source) => [source, parseJsonObject(jsonValues[source])])) as
        Partial<Record<JsonSource, ReturnType<typeof parseJsonObject>>>;
    const hasJsonError = jsonSources.some((source) => parsedJson[source]?.error != null);

    useEffect(() => {
        if (!open || root == null || hasJsonError) {
            requestIdRef.current++;
            setBusy(false);
            onResults(null);
            return;
        }

        const requestId = ++requestIdRef.current;
        const effectiveValues: EffectiveElementValues = {};
        for (const element of referencedElements) {
            const text = elementValues[element.id];
            if (text != null && text.length > 0) {
                effectiveValues[element.id] = parseTestValue(text, NoCodeDataTypeMap[element.type]);
            }
        }

        const timer = setTimeout(() => {
            setBusy(true);
            new NoCodeApiService(api)
                .dryRun({
                    expression: toNoCodeOperand(root),
                    elementData: createDerivedRuntimeElementData({effectiveValues}),
                    processData: parsedJson.processData?.value ?? null,
                    instanceData: parsedJson.instanceData?.value ?? null,
                    nodeData: parsedJson.nodeData?.value ?? null,
                })
                .then((result) => {
                    if (requestId === requestIdRef.current) {
                        setRequestError(null);
                        onResults(mapResults(root, result, new Map()));
                    }
                })
                .catch((error) => {
                    if (requestId === requestIdRef.current) {
                        onResults(null);
                        setRequestError(isApiError(error) && error.displayableToUser
                            ? error.message
                            : 'Der Testlauf konnte nicht ausgeführt werden. Versuchen Sie es erneut.');
                    }
                })
                .finally(() => {
                    if (requestId === requestIdRef.current) {
                        setBusy(false);
                    }
                });
        }, DRY_RUN_DELAY_MS);

        return () => clearTimeout(timer);
        // The parsed values derive from the raw inputs listed here.
    }, [open, root, elementValues, jsonValues, hasJsonError]);

    return (
        <Box sx={{mt: 2}}>
            <Button
                size="small"
                startIcon={<Science/>}
                aria-expanded={open}
                aria-controls={panelId}
                onClick={() => setOpen(!open)}
            >
                Testlauf
            </Button>

            {
                open &&
                <Box
                    id={panelId}
                    role="region"
                    aria-label="Testlauf"
                    sx={{
                        mt: 1,
                        p: 2,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2,
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: 1,
                    }}
                >
                    <Typography variant="body2" color="text.secondary">
                        Geben Sie Testdaten ein. Die Ergebnisse erscheinen an den Einträgen des Ausdrucks. Der Testlauf
                        speichert nichts, und Operatoren, die geschützte Daten lesen, werden nicht ausgeführt.
                    </Typography>

                    {
                        referencedElements.map((element) => {
                            const dataType = NoCodeDataTypeMap[element.type];
                            return (
                                <TextFieldComponent
                                    key={element.id}
                                    label={`Testwert für „${generateComponentTitle(element)}“`}
                                    hint={`${NoCodeDataTypeLabels[dataType]}. ${TEST_VALUE_HINTS[dataType]}`}
                                    value={elementValues[element.id] ?? ''}
                                    onChange={(value) => setElementValues({...elementValues, [element.id]: value ?? ''})}
                                />
                            );
                        })
                    }

                    {
                        jsonSources.map((source) => (
                            <TextFieldComponent
                                key={source}
                                label={JSON_SOURCES[source].label}
                                hint={JSON_SOURCES[source].hint}
                                multiline
                                rows={4}
                                value={jsonValues[source] ?? ''}
                                error={parsedJson[source]?.error ?? undefined}
                                onChange={(value) => setJsonValues({...jsonValues, [source]: value ?? ''})}
                            />
                        ))
                    }

                    {
                        referencedElements.length === 0 && jsonSources.length === 0 &&
                        <Typography variant="body2">
                            Der Ausdruck verwendet keine Werte, für die Testdaten nötig sind.
                        </Typography>
                    }

                    {
                        busy &&
                        <Typography variant="caption" color="text.secondary">
                            Testlauf wird ausgeführt …
                        </Typography>
                    }

                    {
                        requestError != null &&
                        <AlertComponent color="error" text={requestError}/>
                    }
                </Box>
            }
        </Box>
    );
}

const TEST_VALUE_HINTS: Record<NoCodeDataType, string> = {
    [NoCodeDataType.Runtime]: 'Text oder JSON, zum Beispiel ["a", "b"].',
    [NoCodeDataType.Boolean]: 'Geben Sie wahr oder falsch ein.',
    [NoCodeDataType.Number]: 'Zum Beispiel 42 oder 3,5.',
    [NoCodeDataType.String]: 'Beliebiger Text.',
    [NoCodeDataType.Date]: 'Im Format JJJJ-MM-TT, zum Beispiel 2024-12-31.',
    [NoCodeDataType.DateTime]: 'Zum Beispiel 2024-12-31T09:30:00+01:00.',
    [NoCodeDataType.Time]: 'Im Format HH:MM, zum Beispiel 09:30.',
    [NoCodeDataType.List]: 'Als JSON-Liste, zum Beispiel ["a", "b"].',
    [NoCodeDataType.Object]: 'Als JSON-Objekt, zum Beispiel {"strasse": "Hauptstraße 1"}.',
};

/** Converts the entered text to the value a field of this type holds, as the backend receives it from forms. */
export function parseTestValue(text: string, dataType: NoCodeDataType): unknown {
    switch (dataType) {
        case NoCodeDataType.String:
        case NoCodeDataType.Date:
        case NoCodeDataType.DateTime:
        case NoCodeDataType.Time:
            return text;
        case NoCodeDataType.Number: {
            const number = Number(text.trim().replace(',', '.'));
            return text.trim().length > 0 && Number.isFinite(number) ? number : text;
        }
        case NoCodeDataType.Boolean: {
            const normalized = text.trim().toLowerCase();
            return normalized === 'wahr' || normalized === 'true'
                ? true
                : normalized === 'falsch' || normalized === 'false' ? false : text;
        }
        default:
            try {
                return JSON.parse(text);
            } catch {
                return text;
            }
    }
}

function parseJsonObject(text: string | undefined): {value: Record<string, unknown> | null; error: string | null} {
    if (text == null || text.trim().length === 0) {
        return {value: null, error: null};
    }

    try {
        const value = JSON.parse(text);
        if (value != null && typeof value === 'object' && !Array.isArray(value)) {
            return {value, error: null};
        }
    } catch {
        // Reported below like any other value that is not an object.
    }
    return {value: null, error: 'Geben Sie ein JSON-Objekt ein, zum Beispiel {"betrag": 120}.'};
}

function collectReferences(root: PillNoCodeNode | null) {
    const elementIds: string[] = [];
    const nodeTypes = new Set<PillNoCodeNode['type']>();
    const visit = (node: PillNoCodeNode | null) => {
        if (node == null) {
            return;
        }
        nodeTypes.add(node.type);
        if (node.type === 'NoCodeReference' && node.elementId != null && !elementIds.includes(node.elementId)) {
            elementIds.push(node.elementId);
        }
        if (node.type === 'NoCodeExpression') {
            node.operands.forEach(visit);
        }
    };
    visit(root);
    return {elementIds, nodeTypes};
}

/** The result tree follows the operand order, so it maps onto the nodes the request was built from. */
function mapResults(
    node: PillNoCodeNode | null,
    result: NoCodeDryRunResultDTO | undefined,
    results: Map<string, NoCodeDryRunResultDTO>,
): Map<string, NoCodeDryRunResultDTO> {
    if (node == null || result == null) {
        return results;
    }

    results.set(node.id, result);
    if (node.type === 'NoCodeExpression') {
        node.operands.forEach((child, index) => mapResults(child, result.operands[index], results));
    }
    return results;
}
