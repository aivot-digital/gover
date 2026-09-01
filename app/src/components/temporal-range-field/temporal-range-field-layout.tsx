import {type ReactNode} from 'react';
import {Box, type SxProps, type Theme, Typography} from '@mui/material';
import {
    FormFieldGroup,
    type FormFieldGroupContext,
    type FormFieldGroupLayoutProps,
} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export interface TemporalRangeFieldLayoutProps extends FormFieldGroupLayoutProps {
    label: string;
    hint?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    controlSx?: SxProps<Theme>;
    renderStart: (context: FormFieldGroupContext) => ReactNode;
    renderEnd: (context: FormFieldGroupContext) => ReactNode;
}

/**
 * Uses a container query because range fields can live in narrow grid columns even on wide viewports.
 * Both nested fields share the group description while keeping their own visible "Von"/"Bis" labels.
 */
export function TemporalRangeFieldLayout(props: TemporalRangeFieldLayoutProps) {
    return (
        <FormFieldGroup
            id={props.id}
            label={props.label}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={props.hint}
            error={props.error}
            required={props.required}
            disabled={props.disabled}
            busy={props.busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={[
                {containerType: 'inline-size'},
                ...(Array.isArray(props.sx) ? props.sx : [props.sx]),
            ]}
        >
            {(fieldContext) => (
                <Box
                    sx={[
                        {
                            display: 'grid',
                            gridTemplateColumns: 'minmax(0, 1fr) auto minmax(0, 1fr)',
                            columnGap: 1,
                            alignItems: 'start',
                            '@container (max-width: 40rem)': {
                                gridTemplateColumns: 'minmax(0, 1fr)',
                                rowGap: 1.5,
                                '& [data-temporal-range-separator]': {
                                    display: 'none',
                                },
                            },
                        },
                        ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                    ]}
                >
                    <Box sx={{minWidth: 0}}>
                        {props.renderStart(fieldContext)}
                    </Box>

                    <Box
                        data-temporal-range-separator
                        sx={(theme) => ({
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            minHeight: FormFieldTokens.controlMinHeight,
                            mt: `calc(${FormFieldTokens.labelRowMinHeight}px + ${theme.spacing(FormFieldTokens.labelToControlGap)})`,
                        })}
                    >
                        <Typography variant="body1" aria-hidden sx={{mx: 1}}>
                            –
                        </Typography>
                    </Box>

                    <Box sx={{minWidth: 0}}>
                        {props.renderEnd(fieldContext)}
                    </Box>
                </Box>
            )}
        </FormFieldGroup>
    );
}
