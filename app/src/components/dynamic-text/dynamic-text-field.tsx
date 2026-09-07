import {forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState} from 'react';
import {Box, IconButton, Tooltip, Typography} from '@mui/material';
import {alpha, useTheme} from '@mui/material/styles';
import {LexicalComposer, type InitialConfigType} from '@lexical/react/LexicalComposer';
import {ContentEditable} from '@lexical/react/LexicalContentEditable';
import {EditorRefPlugin} from '@lexical/react/LexicalEditorRefPlugin';
import {LexicalErrorBoundary} from '@lexical/react/LexicalErrorBoundary';
import {HistoryPlugin} from '@lexical/react/LexicalHistoryPlugin';
import {OnChangePlugin} from '@lexical/react/LexicalOnChangePlugin';
import {PlainTextPlugin} from '@lexical/react/LexicalPlainTextPlugin';
import {useLexicalComposerContext} from '@lexical/react/LexicalComposerContext';
import {
    $createParagraphNode,
    $createTextNode,
    $getRoot,
    $getSelection,
    $insertNodes,
    $isRangeSelection,
    COMMAND_PRIORITY_HIGH,
    type EditorState,
    KEY_ENTER_COMMAND,
    type LexicalEditor,
} from 'lexical';
import type {EndAction} from '../text-field/text-field-component-props';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {
    $createDynamicTextTokenNode,
    DynamicTextLexicalPlugin,
    DynamicTextTokenNode,
    getDynamicTextTokenStyles,
    useDynamicTextSyntaxHighlights,
} from './dynamic-text-lexical';
import {
    type DynamicTextVariableMetadata,
    type DynamicTextInputMethods,
    useDynamicTextTokenTitles,
} from './dynamic-text-metadata';
import {FormField, type FormFieldLayoutProps} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export type DynamicTextFieldMethods = DynamicTextInputMethods;

const OUTLINED_CONTROL_BORDER_WIDTH = 1;
const INPUT_LINE_HEIGHT_PX = 23;
const SINGLE_LINE_VERTICAL_PADDING_PX = 9.5;
const MULTILINE_VERTICAL_PADDING_PX = 12.5;
const DEFAULT_MULTILINE_MIN_ROWS = 4;
const DEFAULT_MULTILINE_MAX_ROWS = 12;

function getMultilineEditorHeight(rows: number): number {
    return rows * INPUT_LINE_HEIGHT_PX + 2 * MULTILINE_VERTICAL_PADDING_PX;
}

export interface DynamicTextFieldProps {
    id: string;
    ariaLabel?: string;
    ariaDescribedBy?: string;
    ariaLabelledBy?: string;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    required?: boolean;
    invalid?: boolean;
    endAction?: EndAction;
    multiline?: boolean;
    onChange: (value: string | null) => void;
    onBlur?: (value: string | null) => void;
    placeholder?: string;
    rows?: number;
    variableMetadata?: readonly DynamicTextVariableMetadata[];
    value: string | null;
}

interface SavedEditorSelection {
    editor: LexicalEditor;
    editorState: EditorState;
}

function setEditorValue(value: string) {
    const root = $getRoot();
    root.clear();

    const lines = value.split('\n');
    for (const line of lines) {
        const paragraph = $createParagraphNode();
        if (line.length > 0) {
            paragraph.append($createTextNode(line));
        }
        root.append(paragraph);
    }
}

function getEditorValue(): string {
    return $getRoot()
        .getChildren()
        .map((child) => child.getTextContent())
        .join('\n');
}

function ExternalValuePlugin(props: {value: string}) {
    const [editor] = useLexicalComposerContext();

    useEffect(() => {
        let currentValue = '';
        editor.getEditorState().read(() => {
            currentValue = getEditorValue();
        });
        if (currentValue === props.value) {
            return;
        }

        editor.update(() => setEditorValue(props.value));
    }, [editor, props.value]);

    return null;
}

function EditableStatePlugin(props: {editable: boolean}) {
    const [editor] = useLexicalComposerContext();

    useEffect(() => {
        editor.setEditable(props.editable);
    }, [editor, props.editable]);

    return null;
}

