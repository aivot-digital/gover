import {Box, TextField, Typography} from '@mui/material';
import React, {useCallback, useRef} from 'react';
import {RichTextEditorComponentView} from '../../../../../richt-text-editor/rich-text-editor.component.view';
import Editor from '@monaco-editor/react';
import {CodeService} from '../../../../../../services/code.service';
import {selectLoadedApplication} from '../../../../../../slices/app-slice';
import {useAppSelector} from '../../../../../../hooks/use-app-selector';
import {CodeTabProps} from './code-tab-props';
import {AnyElement} from '../../../../../../models/elements/any-element';
import {FunctionSet} from '../../../../../_lib/function-set';

export function CodeTab<T extends AnyElement>({field, element, onChange}: CodeTabProps<T>) {
    const application = useAppSelector(selectLoadedApplication);

    const editorRef = useRef<any>();

    const handleEditorDidMount = useCallback((editor: any, _: any) => {
        // FIXME: Race condition here? Probably? Old comment suggested it.
        editorRef.current = editor;
        if (application != null) {
            CodeService.getCodeString(application.id)
                .then(code => {
                    editorRef.current.setValue(code);
                });
        }
    }, [application, editorRef]);

    const handleChange = (patch: Partial<FunctionSet>) => {
        onChange({
            ...element,
            [field]: {
                ...element[field],
                ...patch,
            },
        });
    };

    return (
        <Box sx={{m: 4}}>
            <Typography
                sx={{mb: 2}}
                variant="subtitle1"
            >
                Fachliche Anforderungen
            </Typography>

            <RichTextEditorComponentView
                value={element[field]?.requirements ?? ''}
                onChange={req => {
                    handleChange({
                        requirements: req,
                    });
                }}
            />

            <Typography
                sx={{mt: 3}}
                variant="subtitle1"
            >
                Referenzierte Funktionsname
            </Typography>

            <TextField
                label="Funktionsname"
                value={element[field]?.functionName ?? ''}
                onChange={event => {
                    handleChange({
                        functionName: event.target.value,
                    });
                }}
            />

            <Typography
                sx={{mt: 3}}
                variant="subtitle1"
            >
                Hochgeladener Code
            </Typography>

            <Box sx={{mt: 2}}>
                <Editor
                    height="calc(100vh - 256px)"
                    defaultLanguage="javascript"
                    options={{
                        minimap: {
                            enabled: false,
                        },
                        readOnly: true,
                    }}
                    onMount={handleEditorDidMount}
                />
            </Box>
        </Box>
    );
}
