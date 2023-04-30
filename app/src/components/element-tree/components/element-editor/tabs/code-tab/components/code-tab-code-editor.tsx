import {FunctionCode} from "../../../../../../../models/functions/function-code";
import React, {useCallback, useRef} from "react";
import {Box, Typography} from "@mui/material";
import Editor from "@monaco-editor/react";

interface CodeTabCodeEditorProps {
    func: FunctionCode;
    onChange: (func: FunctionCode) => void;
}

export function CodeTabCodeEditor({func, onChange}: CodeTabCodeEditorProps) {
    const editorRef = useRef<any>();

    const handleEditorDidMount = useCallback((editor: any, _: any) => {
        editorRef.current = editor;
        editorRef.current.setValue(func.code);
        editorRef.current.getModel().onDidChangeContent(() => {
            onChange({
                ...func,
                code: editorRef.current.getValue(),
            });
        });
    }, [editorRef]);

    return (
        <>
            <Typography variant="subtitle1">
                Code bearbeiten
            </Typography>

            <Box sx={{mt: 2}}>
                <Editor
                    height="calc(100vh - 256px)"
                    defaultLanguage="javascript"
                    options={{
                        minimap: {
                            enabled: false,
                        },
                    }}
                    onMount={handleEditorDidMount}
                />
            </Box>
        </>
    );
}
