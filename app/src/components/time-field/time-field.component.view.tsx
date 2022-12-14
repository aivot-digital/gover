import {TextField} from '@mui/material';
import {TimeFieldElement} from '../../models/elements/form-elements/input-elements/time-field-element';
import {LocalizationProvider, TimePicker} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import deLocale from 'date-fns/locale/de';
import {useCallback} from 'react';
import {BaseViewProps} from '../_lib/base-view-props';

export function TimeFieldComponentView({element, value, error, setValue}: BaseViewProps<TimeFieldElement, string>) {
    const dateValue = value != null ? new Date(value) : null;

    const handleChange = useCallback((changedValue: unknown | null) => {
        if (element.id != null) {
            if (changedValue != null) {
                if (changedValue instanceof Date) {
                    if (isNaN(changedValue.getTime())) {
                        setValue(null)
                    } else {
                        setValue(changedValue.toISOString() ?? '');
                    }
                }
            } else {
                setValue(null);
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

                renderInput={(params: any) => (
                    <TextField
                        {...params}
                        error={error != null}
                        placeholder="hh:mm"
                        helperText={error != null ? error : element.hint}
                    />
                )}

                componentsProps={{
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}
            />
        </LocalizationProvider>
    );
}
