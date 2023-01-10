import {TextField} from '@mui/material';
import {
    DateFieldComponentModelMode,
    DateFieldElement
} from '../../models/elements/form-elements/input-elements/date-field-element';
import {DatePicker, LocalizationProvider} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import deLocale from 'date-fns/locale/de';
import {useCallback} from 'react';
import {faCalendarDay} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {BaseViewProps} from '../_lib/base-view-props';

const formatMap = {
    [DateFieldComponentModelMode.Date]: 'dd.MM.yyyy',
    [DateFieldComponentModelMode.Month]: 'MM.yyyy',
    [DateFieldComponentModelMode.Year]: 'yyyy',
}

const maskMap = {
    [DateFieldComponentModelMode.Date]: '__.__.____',
    [DateFieldComponentModelMode.Month]: '__.____',
    [DateFieldComponentModelMode.Year]: '____',
}

const placeholderMap = {
    [DateFieldComponentModelMode.Date]: 'TT.MM.JJJJ',
    [DateFieldComponentModelMode.Month]: 'MM.JJJJ',
    [DateFieldComponentModelMode.Year]: 'JJJJ',
}

const viewsMap: {
    [k in DateFieldComponentModelMode]: ('day' | 'month' | 'year')[];
} = {
    [DateFieldComponentModelMode.Date]: ['day', 'month', 'year'],
    [DateFieldComponentModelMode.Month]: ['month', 'year'],
    [DateFieldComponentModelMode.Year]: ['year'],
}

export function DateFieldComponentView({setValue, element, value, error}: BaseViewProps<DateFieldElement, string>) {
    const dateValue = value != null ? new Date(value) : null;

    let label = element.label;
    if (label) {
        label += ` (${placeholderMap[element.mode ?? DateFieldComponentModelMode.Date]})`;
    }
    if (element.required) {
        if (label) {
            label += ' *';
        } else {
            label = '*';
        }
    }

    const mask = maskMap[element.mode ?? DateFieldComponentModelMode.Date];
    const format = formatMap[element.mode ?? DateFieldComponentModelMode.Date];
    const views = viewsMap[element.mode ?? DateFieldComponentModelMode.Date];
    const opensTo = element.mode ?? 'day';
    const helper = error != null ? error : element.hint;
    const placeholder = placeholderMap[element.mode ?? DateFieldComponentModelMode.Date];

    const handleChange = useCallback((changedValue: unknown | null) => {
        if (element.id != null) {
            if (changedValue != null) {
                if (changedValue instanceof Date) {
                    if (isNaN(changedValue.getTime())) {
                        setValue(null);
                    } else {
                        setValue(changedValue.toISOString() ?? '');
                    }
                }
            } else {
                setValue(null);
            }
        }
    }, [setValue, element.id]);

    return (
        <LocalizationProvider
            dateAdapter={AdapterDateFns}
            adapterLocale={deLocale}
        >
            <DatePicker
                label={label}

                disableFuture={element.mustBePast}
                disablePast={element.mustBeFuture}

                views={views}
                openTo={opensTo}
                inputFormat={format}
                mask={mask}
                value={dateValue}

                onChange={handleChange}

                disabled={element.disabled}

                renderInput={(params: any) => (
                    <TextField
                        {...params}
                        error={error != null}
                        helperText={helper}
                        inputProps={{
                            ...params.inputProps,
                            placeholder,
                        }}
                    />
                )}

                componentsProps={{
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}

                components={{
                    OpenPickerIcon: OpenIcon,
                }}
            />
        </LocalizationProvider>
    );
}

const OpenIcon = () => <FontAwesomeIcon icon={faCalendarDay}/>;
