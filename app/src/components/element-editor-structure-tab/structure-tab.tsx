import Editor from '@monaco-editor/react';
import {Box, Button, FormControlLabel, Switch} from '@mui/material';
import React, {type ChangeEvent, useCallback, useRef, useState} from 'react';
import {type StructureTabProps} from './structure-tab-props';
import {type AnyElement} from '../../models/elements/any-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';

export function StructureTab<T extends AnyElement>(props: StructureTabProps<T>): JSX.Element {
    const dispatch = useAppDispatch();

    const editorRef = useRef<any>();
    const [editable, setEditable] = useState(false);

    const handleEditorDidMount = useCallback((editor: any, _: any) => {
        editorRef.current = editor;
    }, [editorRef]);

    const handleToggleEditable = (event: ChangeEvent<HTMLInputElement>): void => {
        if (event.target.checked) {
            const confirmed = window.confirm('Sind Sie sicher, dass Sie die Elementstruktur manuell überschreiben möchten? DIES IST EINE GEFÄHRLICHE OPERATION UND SOLLTE NUR VON ENTWICKLER:INNEN DURCHGEFÜHRT WERDEN!');
            if (confirmed && editorRef.current != null) {
                setEditable(true);
            }
        } else {
            const confirmed = window.confirm('Sind Sie sicher, dass Sie die Änderungen verwerfen möchten?');
            if (confirmed && editorRef.current != null) {
                setEditable(false);
                editorRef.current.setValue(JSON.stringify(props.elementModel, null, 4));
            }
        }
    };

    const handleSaveChanges = (): void => {
        if (editorRef.current != null) {
            const struct = editorRef.current.getValue();
            if (struct != null && struct.length > 0) {
                try {
                    const newStruct = JSON.parse(struct);
                    props.onChange(newStruct);
                    setEditable(false);
                } catch (e) {
                    console.error(e);
                    dispatch(showErrorSnackbar('Fehler beim Verarbeiten der Elementstruktur!'));
                }
            }
        }
    };

    return (
        <Box sx={{my: 4}}>
            {
                props.editable &&
                <Box
                    sx={{
                        ml: 8,
                        mr: 2,
                        mb: 2,
                        display: 'flex',
                        justifyContent: 'space-between',
                    }}
                >
                    <FormControlLabel
                        control={<Switch
                            checked={editable}
                            onChange={handleToggleEditable}
                        />}
                        label="Struktur manuell überschreiben"
                    />
                    <Button
                        startIcon={<SaveOutlinedIcon
                            sx={{marginTop: '-2px'}}
                        />}
                        disabled={!editable}
                        variant="outlined"
                        onClick={handleSaveChanges}
                    >
                        Struktur anwenden
                    </Button>
                </Box>

            }

            <Editor
                height="calc(100vh - 256px)"
                defaultLanguage="json"
                value={JSON.stringify(props.elementModel, null, 4)}
                options={{
                    minimap: {
                        enabled: false,
                    },
                    readOnly: !editable,
                }}
                onMount={handleEditorDidMount}
            />
        </Box>
    );
}
