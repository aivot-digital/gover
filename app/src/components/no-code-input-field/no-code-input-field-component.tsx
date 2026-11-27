import {useNoCodePreview} from '../../modules/nocode/hooks/use-no-code-preview';
import React, {useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Stack,
    Typography,
    type SxProps,
    type Theme,
} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {NoCodeIcon} from '../../modules/nocode/data/no-code-icon';
import {alpha} from '@mui/material/styles';
import {NoCodeEditorDialog} from '../../modules/nocode/components/no-code-editor-dialog';
import {type NoCodeInputFieldElementItem} from '../../models/elements/form/input/no-code-input-field-element';
import {type NoCodeDataType} from '../../data/no-code-data-type';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementType} from '../../data/element-type/element-type';
import {type NoCodeOperandEditorContextType} from '../../modules/nocode/components/no-code-operand-editor';
import {type NoCodeOperand} from '../../models/functions/no-code-expression';
import {FormField, type FormFieldLayoutProps} from '../form-field';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {FormFieldTokens} from '../../theming/form-field-tokens';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export interface NoCodeInputFieldComponentProps extends FormFieldLayoutProps {
    rootElement: AnyElement;
    label: string;
    hint?: string | null;
    error?: string | null;
    errorDetails?: Record<string, any> | null;
    required?: boolean | null;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    value?: NoCodeInputFieldElementItem | null;
    desiredReturnType: NoCodeDataType;
    onChange: (value: NoCodeInputFieldElementItem | null) => void;
    controlSx?: SxProps<Theme>;
}

export function NoCodeInputFieldComponent(props: NoCodeInputFieldComponentProps) {
    const {
        rootElement,
        label,
        hint,
        error,
        errorDetails,
        required,
        disabled = false,
        readOnly = false,
        busy = false,
        value,
        desiredReturnType,
        onChange,
    } = props;
    const isFormLayout = (rootElement as { type: ElementType }).type === ElementType.FormLayout;
    const contextType: NoCodeOperandEditorContextType = isFormLayout ? 'FORM' : 'PROCESS';
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `no-code-input-${generatedId}`}-dialog`;
    const isReadOnly = disabled || readOnly;
    const hasExpression = value?.noCode != null;

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [draftNoCode, setDraftNoCode] = useState<NoCodeOperand | null | undefined>(value?.noCode ?? undefined);
    const snippet = useNoCodePreview(value?.noCode ?? null, rootElement, desiredReturnType) ?? 'Kein Ausdruck definiert';
    const isLoadingOperators = snippet === 'Vorschau wird geladen …';

    const openDialog = () => {
        if (busy) {
            return;
        }

        setDraftNoCode(value?.noCode ?? undefined);
        setIsDialogOpen(true);
    };

    return (
        <>
            <FormField
                id={props.id}
                label={label}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                externalAction={props.externalAction}
                labelAction={(field) => {
                    const suppliedLabelAction = typeof props.labelAction === 'function'
                        ? props.labelAction(field)
                        : props.labelAction;
                    return (
                        <Stack direction="row"
                               spacing={1}
                               sx={{alignItems: 'center'}}>
                            {suppliedLabelAction}
                            <Button
                                size="small"
                                startIcon={isReadOnly ? <Visibility/> : <Edit/>}
                                onClick={openDialog}
                                disabled={busy}
                                aria-haspopup="dialog"
                                aria-controls={isDialogOpen ? dialogId : undefined}
                                aria-expanded={isDialogOpen}
                            >
                                {isReadOnly ? 'Ansehen' : 'Bearbeiten'}
                            </Button>
                        </Stack>
                    );
                }}
                hint={hint}
                error={error}
                assistiveText={required ? 'Erforderlicher Ausdruck.' : undefined}
                required={Boolean(required)}
                disabled={disabled}
                readOnly={readOnly}
                busy={busy}
                margin={props.margin}
                showOptionalIndicator={props.showOptionalIndicator}
                sx={props.sx}
            >
                {(field) => (
                    <Box
                        id={field.controlId}
                        role="group"
                        {...field.ariaProps}
                        sx={[
                            (theme) => ({
                                border: field.invalid || hasExpression ? '1px solid' : '1px dashed',
                                borderColor: field.invalid
                                    ? theme.palette.error.main
                                    : hasExpression
                                        ? theme.palette.divider
                                        : alpha(theme.palette.text.primary, 0.18),
                                borderRadius: 1,
                                px: 1.5,
                                py: hasExpression ? 0.75 : 0,
                                height: hasExpression ? undefined : FormFieldTokens.controlMinHeight,
                                minHeight: FormFieldTokens.controlMinHeight,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                textAlign: 'left',
                                backgroundColor: field.disabled || field.busy
                                    ? getDisabledFieldBackground
                                    : 'transparent',
                            }),
                            ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                        ]}
                    >
                        {!hasExpression && (
                            <NoCodeIcon
                                aria-hidden="true"
                                sx={{
                                    flexShrink: 0,
                                    fontSize: 20,
                                    color: 'text.secondary',
                                }}
                            />
                        )}

                        {isLoadingOperators && value?.noCode != null && (
                            <CircularProgress size={14}
                                              aria-hidden="true"/>
                        )}

                        <Typography
                            variant="body2"
                            title={snippet}
                            sx={{
                                color: hasExpression ? 'text.primary' : 'text.secondary',
                                display: '-webkit-box',
                                WebkitLineClamp: 2,
                                WebkitBoxOrient: 'vertical',
                                overflow: 'hidden',
                                lineHeight: 1.4,
                            }}
                        >
                            {snippet}
                        </Typography>
                    </Box>
                )}
            </FormField>

            {isDialogOpen && <NoCodeEditorDialog
                id={dialogId}
                title={label}
                rootElement={rootElement}
                returnType={desiredReturnType}
                value={draftNoCode}
                required={Boolean(required)}
                readOnly={isReadOnly}
                busy={busy}
                contextType={contextType}
                onClose={() => setIsDialogOpen(false)}
                onApply={(noCode) => {
                    if (isReadOnly || busy) return;
                    onChange(noCode == null ? null : {noCode});
                    setIsDialogOpen(false);
                }}
            />}
        </>
    );
}
