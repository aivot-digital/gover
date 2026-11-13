import {Box, TextField} from '@mui/material';
import {useEffect, useRef, useState} from 'react';
import {FormField, FormFieldGroup, FormFieldGroupLayoutProps, getNativeInputAriaProps} from '../form-field';
import {DurationLargestUnit, DurationPrecision} from '../../models/elements/form/input/duration-field-element';
import {validateDurationSeconds} from './duration-value';
import {formFieldInputRootSx} from '../../theming/form-field-tokens';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

export interface DurationFieldComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: number | null;
    onChange: (value: number | null) => void;
    onBlur?: (value: number | null) => void;
    precision?: DurationPrecision | null;
    largestUnit?: DurationLargestUnit | null;
    minDuration?: number | null;
    maxDuration?: number | null;
    hint?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
}

type Parts = [string, string, string, string];

function splitDuration(value: number | null | undefined, largestUnit: DurationLargestUnit): Parts {
    if (value == null) return ['', '', '', ''];
    const showDays = largestUnit === DurationLargestUnit.Day;
    if (!Number.isSafeInteger(value) || value < 0) return showDays ? [String(value), '', '', ''] : ['', String(value), '', ''];
    return [
        showDays ? String(Math.floor(value / 86400)) : '',
        String(Math.floor((showDays ? value % 86400 : value) / 3600)),
        String(Math.floor(value % 3600 / 60)),
        String(value % 60),
    ];
}

export function DurationFieldComponent(props: DurationFieldComponentProps) {
    const precision = props.precision ?? DurationPrecision.Minute;
    const largestUnit = props.largestUnit ?? DurationLargestUnit.Hour;
    const [parts, setParts] = useState<Parts>(() => splitDuration(props.value, largestUnit));
    const previousLargestUnit = useRef(largestUnit);
    const lastEmittedValue = useRef(props.value);
    const valueRef = useRef(props.value);
    const locked = props.disabled || props.readOnly || props.busy;

    useEffect(() => {
        valueRef.current = props.value;
        // Keep authored parts stable while the parent echoes the emitted total.
        if (!Object.is(props.value, lastEmittedValue.current) || largestUnit !== previousLargestUnit.current) {
            setParts(splitDuration(props.value, largestUnit));
            lastEmittedValue.current = props.value;
            previousLargestUnit.current = largestUnit;
        }
    }, [props.value, largestUnit]);

    const error = props.error ?? validateDurationSeconds(props.value, precision, props.minDuration, props.maxDuration, largestUnit);
    const fields = ['Tage', 'Stunden', 'Minuten', 'Sekunden']
        .map((label, index) => ({label, index}))
        .filter(({index}) => (index !== 0 || largestUnit === DurationLargestUnit.Day)
            && (index !== 3 || precision === DurationPrecision.Second));

    return (
        <FormFieldGroup
            {...props}
            error={error}
            sx={[{containerType: 'inline-size'}, ...(Array.isArray(props.sx) ? props.sx : [props.sx])]}
        >
            {(group) => (
                <Box
                    onBlur={(event) => {
                        if (event.currentTarget.contains(event.relatedTarget as Node | null)) return;
                        if (locked) return;
                        setParts(splitDuration(valueRef.current, largestUnit));
                        props.onBlur?.(valueRef.current ?? null);
                    }}
                    sx={{
                        display: 'grid',
                        gridTemplateColumns: `repeat(${fields.length}, minmax(0, 1fr))`,
                        gap: 1.5,
                        '@container (max-width: 24rem)': {gridTemplateColumns: 'minmax(0, 1fr)'},
                    }}
                >
                    {fields.map(({label, index}) => (
                        <FormField
                            key={label}
                            id={`${group.groupId}-${index}`}
                            label={label}
                            ariaDescribedBy={group.describedBy}
                            error={error}
                            hideHelperText
                            disabled={props.disabled}
                            readOnly={props.readOnly}
                            busy={props.busy}
                            margin="none"
                            showOptionalIndicator={false}
                        >
                            {(field) => (
                                <TextField
                                    id={field.controlId}
                                    label={undefined}
                                    helperText={undefined}
                                    variant="outlined"
                                    margin="none"
                                    value={parts[index]}
                                    onChange={(event) => {
                                        if (locked || !/^\d*$/.test(event.target.value)) return;
                                        const next = [...parts] as Parts;
                                        next[index] = event.target.value;
                                        // Hidden seconds are preserved until an explicit edit, never silently rounded on load.
                                        if (precision === DurationPrecision.Minute) next[3] = '';
                                        const value = fields.every(({index: partIndex}) => next[partIndex] === '')
                                            ? null
                                            : Number(next[0]) * 86400 + Number(next[1]) * 3600 + Number(next[2]) * 60 + Number(next[3]);
                                        if (value != null && !Number.isSafeInteger(value)) return;
                                        setParts(next);
                                        valueRef.current = value;
                                        lastEmittedValue.current = value;
                                        props.onChange(value);
                                    }}
                                    fullWidth
                                    size="small"
                                    disabled={props.disabled}
                                    error={field.invalid}
                                    slotProps={{
                                        input: {
                                            readOnly: props.readOnly || props.busy,
                                            sx: [
                                                formFieldInputRootSx,
                                                {backgroundColor: props.busy ? getDisabledFieldBackground : undefined},
                                            ],
                                        },
                                        htmlInput: {...getNativeInputAriaProps(field), inputMode: 'numeric', pattern: '[0-9]*'},
                                    }}
                                />
                            )}
                        </FormField>
                    ))}
                </Box>
            )}
        </FormFieldGroup>
    );
}
