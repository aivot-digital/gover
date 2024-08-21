import {TimeFieldElement} from '../../models/elements/form/input/time-field-element';
import {LocalizationProvider, TimePicker} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import deLocale from 'date-fns/locale/de';
import {useCallback} from 'react';
import {BaseViewProps} from "../../views/base-view";
import { renderTimeViewClock } from '@mui/x-date-pickers/timeViewRenderers';

export function TimeFieldComponentView({
                                           element,
                                           value,
                                           error,
                                           setValue,
                                       }: BaseViewProps<TimeFieldElement, string>) {
    const dateValue = value != null ? new Date(value) : null;

    const handleChange = useCallback((changedValue: unknown | null) => {
        if (element.id != null) {
            if (changedValue != null) {
                if (changedValue instanceof Date) {
                    if (isNaN(changedValue.getTime())) {
                        setValue(undefined);
                    } else {
                        setValue(changedValue.toISOString() ?? '');
                    }
                }
            } else {
                setValue(undefined);
            }
        }
    }, [element.id, setValue]);

    return (
        <LocalizationProvider
            dateAdapter={AdapterDateFns}
            adapterLocale={deLocale}
        >
            <TimePicker
                label={element.label != null && element.label.length > 0 ? `${element.label}${element.required ? ' *' : ''}` : undefined}
                value={dateValue}
                onChange={handleChange}
                disabled={element.disabled}
                // @ts-ignore
                slotProps={{
                    textField: {
                        variant: 'outlined',
                        error: error != null,
                        helperText: error != null ? error : element.hint,
                        InputLabelProps: {
                            title: element.label
                        }
                    },
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}
                // @ts-ignore
                viewRenderers={{
                    hours: renderTimeViewClock,
                    minutes: renderTimeViewClock,
                    seconds: renderTimeViewClock,
                }}
            />
        </LocalizationProvider>
    );
}