function SingleLinePlugin() {
    const [editor] = useLexicalComposerContext();

    useEffect(() => editor.registerCommand(
        KEY_ENTER_COMMAND,
        (event) => {
            event?.preventDefault();
            return true;
        },
        COMMAND_PRIORITY_HIGH,
    ), [editor]);

    return null;
}

export const DynamicTextField = forwardRef<
DynamicTextFieldMethods,
DynamicTextFieldProps
>((props, ref) => {
    const theme = useTheme();
    const [container, setContainer] = useState<HTMLElement | null>(null);
    const editorRef = useRef<LexicalEditor | null>(null);
    const savedSelectionRef = useRef<SavedEditorSelection | null>(null);
    const isReadOnly = Boolean(props.disabled || props.readOnly || props.busy);
    // Browsers currently underline token text despite spellcheck=false on the token itself. Disabling it on the
    // editor is intentional until selective spellchecking works consistently across supported browsers.
    const enableSpellCheck = false;
    const [initialConfig] = useState<InitialConfigType>(() => ({
        namespace: 'ProsunaDynamicTextField',
        nodes: [DynamicTextTokenNode],
        editable: !isReadOnly,
        editorState: () => setEditorValue(props.value ?? ''),
        onError: (error) => {
            throw error;
        },
    }));

    useImperativeHandle(ref, () => ({
        focus: () => editorRef.current?.focus(),
        insertVariableReference: (reference) => {
            const editor = editorRef.current;
            if (editor == null) {
                return;
            }

            const savedSelection = savedSelectionRef.current;
            savedSelectionRef.current = null;
            if (savedSelection?.editor === editor) {
                editor.setEditorState(savedSelection.editorState);
            }

            editor.focus(() => {
                editor.update(() => {
                    $insertNodes([$createDynamicTextTokenNode(`{{ ${reference} }}`)]);
                });
            });
        },
    }), []);

    const captureSelection = () => {
        const editor = editorRef.current;
        if (editor == null) {
            return;
        }

        const editorState = editor.getEditorState();
        editorState.read(() => {
            const selection = $getSelection();
            if ($isRangeSelection(selection)) {
                savedSelectionRef.current = {
                    editor,
                    editorState: editorState.clone(selection.clone()),
                };
            }
        });
    };

    // Keep the Lexical surface on the same sizing baseline as a small MUI outlined input.
    const editorMinHeight = props.multiline
        ? getMultilineEditorHeight(Math.max(props.rows ?? DEFAULT_MULTILINE_MIN_ROWS, 2))
        : FormFieldTokens.controlMinHeight - 2 * OUTLINED_CONTROL_BORDER_WIDTH;
    const editorMaxHeight = props.multiline && props.rows == null
        ? getMultilineEditorHeight(DEFAULT_MULTILINE_MAX_ROWS)
        : editorMinHeight;
    const hasFixedHeight = !props.multiline || props.rows != null;
    const controlMinHeight = editorMinHeight + 2 * OUTLINED_CONTROL_BORDER_WIDTH;
    const verticalPadding = props.multiline
        ? MULTILINE_VERTICAL_PADDING_PX
        : SINGLE_LINE_VERTICAL_PADDING_PX;
    const placeholder = props.placeholder == null ? null : (
        <Typography
            component="span"
            sx={{
                position: 'absolute',
                top: `${verticalPadding}px`,
                left: 14,
                color: 'text.disabled',
                font: 'inherit',
                lineHeight: `${INPUT_LINE_HEIGHT_PX}px`,
                pointerEvents: 'none',
            }}
        >
            {props.placeholder}
        </Typography>
    );
    const outlinedBorderColor = theme.palette.mode === 'light'
        ? 'rgba(0, 0, 0, 0.23)'
        : 'rgba(255, 255, 255, 0.23)';
    const hasDisabledAppearance = Boolean(props.disabled || props.busy);

    useDynamicTextTokenTitles(container, props.variableMetadata);
    useDynamicTextSyntaxHighlights(container);

    return (
        <Box
            ref={setContainer}
            data-dynamic-text-multiline={props.multiline || undefined}
            data-disabled={props.disabled || undefined}
            sx={{
                position: 'relative',
                containerType: 'inline-size',
                display: 'grid',
                gridTemplateColumns: props.endAction == null ? 'minmax(0, 1fr)' : 'minmax(0, 1fr) 44px',
                boxSizing: 'border-box',
                minHeight: controlMinHeight,
                height: hasFixedHeight ? controlMinHeight : undefined,
                border: '1px solid',
                borderRadius: 1,
                bgcolor: hasDisabledAppearance ? getDisabledFieldBackground(theme) : 'transparent',
                cursor: hasDisabledAppearance ? 'not-allowed' : undefined,
                borderColor: props.invalid
                    ? 'error.main'
                    : props.disabled
                        ? 'action.disabled'
                        : outlinedBorderColor,
                transition: theme.transitions.create(['border-color', 'box-shadow']),
                '&:hover': isReadOnly ? undefined : {
                    borderColor: props.invalid ? 'error.main' : 'text.primary',
                },
                '&:focus-within': isReadOnly ? undefined : {
                    borderColor: props.invalid ? 'error.main' : 'primary.main',
                    boxShadow: `0 0 0 1px ${props.invalid ? theme.palette.error.main : theme.palette.primary.main}`,
                },
                '& [contenteditable="true"] p': {
                    m: 0,
                },
                '&[data-disabled="true"] [contenteditable="false"]': {
                    color: 'text.secondary',
                    cursor: 'default',
                },
                ...getDynamicTextTokenStyles(theme),
            }}
        >
            <Box sx={{position: 'relative', minWidth: 0}}>
                <LexicalComposer initialConfig={initialConfig}>
                    <PlainTextPlugin
                        contentEditable={(
                            <ContentEditable
                                id={props.id}
                                aria-label={props.ariaLabel}
                                aria-labelledby={props.ariaLabelledBy}
                                aria-describedby={props.ariaDescribedBy}
                                aria-disabled={props.disabled || props.busy || undefined}
                                aria-readonly={isReadOnly || undefined}
                                aria-busy={props.busy || undefined}
                                aria-required={props.required || undefined}
                                aria-invalid={props.invalid || undefined}
                                aria-multiline={props.multiline || undefined}
                                spellCheck={enableSpellCheck}
                                onBlur={() => {
                                    editorRef.current?.getEditorState().read(() => {
                                        const value = getEditorValue();
                                        props.onBlur?.(value.length === 0 ? null : value);
                                    });
                                }}
                                style={{
                                    boxSizing: 'border-box',
                                    caretColor: theme.palette.text.primary,
                                    font: 'inherit',
                                    letterSpacing: 'inherit',
                                    lineHeight: `${INPUT_LINE_HEIGHT_PX}px`,
                                    minHeight: editorMinHeight,
                                    maxHeight: editorMaxHeight,
                                    height: hasFixedHeight ? editorMinHeight : undefined,
                                    padding: `${verticalPadding}px 14px`,
                                    outline: 'none',
                                    overflowX: props.multiline ? 'hidden' : 'auto',
                                    overflowY: props.multiline ? 'auto' : 'hidden',
                                    overflowWrap: props.multiline ? 'anywhere' : 'normal',
                                    whiteSpace: props.multiline ? 'pre-wrap' : 'pre',
                                }}
                            />
                        )}
                        placeholder={placeholder}
                        ErrorBoundary={LexicalErrorBoundary}
                    />
                    <HistoryPlugin/>
                    <OnChangePlugin
                        ignoreSelectionChange
                        onChange={(editorState) => {
                            editorState.read(() => {
                                const value = getEditorValue();
                                props.onChange(value.length === 0 ? null : value);
                            });
                        }}
                    />
                    <EditorRefPlugin editorRef={editorRef}/>
                    <ExternalValuePlugin value={props.value ?? ''}/>
                    <EditableStatePlugin editable={!isReadOnly}/>
                    <DynamicTextLexicalPlugin/>
                    {!props.multiline && <SingleLinePlugin/>}
                </LexicalComposer>
            </Box>

            {props.endAction != null && (
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: props.multiline ? 'flex-start' : 'center',
                        justifyContent: 'center',
                        pt: props.multiline ? 0.5 : 0,
                    }}
                >
                    <Tooltip
                        title={props.endAction.tooltip ?? props.endAction.ariaLabel ?? 'Aktion ausführen'}
                        arrow
                    >
                        <IconButton
                            aria-label={props.endAction.ariaLabel ?? props.endAction.tooltip ?? 'Aktion ausführen'}
                            disabled={isReadOnly}
                            size="small"
                            onMouseDown={(event) => {
                                event.preventDefault();
                                captureSelection();
                            }}
                            onClick={props.endAction.onClick}
                            sx={{color: alpha(theme.palette.text.primary, 0.65)}}
                        >
                            {props.endAction.icon}
                        </IconButton>
                    </Tooltip>
                </Box>
            )}
        </Box>
    );
});

