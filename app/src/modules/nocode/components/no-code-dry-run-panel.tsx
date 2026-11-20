import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {NoCodeTestData} from '../../../models/functions/no-code-editor-metadata';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {NoCodePresentationContext} from '../utils/humanize-no-code';
import {
    buildTestSource,
    collectNoCodeTestFields,
    NoCodeTestSource,
    parseTestDraft,
    testDraftFromValue,
} from '../utils/no-code-test-data';
import {NoCodeTestValueInput, NO_CODE_TEST_FIELDS_SX} from './no-code-test-value-input';
import {NoCodeTestSourceInput} from './no-code-test-source-input';
import {NoCodeTestResult} from './no-code-test-result';
import {useEffect, useMemo, useRef, useState} from 'react';
import {Alert, Box, Button, Typography} from '@mui/material';
import Science from '@aivot/mui-material-symbols-400-n25-outlined/Science';
import {useApi} from '../../../hooks/use-api';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {createDerivedRuntimeElementData, EffectiveElementValues} from '../../../models/element-data';
import {isApiError} from '../../../models/api-error';
import {NoCodeDryRunResultDTO} from '../../../models/dtos/no-code-dry-run-dto';
import {NoCodeApiService} from '../../../services/no-code-api-service';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {AlertComponent} from '../../../components/alert/alert-component';
import {NoCodeEditorNode, toNoCodeOperand, withoutNoCodeEditorMetadata} from '../utils/no-code-editor-store';

interface NoCodeDryRunPanelProps {
    open?: boolean;
    embedded?: boolean;
    onTestDataChange?: (data: NoCodeTestData | undefined) => void;
    readOnly?: boolean;
    presentation?: NoCodePresentationContext;
    canEvaluate?: boolean;
    onValidate?: () => void;
    root: NoCodeEditorNode | null;
    allElements: ElementWithParents[];
    /** Receives the result of every node by node ID, or null when no results are shown. */
    onResults: (results: Map<string, NoCodeDryRunResultDTO> | null) => void;
}

/**
 * Evaluates examples only on explicit request, without changing live case data.
 * Saving examples is a separate authoring choice.
 */
