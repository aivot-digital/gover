import {type AriaAttributes, type ReactNode} from 'react';
import {Box, FormHelperText, FormLabel, type SxProps, type Theme} from '@mui/material';
import {
    formFieldHelperTextSx,
    formFieldLabelActionSx,
    formFieldLabelRowSx,
    formFieldLabelSx,
    formFieldRootSx,
    getFormFieldMarginSx,
    type FormFieldMargin,
} from '../../theming/form-field-tokens';
import {FormFieldLabelContent} from './form-field-label-content';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export type {FormFieldMargin} from '../../theming/form-field-tokens';

export type FormFieldAriaProps = Pick<AriaAttributes,
    'aria-label' |
    'aria-labelledby' |
    'aria-describedby' |
    'aria-disabled' |
    'aria-readonly' |
    'aria-busy' |
    'aria-required' |
    'aria-invalid'>;

export interface FormFieldControlContext {
    controlId: string;
    labelId?: string;
    helperTextId?: string;
    assistiveTextId?: string;
    disabled: boolean;
    readOnly: boolean;
    busy: boolean;
    required: boolean;
    invalid: boolean;
    ariaProps: FormFieldAriaProps;
}

export type FormFieldLabelAction = ReactNode | ((context: FormFieldControlContext) => ReactNode);

/**
 * Shared layout contract for field adapters rendered through FormField.
 * `sx` styles the field wrapper; an adapter-specific `controlSx` prop styles its primary control.
 */
export interface FormFieldLayoutProps {
    id?: string;
    ariaLabel?: string;
    ariaDescribedBy?: string;
    labelAction?: FormFieldLabelAction;
    margin?: FormFieldMargin;
    sx?: SxProps<Theme>;
    showOptionalIndicator?: boolean;
}

export interface FormFieldProps {
    id?: string;
    label: ReactNode;
    ariaLabel?: string;
    ariaDescribedBy?: string;
    children: ReactNode | ((context: FormFieldControlContext) => ReactNode);
    labelAction?: FormFieldLabelAction;
    hint?: ReactNode;
    error?: ReactNode;
    hideHelperText?: boolean;
    assistiveText?: ReactNode;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    margin?: FormFieldMargin;
    showOptionalIndicator?: boolean;
    sx?: SxProps<Theme>;
}

export function mergeAriaIds(...values: Array<string | undefined>): string | undefined {
    const ids = values
        .flatMap((value) => value?.split(/\s+/) ?? [])
        .filter((value, index, allValues) => value.length > 0 && allValues.indexOf(value) === index);

    return ids.length > 0 ? ids.join(' ') : undefined;
}

function mergeControlAriaProps(
    context: FormFieldControlContext,
    existingProps: object,
    includeFieldLabel: boolean,
): FormFieldAriaProps {
    const fieldAriaProps = context.ariaProps;
    const existingAriaProps = existingProps as FormFieldAriaProps;
    const labelledBy = mergeAriaIds(
        includeFieldLabel ? fieldAriaProps['aria-labelledby'] : undefined,
        existingAriaProps['aria-labelledby'],
    );

    return {
        'aria-label': fieldAriaProps['aria-label'] ?? existingAriaProps['aria-label'],
        ...(labelledBy != null ? {'aria-labelledby': labelledBy} : undefined),
        'aria-describedby': mergeAriaIds(
            fieldAriaProps['aria-describedby'],
            existingAriaProps['aria-describedby'],
        ),
        'aria-disabled': fieldAriaProps['aria-disabled'] ?? existingAriaProps['aria-disabled'],
        'aria-readonly': fieldAriaProps['aria-readonly'] ?? existingAriaProps['aria-readonly'],
        'aria-busy': fieldAriaProps['aria-busy'] ?? existingAriaProps['aria-busy'],
        'aria-required': fieldAriaProps['aria-required'] ?? existingAriaProps['aria-required'],
        'aria-invalid': fieldAriaProps['aria-invalid'] ?? existingAriaProps['aria-invalid'],
    };
}

export function getNativeInputAriaProps(
    context: FormFieldControlContext,
    existingProps: object = {},
): FormFieldAriaProps {
    // Native controls receive their accessible name through the external label's htmlFor link.
    // Keeping the field label in aria-labelledby as well makes some screen readers announce it twice.
    return mergeControlAriaProps(context, existingProps, false);
}

