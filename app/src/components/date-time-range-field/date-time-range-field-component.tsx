import {type SxProps, type Theme} from '@mui/material';
import {useCallback, useEffect, useRef} from 'react';
import {DateTimeFieldComponent} from '../date-time-field/date-time-field-component';
import {type DateTimeRangeValue} from '../../models/elements/form/input/date-time-range-field-element';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import {type FormFieldGroupLayoutProps} from '../form-field';
import {TemporalRangeFieldLayout} from '../temporal-range-field/temporal-range-field-layout';

export interface DateTimeRangeFieldComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: DateTimeRangeValue | null;
    onChange: (value: DateTimeRangeValue | null) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    placeholder?: string;
    mode?: TimeFieldComponentModelMode;
    controlSx?: SxProps<Theme>;
}

function normalizeRange(value: DateTimeRangeValue): DateTimeRangeValue | null {
    const start = value.start ?? null;
    const end = value.end ?? null;

    return start == null && end == null ? null : {start, end};
}

export function DateTimeRangeFieldComponent(props: DateTimeRangeFieldComponentProps) {
    const valueRef = useRef<DateTimeRangeValue | null>(props.value ?? null);

    useEffect(() => {
        valueRef.current = props.value ?? null;
    }, [props.value]);

    const updateRange = useCallback((patch: Partial<DateTimeRangeValue>) => {
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
                <DateTimeFieldComponent
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
                    placeholder={props.placeholder}
                    margin="none"
                    debounce={1000}
                    mode={props.mode}
                />
            )}
            renderEnd={(fieldContext) => (
                <DateTimeFieldComponent
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
                    placeholder={props.placeholder}
                    margin="none"
                    debounce={1000}
                    mode={props.mode}
                />
            )}
        />
    );
}
