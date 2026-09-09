import {Box, Checkbox, FormControlLabel, FormHelperText, Switch} from '@mui/material';
import {type CheckboxFieldComponentProps} from './checkbox-field-component-props';
import {
    type FormFieldControlContext,
    FormFieldLabelContent,
    getNativeInputAriaProps,
    mergeAriaIds,
} from '../form-field';
import {
    formFieldHelperTextSx,
    formFieldLabelActionSx,
    formFieldLabelRowSx,
    formFieldLabelSx,
} from '../../theming/form-field-tokens';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';
import {FormFieldFrame} from '../form-field/form-field-frame';

function hasContent(content: unknown): boolean {
    return content !== null && content !== undefined && content !== false && content !== '';
}

export function CheckboxFieldComponent(props: CheckboxFieldComponentProps) {
    const generatedId = useNormalizedReactId();
    const controlId = props.id ?? `field-${generatedId}`;
    const labelId = `${controlId}-label`;
    const helperTextId = `${controlId}-helper-text`;
    const hasError = hasContent(props.error);
    const helperText = hasError ? props.error : props.hint;
    const hasHelperText = hasContent(helperText);
    const describedBy = mergeAriaIds(
        hasHelperText ? helperTextId : undefined,
        props.ariaDescribedBy,
    );
    const disabled = Boolean(props.disabled);
    const busy = Boolean(props.busy);
    const readOnly = Boolean(props.readOnly);
    const required = Boolean(props.required);
    const isInteractionDisabled = disabled || busy || readOnly;
    const margin = props.margin ?? 'normal';
    const controlContext: FormFieldControlContext = {
        controlId,
        labelId,
        helperTextId: hasHelperText ? helperTextId : undefined,
        disabled,
        readOnly,
        busy,
        required,
        invalid: hasError,
        ariaProps: {
            'aria-label': props.label.length === 0 ? props.ariaLabel : undefined,
            'aria-labelledby': undefined,
            'aria-describedby': describedBy,
            'aria-disabled': isInteractionDisabled ? true : undefined,
            'aria-readonly': readOnly ? true : undefined,
            'aria-busy': busy ? true : undefined,
            'aria-required': required ? true : undefined,
            'aria-invalid': hasError ? true : undefined,
        },
    };
    const labelAction = typeof props.labelAction === 'function'
        ? props.labelAction(controlContext)
        : props.labelAction;
    const controlSx = Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx];
    const nativeInputProps = getNativeInputAriaProps(controlContext);
    const control = props.variant === 'switch' ? (
        <Switch
            id={controlId}
            checked={props.value ?? false}
            onChange={(event) => {
                if (!isInteractionDisabled) {
                    props.onChange(event.target.checked);
                }
            }}
            required={required}
            disabled={isInteractionDisabled}
            slotProps={{input: nativeInputProps}}
            sx={controlSx}
        />
    ) : (
        <Checkbox
            id={controlId}
            checked={props.value ?? false}
            onChange={(event) => {
                if (!isInteractionDisabled) {
                    props.onChange(event.target.checked);
                }
            }}
            required={required}
            disabled={isInteractionDisabled}
            slotProps={{input: nativeInputProps}}
            sx={controlSx}
        />
    );

    return (
        <FormFieldFrame externalAction={props.externalAction} margin={margin} sx={props.sx}>
            {(frameSx) => (
                <Box
                    data-form-field
                    data-disabled={disabled || undefined}
                    data-readonly={readOnly || undefined}
                    data-busy={busy || undefined}
                    data-invalid={hasError || undefined}
                    sx={frameSx}
                >
                    <Box data-form-field-control sx={[formFieldLabelRowSx, {mb: 0}]}>
                        {hasContent(labelAction) && (
                            <Box
                                data-form-field-label-action
                                sx={[
                                    formFieldLabelActionSx,
                                    {gridColumn: 2, gridRow: 1},
                                ]}
                            >
                                {labelAction}
                            </Box>
                        )}

                        <FormControlLabel
                            control={control}
                            required={false}
                            disabled={isInteractionDisabled}
                            disableTypography
                            label={(
                                <Box
                                    component="span"
                                    id={labelId}
                                    title={props.invisibleLabel ? undefined : props.label}
                                    className={[
                                        hasError ? 'Mui-error' : (isInteractionDisabled ? 'Mui-disabled' : ''),
                                        props.invisibleLabel ? 'visually-hidden' : '',
                                    ].filter(Boolean).join(' ')}
                                    sx={formFieldLabelSx}
                                >
                                    <FormFieldLabelContent
                                        required={required}
                                        showOptionalIndicator={props.showOptionalIndicator === true}
                                    >
                                        {props.label}
                                    </FormFieldLabelContent>
                                </Box>
                            )}
                            sx={{
                                gridColumn: 1,
                                gridRow: 1,
                                minWidth: 0,
                                ml: -1.375,
                                mr: 0,
                                alignItems: 'center',
                                cursor: isInteractionDisabled ? 'not-allowed' : undefined,
                                '& .MuiFormControlLabel-label': {
                                    minWidth: 0,
                                },
                            }}
                        />
                    </Box>

                    {hasHelperText && (
                        <FormHelperText
                            id={helperTextId}
                            component="div"
                            error={hasError}
                            disabled={isInteractionDisabled}
                            role={hasError ? 'alert' : undefined}
                            sx={formFieldHelperTextSx}
                        >
                            {helperText}
                        </FormHelperText>
                    )}
                </Box>
            )}
        </FormFieldFrame>
    );
}
