import {useState} from 'react';
import {Box, ToggleButton, ToggleButtonGroup, Typography} from '@mui/material';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';
import {NoCodeTestJsonInput} from './no-code-test-json-input';
import {
    NoCodeTestSource,
    NoCodeTestSourceDraft,
    NoCodeTestSourcePlan,
    buildTestSource,
    parseTestSourceJson,
    testSourceFieldDraft,
    testSourceFieldError,
    testSourceMode,
} from '../utils/no-code-test-data';
import {NoCodeTestValueInput, NO_CODE_TEST_FIELDS_SX} from './no-code-test-value-input';

export const NO_CODE_TEST_SOURCES: Record<NoCodeTestSource, { label: string; hint: string }> = {
    processData: {
        label: 'Vorgangsdaten',
        hint: 'Zum Beispiel {"antrag": {"betrag": 120}}.',
    },
    instanceData: {
        label: 'Geschützte Vorgangsdaten',
        hint: 'Zum Beispiel {"status": "neu"}.',
    },
    nodeData: {
        label: 'Elementdaten',
        hint: 'Ergebnisse je Datenschlüssel, zum Beispiel {"pruefung": {"ok": true}}.',
    },
};

export function NoCodeTestSourceInput({
                                          source,
                                          plan,
                                          value,
                                          onChange,
                                          validationRequested,
                                          touched,
                                          onTouch,
                                      }: {
    source: NoCodeTestSource;
    plan: NoCodeTestSourcePlan;
    value: NoCodeTestSourceDraft;
    onChange: (value: NoCodeTestSourceDraft) => void;
    validationRequested: boolean;
    touched: ReadonlySet<string>;
    onTouch: (key: string) => void;
}) {
    const [toolbarContainer, setToolbarContainer] = useState<HTMLElement | null>(null);
    const hintId = useNormalizedReactId();
    const mode = testSourceMode(plan, value);
    const result = buildTestSource(plan, value);
    const fieldsCheck = buildTestSource(plan, {
        ...value,
        values: {},
    }, 'fields');
    const exportResult = buildTestSource({
        ...plan,
        reason: undefined,
    }, value, 'fields');
    const base = parseTestSourceJson(value.json);
    const hasEdits = plan.fields.some(field => value.values?.[field.key] != null);
    const switchError = mode === 'fields' ? (hasEdits ? exportResult.error : base.error) : fieldsCheck.error;
    const switchHint = result.error && (mode === 'json' || hasEdits) && !plan.reason
        ? 'Der Wechsel ist möglich, sobald die Testeingaben vollständig und gültig sind.' : switchError;
    const label = NO_CODE_TEST_SOURCES[source].label;
    return <Box component="section"
                aria-label={`${label} für den Test`}
                sx={{
                    borderTop: 1,
                    borderColor: 'divider',
                    pt: 2,
                }}>
        <Box sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 1.5,
            flexWrap: 'wrap',
            mb: 1.5,
        }}>
            <Typography component="h4"
                        variant="h6">{label} für den Test</Typography>
            <Box sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
            }}>
                {mode === 'json' && <Box ref={setToolbarContainer}
                                         sx={{display: 'inline-flex'}}/>}
                <ToggleButtonGroup size="small"
                                   exclusive
                                   value={mode}
                                   aria-label={`Eingabeformat für ${label}`}
                                   sx={{'& .MuiToggleButton-root': {textTransform: 'none'}}}
                                   aria-describedby={switchHint ? hintId : undefined}
                                   onChange={(_, next) => {
                                       if (next == null || next === mode || switchError) return;
                                       onChange(next === 'json' ? {
                                           mode: 'json',
                                           json: JSON.stringify(exportResult.error ? base.value : exportResult.value, null, 2),
                                       } : {
                                           mode: 'fields',
                                           json: value.json,
                                       });
                                   }}>
                    <ToggleButton value="fields"
                                  disabled={mode !== 'fields' && Boolean(switchError)}>Eingabefelder</ToggleButton>
                    <ToggleButton value="json"
                                  disabled={mode !== 'json' && Boolean(switchError)}>JSON</ToggleButton>
                </ToggleButtonGroup>
            </Box>
        </Box>
        {switchHint && <Typography id={hintId}
                                   variant="body2"
                                   color="text.secondary"
                                   sx={{mb: 1.5}}>{switchHint}</Typography>}
        {mode === 'json' ? <NoCodeTestJsonInput toolbarContainer={toolbarContainer}
                                                ariaLabel={`${label} (JSON)`}
                                                hint={NO_CODE_TEST_SOURCES[source].hint}
                                                value={value.json ?? ''}
                                                error={validationRequested || touched.has(source) ? result.error : undefined}
                                                onBlur={() => onTouch(source)}
                                                onChange={json => onChange({
                                                    ...value,
                                                    json,
                                                    values: {},
                                                })}/>
            : <Box sx={NO_CODE_TEST_FIELDS_SX}>
                {plan.fields.map(field => <NoCodeTestValueInput key={field.key}
                                                                field={{
                                                                    ...field,
                                                                    hint: testSourceFieldError(field, value) ?? field.hint,
                                                                }}
                                                                disabled={Boolean(testSourceFieldError(field, value))}
                                                                value={testSourceFieldDraft(field, value)}
                                                                showError={validationRequested || touched.has(source + field.key)}
                                                                onBlur={() => onTouch(source + field.key)}
                                                                onChange={(draft) => onChange({
                                                                    ...value,
                                                                    mode: 'fields',
                                                                    values: {
                                                                        ...value.values,
                                                                        [field.key]: draft,
                                                                    },
                                                                })}/>)}
                {result.error && (validationRequested || plan.reason) && <Typography role="status"
                                                                                     color="error"
                                                                                     sx={{gridColumn: '1 / -1'}}>{result.error}</Typography>}
            </Box>}
    </Box>;
}
