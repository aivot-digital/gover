import Editor, {loader, Monaco} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import {editor} from 'monaco-editor';
import {Box, Typography} from '@mui/material';
import React, {useCallback, useEffect, useRef} from 'react';
import {CodeEditorProps} from './code-editor-props';
import {ActionsProps} from '../actions/actions-props';
import {Actions} from '../actions/actions';
import {AlertComponent} from '../alert/alert-component';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import {JavascriptApiService} from '../../modules/javascript/javascript-api-service';

self.MonacoEnvironment = {
    getWorker(_, label) {
        if (label === 'typescript' || label === 'javascript') {
            return new tsWorker();
        }
        return new editorWorker();
    },
};

loader.config({monaco});
loader.init();

export function CodeEditor(props: CodeEditorProps & ActionsProps) {
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

        editor.onDidBlurEditorText(() => {
            onBlurRef.current?.(editor.getValue());
        });

        monacoApplyTypeHints(monaco, typeHints);
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
                        <Typography>
                            {props.label}
                        </Typography>

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
                sx={{
                    mt: hasTopContent ? 2 : 0,
                    py: 2,
                    border: '1px solid',
                    borderColor: props.error ? 'error.main' : 'rgba(0, 0, 0, 0.23)',
                    borderRadius: 1,
                }}
            >
                <Editor
                    height={props.height ?? 'max(100vh - 768px, 320px)'}
                    language={props.language ?? 'javascript'}
                    value={value ?? ''}
                    options={{
                        minimap: {
                            enabled: false,
                        },
                        readOnly: props.disabled || props.readOnly,
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
