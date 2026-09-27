import {type EditorProps} from '@monaco-editor/react';
import {Box, Button, CircularProgress, Stack, Typography} from '@mui/material';
import {useEffect, useState} from 'react';
import {AlertComponent} from '../alert/alert-component';
import {isMonacoStylesheetLoadError, loadMonacoEditor} from './monaco-editor-loader';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';

type EditorImplementation = typeof import('./monaco-editor-runtime')['MonacoEditorImplementation'];

export function MonacoEditor(props: EditorProps) {
    const [Editor, setEditor] = useState<EditorImplementation>();
    const [failure, setFailure] = useState<{retryable: boolean}>();
    const [attempt, setAttempt] = useState(0);

    useEffect(() => {
        let active = true;
        setFailure(undefined);
        withAsyncWrapper({
            desiredMinRuntime: 500,
            // Keep the loading state visible for the minimum duration even on fast failures.
            main: () => Promise.allSettled([loadMonacoEditor()]),
        }).then(([result]) => {
            if (result.status === 'rejected') {
                throw result.reason;
            }
            const {MonacoEditorImplementation} = result.value;
            if (active) {
                setEditor(() => MonacoEditorImplementation);
            }
        }).catch((error: unknown) => {
            if (active) {
                setFailure({retryable: !isMonacoStylesheetLoadError(error)});
            }
        });
        return () => {
            active = false;
        };
    }, [attempt]);

    if (Editor != null) {
        return <Editor {...props}/>;
    }

    return (
        <Box sx={{height: props.height, minHeight: 96, display: 'grid', alignItems: 'center'}}>
            {failure != null ? (
                <AlertComponent color="error" text="Der Code-Editor konnte nicht geladen werden.">
                    <Typography variant="body2" sx={{mt: 1}}>
                        {failure.retryable
                            ? 'Bitte versuchen Sie es erneut. Falls der Fehler bestehen bleibt, sichern Sie Ihre Eingaben, bevor Sie die Seite neu laden.'
                            : 'Bitte sichern Sie Ihre Eingaben und laden Sie anschließend die Seite neu.'}
                    </Typography>
                    {failure.retryable && (
                        <Button onClick={() => setAttempt((value) => value + 1)} sx={{mt: 1}}>
                            Erneut versuchen
                        </Button>
                    )}
                </AlertComponent>
            ) : (
                <Stack role="status" aria-busy="true" direction="row" spacing={1.5} sx={{alignItems: 'center', justifyContent: 'center'}}>
                    <CircularProgress size={20} aria-hidden="true"/>
                    <Typography variant="body2">Code-Editor wird geladen …</Typography>
                </Stack>
            )}
        </Box>
    );
}
