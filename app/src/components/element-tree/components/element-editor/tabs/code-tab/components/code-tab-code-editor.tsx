import React, {useCallback, useRef} from "react";
import {Box, Typography} from "@mui/material";
import Editor from "@monaco-editor/react";
import {Function} from "../../../../../../../models/functions/function";
import {AlertComponent} from "../../../../../../alert/alert-component";

interface CodeTabCodeEditorProps {
    func: Function;
    onChange: (func: Function) => void;
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
                    height="calc(100vh - 768px)"
                    defaultLanguage="javascript"
                    options={{
                        minimap: {
                            enabled: false,
                        },
                    }}
                    onMount={handleEditorDidMount}
                />
            </Box>

            <AlertComponent
                text="Bitte beachten Sie, dass Code immer eine main-Function haben muss."
                title="Hinweis zum Code"
                color="info"
            />
        </>
    );
}
