import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    Typography,
} from '@mui/material';
import {NoCodeEditorWrapper} from '../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {
    NoCodeInputFieldElementItem
} from '../../models/elements/form/input/no-code-input-field-element';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {AnyElement} from '../../models/elements/any-element';
import {ElementType} from '../../data/element-type/element-type';
import {NoCodeOperandEditorContextType} from '../../modules/nocode/components/no-code-operand-editor';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {NoCodeOperand} from '../../models/functions/no-code-expression';
import {useApi} from '../../hooks/use-api';
import {NoCodeApiService} from '../../services/no-code-api-service';
import {NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';
import {flattenElementsWithParents} from '../../utils/flatten-elements';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {humanizeNoCode} from '../../modules/nocode/utils/humanize-no-code';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';

interface NoCodeInputFieldComponentProps {
    rootElement: AnyElement;
    label: string;
    hint?: string | null;
    error?: string | null;
    required?: boolean | null;
    disabled?: boolean;
    value?: NoCodeInputFieldElementItem | null;
    desiredReturnType: NoCodeDataType;
    onChange: (value: NoCodeInputFieldElementItem | undefined) => void;
}

export function NoCodeInputFieldComponent(props: NoCodeInputFieldComponentProps) {
    const api = useApi();
    const dispatch = useAppDispatch();

    const {
        rootElement,
        label,
        hint,
        error,
        required,
        disabled,
        value,
        desiredReturnType,
        onChange,
    } = props;

    const isProcessConfigRoot = (rootElement as { type: ElementType }).type === ElementType.ConfigLayout;
    const contextType: NoCodeOperandEditorContextType = isProcessConfigRoot
        ? 'PROCESS'
        : 'FORM';
    const displayLabel = `${label}${required ? ' *' : ''}`;

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [draftNoCode, setDraftNoCode] = useState<NoCodeOperand | null | undefined>(value?.noCode ?? undefined);
    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[]>([]);
    const [isLoadingOperators, setIsLoadingOperators] = useState(false);

    useEffect(() => {
        if (!isProcessConfigRoot) {
            return;
        }

        setIsLoadingOperators(true);
        new NoCodeApiService(api)
            .getNoCodeOperators()
            .then(setOperators)
            .catch(() => {
                dispatch(showErrorSnackbar('Operatoren für No-Code-Ausdrücke konnten nicht geladen werden.'));
            })
            .finally(() => {
                setIsLoadingOperators(false);
            });
    }, [api, dispatch, isProcessConfigRoot]);

    const allElements = useMemo(() => {
        return flattenElementsWithParents(rootElement, [], true)
            .filter((e) => isAnyInputElement(e.element));
    }, [rootElement]);

    const snippet = useMemo(() => {
        const noCode = value?.noCode;
        if (noCode == null) {
            return 'Kein Ausdruck definiert';
        }

        try {
            return humanizeNoCode(noCode, allElements, operators);
        } catch {
            return 'No-Code-Ausdruck konfiguriert';
        }
    }, [value?.noCode, allElements, operators]);

    return (
        <>
            {
                isProcessConfigRoot ? (
                    <>
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 2,
                                mb: 0.75,
                            }}
                        >
                            <Typography variant="subtitle2">
                                {displayLabel}
                            </Typography>

                            <Button
                                variant="outlined"
                                size="small"
                                startIcon={<Edit />}
                                sx={{ml: 'auto'}}
                                disabled={disabled}
                                onClick={() => {
                                    setDraftNoCode(value?.noCode ?? undefined);
                                    setIsDialogOpen(true);
                                }}
                            >
                                Bearbeiten
                            </Button>
                        </Box>

                        <Box
                            sx={{
                                border: '1px solid',
                                borderColor: error != null ? 'error.main' : 'divider',
                                borderRadius: 1,
                                px: 1.5,
                                py: 1.25,
                                minHeight: 52,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                            }}
                        >
                            {
                                isLoadingOperators &&
                                value?.noCode != null &&
                                <CircularProgress size={14} />
                            }

                            <Typography
                                variant="body2"
                                title={snippet}
                                sx={{
                                    color: value?.noCode == null ? 'text.secondary' : 'text.primary',
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

                        {
                            error == null &&
                            hint != null &&
                            <Typography
                                sx={{
                                    marginTop: 1,
                                    color: 'text.secondary',
                                }}
                                variant="caption"
                            >
                                {hint}
                            </Typography>
                        }

                        <Dialog
                            open={isDialogOpen}
                            onClose={() => setIsDialogOpen(false)}
                            fullWidth
                            maxWidth="lg"
                        >
                            <DialogTitleWithClose onClose={() => setIsDialogOpen(false)}>
                                {displayLabel}
                            </DialogTitleWithClose>

                            <DialogContent>
                                <Box  sx={{pt: 2, pb: 3}}>
                                    <NoCodeEditorWrapper
                                        parents={[rootElement]}
                                        noCode={draftNoCode}
                                        onChange={setDraftNoCode}
                                        editable={!disabled}
                                        desiredReturnType={desiredReturnType}
                                        label={displayLabel}
                                        hint={hint ?? undefined}
                                        contextType={contextType}
                                    />
                                </Box>
                            </DialogContent>

                            <DialogActions>
                                <Button
                                    variant="contained"
                                    onClick={() => {
                                        if (draftNoCode == null) {
                                            onChange(undefined);
                                        } else {
                                            onChange({
                                                noCode: draftNoCode,
                                            });
                                        }
                                        setIsDialogOpen(false);
                                    }}
                                >
                                    Übernehmen
                                </Button>
                                <Button onClick={() => setIsDialogOpen(false)}>
                                    Abbrechen
                                </Button>
                            </DialogActions>
                        </Dialog>
                    </>
                ) : (
                    <NoCodeEditorWrapper
                        parents={[rootElement]}
                        noCode={value?.noCode ?? undefined}
                        onChange={(noCode) => {
                            if (noCode == null) {
                                onChange(undefined);
                                return;
                            }

                            onChange({
                                noCode,
                            });
                        }}
                        editable={!disabled}
                        desiredReturnType={desiredReturnType}
                        label={displayLabel}
                        hint={hint ?? undefined}
                        error={error ?? undefined}
                        contextType={contextType}
                    />
                )
            }

            {
                error != null &&
                <Typography
                    sx={{
                        marginTop: 1,
                        color: 'error.main',
                    }}
                    variant="caption"
                >
                    {error}
                </Typography>
            }
        </>
    );
}
