import Editor from '@monaco-editor/react';
import {Box, Button, FormControlLabel, Switch} from '@mui/material';
import React, {type ChangeEvent, useCallback, useRef, useState} from 'react';
import {type StructureTabProps} from './structure-tab-props';
import {type AnyElement} from '../../models/elements/any-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {IconButton} from '../icon-button/icon-button';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import {downloadObjectFile, uploadObjectFile} from '../../utils/download-utils';
import {generateComponentTitle} from '../../utils/generate-component-title';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';
import {useConfirm} from "../../providers/confirm-provider";
import {AlertComponent} from "../alert/alert-component";

export function StructureTab<T extends AnyElement>(props: StructureTabProps<T>): JSX.Element {
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const editorRef = useRef<any>();
    const [editable, setEditable] = useState(false);

    const handleEditorDidMount = useCallback((editor: any, _: any) => {
        editorRef.current = editor;
    }, [editorRef]);

    const handleToggleEditable = async (event: ChangeEvent<HTMLInputElement>): Promise<void> => {
        if (event.target.checked) {
            const confirmed = await showConfirm({
                title: "Elementstruktur überschreiben?",
                confirmButtonText: "Ja, fortfahren",
                children: (
                    <>
                        <div>
                            Sie sind dabei, die Elementstruktur manuell zu überschreiben.
                            <AlertComponent color={"warning"}>
                                <strong>Achtung:</strong> Diese Aktion kann zu unerwartetem Verhalten führen und sollte
                                {" "}<strong>nur von erfahrenen Entwickler:innen</strong> durchgeführt werden.
                                Änderungen können nicht automatisch rückgängig gemacht werden.
                            </AlertComponent>
                            Möchten Sie wirklich fortfahren?
                        </div>
                    </>
                ),
            });

            if (confirmed && editorRef.current != null) {
                setEditable(true);
            }
        } else {
            const confirmed = await showConfirm({
                title: "Änderungen verwerfen?",
                confirmButtonText: "Ja, verwerfen",
                children: <div>Möchten Sie die Änderungen wirklich verwerfen? Diese Aktion kann nicht rückgängig gemacht werden.</div>,
            });

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

    const handleDownload = (): void => {
        downloadObjectFile(generateComponentTitle(props.elementModel) + '.json', props.elementModel);
    };

    const handleUpload = (): void => {
        uploadObjectFile<AnyElement>('application/json')
            .then((file) => {
                if (file != null && editorRef.current != null) {
                    editorRef.current.setValue(JSON.stringify(file, null, 4));
                }
            });
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
                        sx={{
                            marginLeft: 'auto',
                        }}
                    >
                        Struktur anwenden
                    </Button>

                    {
                        !editable &&
                        <IconButton
                            buttonProps={{
                                onClick: handleDownload,
                                sx: {
                                    marginLeft: 1,
                                },
                            }}
                            tooltipProps={{
                                title: 'Elementstruktur herunterladen',
                            }}
                        >
                            <FileDownloadOutlinedIcon />
                        </IconButton>
                    }

                    {
                        editable &&
                        <IconButton
                            buttonProps={{
                                onClick: handleUpload,
                                sx: {
                                    marginLeft: 1,
                                },
                            }}
                            tooltipProps={{
                                title: 'Elementstruktur hochladen',
                            }}
                        >
                            <FileUploadOutlinedIcon />
                        </IconButton>
                    }
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
