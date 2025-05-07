import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Box, Button, Dialog, DialogContent, List, ListItem, ListItemButton, ListItemText, Typography} from '@mui/material';
import Editor, {Monaco} from '@monaco-editor/react';
import {type Function} from '../models/functions/function';
import ManageSearchOutlinedIcon from '@mui/icons-material/ManageSearchOutlined';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectLoadedForm} from '../slices/app-slice';
import {DialogTitleWithClose} from './dialog-title-with-close/dialog-title-with-close';
import {SearchInput} from './search-input/search-input';
import {ElementWithParents, flattenElementsWithParents, generateElementNameWithParent} from '../utils/flatten-elements';
import {getElementName} from '../data/element-type/element-names';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {showSuccessSnackbar} from '../slices/snackbar-slice';
import {generateComponentTitle} from '../utils/generate-component-title';
import {AnyElement} from '../models/elements/any-element';
import {formToTypeDefinition} from '../utils/form-to-type-definition';
import {elementToTypeDefinition} from '../utils/elemet-to-type-definition';

interface CodeTabCodeEditorProps {
    element?: AnyElement;
    func: Function;
    onChange: (func: Function) => void;
    editable: boolean;
}

export function CodeTabCodeEditor(props: CodeTabCodeEditorProps) {
    const monacoRef = useRef<Monaco>();
    const editorRef = useRef<any>();
    const loadedForm = useAppSelector(selectLoadedForm);
    const [elementSearchDialogOpen, setElementSearchDialogOpen] = useState(false);

    const handleEditorDidMount = useCallback((editor: any, monaco: Monaco) => {
        monacoRef.current = monaco;
        editorRef.current = editor;
        editorRef.current.setValue(props.func.code ?? '');
        editorRef.current.getModel().onDidChangeContent(() => {
            props.onChange({
                ...props.func,
                code: editorRef.current.getValue(),
            });
        });
    }, [editorRef]);

    useEffect(() => {
        if (monacoRef.current != null && props.element != null) {
            monacoRef.current?.languages.typescript.javascriptDefaults.addExtraLib(
                elementToTypeDefinition(props.element),
                '@types/current-element.d.ts',
            );
        }
    }, [monacoRef.current, props.element]);

    useEffect(() => {
        if (monacoRef.current != null && loadedForm != null) {
            monacoRef.current?.languages.typescript.javascriptDefaults.addExtraLib(
                formToTypeDefinition(loadedForm),
                '@types/data.d.ts',
            );
        }
    }, [monacoRef.current, loadedForm]);

    return (
        <>
            <Box
                display="flex"
                alignItems="center"
            >
                <Typography variant="subtitle2">
                    Code bearbeiten
                </Typography>

                {
                    props.editable &&
                    <Button
                        size="small"
                        sx={{ml: 'auto'}}
                        endIcon={<ManageSearchOutlinedIcon />}
                        onClick={() => setElementSearchDialogOpen(true)}
                    >
                        Element ID nachschlagen
                    </Button>
                }
            </Box>

            <Box
                sx={{
                    mt: 2,
                    py: 1,
                    border: '1px solid black',
                }}
            >
                <Editor
                    height="max(100vh - 768px, 320px)"
                    defaultLanguage="javascript"
                    options={{
                        minimap: {
                            enabled: false,
                        },
                        readOnly: !props.editable,
                    }}
                    onMount={handleEditorDidMount}
                />
            </Box>

            <LookupElementIdDialog
                open={elementSearchDialogOpen}
                onClose={() => setElementSearchDialogOpen(false)}
            />
        </>
    );
}

interface SearchElement extends ElementWithParents {
    title: string;
    path: string;
}

function LookupElementIdDialog(props: { open: boolean, onClose: () => void }) {
    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);

    const [search, setSearch] = useState('');

    const allElements: SearchElement[] = useMemo(() => {
        if (form == null) {
            return [];
        }
        return form.root.children
            .flatMap(ch => flattenElementsWithParents(ch, []))
            .map((e) => ({
                ...e,
                title: getElementName(e.element),
                path: e.parents.map(generateComponentTitle).join(' > '),
            }));

    }, [form]);

    const elementsToShow = useMemo(() => {
        if (search === '') {
            return allElements;
        }

        const searchLowerCase = search.toLowerCase();

        return allElements.filter((element) => {
            return element.title.includes(searchLowerCase) || element.path.includes(searchLowerCase);
        });
    }, [allElements, search]);

    return (
        <Dialog
            open={props.open}
            fullWidth
        >
            <DialogTitleWithClose
                onClose={() => {
                    props.onClose();
                    setSearch('');
                }}
            >
                Element ID nachschlagen
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                <SearchInput
                    value={search}
                    onChange={setSearch}
                    placeholder="Element suchen…"
                />

                <List dense>
                    {
                        elementsToShow.map((element) => (
                            <ListItem
                                key={element.element.id}
                            >
                                <ListItemButton
                                    onClick={() => {
                                        navigator
                                            .clipboard
                                            .writeText(`${element.element.id}`)
                                            .then(() => {
                                                dispatch(showSuccessSnackbar('Element ID in die Zwischenablage kopiert'));
                                            });
                                        props.onClose();
                                        setSearch('');
                                    }}
                                >
                                    <ListItemText
                                        primary={generateElementNameWithParent(element)}
                                        secondary={element.element.id}
                                    />
                                </ListItemButton>
                            </ListItem>
                        ))
                    }
                </List>
            </DialogContent>
        </Dialog>
    );
}
