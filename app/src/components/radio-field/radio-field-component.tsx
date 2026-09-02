import {
    FormControlLabel,
    FormHelperText,
    Radio,
    RadioGroup,
    type SxProps,
    type Theme,
    ToggleButton,
    ToggleButtonGroup,
} from '@mui/material';
import {Fragment} from 'react';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {
    FormFieldGroup,
    type FormFieldGroupContext,
    type FormFieldGroupLayoutProps,
} from '../form-field';

export interface RadioFieldComponentProps extends FormFieldGroupLayoutProps {
    label: string;
    value?: string | undefined | null;
    onChange: (val: string | null) => void;
    options: SelectFieldComponentOption[] | undefined | null;
    error?: string | undefined | null;
    hint?: string | undefined | null;
    disabled?: boolean | undefined | null;
    busy?: boolean | undefined | null;
    required?: boolean | undefined | null;
    displayInline?: boolean | undefined | null;
    toggleButtons?: boolean | undefined | null;
    controlSx?: SxProps<Theme>;
}

export function RadioFieldComponent(props: RadioFieldComponentProps) {
    const {
        id,
        ariaDescribedBy,
        label,
        labelAction,
        value,
        onChange,
        options = [],
        error,
        hint,
        disabled = false,
        busy = false,
        required = false,
        displayInline = false,
        toggleButtons = false,
        margin = 'normal',
        sx,
        controlSx,
        showOptionalIndicator,
    } = props;
    const isInteractionDisabled = Boolean(disabled || busy);
    const controlSxArray = Array.isArray(controlSx) ? controlSx : [controlSx];

    return (
        <FormFieldGroup
            id={id}
            label={label}
            ariaDescribedBy={ariaDescribedBy}
            labelAction={labelAction}
            hint={hint}
            error={error}
            disabled={Boolean(disabled)}
            busy={Boolean(busy)}
            required={Boolean(required)}
            margin={margin}
            showOptionalIndicator={showOptionalIndicator}
            sx={sx}
        >
            {(fieldContext: FormFieldGroupContext) => toggleButtons ? (
                <ToggleButtonGroup
                    role="presentation"
                    exclusive
                    value={value ?? null}
                    onChange={(_, newValue: string | null) => {
                        if (!isInteractionDisabled) {
                            onChange(isStringNullOrEmpty(newValue) ? null : newValue);
                        }
                    }}
                    fullWidth={!displayInline}
                    sx={[
                        {
                            alignSelf: displayInline ? 'flex-start' : undefined,
                            '& .MuiToggleButton-root': {
                                textTransform: 'none',
                            },
                        },
                        ...controlSxArray,
                    ]}
                >
                    {(options ?? []).map((option) => (
                        <ToggleButton
                            key={option.value}
                            value={option.value}
                            disabled={isInteractionDisabled}
                            size="small"
                        >
                            {option.label}
                        </ToggleButton>
                    ))}
                </ToggleButtonGroup>
            ) : (
                <RadioGroup
                    role="presentation"
                    name={`${fieldContext.groupId}-options`}
                    value={value ?? ''}
                    onChange={(event) => {
                        if (isInteractionDisabled) {
                            return;
                        }

                        if (isStringNullOrEmpty(event.target.value)) {
                            onChange(null);
                        } else {
                            onChange(event.target.value ?? '');
                        }
                    }}
                    row={Boolean(displayInline)}
                    sx={controlSxArray}
                >
                    {!required && (
                        <FormControlLabel
                            value=""
                            control={<Radio/>}
                            label="Keine Auswahl"
                            disabled={isInteractionDisabled}
                            sx={{
                                fontStyle: 'italic',
                                mr: displayInline ? 3 : undefined,
                            }}
                        />
                    )}

                    {(options ?? []).map((option) => (
                        <Fragment key={option.value}>
                            <FormControlLabel
                                value={option.value}
                                control={<Radio/>}
                                label={option.label}
                                disabled={isInteractionDisabled}
                                sx={{
                                    ...(displayInline ? {mr: 3} : {}),
                                    '& .MuiFormControlLabel-label': {
                                        wordBreak: 'break-word',
                                        whiteSpace: 'normal',
                                    },
                                }}
                            />
                            {option.subLabel != null && (
                                <FormHelperText>
                                    {option.subLabel}
                                </FormHelperText>
                            )}
                        </Fragment>
                    ))}
                </RadioGroup>
            )}
        </FormFieldGroup>
    );
}
