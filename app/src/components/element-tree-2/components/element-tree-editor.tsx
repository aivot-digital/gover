import {Box, Breadcrumbs, Button, Drawer, Tab, Tabs, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {createTheme, ThemeProvider, useTheme} from '@mui/material/styles';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeContext} from '../element-tree-context';
import {useConfirm} from '../../../providers/confirm-provider';
import {
    createElementEditorNavigationLink,
    useElementEditorNavigation,
} from '../../../hooks/use-element-editor-navigation';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {ElementTreeEditorContextProvider} from './element-tree-editor-context';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {ElementIsInput} from '../../../data/element-type/element-is-input';
import {ElementTreeEditorContentDispatcher} from './element-tree-editor-content-dispatcher';
import {Actions} from '../../actions/actions';
import Save from '@aivot/mui-material-symbols-400-outlined/dist/save/Save';
import ContentCopy from '@aivot/mui-material-symbols-400-outlined/dist/content-copy/ContentCopy';

interface ElementTreeEditorProps<T extends AnyElement> {
    open: boolean;
    parents: Array<AnyElement>;
    value: T;
    onChange: (value: T) => void;
    onDelete: () => void;
    onCancel: () => void;
    onClone: () => void;
}

export function ElementTreeEditor<T extends AnyElement>(props: ElementTreeEditorProps<T>): React.ReactNode | null {
    const showConfirm = useConfirm();
    const theme = useTheme();

    const {
        currentEditorTab,
        navigateToEditorTab,
    } = useElementEditorNavigation();

    const {
        root,
        editable,
        parentModalZIndex,
    } = useElementTreeContext();

    const {
        open,
        parents,
        value,
        onChange,
        onDelete,
        onCancel,
        onClone,
    } = props;

    const [updatedElement, setUpdatedElement] = useState<T>();
    const [isBusy, setIsBusy] = useState<boolean>(false);

    // Reset the updated states when opening the editor
    useEffect(() => {
        if (open) {
            setUpdatedElement(undefined);
            setIsBusy(false);
        }
    }, [open]);

    const handleSetCurrentTab = (newTab: string): void => {
        navigateToEditorTab(newTab);
    };

    const handleClose = async (): Promise<void> => {
        if (updatedElement != null) {
            const confirmed = await showConfirm({
                theme: drawerTheme,
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

    const handleClone = () => {
        setIsBusy(true);
        onClone();
    };

    const isRoot = root === value;
    const componentTitle = useMemo(() => generateComponentTitle(value), [value]);
    const typeName = useMemo(() => getElementNameForType(value.type), [value.type]);
    const showTypeSuffix = useMemo(() => componentTitle.trim() !== typeName.trim(), [componentTitle, typeName]);

    const drawerTheme = useMemo(() => {
        if (parentModalZIndex == null) {
            return theme;
        }

        // Move the whole drawer modal layer above the parent dialog and keep nested dialogs above the drawer.
        const drawerZIndex = Math.max(theme.zIndex.drawer, parentModalZIndex + 10);
        const modalZIndex = Math.max(theme.zIndex.modal, drawerZIndex + 10);

        if (drawerZIndex === theme.zIndex.drawer && modalZIndex === theme.zIndex.modal) {
            return theme;
        }

        return createTheme(theme, {
            zIndex: {
                ...theme.zIndex,
                drawer: drawerZIndex,
                modal: modalZIndex,
            },
        });
    }, [parentModalZIndex, theme]);

    return (
        <ThemeProvider theme={drawerTheme}>
            <Drawer
                anchor="right"
                open={open}
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
                            pt: 1,
                            pb: 0.5,
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
                            {componentTitle}
                            {
                                showTypeSuffix &&
                                <>
                                    <Typography
                                        component="span"
                                        color={'text.secondary'}
                                        sx={{
                                            fontSize: 'inherit',
                                            px: 0.5,
                                        }}
                                    >
                                        –
                                    </Typography>
                                    <Typography
                                        component="span"
                                        color={'text.secondary'}
                                        sx={{fontSize: 'inherit'}}
                                    >
                                        {typeName}
                                    </Typography>
                                </>
                            }
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
                                        <a
                                            href={createElementEditorNavigationLink(element.id)}
                                        >
                                            {generateComponentTitle(element)}
                                        </a>
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
                            p: 2,
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
                            pt: 2,
                            pb: 2.5,
                            px: 2,
                            display: 'flex',
                            borderTop: '1px solid #E0E0E0',
                            gap: 2,
                        }}
                    >
                        <Actions
                            actions={[
                                {
                                    icon: <Save/>,
                                    label: 'Speichern',
                                    onClick: () => {
                                        if (updatedElement != null) {
                                            onChange(updatedElement);
                                        } else {
                                            handleClose();
                                        }
                                    },
                                    disabled: isBusy || updatedElement == null,
                                    visible: editable,
                                    variant: 'contained',
                                },
                                {
                                    icon: null,
                                    label: editable ? 'Abbrechen' : 'Schließen',
                                    onClick: handleClose,
                                    disabled: isBusy,
                                },
                                {
                                    icon: <ContentCopy/>,
                                    tooltip: 'Duplizieren',
                                    onClick: handleClone,
                                    visible: editable,
                                    disabled: isBusy,
                                },
                            ]}
                        />

                        <Button
                            color="error"
                            sx={{
                                ml: 'auto',
                            }}
                            onClick={() => {
                                showConfirm({
                                    theme: drawerTheme,
                                    title: 'Element löschen',
                                    confirmButtonText: 'Löschen',
                                    children: (
                                        <Typography>
                                            Soll das Element wirklich gelöscht werden?
                                        </Typography>
                                    ),
                                }).then((confirmed) => {
                                    if (confirmed) {
                                        onDelete();
                                    }
                                });
                            }}
                            disabled={isBusy || !editable}
                        >
                            Löschen
                        </Button>
                    </Box>
                </Box>
            </Drawer>
        </ThemeProvider>
    );
}
