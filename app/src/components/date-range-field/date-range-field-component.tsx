import {type SxProps, type Theme} from '@mui/material';
import {useCallback, useEffect, useRef} from 'react';
import {DateFieldComponent} from '../date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {type DateRangeValue} from '../../models/elements/form/input/date-range-field-element';
import {type FormFieldGroupLayoutProps} from '../form-field';
import {TemporalRangeFieldLayout} from '../temporal-range-field/temporal-range-field-layout';

export interface DateRangeFieldComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: DateRangeValue | null;
    onChange: (value: DateRangeValue | null) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    mode?: DateFieldComponentModelMode;
    controlSx?: SxProps<Theme>;
}

function normalizeRange(value: DateRangeValue): DateRangeValue | null {
    const start = value.start ?? null;
    const end = value.end ?? null;

    return start == null && end == null ? null : {start, end};
}

export function DateRangeFieldComponent(props: DateRangeFieldComponentProps) {
    const mode = props.mode ?? DateFieldComponentModelMode.Day;
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
                <DateFieldComponent
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
                    mode={mode}
                    debounce={1000}
                />
            )}
            renderEnd={(fieldContext) => (
                <DateFieldComponent
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
                    mode={mode}
                    debounce={1000}
                />
            )}
        />
    );
}
