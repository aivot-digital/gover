import React, {useRef, useState} from 'react';
import {
    Box,
    Chip,
    FormControlLabel,
    Paper,
    Switch,
    Typography,
} from '@mui/material';
import Numbers from '@aivot/mui-material-symbols-400-n25-outlined/Numbers';
import {
    InputModeField,
    getInputModeVariableCategoryLabel,
    getInputModeVariableReference,
    type InputModeValue,
    type InputModeVariable,
} from '../../components/input-mode-field/input-mode-field';
import {NumberFieldComponent} from '../../components/number-field/number-field-component';
import {SelectFieldComponent} from '../../components/select-field/select-field-component';
import {
    ProcessDataVariableField,
    type ProcessDataVariableOption,
} from '../../components/process-data-variable-field/process-data-variable-field';
import {
    RichTextInputComponent,
    type RichTextInputComponentMethods,
} from '../../components/rich-text-input-component/rich-text-input-component';
import {
    DynamicTextInputField,
    type DynamicTextFieldMethods,
} from '../../components/dynamic-text/dynamic-text-field';
import {RadioFieldComponent} from '../../components/radio-field/radio-field-component';

const VARIABLES: InputModeVariable[] = [
    {
        id: 'applicant-first-name',
        label: 'Vorname der antragstellenden Person',
        path: 'antragsteller.vorname',
        origin: 'Antrag eingereicht',
        category: 'processData',
        description: 'Aus dem Antragsformular',
    },
    {
        id: 'applicant-last-name',
        label: 'Nachname der antragstellenden Person',
        path: 'antragsteller.nachname',
        origin: 'Antrag eingereicht',
        category: 'processData',
        description: 'Aus dem Antragsformular',
    },
    {
        id: 'application-date',
        label: 'Eingangsdatum',
        path: 'antrag.eingangsdatum',
        origin: 'Antrag eingereicht',
        category: 'processData',
    },
    {
        id: 'cart-item-count',
        label: 'Anzahl der Positionen',
        path: 'warenkorb.positionen.anzahl',
        origin: 'Warenkorb laden',
        category: 'processData',
        description: 'Anzahl aller geladenen Warenkorbpositionen',
    },
    {
        id: 'cart-total',
        label: 'Gesamtbetrag',
        path: 'warenkorb.gesamtbetrag',
        origin: 'Warenkorb laden',
        category: 'processData',
    },
    {
        id: 'default-increment',
        label: 'Standardinkrement',
        path: 'konfiguration.standardInkrement',
        origin: 'Grenzwerte bestimmen',
        category: 'processData',
    },
    {
        id: 'missing-value-behavior',
        label: 'Verhalten bei fehlendem Wert',
        path: 'konfiguration.fehlerbehandlung',
        origin: 'Grenzwerte bestimmen',
        category: 'processData',
    },
    {
        id: 'element-cart-item-count',
        label: 'Erzeugte Anzahl der Positionen',
        path: 'warenkorbLaden.anzahlPositionen',
        origin: 'Warenkorb laden',
        category: 'elementData',
        description: 'Ausgangsdaten des Prozesselements',
    },
    {
        id: 'element-check-result',
        label: 'Ergebnis der Antragsprüfung',
        path: 'antragPruefen.ergebnis',
        origin: 'Antrag prüfen',
        category: 'elementData',
    },
    {
        id: 'element-finished',
        label: 'Abschlusszeit von Warenkorb laden',
        path: 'warenkorbLaden.finished',
        origin: 'Warenkorb laden',
        category: 'elementMetadata',
    },
    {
        id: 'element-runtime',
        label: 'Laufzeit der Antragsprüfung',
        path: 'antragPruefen.runtime',
        origin: 'Antrag prüfen',
        category: 'elementMetadata',
    },
    {
        id: 'protected-case-number',
        label: 'Aktenzeichen des Vorgangs',
        path: 'caseNumber',
        origin: 'Vorgang',
        category: 'protectedProcessData',
    },
    {
        id: 'protected-assigned-file-numbers',
        label: 'Zugewiesene Geschäftszeichen',
        path: 'assignedFileNumbers',
        origin: 'Vorgang',
        category: 'protectedProcessData',
    },
    {
        id: 'protected-current-task',
        label: 'ID der aktuellen Aufgabe',
        path: 'currentTaskId',
        origin: 'Aktuelle Aufgabe',
        category: 'protectedProcessData',
    },
];

