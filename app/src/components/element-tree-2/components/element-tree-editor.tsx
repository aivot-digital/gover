import {Box, Breadcrumbs, Button, Drawer, Tab, Tabs, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeContext} from '../element-tree-context';
import {useConfirm} from '../../../providers/confirm-provider';
import {
    createElementEditorNavigationLink,
    useElementEditorNavigation,
} from '../../../hooks/use-element-editor-navigation';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {Link} from 'react-router-dom';
import {ElementTreeEditorContextProvider} from './element-tree-editor-context';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {ElementIsInput} from '../../../data/element-type/element-is-input';
import {ElementTreeEditorContentDispatcher} from './element-tree-editor-content-dispatcher';

interface ElementTreeEditorProps<T extends AnyElement> {
    open: boolean;
    parents: Array<AnyElement>;
    value: T;
    onChange: (value: T) => void;
    onDelete: () => void;
    onCancel: () => void;
}

export function ElementTreeEditor<T extends AnyElement>(props: ElementTreeEditorProps<T>): React.ReactNode | null {
    const showConfirm = useConfirm();

    const {
        currentEditorTab,
        navigateToEditorTab,
    } = useElementEditorNavigation();

    const {
        root,
        editable,
        drawerZIndexOverride,
    } = useElementTreeContext();

    const {
        open,
        parents,
        value,
        onChange,
        onDelete,
        onCancel,
    } = props;

    const [updatedElement, setUpdatedElement] = useState<T>();

    // Reset the updated states when opening the editor
    useEffect(() => {
        if (open) {
            setUpdatedElement(undefined);
        }
    }, [open]);

    const handleSetCurrentTab = (newTab: string): void => {
        navigateToEditorTab(newTab);
    };

    const handleClose = async (): Promise<void> => {
        if (updatedElement != null) {
            const confirmed = await showConfirm({
                title: 'Änderungen verwerfen?',
                confirmButtonText: 'Ja, verwerfen',
                children: (
                    <div>
                        Möchten Sie die Änderungen an diesem{' '}
                        {updatedElement != null ? 'Element' : 'Formular'} wirklich verwerfen? Diese Aktion kann nicht
                        rückgängig gemacht werden.
                    </div>
                ),
            });

            if (confirmed) {
                onCancel();
            }
        } else {
            onCancel();
        }
    };

    const isRoot = root === value;

    return (
        <Drawer
            anchor="right"
            open={open}
            sx={{
                zIndex: drawerZIndexOverride,
            }}
            slotProps={{
                paper: {
                    sx: {
                        width: {
                            xs: '100%',
                            sm: '100%',
                            md: '90%',
                            lg: '85%',
                            xl: '75%',
                        },
                        maxWidth: '1720px',
                    },
                },
            }}
            onClose={handleClose}
        >
            <Box
                sx={{
                    height: '100vh',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        px: 2,
                        py: 1,
                    }}
                >
                    <Typography
                        variant="h6"
                        component="div"
                        sx={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            mr: 2,
                        }}
                    >
                        {generateComponentTitle(value)} ({getElementNameForType(value.type)})
                    </Typography>

                    <Breadcrumbs
                        sx={{
                            ml: 'auto',
                            color: 'text.secondary',
                            '& a': {
                                color: 'text.secondary',
                                textDecoration: 'none',
                                '&:hover': {
                                    textDecoration: 'underline',
                                },
                            },
                        }}
                        maxItems={3}
                    >
                        {
                            props
                                .parents
                                .map((element) => (
                                    <Link
                                        to={createElementEditorNavigationLink(element.id)}
                                        replace={true}
                                    >
                                        {generateComponentTitle(element)}
                                    </Link>
                                ))
                        }
                    </Breadcrumbs>
                </Box>

                <Tabs
                    value={currentEditorTab}
                    onChange={(_, newTab) => {
                        handleSetCurrentTab(newTab);
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
                >
                    <Tab
                        label="Eigenschaften"
                        value={DefaultTabs.properties}
                    />

                    <Box
                        sx={{
                            height: 24,
                            alignSelf: 'center',
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            mx: 1,
                        }}
                    />

                    {
                        // The root of an element tree cannot have visibility.
                        !isRoot &&
                        <Tab
                            label="Sichtbarkeit"
                            value={DefaultTabs.visibility}
                        />
                    }

                    {
                        ElementIsInput[value.type] &&
                        <Tab
                            label="Validierung"
                            value={DefaultTabs.validation}
                        />
                    }

                    {
                        ElementIsInput[value.type] &&
                        <Tab
                            label="Dynamischer Wert"
                            value={DefaultTabs.value}
                        />
                    }

                    {
                        // The root of an element tree cannot have visibility.
                        <Tab
                            label="Dynamische Struktur"
                            value={DefaultTabs.patch}
                        />
                    }

                    <Box
                        sx={{
                            height: 24,
                            alignSelf: 'center',
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            mx: 1,
                        }}
                    />

                    <Tab
                        label="Elementstruktur"
                        value={DefaultTabs.structure}
                    />
                </Tabs>

                <Box
                    sx={{
                        flex: '1',
                        overflowY: 'scroll',
                        borderTop: '1px solid #E0E0E0',
                        p: 4,
                    }}
                >
                    <ElementTreeEditorContextProvider
                        value={{
                            currentElement: updatedElement ?? value,
                            onChangeCurrentElement: (updated) => {
                                setUpdatedElement(updated);
                            },
                            parents: parents,
                        }}
                    >
                        <ElementTreeEditorContentDispatcher
                            element={updatedElement ?? value}
                            currentTab={currentEditorTab}
                            onChange={(changedElement) => {
                                setUpdatedElement({
                                    ...(updatedElement ?? value),
                                    ...changedElement,
                                });
                            }}
                            editable={editable}
                        />
                    </ElementTreeEditorContextProvider>
                </Box>

                <Box
                    sx={{
                        p: 4,
                        display: 'flex',
                        gap: 2,
                    }}
                >
                    <Button
                        variant="contained"
                        onClick={() => {
                            if (updatedElement != null) {
                                onChange(updatedElement);
                            } else {
                                handleClose();
                            }
                        }}
                    >
                        Speichern
                    </Button>

                    <Button
                        onClick={() => {
                            handleClose();
                        }}
                    >
                        Abbrechen
                    </Button>

                    <Button
                        color="error"
                        sx={{
                            ml: 'auto',
                        }}
                        onClick={() => {
                            showConfirm({
                                title: 'Element löschen',
                                confirmButtonText: 'Löschen',
                                children: (
                                    <Typography>
                                        Soll das Element wirklich gelöscht werden?
                                    </Typography>
                                ),
                                zIndex: drawerZIndexOverride != null ? drawerZIndexOverride + 10 : undefined,
                            }).then((confirmed) => {
                                if (confirmed) {
                                    onDelete();
                                }
                            });
                        }}
                    >
                        Löschen
                    </Button>
                </Box>
            </Box>
        </Drawer>
    );
}
