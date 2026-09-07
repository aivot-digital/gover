import {useState} from 'react';
import {Box, ToggleButton, ToggleButtonGroup, Typography} from '@mui/material';
import {InputModeField, type InputModeValue, type InputModeVariable} from '../../components/input-mode-field/input-mode-field';
import {InputModeDefinitions, InputModes, type InputMode} from '../../components/input-mode-selector';
import {NumberFieldComponent} from '../../components/number-field/number-field-component';
import {ElementType} from '../../data/element-type/element-type';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';

type ComparisonState = 'filled' | 'empty' | 'readOnly';
type ComparisonValues = Record<InputMode, InputModeValue<number>>;

function createComparisonValues(empty: boolean): ComparisonValues {
    return {
        Literal: {type: 'Literal', value: empty ? null : 13},
        Variable: {type: 'Variable', reference: {source: 'ProcessData', path: empty ? '' : 'warenkorb.positionen.anzahl'}},
        NoCode: {type: 'NoCode', operand: empty ? {type: 'NoCodeStaticValue', value: null} : {
            type: 'NoCodeExpression',
            operatorIdentifier: 'add',
            operands: [
                {type: 'NoCodeProcessDataReference', path: 'warenkorb.positionen.anzahl'},
                {type: 'NoCodeStaticValue', value: '1'},
            ],
        }},
        LowCode: {type: 'LowCode', code: empty ? '' : [
            'const anzahl = $.warenkorb?.positionen?.anzahl ?? 0;',
            'const standardInkrement = $.konfiguration?.standardInkrement ?? 1;',
            '',
            'anzahl > 10 ? standardInkrement * 2 : standardInkrement;',
        ].join('\n')},
    };
}

export function InputModeComparison({variables}: {variables: InputModeVariable[]}) {
    const [state, setState] = useState<ComparisonState>('filled');
    const [filledValues, setFilledValues] = useState(() => createComparisonValues(false));
    const [emptyValues, setEmptyValues] = useState(() => createComparisonValues(true));
    const [rootElement] = useState(() => generateElementWithDefaultValues(ElementType.GroupLayout));
    const empty = state === 'empty';
    const values = empty ? emptyValues : filledValues;

    return (
        <Box component="section" aria-labelledby="input-mode-comparison-title" sx={{mt: 3, pb: 3, borderBottom: 1, borderColor: 'divider'}}>
            <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2}}>
                <Typography id="input-mode-comparison-title" component="h3" variant="h6">
                    Direktvergleich
                </Typography>
                <ToggleButtonGroup
                    size="small"
                    exclusive
                    value={state}
                    onChange={(_, next: ComparisonState | null) => {
                        if (next != null) setState(next);
                    }}
                    aria-label="Zustand im Eingabemodusvergleich"
                >
                    <ToggleButton value="filled">Befüllt</ToggleButton>
                    <ToggleButton value="empty">Leer</ToggleButton>
                    <ToggleButton value="readOnly">Schreibgeschützt</ToggleButton>
                </ToggleButtonGroup>
            </Box>
            <Box sx={{
                display: 'grid',
                gridTemplateColumns: {
                    xs: 'minmax(0, 1fr)',
                    md: 'repeat(2, minmax(0, 1fr))',
                    xl: 'repeat(4, minmax(0, 1fr))',
                },
                gap: 3,
            }}>
                {InputModes.map((mode) => (
                    <Box key={mode} role="group" aria-label={InputModeDefinitions[mode].label} sx={{minWidth: 0}}>
                        <InputModeField
                            key={`${empty}-${mode}`}
                            label="Inkrement"
                            required
                            margin="none"
                            variables={variables}
                            rootElement={rootElement}
                            noCodeReturnType={NoCodeDataType.Number}
                            value={values[mode]}
                            readOnly={state === 'readOnly'}
                            onChange={(value) => {
                                const setValues = empty ? setEmptyValues : setFilledValues;
                                setValues((current) => ({...current, [mode]: value}));
                            }}
                            renderLiteral={({value, onChange, fieldProps}) => (
                                <NumberFieldComponent {...fieldProps} value={value} onChange={onChange}/>
                            )}
                        />
                    </Box>
                ))}
            </Box>
        </Box>
    );
}
