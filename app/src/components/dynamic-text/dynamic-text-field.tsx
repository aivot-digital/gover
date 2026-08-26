import {forwardRef, useEffect, useImperativeHandle, useRef, useState} from 'react';
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
    useDynamicTextTokenTitles,
} from './dynamic-text-metadata';

export interface DynamicTextFieldMethods {
    focus: () => void;
    insertVariableReference: (reference: string) => void;
}

interface DynamicTextFieldProps {
    id: string;
    ariaDescribedBy?: string;
    ariaLabelledBy: string;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    required?: boolean;
    invalid?: boolean;
    endAction?: EndAction;
    multiline?: boolean;
    onChange: (value: string | null) => void;
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

    const placeholder = props.placeholder == null ? null : (
        <Typography
            component="span"
            sx={{
                position: 'absolute',
                top: 12,
                left: 14,
                color: 'text.disabled',
                pointerEvents: 'none',
            }}
        >
            {props.placeholder}
        </Typography>
    );
    const minHeight = props.multiline ? Math.max(props.rows ?? 3, 2) * 24 + 24 : 48;
    const outlinedBorderColor = theme.palette.mode === 'light'
        ? 'rgba(0, 0, 0, 0.23)'
        : 'rgba(255, 255, 255, 0.23)';

    useDynamicTextTokenTitles(container, props.variableMetadata);
    useDynamicTextSyntaxHighlights(container);

    return (
        <Box
            ref={setContainer}
            data-dynamic-text-multiline={props.multiline || undefined}
            sx={{
                position: 'relative',
                containerType: 'inline-size',
                display: 'grid',
                gridTemplateColumns: props.endAction == null ? 'minmax(0, 1fr)' : 'minmax(0, 1fr) 44px',
                minHeight,
                border: '1px solid',
                borderRadius: 1,
                bgcolor: isReadOnly ? getDisabledFieldBackground(theme) : 'transparent',
                borderColor: props.invalid ? 'error.main' : outlinedBorderColor,
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
                '& [contenteditable="false"]': {
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
                                aria-labelledby={props.ariaLabelledBy}
                                aria-describedby={props.ariaDescribedBy}
                                aria-disabled={props.disabled || props.busy || undefined}
                                aria-readonly={props.readOnly || undefined}
                                aria-busy={props.busy || undefined}
                                aria-required={props.required || undefined}
                                aria-invalid={props.invalid || undefined}
                                aria-multiline={props.multiline || undefined}
                                spellCheck={enableSpellCheck}
                                style={{
                                    boxSizing: 'border-box',
                                    caretColor: theme.palette.text.primary,
                                    minHeight,
                                    padding: '12px 14px',
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
                    <Tooltip title={props.endAction.tooltip ?? 'Aktion ausführen'} arrow>
                        <IconButton
                            aria-label={props.endAction.tooltip ?? 'Aktion ausführen'}
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
