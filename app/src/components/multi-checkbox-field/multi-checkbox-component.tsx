import {
    Checkbox,
    FormControlLabel,
    FormGroup,
    type SxProps,
    type Theme,
} from '@mui/material';
import {FormFieldGroup, type FormFieldGroupLayoutProps} from '../form-field';

export interface MultiCheckboxOptions {
    value: string;
    label: string;
}

export interface MultiCheckboxComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: string[] | null;
    onChange: (val: string[] | null) => void;
    options: MultiCheckboxOptions[];
    error?: string;
    hint?: string;
    disabled?: boolean;
    busy?: boolean;
    required?: boolean;
    displayInline?: boolean;
    controlSx?: SxProps<Theme>;
}

export function MultiCheckboxComponent(props: MultiCheckboxComponentProps) {
    const value = props.value;
    const options = props.options;
    const displayInline = props.displayInline ?? false;
    const isInteractionDisabled = Boolean(props.disabled || props.busy);

    const handleOptionToggle = (toggledOption: string): void => {
        if (value == null || value.length === 0) {
            props.onChange([toggledOption]);
            return;
        }

        if (value.includes(toggledOption)) {
            const splicedList = value.filter((currentValue) => currentValue !== toggledOption);
            props.onChange(splicedList.length > 0 ? splicedList : null);
            return;
        }

        const filteredOptions = options
            .filter((option) => value.includes(option.value) || option.value === toggledOption)
            .map((option) => option.value);
        props.onChange(filteredOptions);
    };

    return (
        <FormFieldGroup
            id={props.id}
            label={props.label}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={props.hint}
            error={props.error}
            disabled={props.disabled}
            busy={props.busy}
            required={props.required}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {() => (
                <FormGroup
                    role="presentation"
                    row={displayInline}
                    sx={props.controlSx}
                >
                    {(options ?? []).map((option) => (
                        <FormControlLabel
                            key={option.value}
                            control={(
                                <Checkbox
                                    checked={(value ?? []).includes(option.value)}
                                    onChange={() => {
                                        if (!isInteractionDisabled) {
                                            handleOptionToggle(option.value);
                                        }
                                    }}
                                    disabled={isInteractionDisabled}
                                    sx={{
                                        color: props.busy
                                            ? (theme) => `${theme.palette.action.disabled}!important`
                                            : undefined,
                                    }}
                                />
                            )}
                            label={option.label}
                            sx={{
                                ...(displayInline ? {mr: 3} : {}),
                                ...(props.busy ? {
                                    color: (theme) => `${theme.palette.text.disabled}!important`,
                                    cursor: 'not-allowed',
                                } : {}),
                                '& .MuiFormControlLabel-label': {
                                    wordBreak: 'break-word',
                                    whiteSpace: 'normal',
                                },
                            }}
                        />
                    ))}
                </FormGroup>
            )}
        </FormFieldGroup>
    );
}
