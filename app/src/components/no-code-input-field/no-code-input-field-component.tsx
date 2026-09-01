import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    Stack,
    Typography,
    type SxProps,
    type Theme,
} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import Function from '@aivot/mui-material-symbols-400-n25-outlined/Function';
import {alpha} from '@mui/material/styles';
import {NoCodeEditorWrapper} from '../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {type NoCodeInputFieldElementItem} from '../../models/elements/form/input/no-code-input-field-element';
import {type NoCodeDataType} from '../../data/no-code-data-type';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementType} from '../../data/element-type/element-type';
import {type NoCodeOperandEditorContextType} from '../../modules/nocode/components/no-code-operand-editor';
import {type NoCodeOperand, type NoCodeOperandError} from '../../models/functions/no-code-expression';
import {useApi} from '../../hooks/use-api';
import {NoCodeApiService} from '../../services/no-code-api-service';
import {type NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';
import {flattenElementsWithParents} from '../../utils/flatten-elements';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {humanizeNoCode} from '../../modules/nocode/utils/humanize-no-code';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
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
    disablePopoutModeWhenFormLayoutChild?: boolean;
}

export function NoCodeInputFieldComponent(props: NoCodeInputFieldComponentProps) {
    const api = useApi();
    const dispatch = useAppDispatch();
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
        disablePopoutModeWhenFormLayoutChild = false,
    } = props;
    const isFormLayout = (rootElement as {type: ElementType}).type === ElementType.FormLayout;
    const contextType: NoCodeOperandEditorContextType = isFormLayout ? 'FORM' : 'PROCESS';
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `no-code-input-${generatedId}`}-dialog`;
    const isReadOnly = disabled || readOnly;
    const hasExpression = value?.noCode != null;

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [draftNoCode, setDraftNoCode] = useState<NoCodeOperand | null | undefined>(value?.noCode ?? undefined);
    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[]>([]);
    const [isLoadingOperators, setIsLoadingOperators] = useState(false);

    useEffect(() => {
        if (!isFormLayout) {
            return;
        }

        let active = true;
        setIsLoadingOperators(true);
        new NoCodeApiService(api)
            .getNoCodeOperators()
            .then((loadedOperators) => {
                if (active) {
                    setOperators(loadedOperators);
                }
            })
            .catch(() => {
                if (active) {
                    dispatch(showErrorSnackbar('Operatoren für No-Code-Ausdrücke konnten nicht geladen werden.'));
                }
            })
            .finally(() => {
                if (active) {
                    setIsLoadingOperators(false);
                }
            });

        return () => {
            active = false;
        };
    }, [api, dispatch, isFormLayout]);

    const allElements = useMemo(() => {
        return flattenElementsWithParents(rootElement, [], true)
            .filter((entry) => isAnyInputElement(entry.element));
    }, [rootElement]);

    const snippet = useMemo(() => {
        const noCode = value?.noCode;
        if (noCode == null) {
            return 'Kein Ausdruck definiert';
        }

        try {
            return humanizeNoCode(noCode, allElements, operators);
        } catch {
            return 'Ausdruck konfiguriert';
        }
    }, [value?.noCode, allElements, operators]);

    const openDialog = () => {
        if (busy) {
            return;
        }

        setDraftNoCode(value?.noCode ?? undefined);
        setIsDialogOpen(true);
    };

    if (!isFormLayout || disablePopoutModeWhenFormLayoutChild) {
        return (
            <NoCodeEditorWrapper
                parents={[rootElement]}
                noCode={value?.noCode ?? undefined}
                onChange={(noCode) => {
                    if (noCode == null) {
                        onChange(null);
                        return;
                    }

                    onChange({noCode});
                }}
                editable={!isReadOnly && !busy}
                desiredReturnType={desiredReturnType}
                label={label}
                hint={hint ?? undefined}
                error={error ?? undefined}
                contextType={contextType}
                operandError={errorDetails as NoCodeOperandError ?? undefined}
            />
        );
    }

    return (
        <>
            <FormField
                id={props.id}
                label={label}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                labelAction={(field) => {
                    const suppliedLabelAction = typeof props.labelAction === 'function'
                        ? props.labelAction(field)
                        : props.labelAction;
                    return (
                        <Stack direction="row" spacing={1} sx={{alignItems: 'center'}}>
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
                            <Function
                                aria-hidden="true"
                                sx={{
                                    flexShrink: 0,
                                    fontSize: 20,
                                    color: 'text.secondary',
                                }}
                            />
                        )}

                        {isLoadingOperators && value?.noCode != null && (
                            <CircularProgress size={14} aria-hidden="true" />
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

            <Dialog
                open={isDialogOpen}
                onClose={() => setIsDialogOpen(false)}
                fullWidth
                maxWidth="lg"
                slotProps={{paper: {id: dialogId}}}
            >
                <DialogTitleWithClose onClose={() => setIsDialogOpen(false)}>
                    {label}
                </DialogTitleWithClose>

                <DialogContent>
                    <Box sx={{pt: 2, pb: 3}}>
                        <NoCodeEditorWrapper
                            parents={[rootElement]}
                            noCode={draftNoCode}
                            onChange={setDraftNoCode}
                            editable={!isReadOnly && !busy}
                            desiredReturnType={desiredReturnType}
                            label={label}
                            hint={hint ?? undefined}
                            contextType={contextType}
                            operandError={errorDetails as NoCodeOperandError ?? undefined}
                        />
                    </Box>
                </DialogContent>

                <DialogActions sx={{justifyContent: 'flex-start'}}>
                    {!isReadOnly && !busy && (
                        <Button
                            variant="contained"
                            onClick={() => {
                                onChange(draftNoCode == null ? null : {noCode: draftNoCode});
                                setIsDialogOpen(false);
                            }}
                        >
                            Übernehmen
                        </Button>
                    )}
                    <Button onClick={() => setIsDialogOpen(false)}>
                        {isReadOnly ? 'Schließen' : 'Abbrechen'}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
