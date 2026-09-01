import {type ReactNode} from 'react';
import {
    Box,
    ButtonBase,
    CircularProgress,
    IconButton,
    Tooltip,
    Typography,
    type SxProps,
    type Theme,
} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-n25-outlined/ChevronRight';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import {FormField, type FormFieldLayoutProps} from './form-field';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export interface DialogSelectionFieldProps extends FormFieldLayoutProps {
    label: string;
    hint?: ReactNode;
    error?: ReactNode;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    open: boolean;
    dialogId: string;
    hasValue: boolean;
    primaryText: string;
    secondaryText?: string;
    leadingVisual: ReactNode;
    onOpen: () => void;
    onClear: () => void;
    minHeight?: number;
    controlSx?: SxProps<Theme>;
}

export function DialogSelectionField(props: DialogSelectionFieldProps) {
    const isInteractionDisabled = Boolean(props.disabled || props.readOnly || props.busy);

    return (
        <FormField
            id={props.id}
            label={props.label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={props.hint}
            error={props.error}
            assistiveText={props.required ? 'Erforderliche Auswahl.' : undefined}
            required={props.required}
            disabled={props.disabled}
            readOnly={props.readOnly}
            busy={props.busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(field) => {
                const statusId = `${field.controlId}-selection-status`;
                const labelledBy = [field.labelId, statusId].filter(Boolean).join(' ') || undefined;

                return (
                    <Box
                        sx={[
                            {
                                display: 'grid',
                                gridTemplateColumns: 'minmax(0, 1fr) auto auto',
                                alignItems: 'stretch',
                                minWidth: 0,
                                minHeight: props.minHeight ?? (
                                    props.secondaryText == null
                                        ? FormFieldTokens.controlMinHeight
                                        : FormFieldTokens.controlWithSecondaryTextMinHeight
                                ),
                                overflow: 'hidden',
                                border: '1px solid',
                                borderColor: field.invalid ? 'error.main' : 'divider',
                                borderRadius: 1,
                                backgroundColor: isInteractionDisabled
                                    ? getDisabledFieldBackground
                                    : 'transparent',
                                transition: (theme) => theme.transitions.create([
                                    'border-color',
                                    'box-shadow',
                                    'background-color',
                                ], {
                                    duration: theme.transitions.duration.shorter,
                                }),
                                '&:hover': isInteractionDisabled ? undefined : {
                                    borderColor: 'text.primary',
                                },
                                '&:focus-within': {
                                    borderColor: field.invalid ? 'error.main' : 'primary.main',
                                    boxShadow: (theme) => `0 0 0 1px ${
                                        field.invalid ? theme.palette.error.main : theme.palette.primary.main
                                    }`,
                                },
                            },
                            ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                        ]}
                    >
                        <ButtonBase
                            id={field.controlId}
                            disabled={isInteractionDisabled}
                            onClick={props.onOpen}
                            aria-labelledby={labelledBy}
                            aria-describedby={field.ariaProps['aria-describedby']}
                            aria-haspopup="dialog"
                            aria-controls={props.open ? props.dialogId : undefined}
                            aria-expanded={props.open}
                            aria-busy={field.busy || undefined}
                            aria-invalid={field.invalid || undefined}
                            aria-readonly={field.readOnly || undefined}
                            aria-required={field.required || undefined}
                            sx={{
                                minWidth: 0,
                                justifyContent: 'flex-start',
                                gap: 1.25,
                                px: 1.5,
                                py: props.secondaryText == null ? 0.5 : 0.25,
                                textAlign: 'left',
                                color: isInteractionDisabled ? 'text.disabled' : 'text.primary',
                            }}
                        >
                            <Box
                                component="span"
                                aria-hidden="true"
                                sx={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    flexShrink: 0,
                                }}
                            >
                                {props.leadingVisual}
                            </Box>

                            <Box
                                component="span"
                                sx={{
                                    minWidth: 0,
                                    flex: 1,
                                    display: 'flex',
                                    flexDirection: 'column',
                                    justifyContent: 'center',
                                    gap: props.secondaryText == null ? 0 : 0.25,
                                }}
                            >
                                <Typography
                                    component="span"
                                    id={statusId}
                                    variant="body2"
                                    title={props.primaryText}
                                    sx={{
                                        display: 'block',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                        color: props.hasValue && !isInteractionDisabled
                                            ? 'text.primary'
                                            : 'text.secondary',
                                        fontSize: '1rem',
                                        lineHeight: 1.25,
                                    }}
                                >
                                    {props.primaryText}
                                </Typography>

                                {props.secondaryText != null && (
                                    <Typography
                                        component="span"
                                        variant="caption"
                                        title={props.secondaryText}
                                        sx={{
                                            display: 'block',
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            whiteSpace: 'nowrap',
                                            color: isInteractionDisabled ? 'text.disabled' : 'text.secondary',
                                            fontSize: '0.75rem',
                                            lineHeight: 1.2,
                                        }}
                                    >
                                        {props.secondaryText}
                                    </Typography>
                                )}
                            </Box>
                        </ButtonBase>

                        {props.busy && (
                            <Box
                                aria-hidden="true"
                                sx={{display: 'flex', alignItems: 'center', px: 0.5, color: 'text.disabled'}}
                            >
                                <CircularProgress size={18} color="inherit" />
                            </Box>
                        )}

                        <Box sx={{display: 'flex', alignItems: 'center', gap: 0.25, pr: 0.75}}>
                            <Tooltip
                                title={!isInteractionDisabled && props.hasValue ? 'Auswahl entfernen' : ''}
                                arrow
                            >
                                <span>
                                    <IconButton
                                        size="small"
                                        disabled={isInteractionDisabled || !props.hasValue}
                                        aria-label={`${props.label}: Auswahl entfernen`}
                                        onClick={props.onClear}
                                    >
                                        <Close fontSize="small" />
                                    </IconButton>
                                </span>
                            </Tooltip>

                            <ChevronRight
                                aria-hidden="true"
                                fontSize="small"
                                sx={{color: isInteractionDisabled ? 'text.disabled' : 'action.active'}}
                            />
                        </Box>
                    </Box>
                );
            }}
        </FormField>
    );
}