export function getCompositeControlAriaProps(
    context: FormFieldControlContext,
    existingProps: object = {},
): FormFieldAriaProps {
    // Composite controls cannot rely on htmlFor alone, so their visible label remains part of the name.
    return mergeControlAriaProps(context, existingProps, true);
}

function hasContent(content: ReactNode): boolean {
    return content !== null && content !== undefined && content !== false && content !== '';
}

/**
 * Owns the visual label, helper/error text, spacing, and ARIA state for one interactive control.
 * Adapters must apply the supplied context to their native input or composite control; label actions
 * intentionally remain outside the label so they do not become part of the control's accessible name.
 */
export function FormField(props: FormFieldProps) {
    const generatedId = useNormalizedReactId();
    const controlId = props.id ?? `field-${generatedId}`;
    const labelId = `${controlId}-label`;
    const helperTextId = `${controlId}-helper-text`;
    const assistiveTextId = `${controlId}-assistive-text`;
    const hasLabel = hasContent(props.label);
    const hasError = hasContent(props.error);
    const helperText = props.hideHelperText ? undefined : (hasError ? props.error : props.hint);
    const hasHelperText = hasContent(helperText);
    const hasAssistiveText = hasContent(props.assistiveText);
    const describedBy = mergeAriaIds(
        hasHelperText ? helperTextId : undefined,
        hasAssistiveText ? assistiveTextId : undefined,
        props.ariaDescribedBy,
    );
    const margin = props.margin ?? 'normal';
    const disabled = Boolean(props.disabled);
    const readOnly = Boolean(props.readOnly);
    const busy = Boolean(props.busy);
    const required = Boolean(props.required);

    const controlContext: FormFieldControlContext = {
        controlId,
        labelId: hasLabel ? labelId : undefined,
        helperTextId: hasHelperText ? helperTextId : undefined,
        assistiveTextId: hasAssistiveText ? assistiveTextId : undefined,
        disabled,
        readOnly,
        busy,
        required,
        invalid: hasError,
        ariaProps: {
            'aria-label': !hasLabel ? props.ariaLabel : undefined,
            'aria-labelledby': hasLabel ? labelId : undefined,
            'aria-describedby': describedBy,
            'aria-disabled': disabled || busy ? true : undefined,
            'aria-readonly': readOnly || busy ? true : undefined,
            'aria-busy': busy ? true : undefined,
            'aria-required': required ? true : undefined,
            'aria-invalid': hasError ? true : undefined,
        },
    };
    const labelAction = typeof props.labelAction === 'function'
        ? props.labelAction(controlContext)
        : props.labelAction;
    const hasLabelAction = hasContent(labelAction);

    return (
        <Box
            data-form-field
            data-disabled={disabled || undefined}
            data-readonly={readOnly || undefined}
            data-busy={busy || undefined}
            data-invalid={hasError || undefined}
            sx={[
                formFieldRootSx,
                getFormFieldMarginSx(margin),
                ...(Array.isArray(props.sx) ? props.sx : [props.sx]),
            ]}
        >
            {hasLabel && (
                <Box sx={formFieldLabelRowSx}>
                    <FormLabel
                        id={labelId}
                        htmlFor={controlId}
                        title={typeof props.label === 'string' ? props.label : undefined}
                        disabled={disabled || busy}
                        error={hasError}
                        sx={formFieldLabelSx}
                    >
                        <FormFieldLabelContent
                            required={required}
                            showOptionalIndicator={props.showOptionalIndicator}
                        >
                            {props.label}
                        </FormFieldLabelContent>
                    </FormLabel>

                    {hasLabelAction && (
                        <Box data-form-field-label-action sx={formFieldLabelActionSx}>
                            {labelAction}
                        </Box>
                    )}
                </Box>
            )}

            {typeof props.children === 'function'
                ? props.children(controlContext)
                : props.children}

            {hasAssistiveText && (
                <Box
                    component="span"
                    id={assistiveTextId}
                    sx={{
                        position: 'absolute',
                        width: 1,
                        height: 1,
                        p: 0,
                        m: -1,
                        overflow: 'hidden',
                        clip: 'rect(0 0 0 0)',
                        whiteSpace: 'nowrap',
                        border: 0,
                    }}
                >
                    {props.assistiveText}
                </Box>
            )}

            {hasHelperText && (
                <FormHelperText
                    id={helperTextId}
                    component="div"
                    error={hasError}
                    disabled={disabled || busy}
                    role={hasError ? 'alert' : undefined}
                    sx={formFieldHelperTextSx}
                >
                    {helperText}
                </FormHelperText>
            )}
        </Box>
    );
}