export function NoCodeDryRunPanel(props: NoCodeDryRunPanelProps) {
    const {
        root,
        allElements,
        onResults,
        canEvaluate = true,
    } = props;
    const api = useApi();
    const panelId = `${useNormalizedReactId()}-dry-run`;
    const [localOpen, setOpen] = useState(false);
    const open = props.open ?? localOpen;
    const savedData = root?.editorMetadata?.testData;
    const [testData, setTestData] = useState<NoCodeTestData>(() => savedData ?? emptyTestData());
    const {
        elementValues,
        sourceValues,
        visibility,
    } = testData;
    const savedKey = JSON.stringify(savedData ?? null);
    // Parent echoes keep local input intact; undo or external replacements restore the saved draft.
    const emittedKey = useRef(savedKey);
    useEffect(() => {
        if (emittedKey.current !== savedKey) {
            emittedKey.current = savedKey;
            setTestData(savedData ?? emptyTestData());
        }
    }, [savedKey]);
    const persist = (data: NoCodeTestData | undefined) => {
        emittedKey.current = JSON.stringify(data ?? null);
        props.onTestDataChange?.(data);
    };
    const updateTestData = (patch: Partial<NoCodeTestData>) => {
        const next = {...testData, ...patch};
        setTestData(next);
        if (savedData != null && !props.readOnly) persist(next);
    };
    const [validationRequested, setValidationRequested] = useState(false);
    const [touched, setTouched] = useState<Set<string>>(new Set());
    const touch = (key: string) => setTouched((current) => new Set(current).add(key));
    const cancelRequest = useRef<(() => void) | null>(null);
    const regionRef = useRef<HTMLDivElement>(null);
    const [finished, setFinished] = useState<{
        key: string;
        result: NoCodeDryRunResultDTO;
        stale: boolean
    } | null>(null);
    const [busy, setBusy] = useState(false);
    const [requestError, setRequestError] = useState<string | null>(null);
    const requestIdRef = useRef(0);

    const {
        visibilityIds,
        dynamicVisibility,
    } = useMemo(() => collectReferences(root), [root]);
    const fields = useMemo(() => collectNoCodeTestFields(root, allElements, props.presentation), [root, allElements, props.presentation]);
    const sources = Object.keys(fields.sources) as NoCodeTestSource[];
    const parsedJson = Object.fromEntries(sources.map(source => [source, buildTestSource(fields.sources[source]!, sourceValues[source] ?? {})]));
    const hasJsonError = sources.some(source => parsedJson[source].error != null);
    const parsedValues = Object.fromEntries(fields.elements.map(field => [field.key,
        parseTestDraft((Object.hasOwn(elementValues, field.key) ? elementValues[field.key] : testDraftFromValue(undefined, field.type)))]));
    const invalidValues = Object.values(parsedValues).some(value => value.error != null);
    const missingVisibility = visibilityIds.some((id) => !['true', 'false'].includes(visibility[id]));
    const blocked = hasJsonError || invalidValues || missingVisibility || dynamicVisibility || !canEvaluate;
    const evaluationRoot = root == null ? null : withoutNoCodeEditorMetadata(root);
    const resultKey = JSON.stringify([evaluationRoot, elementValues, sourceValues, visibility, canEvaluate, allElements]);
    const resultStale = finished != null && (finished.stale || finished.key !== resultKey || busy || requestError != null);
    useEffect(() => {
        cancelRequest.current?.();
        setBusy(false);
        setFinished((current) => current == null || current.stale ? current : {
            ...current,
            stale: true,
        });
        setRequestError(null);
        onResults(null);
        return () => cancelRequest.current?.();
    }, [resultKey]);
    // Hiding test inputs must not invalidate completed results in the expression.
    useEffect(() => {
        if (!open) {
            cancelRequest.current?.();
            setBusy(false);
        }
    }, [open]);

    const run = () => {
        setValidationRequested(true);
        props.onValidate?.();
        if (!open || root == null || blocked) {
            // The field error attributes are committed on the next render.
            if (canEvaluate) requestAnimationFrame(() => regionRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());
            return;
        }
        cancelRequest.current?.();
        const requestId = ++requestIdRef.current;
        onResults(null);
        setRequestError(null);
        setBusy(true);
        setFinished(current => current == null ? null : {
            ...current,
            stale: true,
        });
        const effectiveValues: EffectiveElementValues = Object.fromEntries(fields.elements
            .filter(field => parsedValues[field.key].value !== undefined)
            .map(field => [field.key, parsedValues[field.key].value]));
        const elementStates = Object.fromEntries(visibilityIds.map((id) => [id, {visible: visibility[id] === 'true'}]));

        const controller = new AbortController();
        const timeout = setTimeout(() => {
            if (requestId !== requestIdRef.current) return;
            requestIdRef.current++;
            controller.abort();
            setBusy(false);
            setRequestError('Der Testlauf dauert zu lange. Verkleinern Sie die Testdaten oder versuchen Sie es erneut.');
        }, 30_000);
        cancelRequest.current = () => {
            clearTimeout(timeout);
            requestIdRef.current++;
            controller.abort();
        };
        new NoCodeApiService(api)
            .dryRun({
                expression: toNoCodeOperand(evaluationRoot),
                elementData: createDerivedRuntimeElementData({
                    effectiveValues,
                    elementStates,
                }),
                processData: parsedJson.processData?.value ?? null,
                instanceData: parsedJson.instanceData?.value ?? null,
                nodeData: parsedJson.nodeData?.value ?? null,
            }, controller)
            .then((result) => {
                if (requestId === requestIdRef.current) {
                    setRequestError(null);
                    setFinished({
                        key: resultKey,
                        result,
                        stale: false,
                    });
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
                clearTimeout(timeout);
                if (requestId === requestIdRef.current) {
                    setBusy(false);
                }
            });
    };

    const toggle = <Button size="small"
                           startIcon={<Science/>}
                           aria-expanded={open}
                           aria-controls={panelId}
                           variant={open ? 'outlined' : 'text'}
                           onClick={() => {
                               setOpen(!open);
                           }}>
        Testlauf
    </Button>;

    return <>
        <Box sx={{
            minWidth: 0,
            minHeight: 0,
            containerType: 'inline-size',
            containerName: 'no-code-test',
            display: props.embedded && !open ? 'none' : 'flex',
            flexDirection: 'column',
        }}>
            <Box sx={{mb: open ? 2 : 0}}>
                {!props.embedded ? toggle : <Typography variant="h5"
                                                        component="h3">Testlauf</Typography>}
            </Box>

            {open && validationRequested && !canEvaluate &&
                <Typography color="text.secondary">Vervollständigen Sie den Ausdruck, bevor Sie ihn
                                                   testen.</Typography>}
            {
                open &&
                <Box
                    ref={regionRef}
                    id={panelId}
                    role="region"
                    aria-label="Testlauf"
                    sx={{
                        p: props.embedded ? 0 : 2,
                        minHeight: 0,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2,
                        border: props.embedded ? 0 : 1,
                        borderColor: 'divider',
                        borderRadius: 1,
                    }}
                >
                    <Typography variant="body2"
                                color="text.secondary"
                                sx={{maxWidth: 820}}>
                        Prüfen Sie mit Beispieldaten, ob Ihr Ausdruck das erwartete Ergebnis liefert. Testen Sie dabei
                        auch
                        leere Eingaben und Sonderfälle. Echte Vorgangsdaten werden dafür nicht verwendet.
                    </Typography>

                    {(fields.elements.length > 0 || sources.length > 0) && <Typography variant="caption"
                                                                                       color="text.secondary">
                        Leere Eingabefelder stellen keinen Testwert bereit. Über „Wertoptionen“ können Sie ausdrücklich
                        leeren Text oder null festlegen.
                    </Typography>}
                    {fields.elements.length > 0 && <Box sx={NO_CODE_TEST_FIELDS_SX}>{fields.elements.map(field =>
                        <NoCodeTestValueInput key={field.key}
                                              field={field}
                                              value={(Object.hasOwn(elementValues, field.key) ? elementValues[field.key] : testDraftFromValue(undefined, field.type))}
                                              showError={validationRequested || touched.has(field.key)}
                                              onBlur={() => touch(field.key)}
                                              onChange={value => updateTestData({
                                                  elementValues: {
                                                      ...elementValues,
                                                      [field.key]: value,
                                                  },
                                              })}/>)}</Box>}

                    {visibilityIds.map((id) => <SelectFieldComponent key={id}
                                                                     label={`Sichtbarkeit für „${allElements.some(({element}) => element.id === id) ? generateComponentTitle(allElements.find(({element}) => element.id === id)!.element) : id}“`}
                                                                     hint={`Testzustand des Elements mit der ID ${id}.`}
                                                                     error={validationRequested && !visibility[id] ? 'Wählen Sie einen Sichtbarkeitszustand.' : undefined}
                                                                     required
                                                                     value={visibility[id] ?? ''}
                                                                     includeEmptyOption={false}
                                                                     options={[{
                                                                         value: 'true',
                                                                         label: 'Sichtbar',
                                                                     }, {
                                                                         value: 'false',
                                                                         label: 'Nicht sichtbar',
                                                                     }]}
                                                                     onChange={(value) => updateTestData({
                                                                         visibility: {
                                                                             ...visibility,
                                                                             [id]: value ?? '',
                                                                         },
                                                                     })}
                    />)}
                    {dynamicVisibility && <AlertComponent color="warning"
                                                          text="Die Element-ID für die Sichtbarkeitsprüfung wird dynamisch berechnet. Dieser Ausdruck kann hier noch nicht getestet werden."/>}
                    {validationRequested && missingVisibility &&
                        <Typography>Wählen Sie die benötigten Sichtbarkeitszustände aus.</Typography>}

                    {sources.map(source => <NoCodeTestSourceInput key={source}
                                                                  source={source}
                                                                  plan={fields.sources[source]!}
                                                                  value={sourceValues[source] ?? {}}
                                                                  onChange={value => updateTestData({
                                                                      sourceValues: {
                                                                          ...sourceValues,
                                                                          [source]: value,
                                                                      },
                                                                  })}
                                                                  validationRequested={validationRequested}
                                                                  touched={touched}
                                                                  onTouch={touch}/>)}

                    {
                        fields.elements.length === 0 && sources.length === 0 && visibilityIds.length === 0 && !dynamicVisibility &&
                        <Typography variant="body2">
                            Der Ausdruck verwendet keine Werte, für die Testdaten nötig sind.
                        </Typography>
                    }

                    <Button variant="contained"
                            startIcon={<Science/>}
                            onClick={run}
                            disabled={busy}
                            sx={{alignSelf: 'flex-start'}}>
                        Test ausführen
                    </Button>
                    {validationRequested && (hasJsonError || invalidValues) &&
                        <Typography role="status">Korrigieren Sie die Testeingaben. Es wird kein Ergebnis
                                                  berechnet.</Typography>}

                    {
                        busy &&
                        <Typography role="status"
                                    variant="caption"
                                    color="text.secondary">
                            Testlauf wird ausgeführt …
                        </Typography>
                    }

                    {
                        requestError != null &&
                        <Box>
                            <AlertComponent color="error"
                                            text={requestError}/>
                            {!resultStale && <Button onClick={run}>Erneut testen</Button>}
                        </Box>
                    }
                    {finished != null && <Box role="region"
                                              aria-label="Gesamtergebnis"
                                              sx={{
                                                  borderTop: 1,
                                                  borderColor: 'divider',
                                                  pt: 2,
                                              }}>
                        <Typography variant="h6"
                                    component="h4"
                                    sx={{mb: 1.5}}>Gesamtergebnis</Typography>
                        {resultStale && <Alert severity="warning"
                                               role="status"
                                               sx={{mb: 1.5}}
                                               action={<Button color="inherit"
                                                               size="small"
                                                               disabled={busy}
                                                               onClick={run}>Erneut testen</Button>}>
                            Nicht mehr aktuell. Führen Sie den Test erneut aus, um das Ergebnis zu aktualisieren.
                        </Alert>}
                        <NoCodeTestResult result={finished.result}/>
                    </Box>}

                    {props.onTestDataChange != null && <Box sx={{
                        borderTop: 1,
                        borderColor: 'divider',
                        pt: 2,
                    }}>
                        <CheckboxFieldComponent
                            label="Testdaten mit dem Ausdruck speichern"
                            hint="Die Testdaten werden mit der Konfiguration gespeichert und stehen beim nächsten Öffnen des Ausdrucks wieder zur Verfügung."
                            value={savedData != null}
                            disabled={props.readOnly || root == null}
                            onChange={(save) => persist(save ? testData : undefined)}
                            margin="none"
                        />
                    </Box>}
                </Box>
            }
        </Box>
    </>;
}

function collectReferences(root: NoCodeEditorNode | null) {
    const visibilityIds: string[] = [];
    let dynamicVisibility = false;
    const visit = (node: NoCodeEditorNode | null) => {
        if (node == null) {
            return;
        }
        if (node.type === 'NoCodeExpression') {
            if (node.operatorIdentifier === 'is-visible' || node.operatorIdentifier === 'is-invisible') {
                const target = node.operands[0];
                if (target?.type === 'NoCodeStaticValue' && typeof target.value === 'string' && target.value.length > 0) {
                    if (!visibilityIds.includes(target.value)) visibilityIds.push(target.value);
                } else dynamicVisibility = true;
            }
            node.operands.forEach(visit);
        }
    };
    visit(root);
    return {
        visibilityIds,
        dynamicVisibility,
    };
}

/** The result tree follows the operand order, so it maps onto the nodes the request was built from. */
function mapResults(
    node: NoCodeEditorNode | null,
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

function emptyTestData(): NoCodeTestData {
    return {
        version: 1,
        elementValues: {},
        sourceValues: {},
        visibility: {},
    };
}
