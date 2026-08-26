import {type ReactNode, useId} from 'react';
import {Box, FormControl, FormHelperText, FormLabel, type SxProps, type Theme} from '@mui/material';

export interface FieldLayoutControlContext {
    inputId: string;
    labelId: string;
    helperTextId?: string;
    disabled: boolean;
    readOnly: boolean;
    busy: boolean;
    required: boolean;
    invalid: boolean;
    ariaProps: {
        'aria-labelledby': string;
        'aria-describedby'?: string;
        'aria-disabled'?: true;
        'aria-readonly'?: true;
        'aria-busy'?: true;
        'aria-required'?: true;
        'aria-invalid'?: true;
    };
}

interface FieldLayoutProps {
    label: string;
    children: ReactNode | ((context: FieldLayoutControlContext) => ReactNode);
    labelAction?: ReactNode;
    hint?: ReactNode;
    error?: ReactNode;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    sx?: SxProps<Theme>;
}

export function FieldLayout(props: FieldLayoutProps) {
    const generatedId = useId().replaceAll(':', '');
    const inputId = `field-${generatedId}`;
    const labelId = `${inputId}-label`;
    const helperTextId = props.error != null || props.hint != null
        ? `${inputId}-helper-text`
        : undefined;
    const controlContext: FieldLayoutControlContext = {
        inputId,
        labelId,
        helperTextId,
        disabled: Boolean(props.disabled),
        readOnly: Boolean(props.readOnly),
        busy: Boolean(props.busy),
        required: Boolean(props.required),
        invalid: props.error != null,
        ariaProps: {
            'aria-labelledby': labelId,
            'aria-describedby': helperTextId,
            'aria-disabled': props.disabled || props.busy ? true : undefined,
            'aria-readonly': props.readOnly ? true : undefined,
            'aria-busy': props.busy ? true : undefined,
            'aria-required': props.required ? true : undefined,
            'aria-invalid': props.error != null ? true : undefined,
        },
    };
    const sxArray = Array.isArray(props.sx) ? props.sx : [props.sx];

    return (
        <FormControl
            fullWidth
            margin="none"
            error={props.error != null}
            disabled={props.disabled || props.busy}
            required={props.required}
            sx={sxArray}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    minHeight: 28,
                    mb: 0.5,
                }}
            >
                <FormLabel
                    id={labelId}
                    htmlFor={inputId}
                    sx={{
                        minWidth: 0,
                        flex: 1,
                        color: 'text.primary',
                        fontSize: '0.875rem',
                        fontWeight: 500,
                        lineHeight: 1.35,
                        overflowWrap: 'anywhere',
                        '&.Mui-focused': {
                            color: 'text.primary',
                        },
                        '&.Mui-disabled': {
                            color: 'text.disabled',
                        },
                        '&.Mui-error': {
                            color: 'error.main',
                        },
                    }}
                >
                    {props.label}
                </FormLabel>

                {props.labelAction != null && (
                    <Box
                        sx={{
                            alignSelf: 'center',
                            flexShrink: 0,
                        }}
                    >
                        {props.labelAction}
                    </Box>
                )}
            </Box>

            {typeof props.children === 'function'
                ? props.children(controlContext)
                : props.children}

            {(props.error != null || props.hint != null) && (
                <FormHelperText
                    id={helperTextId}
                    component="div"
                    sx={{
                        mx: 0,
                        mt: 0.75,
                        lineHeight: 1.35,
                    }}
                >
                    {props.error ?? props.hint}
                </FormHelperText>
            )}
        </FormControl>
    );
}
