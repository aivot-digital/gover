import {
    FormControl,
    FormControlLabel,
    FormHelperText,
    FormLabel,
    Radio,
    RadioGroup,
    ToggleButton,
    ToggleButtonGroup
} from '@mui/material';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {Fragment, useMemo} from 'react';

export interface RadioFieldComponentProps {
    label: string;
    value?: string;
    onChange: (val: string | undefined) => void;
    options: SelectFieldComponentOption[];
    error?: string;
    hint?: string;
    disabled?: boolean;
    required?: boolean;
    displayInline?: boolean;
    toggleButtons?: boolean;
}

const generateRandomId = () => {
    return 'id-' + Math.random().toString(36).substr(2, 9);
};

export function RadioFieldComponent({
                                        label,
                                        value,
                                        onChange,
                                        options,
                                        error,
                                        hint,
                                        disabled,
                                        required,
                                        displayInline = false,
                                        toggleButtons = false,
                                    }: RadioFieldComponentProps) {

    const uniqueId = useMemo(() => generateRandomId(), []);

    return (
        <FormControl
            component="fieldset"
            error={error != null}
            disabled={disabled}
        >
            <FormLabel
                id={'label-' + uniqueId}
            >
                {label} {required && ' *'}
            </FormLabel>
            {
                toggleButtons ?
                    <ToggleButtonGroup
                        aria-labelledby={'label-' + uniqueId}
                        exclusive
                        value={value ?? null}
                        onChange={(_, newValue: string | null) => {
                            onChange(isStringNullOrEmpty(newValue) ? undefined : (newValue ?? undefined));
                        }}
                        fullWidth={!displayInline}
                        sx={{
                            mt: 1,
                            alignSelf: displayInline ? 'flex-start' : undefined,
                            '& .MuiToggleButton-root': {
                                textTransform: 'none',
                            },
                        }}
                    >
                        {
                            (options ?? []).map(option => (
                                <ToggleButton
                                    key={option.value}
                                    value={option.value}
                                    disabled={disabled}
                                    size="small"
                                >
                                    {option.label}
                                </ToggleButton>
                            ))
                        }
                    </ToggleButtonGroup>
                    :
                    <RadioGroup
                        aria-labelledby={'label-' + uniqueId}
                        name={'radio-group-' + uniqueId}
                        value={value ?? ''}
                        onChange={event => {
                            if (isStringNullOrEmpty(event.target.value)) {
                                onChange(undefined);
                            } else {
                                onChange(event.target.value ?? '');
                            }
                        }}
                        row={displayInline}
                    >
                        {
                            !required &&
                            <FormControlLabel
                                value={''}
                                control={<Radio />}
                                label="Keine Auswahl"
                                disabled={disabled}
                                sx={{
                                    fontStyle: 'italic',
                                    mr: displayInline ? 3 : undefined,
                                }}
                            />
                        }
                        {
                            (options ?? []).map(option => (
                                <Fragment key={option.value}>
                                    <FormControlLabel
                                        value={option.value}
                                        control={<Radio />}
                                        label={option.label}
                                        disabled={disabled}
                                        sx={{
                                            ...(displayInline ? {mr: 3} : {}),
                                            '& .MuiFormControlLabel-label': {
                                                wordBreak: 'break-word',
                                                whiteSpace: 'normal',
                                            },
                                        }}
                                    />
                                    {
                                        option.subLabel != null &&
                                        <FormHelperText>
                                            {option.subLabel}
                                        </FormHelperText>
                                    }
                                </Fragment>
                            ))
                        }
                    </RadioGroup>
            }
            {
                (hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        hint != null &&
                        error == null &&
                        <span>{hint}</span>
                    }
                    {
                        error != null &&
                        <span>{error}</span>
                    }
                </FormHelperText>
            }
        </FormControl>
    );
}
