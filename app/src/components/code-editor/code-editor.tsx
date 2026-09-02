import Editor, {loader, Monaco} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import {editor} from 'monaco-editor';
import {Box, useTheme} from '@mui/material';
import React, {useCallback, useEffect, useRef} from 'react';
import {CodeEditorProps} from './code-editor-props';
import {ActionsProps} from '../actions/actions-props';
import {Actions} from '../actions/actions';
import {AlertComponent} from '../alert/alert-component';
import {JavascriptApiService} from '../../modules/javascript/javascript-api-service';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

export function CodeEditor(props: CodeEditorProps & ActionsProps) {
    const theme = useTheme();
    const {
        onChange,
        value,
        typeHints,
        onEditorMount,
    } = props;

    const monacoRef = useRef<Monaco>(undefined);
    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const onBlurRef = useRef<CodeEditorProps['onBlur']>(props.onBlur);

    useEffect(() => {
        onBlurRef.current = props.onBlur;
    }, [props.onBlur]);

    useEffect(() => {
        new JavascriptApiService()
            .getTypes()
            .then((globalTypeHints) => {
                if (monacoRef.current == null) {
                    return;
                }

                monacoRef
                    .current
                    .languages
                    .typescript
                    .javascriptDefaults
                    .addExtraLib(globalTypeHints, `@types/global.d.ts`,)
            });
    }, []);

    const hasTopContent = props.label != null || props.actions.length > 0;

    const handleEditorMount = useCallback((editor: editor.IStandaloneCodeEditor, monaco: Monaco) => {
        monacoRef.current = monaco;
        editorRef.current = editor;

        if (onEditorMount) {
            onEditorMount(editor);
        }

        const blurDisposable = editor.onDidBlurEditorText(() => {
            onBlurRef.current?.(editor.getValue());
        });

        monacoApplyTypeHints(monaco, typeHints);

        return () => {
            blurDisposable.dispose();
        };
    }, [onEditorMount, typeHints]);

    useEffect(() => {
        monacoApplyTypeHints(monacoRef.current, typeHints);
    }, [typeHints]);

    useEffect(() => {
        const activeEditor = editorRef.current;
        const nextValue = value ?? '';
        if (activeEditor == null || activeEditor.getValue() === nextValue) {
            return;
        }

        activeEditor.setValue(nextValue);
    }, [value]);

    const handleEditorChange = useCallback((value: string | undefined) => {
        onChange(value ?? '');
    }, [onChange]);
    const editorAriaLabel = props.ariaLabel ?? props.label ?? 'Code-Editor';
    const isInteractionDisabled = Boolean(props.disabled || props.readOnly || props.busy);

    return (
        <Box sx={props.sx}>
            {
                props.alert &&
                <AlertComponent {...props.alert} />
            }
            {
                hasTopContent && (
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        <Box component="span" sx={{fontWeight: 500}}>
                            {props.label}
                        </Box>

                        <Box
                            sx={{
                                ml: 'auto',
                            }}
                        >
                            <Actions actions={props.actions} />
                        </Box>
                    </Box>
                )
            }
            <Box
                id={props.id}
                role="group"
                aria-label={props.ariaLabelledBy == null ? editorAriaLabel : undefined}
                aria-labelledby={props.ariaLabelledBy}
                aria-describedby={props.ariaDescribedBy}
                aria-disabled={props.disabled || props.busy || undefined}
                aria-readonly={props.readOnly || props.busy || undefined}
                aria-busy={props.busy || undefined}
                aria-required={props.required || undefined}
                aria-invalid={props.error || undefined}
                sx={{
                    mt: hasTopContent ? 2 : 0,
                    py: 2,
                    overflow: 'hidden',
                    border: '1px solid',
                    borderColor: props.error ? 'error.main' : 'divider',
                    borderRadius: 1,
                    backgroundColor: isInteractionDisabled
                        ? getDisabledFieldBackground
                        : 'transparent',
                    '&:focus-within': {
                        borderColor: props.error ? 'error.main' : 'primary.main',
                        boxShadow: (theme) => `0 0 0 1px ${
                            props.error ? theme.palette.error.main : theme.palette.primary.main
                        }`,
                    },
                    // Monaco paints its own surface and focus outline. The field wrapper owns both
                    // so its light/dark appearance and focus state match the other form controls.
                    '& .monaco-editor': {
                        backgroundColor: 'transparent !important',
                        outline: 'none !important',
                    },
                    '& .monaco-editor-background, & .monaco-editor .margin': {
                        backgroundColor: 'transparent !important',
                    },
                }}
            >
                <Editor
                    height={props.height ?? 'max(100vh - 768px, 320px)'}
                    language={props.language ?? 'javascript'}
                    theme={theme.palette.mode === 'dark' ? 'vs-dark' : 'light'}
                    value={value ?? ''}
                    options={{
                        minimap: {
                            enabled: false,
                        },
                        readOnly: isInteractionDisabled,
                        ariaLabel: editorAriaLabel,
                        wordWrap: props.wordWrap ? 'on' : 'off',
                    }}
                    onMount={handleEditorMount}
                    onChange={handleEditorChange}
                />
            </Box>
        </Box>
    );
}

function monacoApplyTypeHints(monaco: any, typeHints: CodeEditorProps['typeHints']) {
    if (monaco == null || typeHints == null || typeHints.length === 0) {
        return;
    }

    for (const typeHint of typeHints) {
        monaco.languages.typescript.javascriptDefaults.addExtraLib(
            typeHint.content,
            `@types/${typeHint.name}.d.ts`,
        );
    }
}
