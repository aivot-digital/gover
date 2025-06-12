import Editor, {loader, Monaco} from '@monaco-editor/react';
import {Box, Typography} from '@mui/material';
import React, {useCallback, useEffect, useRef} from 'react';
import {CodeEditorProps} from './code-editor-props';
import {ActionsProps} from '../actions/actions-props';
import {Actions} from '../actions/actions';

import * as monaco from 'monaco-editor';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker';
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';

self.MonacoEnvironment = {
    getWorker(_, label) {
        if (label === 'json') {
            return new jsonWorker();
        }
        if (label === 'css' || label === 'scss' || label === 'less') {
            return new cssWorker();
        }
        if (label === 'html' || label === 'handlebars' || label === 'razor') {
            return new htmlWorker();
        }
        if (label === 'typescript' || label === 'javascript') {
            return new tsWorker();
        }
        return new editorWorker();
    },
};

loader.config({monaco});
loader.init().then((rs) => {
    console.log('Monaco Editor loaded', rs);
});

export function CodeEditor(props: CodeEditorProps & ActionsProps) {
    const {
        onChange,
        value,
        typeHints,
    } = props;

    const monacoRef = useRef<Monaco>();
    const editorRef = useRef<any>();

    const hasTopContent = props.label != null || props.actions.length > 0;

    const handleEditorMount = useCallback((editor: any, monaco: Monaco) => {
        monacoRef.current = monaco;
        editorRef.current = editor;
        editorRef.current.setValue(value ?? '');

        monacoApplyTypeHints(monaco, typeHints);
    }, []);

    useEffect(() => {
        monacoApplyTypeHints(monacoRef.current, typeHints);
    }, [typeHints]);

    const handleEditorChange = useCallback((value: string | undefined) => {
        onChange(value ?? '');
    }, [onChange]);

    return (
        <Box sx={props.sx}>
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
                    border: '1px solid rgba(0, 0, 0, 0.23)',
                    borderRadius: 1,
                }}
            >
                <Editor
                    height={props.height ?? 'max(100vh - 768px, 320px)'}
                    defaultLanguage={props.language ?? 'javascript'}
                    options={{
                        minimap: {
                            enabled: false,
                        },
                        readOnly: props.disabled,
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
        console.log('Applying Type Hint', typeHint);
    }

    console.log(monaco);
    console.log(monaco.languages);
    console.log(monaco.languages.typescript);
    console.log(monaco.languages.typescript.javascriptDefaults);
}