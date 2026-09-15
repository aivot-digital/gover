import {type ReactNode} from 'react';
import {Box, FormHelperText, FormLabel, type SxProps, type Theme} from '@mui/material';
import {
    formFieldHelperTextSx,
    formFieldLabelActionSx,
    formFieldLabelRowSx,
    formFieldLabelSx,
} from '../../theming/form-field-tokens';
import {type FormFieldMargin, mergeAriaIds} from './form-field';
import {FormFieldLabelContent} from './form-field-label-content';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';
import {FormFieldFrame, hasExternalAction} from './form-field-frame';

export interface FormFieldGroupContext {
    groupId: string;
    labelId: string;
    helperTextId?: string;
    describedBy?: string;
    disabled: boolean;
    readOnly: boolean;
    busy: boolean;
    required: boolean;
    invalid: boolean;
}

export type FormFieldGroupLabelAction = ReactNode | ((context: FormFieldGroupContext) => ReactNode);

export interface FormFieldGroupLayoutProps {
    /** Independent action outside the disabled fieldset, stacked after its helper in narrow containers. */
    externalAction?: ReactNode;
    id?: string;
    ariaDescribedBy?: string;
    labelAction?: FormFieldGroupLabelAction;
    margin?: FormFieldMargin;
    sx?: SxProps<Theme>;
    showOptionalIndicator?: boolean;
}

export interface FormFieldGroupProps {
    externalAction?: ReactNode;
    id?: string;
    label: ReactNode;
    ariaDescribedBy?: string;
    children: ReactNode | ((context: FormFieldGroupContext) => ReactNode);
    labelAction?: FormFieldGroupLabelAction;
    hint?: ReactNode;
    error?: ReactNode;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    margin?: FormFieldMargin;
    showOptionalIndicator?: boolean;
    sx?: SxProps<Theme>;
}

function hasContent(content: ReactNode): boolean {
    return content !== null && content !== undefined && content !== false && content !== '';
}

/**
 * Fieldset-based counterpart to FormField for controls made up of multiple interactive descendants.
 * Nested controls should reference `describedBy` when the group's helper or error text also applies to them.
 */
export function FormFieldGroup(props: FormFieldGroupProps) {
    const generatedId = useNormalizedReactId();
    const groupId = props.id ?? `field-group-${generatedId}`;
    const labelId = `${groupId}-label`;
    const helperTextId = `${groupId}-helper-text`;
    const hasError = hasContent(props.error);
    const helperText = hasError ? props.error : props.hint;
    const hasHelperText = hasContent(helperText);
    const describedBy = mergeAriaIds(
        hasHelperText ? helperTextId : undefined,
        props.ariaDescribedBy,
    );
    const margin = props.margin ?? 'normal';
    const disabled = Boolean(props.disabled);
    const readOnly = Boolean(props.readOnly);
    const busy = Boolean(props.busy);
    const required = Boolean(props.required);

    const groupContext: FormFieldGroupContext = {
        groupId,
        labelId,
        helperTextId: hasHelperText ? helperTextId : undefined,
        describedBy,
        disabled,
        readOnly,
        busy,
        required,
        invalid: hasError,
    };
    const labelAction = typeof props.labelAction === 'function'
        ? props.labelAction(groupContext)
        : props.labelAction;
    const control = typeof props.children === 'function' ? props.children(groupContext) : props.children;

    return (
        <FormFieldFrame externalAction={props.externalAction} margin={margin} sx={props.sx}>
            {(frameSx) => (
                <Box
                    component="fieldset"
                    id={groupId}
                    disabled={disabled || undefined}
                    data-form-field-group
                    data-readonly={readOnly || undefined}
                    data-busy={busy || undefined}
                    data-invalid={hasError || undefined}
                    aria-labelledby={labelId}
                    aria-describedby={describedBy}
                    aria-disabled={disabled || busy ? true : undefined}
                    aria-readonly={readOnly || busy ? true : undefined}
                    aria-busy={busy ? true : undefined}
                    aria-required={required ? true : undefined}
                    aria-invalid={hasError ? true : undefined}
                    sx={[
                        ...(Array.isArray(frameSx) ? frameSx : [frameSx]),
                        // Native fieldsets must shrink inside Grid/Flex just like a single control.
                        {border: 0, p: 0, minInlineSize: 0},
                    ]}
                >
                    <Box
                        component="legend"
                        data-form-field-label
                        sx={{float: 'none', width: '100%', maxWidth: '100%', p: 0}}
                    >
                        <Box component="span" sx={formFieldLabelRowSx}>
                            <FormLabel
                                component="span"
                                id={labelId}
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

                            {hasContent(labelAction) && (
                                <Box data-form-field-label-action sx={formFieldLabelActionSx}>
                                    {labelAction}
                                </Box>
                            )}
                        </Box>
                    </Box>

                    {hasExternalAction(props.externalAction)
                        ? <Box data-form-field-control>{control}</Box>
                        : control}

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
            )}
        </FormFieldFrame>
    );
}