DynamicTextField.displayName = 'DynamicTextField';

export interface DynamicTextInputFieldProps extends FormFieldLayoutProps {
    label: string;
    hint?: string;
    error?: string;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    endAction?: EndAction;
    multiline?: boolean;
    onChange: (value: string | null) => void;
    onBlur?: (value: string | null) => void;
    debounce?: number;
    placeholder?: string;
    rows?: number;
    variableMetadata?: readonly DynamicTextVariableMetadata[];
    value: string | null;
}

export const DynamicTextInputField = forwardRef<DynamicTextFieldMethods, DynamicTextInputFieldProps>((props, ref) => {
    const debounceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const clearPendingChange = useCallback(() => {
        if (debounceTimeoutRef.current != null) {
            clearTimeout(debounceTimeoutRef.current);
            debounceTimeoutRef.current = null;
        }
    }, []);

    useEffect(() => clearPendingChange, [clearPendingChange]);

    // An authoritative value refresh supersedes locally queued input, matching the regular text field behavior.
    useEffect(() => {
        clearPendingChange();
    }, [clearPendingChange, props.value]);

    const handleChange = useCallback((value: string | null) => {
        if (props.debounce == null || props.debounce <= 0) {
            props.onChange(value);
            return;
        }

        clearPendingChange();
        debounceTimeoutRef.current = setTimeout(() => {
            debounceTimeoutRef.current = null;
            props.onChange(value);
        }, props.debounce);
    }, [clearPendingChange, props.debounce, props.onChange]);

    const handleBlur = useCallback((value: string | null) => {
        const cleanedValue = value?.trim() || null;
        if (debounceTimeoutRef.current != null) {
            clearPendingChange();
            props.onChange(cleanedValue);
        } else if (cleanedValue !== value) {
            props.onChange(cleanedValue);
        }
        props.onBlur?.(cleanedValue);
    }, [clearPendingChange, props.onBlur, props.onChange]);

    return <FormField
        id={props.id}
        label={props.label}
        hint={props.hint}
        error={props.error}
        required={props.required}
        disabled={props.disabled}
        readOnly={props.readOnly}
        busy={props.busy}
        ariaLabel={props.ariaLabel}
        ariaDescribedBy={props.ariaDescribedBy}
        labelAction={props.labelAction}
        margin={props.margin}
        showOptionalIndicator={props.showOptionalIndicator}
        sx={props.sx}
    >
        {(control) => (
            <DynamicTextField
                ref={ref}
                id={control.controlId}
                ariaLabel={control.ariaProps['aria-label']}
                ariaLabelledBy={control.labelId}
                ariaDescribedBy={control.ariaProps['aria-describedby']}
                disabled={control.disabled}
                readOnly={control.readOnly}
                busy={control.busy}
                required={control.required}
                invalid={control.invalid}
                endAction={props.endAction}
                multiline={props.multiline}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder={props.placeholder}
                rows={props.rows}
                variableMetadata={props.variableMetadata}
                value={props.value}
            />
        )}
    </FormField>;
});

DynamicTextInputField.displayName = 'DynamicTextInputField';
