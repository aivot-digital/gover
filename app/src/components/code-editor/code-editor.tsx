import Editor, {Monaco} from '@monaco-editor/react';
import {Box, Typography} from '@mui/material';
import React, {useCallback, useEffect, useRef} from 'react';
import {CodeEditorProps} from './code-editor-props';
import {ActionsProps} from '../actions/actions-props';
import {Actions} from '../actions/actions';

export function CodeEditor(props: CodeEditorProps & ActionsProps) {
    const {
        onChange,
        value,
    } = props;

    const monacoRef = useRef<Monaco>();
    const editorRef = useRef<any>();

    const hasTopContent = props.label != null || props.actions.length > 0;

    const handleEditorMount = useCallback((editor: any, monaco: Monaco) => {
        monacoRef.current = monaco;
        editorRef.current = editor;
        editorRef.current.setValue(value ?? '');
    }, []);

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
                    py: 1,
                    border: '1px solid black',
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