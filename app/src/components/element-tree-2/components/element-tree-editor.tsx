import {Box, Breadcrumbs, Drawer, Tab, Tabs, Typography} from '@mui/material';
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
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {ElementsApiService} from '../../../modules/elements/elements-api-service';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {validateDataKeyPath} from '../../../utils/data-key-input';
import {suggestDestinationKey} from './element-tree-editor-content-tab-properties-utils';

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
        displayContext,
        identityMappingInformation,
        newElementIds,
        markElementApplied,
        allElements,
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
    const [dataKeyUnlocked, setDataKeyUnlocked] = useState(false);
    const [automaticDataKey, setAutomaticDataKey] = useState(false);
    const [dataKeyValidationRequested, setDataKeyValidationRequested] = useState(false);

    const hasSummaryAsParent = useMemo(() => {
        return parents.some(p => p.type === ElementType.SummaryLayout);
    }, [parents]);

    // Only a newly added field follows its title. A loaded empty key deliberately keeps values in element data.
    // Snapshot this policy per editor session; ordinary parent updates must not reset an in-progress edit.
    useEffect(() => {
        if (open) {
            const automatic = displayContext !== ElementDisplayContext.DataObjectSchema &&
                newElementIds?.has(value.id) === true && isAnyInputElement(value) && !value.destinationKey;
            setAutomaticDataKey(automatic);
            setDataKeyUnlocked(false);
            setDataKeyValidationRequested(false);
            const generatedKey = automatic && isAnyInputElement(value)
                ? suggestDestinationKey(value.label, {element: value, parents}, allElements)
                : null;
            setUpdatedElement(generatedKey == null ? undefined : {...value, destinationKey: generatedKey});
            setIsBusy(false);
        }
    }, [open, value.id]);

    const changeCurrentElement = (updated: T) => {
        const previous = updatedElement ?? value;
        if (automaticDataKey && isAnyInputElement(previous) && isAnyInputElement(updated)) {
            if (updated.destinationKey !== previous.destinationKey) {
                setAutomaticDataKey(false);
            } else if (updated.label !== previous.label) {
                updated = {...updated, destinationKey: suggestDestinationKey(updated.label, {element: updated, parents}, allElements)};
            }
        }
        setUpdatedElement(updated);
    };

    const handleSetCurrentTab = (newTab: string): void => {
        navigateToEditorTab(newTab);
    };

    const handleSave = (): void => {
        const current = updatedElement ?? value;
        setDataKeyValidationRequested(true);
        if (displayContext !== ElementDisplayContext.DataObjectSchema && isAnyInputElement(current) &&
            validateDataKeyPath(current.destinationKey) != null) {
            navigateToEditorTab(DefaultTabs.properties);
            return;
        }
        if (updatedElement == null) {
            markElementApplied?.(value.id);
            handleClose();
            return;
        }

        setIsBusy(true);
        new ElementsApiService()
            .recalculateReferencedIds(updatedElement)
            .then((recalculatedElement) => {
                setDataKeyUnlocked(false);
                markElementApplied?.(value.id);
                onChange(recalculatedElement);
            })
            .catch(() => {
                // If the recalculation fails, we can still try to save the element with the risk of broken references.
                setDataKeyUnlocked(false);
                markElementApplied?.(value.id);
                onChange(updatedElement);
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handleClose = async (): Promise<void> => {
        if (updatedElement != null) {
            const confirmed = await showConfirm({
                theme: drawerTheme,
                confirmButtonColor: 'error',
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
        try {
            onClone();
        } finally {
            setIsBusy(false);
        }
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
                                        sx={{
                                            color: 'text.secondary',
                                            fontSize: 'inherit',
                                            px: 0.5
                                        }}>
                                        –
                                    </Typography>
                                    <Typography
                                        component="span"
                                        sx={{
                                            color: 'text.secondary',
                                            fontSize: 'inherit'
                                        }}>
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
                                            key={element.id}
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

                        {
                            ElementIsInput[value.type] &&
                            identityMappingInformation != null &&
                            identityMappingInformation.length > 0 &&
                            <Tab
                                label="Identitätsdaten"
                                value={DefaultTabs.metadata}
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

                        {
                            // The root of an element tree cannot have visibility.
                            !isRoot &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Tab
                                label="Sichtbarkeit"
                                value={DefaultTabs.visibility}
                            />
                        }

                        {
                            ElementIsInput[value.type] &&
                            !hasSummaryAsParent &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Tab
                                label="Validierung"
                                value={DefaultTabs.validation}
                            />
                        }

                        {
                            ElementIsInput[value.type] &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            value.type != ElementType.FileUpload &&
                            <Tab
                                label="Dynamischer Wert"
                                value={DefaultTabs.value}
                            />
                        }

                        {
                            // The root of an element tree cannot have a dynamic structure.
                            !isRoot &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Tab
                                label="Dynamische Struktur"
                                value={DefaultTabs.patch}
                            />
                        }

                        {
                            !isRoot &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Box
                                sx={{
                                    height: 24,
                                    alignSelf: 'center',
                                    borderLeft: '1px solid',
                                    borderColor: 'divider',
                                    mx: 1,
                                }}
                            />
                        }

                        <Tab
                            label="Elementstruktur"
                            value={DefaultTabs.structure}
                        />
                    </Tabs>

                    <Box
                        sx={{
                            flex: '1',
                            overflowY: 'scroll',
                            borderTop: '1px solid',
                            borderTopColor: 'divider',
                            p: 2,
                        }}
                    >
                        <ElementTreeEditorContextProvider
                            value={{
                                currentElement: updatedElement ?? value,
                                originalElement: value,
                                dataKeyUnlocked,
                                onUnlockDataKey: () => {
                                    setDataKeyUnlocked(true);
                                    // Manual control ends title tracking, even if the user later locks the key again.
                                    setAutomaticDataKey(false);
                                },
                                onLockDataKey: () => setDataKeyUnlocked(false),
                                dataKeyValidationRequested,
                                onChangeCurrentElement: changeCurrentElement,
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
                            borderTop: '1px solid',
                            borderTopColor: 'divider',
                            gap: 2,
                        }}
                    >
                        <Actions
                            actions={[
                                {
                                    label: 'Übernehmen',
                                    onClick: handleSave,
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
                            ]}
                        />

                        <Actions
                            sx={{
                                ml: 'auto',
                            }}
                            actions={[
                                {
                                    icon: <ContentCopy/>,
                                    iconPosition: 'start',
                                    label: 'Duplizieren',
                                    onClick: handleClone,
                                    visible: editable && !isRoot,
                                    disabled: isBusy,
                                },
                                {
                                    icon: <Delete/>,
                                    iconPosition: 'start',
                                    label: 'Löschen',
                                    visible: !isRoot,
                                    onClick: onDelete,
                                    disabled: isBusy || !editable,
                                    variant: 'outlined',
                                    color: 'error',
                                },
                            ]}
                        />
                    </Box>
                </Box>
            </Drawer>
        </ThemeProvider>
    );
}
