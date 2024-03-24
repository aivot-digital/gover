import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DatePicker, LocalizationProvider} from '@mui/x-date-pickers';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import deLocale from 'date-fns/locale/de';
import {DateFieldComponentProps} from "./date-field-component-props";

const formatMap = {
    [DateFieldComponentModelMode.Date]: 'dd.MM.yyyy',
    [DateFieldComponentModelMode.Month]: 'MM.yyyy',
    [DateFieldComponentModelMode.Year]: 'yyyy',
};

const formatReadableMap = {
    [DateFieldComponentModelMode.Date]: 'TT.MM.JJJJ',
    [DateFieldComponentModelMode.Month]: 'MM.JJJJ',
    [DateFieldComponentModelMode.Year]: 'JJJJ',
};

const viewsMap: {
    [k in DateFieldComponentModelMode]: ('day' | 'month' | 'year')[];
} = {
    [DateFieldComponentModelMode.Date]: ['day', 'month', 'year'],
    [DateFieldComponentModelMode.Month]: ['month', 'year'],
    [DateFieldComponentModelMode.Year]: ['year'],
};

export function DateFieldComponent({
                                       label,
                                       error,
                                       hint,
                                       required,
                                       disabled,
                                       value,
                                       minDate,
                                       maxDate,
                                       mode,
                                       onChange,
                                   }: DateFieldComponentProps) {
    const dateValue = value != null ? new Date(value) : null;

    let computedLabel = label;
    if (computedLabel) {
        computedLabel += ` (${formatReadableMap[mode ?? DateFieldComponentModelMode.Date]})`;
    }
    if (required) {
        if (computedLabel) {
            computedLabel += ' *';
        } else {
            computedLabel = '*';
        }
    }

    const format = formatMap[mode ?? DateFieldComponentModelMode.Date];
    const views = viewsMap[mode ?? DateFieldComponentModelMode.Date];
    const opensTo = mode ?? 'day';
    const helper = error != null ? error : hint;

    const handleChange = (changedValue: unknown | null) => {
        if (changedValue != null) {
            if (changedValue instanceof Date) {
                if (isNaN(changedValue.getTime())) {
                    onChange(undefined);
                } else {
                    onChange(changedValue.toISOString() ?? '');
                }
            }
        } else {
            onChange(undefined);
        }
    };

    return (
        <LocalizationProvider
            dateAdapter={AdapterDateFns}
            adapterLocale={deLocale}
        >
            <DatePicker
                label={computedLabel}

                minDate={minDate}
                maxDate={maxDate}

                views={views}
                openTo={opensTo}
                // @ts-ignore
                format={format}
                value={dateValue}

                onChange={handleChange}

                disabled={disabled}

                // @ts-ignore
                slotProps={{
                    textField: {
                        variant: 'outlined',
                        error: error != null,
                        helperText: helper
                    },
                    actionBar: {
                        actions: ['accept', 'cancel', 'clear'],
                    },
                }}

            />
        </LocalizationProvider>
    );
}