const DYNAMIC_TEXT_VARIABLE_METADATA = VARIABLES.map((variable) => ({
    reference: getInputModeVariableReference(variable),
    label: variable.label,
    category: getInputModeVariableCategoryLabel(variable.category),
    origin: variable.origin,
    description: variable.description,
}));

const DESTINATION_SUGGESTIONS: ProcessDataVariableOption[] = [
    {
        path: 'zaehler.aktuellerStand',
        label: 'Aktueller Zählerstand',
        origin: 'Bestehende Zählervariable',
    },
    {
        path: 'warenkorb.verarbeitetePositionen',
        label: 'Verarbeitete Positionen',
        origin: 'Warenkorb laden',
    },
    {
        path: 'antrag.pruefschritte',
        label: 'Prüfschritte',
        origin: 'Antrag eingereicht',
    },
    ...VARIABLES
        .filter((variable) => variable.category === 'processData')
        .map((variable) => ({
            path: variable.path,
            label: variable.label,
            origin: variable.origin,
        })),
];

const MISSING_VALUE_OPTIONS = [
    {value: 'zero', label: 'Bei 0 beginnen'},
    {value: 'skip', label: 'Aktualisierung überspringen'},
    {value: 'error', label: 'Ausführung mit Fehler beenden'},
];

const LOGGING_SCOPE_OPTIONS = [
    {value: 'always', label: 'Immer'},
    {value: 'changed', label: 'Nur bei Änderung'},
    {value: 'never', label: 'Nie'},
];

function createInitialIncrementValue(): InputModeValue<number> {
    return {
        mode: 'literal',
        literal: 1,
        variableId: 'default-increment',
        noCode: {
            sourceVariableId: 'cart-item-count',
            operator: 'multiply',
            operand: '2',
        },
        lowCode: 'return $.warenkorb.positionen.anzahl ?? 1;',
    };
}

function createInitialLogMessageValue(): InputModeValue<string> {
    return {
        mode: 'literal',
        literal: [
            '{% if $.warenkorb.positionen.anzahl > 0 %}',
            'Der Zähler wurde um {{ $.warenkorb.positionen.anzahl }} erhöht.',
            '{% else %}',
            'Der Zähler wurde nicht verändert.',
            '{% endif %}',
        ].join('\n'),
        variableId: 'applicant-first-name',
        noCode: {
            sourceVariableId: 'applicant-first-name',
            operator: 'add',
            operand: ' - Zähler aktualisiert',
        },
        lowCode: 'return `${$.antragsteller.vorname} ${$.antragsteller.nachname}: Zähler aktualisiert`;',
    };
}

function createInitialLogTitleValue(): InputModeValue<string> {
    return {
        mode: 'literal',
        literal: 'Zähler für {{ $.antragsteller.nachname }} aktualisiert',
        variableId: 'protected-case-number',
        noCode: {
            sourceVariableId: 'protected-case-number',
            operator: 'add',
            operand: ' - Zähler aktualisiert',
        },
        lowCode: 'return `Zähler für ${$.antragsteller.nachname} aktualisiert`;',
    };
}

function createInitialMissingValueBehavior(): InputModeValue<string> {
    return {
        mode: 'literal',
        literal: 'zero',
        variableId: 'missing-value-behavior',
        noCode: {
            sourceVariableId: 'cart-item-count',
            operator: 'fallback',
            operand: 'zero',
        },
        lowCode: "return $.warenkorb.positionen.anzahl == null ? 'error' : 'zero';",
    };
}

function createInitialRichTextValue(): InputModeValue<string> {
    return {
        mode: 'literal',
        literal: '**Zähler aktualisiert**\n\nDer Zähler für {{ $.antragsteller.nachname }} wurde auf ' +
            '{{ $.warenkorb.positionen.anzahl }} gesetzt.',
        variableId: 'applicant-first-name',
        noCode: {
            sourceVariableId: 'applicant-first-name',
            operator: 'add',
            operand: ' - Zähler aktualisiert',
        },
        lowCode: 'return `**Zähler aktualisiert**\\n\\nNeuer Wert: ${$.zaehler.aktuellerStand}`;',
    };
}

