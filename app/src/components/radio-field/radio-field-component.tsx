import {
    FormControl,
    FormControlLabel,
    FormHelperText,
    FormLabel,
    Radio,
    RadioGroup,
    ToggleButton,
    ToggleButtonGroup,
} from '@mui/material';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {Fragment, useMemo} from 'react';

export interface RadioFieldComponentProps {
    label: string;
    value?: string | undefined | null;
    onChange: (val: string | undefined) => void;
    options: SelectFieldComponentOption[] | undefined | null;
    error?: string | undefined | null;
    hint?: string | undefined | null;
    disabled?: boolean | undefined | null;
    required?: boolean | undefined | null;
    displayInline?: boolean | undefined | null;
    toggleButtons?: boolean | undefined | null;
}

const generateRandomId = () => {
    return 'id-' + Math.random().toString(36).substr(2, 9);
};

export function RadioFieldComponent(props: RadioFieldComponentProps) {
    const {
        label,
        value,
        onChange,
        options = [],
        error,
        hint,
        disabled = false,
        required = false,
        displayInline = false,
        toggleButtons = false,
    } = props;

    const uniqueId = useMemo(() => generateRandomId(), []);

    return (
        <FormControl
            component="fieldset"
            error={error != null}
            disabled={disabled ?? false}
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
                        exclusive={true}
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
                            (options ?? [])
                                .map(option => (
                                    <ToggleButton
                                        key={option.value}
                                        value={option.value}
                                        disabled={disabled ?? false}
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
                        row={displayInline ?? false}
                    >
                        {
                            !required &&
                            <FormControlLabel
                                value={''}
                                control={<Radio/>}
                                label="Keine Auswahl"
                                disabled={disabled ?? false}
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
                                        control={<Radio/>}
                                        label={option.label}
                                        disabled={disabled ?? false}
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
