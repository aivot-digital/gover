import {Grid, Typography} from '@mui/material';
import {TimeFieldComponent} from '../time-field/time-field-component';
import {TimeRangeValue} from '../../models/elements/form/input/time-range-field-element';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';

interface TimeRangeFieldComponentProps {
    label: string;
    value?: TimeRangeValue | null;
    onChange: (value: TimeRangeValue | undefined) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    mode?: TimeFieldComponentModelMode;
}

function normalizeRange(value: TimeRangeValue): TimeRangeValue | undefined {
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

export function TimeRangeFieldComponent(props: TimeRangeFieldComponentProps) {
    return (
        <Grid container columnSpacing={1} alignItems="center">
            <Grid size={{xs: 12, md: 'grow'}}>
                <TimeFieldComponent
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
                <TimeFieldComponent
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
                    debounce={1000}
                    mode={props.mode}
                />
            </Grid>
        </Grid>
    );
}
