import {KeyboardEvent, useMemo, useState} from 'react';
import {Autocomplete, TextField} from '@mui/material';

interface ChipInputFieldComponentProps {
    label: string;
    value: string[] | null | undefined;
    onChange: (value: string[] | null | undefined) => void;
    placeholder?: string;
    hint?: string;
    error?: string;
    disabled?: boolean;
    required?: boolean;
    readOnly?: boolean;
    suggestions?: string[];
    allowDuplicates?: boolean;
}

function normalizeValues(values: string[], allowDuplicates: boolean): string[] | undefined {
    const cleaned = values
        .map((entry) => entry.trim())
        .filter((entry) => entry.length > 0);

    if (cleaned.length === 0) {
        return undefined;
    }

    if (allowDuplicates) {
        return cleaned;
    }

    return Array.from(new Set(cleaned));
}

export function ChipInputFieldComponent(props: ChipInputFieldComponentProps) {
    const {
        label,
        value,
        onChange,
        placeholder,
        hint,
        error,
        disabled,
        required,
        readOnly,
        suggestions,
        allowDuplicates,
    } = props;
    const [inputValue, setInputValue] = useState('');

    const options = useMemo(() => {
        return Array.from(new Set((suggestions ?? []).map((entry) => entry.trim()).filter((entry) => entry.length > 0)));
    }, [suggestions]);

    return (
        <Autocomplete
            multiple
            freeSolo
            readOnly={readOnly}
            disabled={disabled}
            options={options}
            value={value ?? []}
            inputValue={inputValue}
            filterSelectedOptions={allowDuplicates !== true}
            onInputChange={(_, updatedValue) => {
                setInputValue(updatedValue);
            }}
            onChange={(_, updatedValue) => {
                onChange(normalizeValues(updatedValue, allowDuplicates === true));
            }}
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={label}
                    placeholder={placeholder}
                    required={required}
                    error={error != null}
                    helperText={error ?? hint}
                    InputLabelProps={{
                        title: label,
                    }}
                    onKeyDown={(event: KeyboardEvent<HTMLInputElement>) => {
                        (params.inputProps as any).onKeyDown?.(event);

                        if (event.key !== 'Tab') {
                            return;
                        }

                        const normalizedInput = inputValue.trim();
                        if (normalizedInput.length === 0) {
                            return;
                        }

                        event.preventDefault();
                        onChange(normalizeValues([...(value ?? []), normalizedInput], allowDuplicates === true));
                        setInputValue('');
                    }}
                />
            )}
        />
    );
}
