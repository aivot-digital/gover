import {useEffect, useRef, useState} from 'react';
import {Box, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import {createPortal} from 'react-dom';
import FormatAlignLeft from '@aivot/mui-material-symbols-400-n25-outlined/FormatAlignLeft';
import type {editor} from 'monaco-editor';
import {MonacoEditor} from '../../../components/code-editor/monaco-editor';
import {FormField} from '../../../components/form-field';

interface Props {
    toolbarContainer: HTMLElement | null;
    id?: string;
    ariaDescribedBy?: string;
    label?: string;
    ariaLabel: string;
    value: string;
    error?: string;
    hint?: string;
    disabled?: boolean;
    onChange: (value: string) => void;
    onBlur: () => void;
}

/** Mount only in JSON mode: the shared loader supplies the local JSON worker and failure/retry UI. */
export function NoCodeTestJsonInput(props: Props) {
    const theme = useTheme();
    const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);
    const blurSubscription = useRef<{ dispose: () => void } | null>(null);
    const blurRef = useRef(props.onBlur);
    const [ready, setReady] = useState(false);
    const [formattingFailed, setFormattingFailed] = useState(false);
    useEffect(() => {
        blurRef.current = props.onBlur;
    }, [props.onBlur]);
    useEffect(() => {
        setFormattingFailed(false);
    }, [props.value]);
    useEffect(() => () => {
        blurSubscription.current?.dispose();
        editorRef.current = null;
    }, []);

    return <FormField id={props.id}
                      ariaDescribedBy={props.ariaDescribedBy}
                      label={props.label ?? ''}
                      ariaLabel={props.ariaLabel}
                      error={props.error}
                      hint={props.hint}
                      disabled={props.disabled}
                      margin="none"
                      showOptionalIndicator={false}>
        {context => <Box>
            {props.toolbarContainer != null && createPortal(
                <Tooltip arrow
                         title="JSON formatieren"><span>
                    <IconButton size="small"
                                aria-label="JSON formatieren"
                                disabled={!ready || props.disabled}
                                onClick={() => {
                                    setFormattingFailed(false);
                                    void editorRef.current?.getAction('editor.action.formatDocument')?.run().catch(() => setFormattingFailed(true));
                                }}><FormatAlignLeft fontSize="small"/></IconButton>
                </span></Tooltip>, props.toolbarContainer)}
            <Box {...context.ariaProps} id={context.controlId}
                 role="group"
                 tabIndex={-1}
                 onFocus={event => {
                     if (event.target === event.currentTarget) editorRef.current?.focus();
                 }}
                 sx={{
                     border: 1,
                     borderColor: props.error ? 'error.main' : 'divider',
                     borderRadius: 1,
                     overflow: 'hidden',
                     '&:focus-within': {
                         borderColor: props.error ? 'error.main' : 'primary.main',
                         boxShadow: `0 0 0 1px ${props.error ? theme.palette.error.main : theme.palette.primary.main}`,
                     },
                     '& .monaco-editor': {
                         backgroundColor: 'transparent !important',
                         outline: 'none !important',
                     },
                     '& .monaco-editor-background, & .monaco-editor .margin': {backgroundColor: 'transparent !important'},
                 }}>
                {/* Match the shared code editor's exemption from React Flow keyboard shortcuts. */}
                <MonacoEditor className="nokey"
                              language="json"
                              height="220px"
                              value={props.value}
                              theme={theme.palette.mode === 'dark' ? 'vs-dark' : 'light'}
                              options={{
                                  ariaLabel: props.ariaLabel,
                                  readOnly: props.disabled,
                                  automaticLayout: true,
                                  minimap: {enabled: false},
                                  wordWrap: 'on',
                                  scrollBeyondLastLine: false,
                                  scrollbar: {alwaysConsumeMouseWheel: false},
                                  tabSize: 2,
                                  renderValidationDecorations: props.error ? 'on' : 'off',
                              }}
                              onChange={value => props.onChange(value ?? '')}
                              onMount={instance => {
                                  editorRef.current = instance;
                                  blurSubscription.current?.dispose();
                                  blurSubscription.current = instance.onDidBlurEditorText(() => blurRef.current());
                                  setReady(true);
                              }}/>
            </Box>
            {formattingFailed && <Typography role="alert"
                                             variant="body2"
                                             color="error"
                                             sx={{mt: 0.5}}>
                Das JSON konnte nicht formatiert werden. Sie können die Eingabe weiterhin bearbeiten.
            </Typography>}
        </Box>}
    </FormField>;
}
