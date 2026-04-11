import {Grid, Typography} from '@mui/material';
import {DateTimeFieldComponent} from '../date-time-field/date-time-field-component';
import {DateTimeRangeValue} from '../../models/elements/form/input/date-time-range-field-element';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';

interface DateTimeRangeFieldComponentProps {
    label: string;
    value?: DateTimeRangeValue | null;
    onChange: (value: DateTimeRangeValue | undefined) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    placeholder?: string;
    mode?: TimeFieldComponentModelMode;
}

function normalizeRange(value: DateTimeRangeValue): DateTimeRangeValue | undefined {
    const start = value.start ?? undefined;
    const end = value.end ?? undefined;

    if (start == null && end == null) {
        return undefined;
    }

    return {
        start,
        end,
    };
}

export function DateTimeRangeFieldComponent(props: DateTimeRangeFieldComponentProps) {
    return (
        <Grid container columnSpacing={1} alignItems="center">
            <Grid size={{xs: 12, md: 'grow'}}>
                <DateTimeFieldComponent
                    label={`${props.label} (Von)`}
                    value={props.value?.start ?? undefined}
                    onChange={(start) => {
                        props.onChange(normalizeRange({
                            start,
                            end: props.value?.end,
                        }) ?? undefined);
                    }}
                    hint={props.hint}
                    required={props.required}
                    disabled={props.disabled}
                    busy={props.busy}
                    error={props.error}
                    placeholder={props.placeholder}
                    debounce={1000}
                    mode={props.mode}
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
                <Typography variant="body1" aria-hidden sx={{mx: 1}}>
                    –
                </Typography>
            </Grid>
            <Grid size={{xs: 12, md: 'grow'}}>
                <DateTimeFieldComponent
                    label={`${props.label} (Bis)`}
                    value={props.value?.end ?? undefined}
                    onChange={(end) => {
                        props.onChange(normalizeRange({
                            start: props.value?.start,
                            end,
                        }) ?? undefined);
                    }}
                    hint={props.hint}
                    required={props.required}
                    disabled={props.disabled}
                    busy={props.busy}
                    error={props.error}
                    placeholder={props.placeholder}
                    debounce={1000}
                    mode={props.mode}
                />
            </Grid>
        </Grid>
    );
}
