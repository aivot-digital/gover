import {type SxProps, type Theme} from '@mui/material';
import {useCallback, useEffect, useRef} from 'react';
import {TimeFieldComponent} from '../time-field/time-field-component';
import {type TimeRangeValue} from '../../models/elements/form/input/time-range-field-element';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import {type FormFieldGroupLayoutProps} from '../form-field';
import {TemporalRangeFieldLayout} from '../temporal-range-field/temporal-range-field-layout';

export interface TimeRangeFieldComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: TimeRangeValue | null;
    onChange: (value: TimeRangeValue | null) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    mode?: TimeFieldComponentModelMode;
    controlSx?: SxProps<Theme>;
}

function normalizeRange(value: TimeRangeValue): TimeRangeValue | null {
    const start = value.start ?? null;
    const end = value.end ?? null;

    return start == null && end == null ? null : {start, end};
}

export function TimeRangeFieldComponent(props: TimeRangeFieldComponentProps) {
    const valueRef = useRef<TimeRangeValue | null>(props.value ?? null);

    useEffect(() => {
        valueRef.current = props.value ?? null;
    }, [props.value]);

    const updateRange = useCallback((patch: Partial<TimeRangeValue>) => {
        const nextValue = normalizeRange({
            start: valueRef.current?.start,
            end: valueRef.current?.end,
            ...patch,
        });
        valueRef.current = nextValue;
        props.onChange(nextValue);
    }, [props.onChange]);

    return (
        <TemporalRangeFieldLayout
            id={props.id}
            label={props.label}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={props.hint}
            error={props.error}
            required={props.required}
            disabled={props.disabled}
            busy={props.busy}
            margin={props.margin}
            sx={props.sx}
            showOptionalIndicator={props.showOptionalIndicator}
            controlSx={props.controlSx}
            renderStart={(fieldContext) => (
                <TimeFieldComponent
                    id={`${fieldContext.groupId}-start`}
                    label="Von"
                    ariaDescribedBy={fieldContext.describedBy}
                    showOptionalIndicator={false}
                    value={props.value?.start ?? undefined}
                    onChange={(start) => updateRange({start})}
                    required={props.required}
                    disabled={props.disabled}
                    busy={props.busy}
                    error={props.error}
                    hideHelperText
                    margin="none"
                    debounce={1000}
                    mode={props.mode}
                />
            )}
            renderEnd={(fieldContext) => (
                <TimeFieldComponent
                    id={`${fieldContext.groupId}-end`}
                    label="Bis"
                    ariaDescribedBy={fieldContext.describedBy}
                    showOptionalIndicator={false}
                    value={props.value?.end ?? undefined}
                    onChange={(end) => updateRange({end})}
                    required={props.required}
                    disabled={props.disabled}
                    busy={props.busy}
                    error={props.error}
                    hideHelperText
                    margin="none"
                    debounce={1000}
                    mode={props.mode}
                />
            )}
        />
    );
}
