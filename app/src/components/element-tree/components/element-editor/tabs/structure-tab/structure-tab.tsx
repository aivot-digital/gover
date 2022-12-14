import Editor from '@monaco-editor/react';
import {Box, Button, FormControlLabel, Switch} from '@mui/material';
import React, {ChangeEvent, useCallback, useRef, useState} from 'react';
import {faSave} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {StructureTabProps} from './structure-tab-props';
import {AnyElement} from '../../../../../../models/elements/any-element';
import {useAppDispatch} from '../../../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../../../slices/snackbar-slice';

export function StructureTab<T extends AnyElement>({elementModel, onChange}: StructureTabProps<T>) {
    const dispatch = useAppDispatch();

    const editorRef = useRef<any>();
    const [editable, setEditable] = useState(false);

    const handleEditorDidMount = useCallback((editor: any, _: any) => {
        editorRef.current = editor;
    }, [editorRef]);

    const handleToggleEditable = (event: ChangeEvent<HTMLInputElement>) => {
        if (event.target.checked) {
            const confirmed = window.confirm('Sind Sie sicher, dass Sie die Elementstruktur manuell überschreiben möchten? DIES IST EINE GEFÄHRLICHE OPERATION UND SOLLTE NUR VON ENTWICKLER:INNEN DURCHGEFÜHRT WERDEN!');
            if (confirmed && editorRef.current) {
                setEditable(true);
            }
        } else {
            const confirmed = window.confirm('Sind Sie sicher, dass Sie die Änderungen verwerfen möchten?');
            if (confirmed && editorRef.current) {
                setEditable(false);
                editorRef.current.setValue(JSON.stringify(elementModel, null, 4));
            }
        }
    };

    const handleSaveChanges = () => {
        if (editorRef.current) {
            const struct = editorRef.current.getValue();
            if (struct && struct.length > 0) {
                try {
                    const newStruct = JSON.parse(struct);
                    onChange(newStruct);
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
            <Box sx={{
                ml: 8,
                mr: 2,
                mb: 2,
                display: 'flex',
                justifyContent: 'space-between',
            }}>
            <FormControlLabel
                control={<Switch checked={editable} onChange={handleToggleEditable}/>}
                label="Struktur manuell überschreiben"
            />
            <Button
                startIcon={<FontAwesomeIcon
                    icon={faSave}
                    style={{marginTop: '-2px'}}
                    fixedWidth
                />}
                disabled={!editable}
                variant="outlined"
                onClick={handleSaveChanges}
            >
                Struktur anwenden
            </Button>
            </Box>

            <Editor
                height="calc(100vh - 256px)"
                defaultLanguage="json"
                value={JSON.stringify(elementModel, null, 4)}
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
