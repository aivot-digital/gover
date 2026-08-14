import {FormHelperText, Grid, Typography} from '@mui/material';
import {DateFieldComponent} from '../date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DateRangeValue} from '../../models/elements/form/input/date-range-field-element';
import {useCallback, useEffect, useRef} from 'react';

interface DateRangeFieldComponentProps {
    label: string;
    value?: DateRangeValue | null;
    onChange: (value: DateRangeValue | null) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    mode?: DateFieldComponentModelMode;
}

function normalizeRange(value: DateRangeValue): DateRangeValue | null {
    const start = value.start ?? null;
    const end = value.end ?? null;

    if (start == null && end == null) {
        return null;
    }

    return {
        start,
        end,
    };
}

export function DateRangeFieldComponent(props: DateRangeFieldComponentProps) {
    const mode = props.mode ?? DateFieldComponentModelMode.Day;
    const helperText = props.error ?? props.hint;
    const valueRef = useRef<DateRangeValue | null>(props.value ?? null);

    useEffect(() => {
        valueRef.current = props.value ?? null;
    }, [props.value]);

    const updateRange = useCallback((patch: Partial<DateRangeValue>) => {
        const nextValue = normalizeRange({
            start: valueRef.current?.start,
            end: valueRef.current?.end,
            ...patch,
        });
        valueRef.current = nextValue;
        props.onChange(nextValue);
    }, [props.onChange]);

    return (
        <Grid container rowSpacing={0.5}>
            <Grid size={12}>
                <Grid container columnSpacing={1} sx={{
                    alignItems: "center"
                }}>
                    <Grid size={{xs: 12, md: 'grow'}}>
                        <DateFieldComponent
                            label={`${props.label} (Von)`}
                            value={props.value?.start ?? undefined}
                            onChange={(start) => {
                                updateRange({start});
                            }}
                            required={props.required}
                            disabled={props.disabled}
                            busy={props.busy}
                            error={props.error}
                            hideHelperText={true}
                            mode={mode}
                            debounce={1000}
                        />
                    </Grid>
                    <Grid
                        size={{xs: 12, md: 'auto'}}
                        sx={{
                            display: {
                                xs: 'none',
                                md: 'flex',
                            },
                            alignItems: 'center',
                            justifyContent: 'center',
                        }}
                    >
                        <Typography variant="body1" aria-hidden sx={{mx: 1, transform: 'translateY(2px)'}}>
                            –
                        </Typography>
                    </Grid>
                    <Grid size={{xs: 12, md: 'grow'}}>
                        <DateFieldComponent
                            label={`${props.label} (Bis)`}
                            value={props.value?.end ?? undefined}
                            onChange={(end) => {
                                updateRange({end});
                            }}
                            required={props.required}
                            disabled={props.disabled}
                            busy={props.busy}
                            error={props.error}
                            hideHelperText={true}
                            mode={mode}
                            debounce={1000}
                        />
                    </Grid>
                </Grid>
            </Grid>
            {helperText != null && helperText.length > 0 && (
                <Grid size={12}>
                    <FormHelperText error={props.error != null} sx={{mx: 1.75, mt: -1}}>
                        {helperText}
                    </FormHelperText>
                </Grid>
            )}
        </Grid>
    );
}
