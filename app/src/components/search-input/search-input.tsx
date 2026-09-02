import {
    IconButton,
    InputAdornment,
    type SxProps,
    TextField,
    type Theme,
    Tooltip,
} from '@mui/material';
import React from 'react';
import Search from '@aivot/mui-material-symbols-400-n25-outlined/Search';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import {
    FormField,
    type FormFieldControlContext,
    type FormFieldLayoutProps,
    getNativeInputAriaProps,
} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export interface SearchInputProps extends FormFieldLayoutProps {
    value: string;
    onChange: (val: string) => void;
    label: string;
    placeholder?: string;
    autoFocus?: boolean;
    controlSx?: SxProps<Theme>;
    disabled?: boolean;
    clearable?: boolean;
    size?: 'small' | 'medium';
    fullWidth?: boolean;
    hideLabel?: boolean;
    debounce?: number;
}

export function SearchInput(props: SearchInputProps) {
    const hideLabel = props.hideLabel ?? false;
    const clearable = props.clearable ?? true;
    const debounce = props.debounce ?? 0;
    const debounceTimeoutRef = React.useRef<number | null>(null);
    // Keep typing responsive while parent state may update debounced.
    const [localValue, setLocalValue] = React.useState(props.value);

    React.useEffect(() => {
        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
            debounceTimeoutRef.current = null;
        }
        setLocalValue(props.value);
    }, [props.value]);

    React.useEffect(() => {
        return () => {
            if (debounceTimeoutRef.current != null) {
                window.clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, []);

    const emitChange = (nextValue: string): void => {
        if (debounce <= 0) {
            props.onChange(nextValue);
            return;
        }

        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
        }

        debounceTimeoutRef.current = window.setTimeout(() => {
            debounceTimeoutRef.current = null;
            props.onChange(nextValue);
        }, debounce);
    };

    const handleInputChange = (nextValue: string): void => {
        setLocalValue(nextValue);
        emitChange(nextValue);
    };

    const handleClear = (): void => {
        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
            debounceTimeoutRef.current = null;
        }

        setLocalValue('');
        props.onChange('');
    };

    const handleBlur = (): void => {
        // Align with TextFieldComponent: commit pending debounced change immediately on blur.
        if (debounceTimeoutRef.current != null) {
            window.clearTimeout(debounceTimeoutRef.current);
            debounceTimeoutRef.current = null;
            const cleaned = localValue.trim();
            props.onChange(cleaned);
            if (cleaned !== localValue) {
                setLocalValue(cleaned);
            }
            return;
        }

        const cleaned = localValue.trim();
        if (cleaned !== localValue) {
            setLocalValue(cleaned);
            props.onChange(cleaned);
        }
    };

    return (
        <FormField
            id={props.id}
            label={hideLabel ? '' : props.label}
            ariaLabel={props.ariaLabel ?? props.label}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            disabled={props.disabled}
            margin={props.margin ?? 'none'}
            showOptionalIndicator={props.showOptionalIndicator ?? false}
            sx={[
                {width: props.fullWidth === false ? 'auto' : '100%'},
                ...(Array.isArray(props.sx) ? props.sx : [props.sx]),
            ]}
        >
            {(fieldContext: FormFieldControlContext) => (
                <TextField
                    id={fieldContext.controlId}
                    type="search"
                    value={localValue}
                    onChange={event => {
                        handleInputChange(event.target.value ?? '');
                    }}
                    onBlur={handleBlur}
                    label={undefined}
                    placeholder={props.placeholder}
                    variant="outlined"
                    margin="none"
                    fullWidth={props.fullWidth ?? true}
                    autoFocus={props.autoFocus}
                    disabled={props.disabled}
                    size={props.size ?? 'small'}
                    sx={[
                        {
                            margin: 0,
                            '& .MuiInputBase-root': {
                                minHeight: FormFieldTokens.controlMinHeight,
                            },
                            '& input[type="search"]::-webkit-search-cancel-button': {
                                appearance: 'none',
                            },
                        },
                        ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                    ]}
                    slotProps={{
                        input: {
                            startAdornment: (
                                <InputAdornment position="start">
                                    <Search fontSize="small" aria-hidden="true" />
                                </InputAdornment>
                            ),
                            endAdornment: clearable && localValue.trim().length > 0 ? (
                                <InputAdornment position="end">
                                    <Tooltip title="Suche löschen" arrow>
                                        <span>
                                            <IconButton
                                                size={props.size ?? 'small'}
                                                onMouseDown={(event) => event.preventDefault()}
                                                onClick={handleClear}
                                                disabled={props.disabled}
                                                aria-label="Suche löschen"
                                            >
                                                <Close fontSize="small" />
                                            </IconButton>
                                        </span>
                                    </Tooltip>
                                </InputAdornment>
                            ) : undefined,
                        },
                        htmlInput: {
                            ...getNativeInputAriaProps(fieldContext),
                            spellCheck: false,
                        },
                    }}
                />
            )}
        </FormField>
    );
}