function createInitialLoggingScope(): InputModeValue<string> {
    return {
        mode: 'literal',
        literal: 'changed',
        variableId: 'missing-value-behavior',
        noCode: {
            sourceVariableId: 'cart-item-count',
            operator: 'fallback',
            operand: 'changed',
        },
        lowCode: "return $.warenkorb.positionen.anzahl > 0 ? 'changed' : 'never';",
    };
}

export function InputModePrototype() {
    const [narrow, setNarrow] = useState(false);
    const [showQualityStates, setShowQualityStates] = useState(false);
    const [destination, setDestination] = useState<string | null>('zaehler.aktuellerStand');
    const [increment, setIncrement] = useState(createInitialIncrementValue);
    const [logTitle, setLogTitle] = useState(createInitialLogTitleValue);
    const [logMessage, setLogMessage] = useState(createInitialLogMessageValue);
    const [richText, setRichText] = useState(createInitialRichTextValue);
    const [missingValueBehavior, setMissingValueBehavior] = useState(createInitialMissingValueBehavior);
    const [loggingScope, setLoggingScope] = useState(createInitialLoggingScope);
    const [emptyRequiredTitle, setEmptyRequiredTitle] = useState<InputModeValue<string>>(() => ({
        ...createInitialLogTitleValue(),
        literal: null,
    }));
    const [readOnlyTitle, setReadOnlyTitle] = useState(createInitialLogTitleValue);
    const [longExpression, setLongExpression] = useState<InputModeValue<string>>(() => ({
        ...createInitialLogMessageValue(),
        literal: '{% if $.warenkorb.positionen.anzahl > 0 and $.antragsteller.nachname != empty and $.antrag.eingangsdatum != null %}',
    }));

    const logTitleInputRef = useRef<DynamicTextFieldMethods | null>(null);
    const logMessageInputRef = useRef<DynamicTextFieldMethods | null>(null);
    const richTextInputRef = useRef<RichTextInputComponentMethods | null>(null);

    return (
        <Box component="section" aria-labelledby="input-mode-prototype-title">
            <Typography id="input-mode-prototype-title" component="h2" variant="h5">
                Dynamische Eingabemodi
            </Typography>
            <Box
                sx={{
                    mt: 3,
                    display: 'flex',
                    justifyContent: 'flex-end',
                    flexWrap: 'wrap',
                    gap: 2,
                }}
            >
                <FormControlLabel
                    label="Schmale Editoransicht"
                    control={(
                        <Switch
                            checked={narrow}
                            onChange={(_, checked) => setNarrow(checked)}
                        />
                    )}
                    sx={{
                        mr: 0,
                        '& .MuiFormControlLabel-label': {
                            color: 'text.secondary',
                            fontSize: '0.875rem',
                        },
                    }}
                />
                <FormControlLabel
                    label="Beispielzustände anzeigen"
                    control={(
                        <Switch
                            checked={showQualityStates}
                            onChange={(_, checked) => setShowQualityStates(checked)}
                        />
                    )}
                    sx={{
                        mr: 0,
                        '& .MuiFormControlLabel-label': {
                            color: 'text.secondary',
                            fontSize: '0.875rem',
                        },
                    }}
                />
            </Box>

            <Box
                sx={{
                    mt: 1,
                    display: 'flex',
                    justifyContent: 'center',
                }}
            >
                <Paper
                    variant="outlined"
                    sx={{
                        width: '100%',
                        maxWidth: narrow ? 480 : 720,
                        transition: 'max-width 180ms ease-out',
                        overflow: 'hidden',
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 1.5,
                            px: {xs: 2, sm: 3},
                            py: 2,
                            borderBottom: '1px solid',
                            borderColor: 'divider',
                            bgcolor: 'background.paper',
                        }}
                    >
                        <Box
                            sx={{
                                width: 40,
                                height: 40,
                                display: 'grid',
                                placeItems: 'center',
                                borderRadius: 1,
                                bgcolor: 'action.selected',
                                color: 'primary.main',
                                flexShrink: 0,
                            }}
                        >
                            <Numbers/>
                        </Box>
                        <Box sx={{minWidth: 0}}>
                            <Typography variant="subtitle1" noWrap>
                                Zähler aktualisieren
                            </Typography>
                            <Typography variant="caption" color="text.secondary" noWrap component="div">
                                de.aivot.core.counter
                            </Typography>
                        </Box>
                        <Chip
                            label="Aktion"
                            size="small"
                            variant="outlined"
                            sx={{ml: 'auto', flexShrink: 0}}
                        />
                    </Box>

                    <Box sx={{px: {xs: 2, sm: 3}, py: 3}}>
                        <Typography variant="h4" component="h2">
                            Eigenschaften des Elements
                        </Typography>
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{mt: 0.75, mb: 3}}
                        >
                            Zählerstand und Verhalten dieses Prozesselements konfigurieren.
                        </Typography>

                        <Box
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: narrow
                                    ? 'minmax(0, 1fr)'
                                    : {xs: 'minmax(0, 1fr)', sm: 'minmax(0, 2fr) minmax(220px, 1fr)'},
                                gap: 2.5,
                            }}
                        >
                            <ProcessDataVariableField
                                label="Vorgangsdatenvariable"
                                hint="Pfad, unter dem der Zählerstand gespeichert wird."
                                value={destination}
                                onChange={setDestination}
                                options={DESTINATION_SUGGESTIONS}
                            />

                            <InputModeField
                                label="Inkrement"
                                hint="Zahl, um die der Zähler erhöht wird."
                                variables={VARIABLES}
                                value={increment}
                                onChange={setIncrement}
                                renderLiteral={({value, onChange, fieldProps}) => (
                                    <NumberFieldComponent
                                        {...fieldProps}
                                        value={value}
                                        onChange={onChange}
                                        decimalPlaces={0}
                                    />
                                )}
                            />
                        </Box>

                        <Box sx={{mt: 3}}>
                            <InputModeField
                                label="Protokolltitel"
                                hint="Kurzer Titel für das Ausführungsprotokoll."
                                variables={VARIABLES}
                                value={logTitle}
                                onChange={setLogTitle}
                                onInsertVariable={(variable) => {
                                    const reference = getInputModeVariableReference(variable);
                                    requestAnimationFrame(() => logTitleInputRef.current?.insertVariableReference(reference));
                                }}
                                renderLiteral={({value, onChange, variableInsertAction, fieldProps}) => (
                                    <DynamicTextInputField
                                        ref={logTitleInputRef}
                                        {...fieldProps}
                                        variableMetadata={DYNAMIC_TEXT_VARIABLE_METADATA}
                                        value={value}
                                        onChange={onChange}
                                        endAction={variableInsertAction}
                                    />
                                )}
                            />
                        </Box>

                        <Box sx={{mt: 3}}>
                            <InputModeField
                                label="Ausführungsnotiz"
                                hint="Wird im Ausführungsprotokoll des Prozesselements gespeichert."
                                variables={VARIABLES}
                                value={logMessage}
                                onChange={setLogMessage}
                                onInsertVariable={(variable) => {
                                    const reference = getInputModeVariableReference(variable);
                                    requestAnimationFrame(() => logMessageInputRef.current?.insertVariableReference(reference));
                                }}
                                renderLiteral={({value, onChange, variableInsertAction, fieldProps}) => (
                                    <DynamicTextInputField
                                        ref={logMessageInputRef}
                                        {...fieldProps}
                                        multiline
                                        rows={5}
                                        variableMetadata={DYNAMIC_TEXT_VARIABLE_METADATA}
                                        value={value}
                                        onChange={onChange}
                                        endAction={variableInsertAction}
                                    />
                                )}
                            />
                        </Box>

                        <Box sx={{mt: 3}}>
                            <InputModeField
                                label="Wenn noch kein Zählerstand vorhanden ist"
                                hint="Bestimmt das Verhalten beim ersten Durchlauf."
                                variables={VARIABLES}
                                value={missingValueBehavior}
                                onChange={setMissingValueBehavior}
                                renderLiteral={({value, onChange, fieldProps}) => (
                                    <SelectFieldComponent
                                        {...fieldProps}
                                        value={value}
                                        onChange={onChange}
                                        options={MISSING_VALUE_OPTIONS}
                                        includeEmptyOption={false}
                                    />
                                )}
                            />
                        </Box>

                        <Box sx={{mt: 3}}>
                            <InputModeField
                                label="Formatierte Protokollnachricht"
                                hint="Unterstützt Formatierung und dynamischen Text."
                                variables={VARIABLES}
                                value={richText}
                                onChange={setRichText}
                                onInsertVariable={(variable) => {
                                    const placeholder = `{{ ${getInputModeVariableReference(variable)} }}`;
                                    requestAnimationFrame(() => richTextInputRef.current?.insertMarkdown(placeholder));
                                }}
                                renderLiteral={({value, onChange, variableInsertAction, fieldProps}) => (
                                    <RichTextInputComponent
                                        ref={richTextInputRef}
                                        {...fieldProps}
                                        value={value}
                                        onChange={onChange}
                                        endAction={variableInsertAction}
                                        reducedMode
                                        dynamicText
                                        dynamicTextVariableMetadata={DYNAMIC_TEXT_VARIABLE_METADATA}
                                    />
                                )}
                            />
                        </Box>

                        <Box sx={{mt: 3}}>
                            <InputModeField
                                label="Protokollierung"
                                hint="Legt fest, wann ein Eintrag im Ausführungsprotokoll erzeugt wird."
                                variables={VARIABLES}
                                value={loggingScope}
                                onChange={setLoggingScope}
                                renderLiteral={({value, onChange, fieldProps}) => (
                                    <RadioFieldComponent
                                        {...fieldProps}
                                        value={value ?? ''}
                                        onChange={onChange}
                                        options={LOGGING_SCOPE_OPTIONS}
                                        disabled={fieldProps.disabled || fieldProps.readOnly}
                                        displayInline
                                        controlSx={{minHeight: 44, columnGap: 2, alignItems: 'center'}}
                                    />
                                )}
                            />
                        </Box>

                        {showQualityStates && (
                            <Box
                                sx={{
                                    mt: 4,
                                    pt: 3,
                                    borderTop: '1px solid',
                                    borderColor: 'divider',
                                }}
                            >
                                <Typography variant="h5" component="h3" sx={{mb: 2.5}}>
                                    Beispielzustände
                                </Typography>
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gridTemplateColumns: {xs: 'minmax(0, 1fr)', sm: 'repeat(2, minmax(0, 1fr))'},
                                        gap: 3,
                                    }}
                                >
                                    <InputModeField
                                        label="Leerer Pflichtwert"
                                        hint="Ein Pflichtfeld mit sichtbarer Validierung."
                                        error="Bitte gib einen Wert ein."
                                        required
                                        variables={VARIABLES}
                                        value={emptyRequiredTitle}
                                        onChange={setEmptyRequiredTitle}
                                        renderLiteral={({value, onChange, fieldProps}) => (
                                            <DynamicTextInputField
                                                {...fieldProps}
                                                value={value}
                                                onChange={onChange}
                                            />
                                        )}
                                    />

                                    <InputModeField
                                        label="Deaktiviertes Inkrement"
                                        hint="Der Wert kann in diesem Zustand nicht geändert werden."
                                        disabled
                                        variables={VARIABLES}
                                        value={increment}
                                        onChange={setIncrement}
                                        renderLiteral={({value, onChange, fieldProps}) => (
                                            <NumberFieldComponent
                                                {...fieldProps}
                                                value={value}
                                                onChange={onChange}
                                            />
                                        )}
                                    />

                                    <InputModeField
                                        label="Schreibgeschützter Titel"
                                        hint="Der bestehende Ausdruck bleibt lesbar."
                                        readOnly
                                        variables={VARIABLES}
                                        value={readOnlyTitle}
                                        onChange={setReadOnlyTitle}
                                        renderLiteral={({value, onChange, fieldProps}) => (
                                            <DynamicTextInputField
                                                {...fieldProps}
                                                value={value}
                                                onChange={onChange}
                                            />
                                        )}
                                    />

                                    <Box sx={{maxWidth: 340}}>
                                        <InputModeField
                                            label="Sehr langer Ausdruck"
                                            hint="Der Ausdruck bleibt auch bei geringer Breite vollständig sichtbar."
                                            variables={VARIABLES}
                                            value={longExpression}
                                            onChange={setLongExpression}
                                            renderLiteral={({value, onChange, fieldProps}) => (
                                                <DynamicTextInputField
                                                    {...fieldProps}
                                                    multiline
                                                    rows={4}
                                                    value={value}
                                                    onChange={onChange}
                                                />
                                            )}
                                        />
                                    </Box>
                                </Box>
                            </Box>
                        )}
                    </Box>
                </Paper>
            </Box>
        </Box>
    );
}
