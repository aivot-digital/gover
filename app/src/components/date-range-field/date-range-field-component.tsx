import {Grid, Typography} from '@mui/material';
import {DateFieldComponent} from '../date-field/date-field-component';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DateRangeValue} from '../../models/elements/form/input/date-range-field-element';

interface DateRangeFieldComponentProps {
    label: string;
    value?: DateRangeValue | null;
    onChange: (value: DateRangeValue | undefined) => void;
    hint?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    mode?: DateFieldComponentModelMode;
}

function normalizeRange(value: DateRangeValue): DateRangeValue | undefined {
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

export function DateRangeFieldComponent(props: DateRangeFieldComponentProps) {
    const mode = props.mode ?? DateFieldComponentModelMode.Day;

    return (
        <Grid container columnSpacing={1} alignItems="center">
            <Grid size={{xs: 12, md: 'grow'}}>
                <DateFieldComponent
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
                <Typography variant="body1" aria-hidden sx={{mx: 1}}>
                    –
                </Typography>
            </Grid>
            <Grid size={{xs: 12, md: 'grow'}}>
                <DateFieldComponent
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
                    mode={mode}
                    debounce={1000}
                />
            </Grid>
        </Grid>
    );
}
