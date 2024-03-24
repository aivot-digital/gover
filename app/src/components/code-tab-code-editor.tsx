import React, {useCallback, useMemo, useRef, useState} from 'react';
import {Box, Button, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText, Typography} from '@mui/material';
import Editor from '@monaco-editor/react';
import {type Function} from '../models/functions/function';
import ManageSearchOutlinedIcon from '@mui/icons-material/ManageSearchOutlined';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectLoadedForm} from '../slices/app-slice';
import {DialogTitleWithClose} from './dialog-title-with-close/dialog-title-with-close';
import {SearchInput} from './search-input/search-input';
import {flattenElementsWithParents} from '../utils/flatten-elements';
import {getElementName} from '../data/element-type/element-names';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {showSuccessSnackbar} from '../slices/snackbar-slice';

interface CodeTabCodeEditorProps {
    func: Function;
    onChange: (func: Function) => void;
    editable: boolean;
}

export function CodeTabCodeEditor({
                                      func,
                                      onChange,
                                      editable,
                                  }: CodeTabCodeEditorProps) {
    const dispatch = useAppDispatch();

    const editorRef = useRef<any>();

    const [elementSearchDialogOpen, setElementSearchDialogOpen] = useState(false);
    const [elementSearch, setElementSearch] = useState('');

    const form = useAppSelector(selectLoadedForm);
    const allElements = useMemo(() => {
        if (form == null) {
            return [];
        }
        return form.root.children.flatMap(ch => flattenElementsWithParents(ch, []));
    }, [form]);
    const elementsToShow = useMemo(() => {
        if (elementSearch === '') {
            return allElements;
        }
        const searchLowerCase = elementSearch.toLowerCase();
        return allElements.filter((element) => {
            return getElementName(element.element).toLowerCase().includes(searchLowerCase);
        });
    }, [allElements, elementSearch]);

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
            <Box
                display="flex"
                alignItems="center"
            >
                <Typography variant="subtitle2">
                    Code bearbeiten
                </Typography>

                <Button
                    size="small"
                    sx={{ml: 'auto'}}
                    endIcon={<ManageSearchOutlinedIcon />}
                    onClick={() => setElementSearchDialogOpen(true)}
                >
                    Element ID nachschlagen
                </Button>
            </Box>

            <Box
                sx={{
                    mt: 2,
                    py: 1,
                    border: '1px solid black',
                }}
            >
                <Editor
                    height="calc(100vh - 768px)"
                    defaultLanguage="javascript"
                    options={{
                        minimap: {
                            enabled: false,
                        },
                        readOnly: !editable,
                    }}
                    onMount={handleEditorDidMount}
                />
            </Box>

            <Dialog
                open={elementSearchDialogOpen}
                fullWidth
            >
                <DialogTitleWithClose
                    onClose={() => {
                        setElementSearchDialogOpen(false);
                        setElementSearch('');
                    }}
                >
                    Element ID nachschlagen
                </DialogTitleWithClose>
                <DialogContent>
                    <SearchInput
                        value={elementSearch}
                        onChange={setElementSearch}
                        placeholder="Element suchen..."
                    />

                    <List dense>
                        {
                            elementsToShow.map((element) => (
                                <ListItem>
                                    <ListItemButton
                                        onClick={() => {
                                            navigator
                                                .clipboard
                                                .writeText(`${element.element.id}`)
                                                .then(() => {
                                                    dispatch(showSuccessSnackbar('Element ID in die Zwischenablage kopiert'));
                                                });
                                            setElementSearchDialogOpen(false);
                                            setElementSearch('');
                                        }}
                                    >
                                        <ListItemText
                                            primary={
                                                (
                                                    element.parents.length > 0 ?
                                                        (element.parents.map(getElementName).join(' > ') + ' > ') :
                                                        ''
                                                ) + getElementName(element.element)}
                                            secondary={element.element.id}
                                        />
                                    </ListItemButton>
                                </ListItem>
                            ))
                        }
                    </List>
                </DialogContent>
            </Dialog>
        </>
    );
}
