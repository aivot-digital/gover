import React, {useCallback, useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography, useTheme} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {flattenElements} from '../../utils/flatten-elements';
import {getElementNameForType} from '../../data/element-type/element-names';
import {ElementType} from '../../data/element-type/element-type';
import {UiDefinitionInputFieldElementItem} from '../../models/elements/form/input/ui-definition-input-field-element';
import {ElementTree} from '../element-tree-2/element-tree';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementDerivationContext} from '../../modules/elements/components/element-derivation-context';
import {Allotment} from 'allotment';
import {AuthoredElementValues} from '../../models/element-data';
import {ElementChildOptions, ElementDisplayContext} from '../../data/element-type/element-child-options';
import {Hint} from '../hint/hint';
import {humanizeNumberCapitalized} from '../../utils/humanization-utils';
import {ElementTreeInlineEditorContextProvider} from '../element-tree-2/components/element-tree-inline-editor-context';
import {useElementEditorNavigation} from '../../hooks/use-element-editor-navigation';
import {AnyElement} from '../../models/elements/any-element';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {useConfirm} from '../../providers/confirm-provider';
import {cloneElement} from '../../utils/clone-element';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {isRootElement} from '../../models/elements/form-layout-element';
import {UiDefinitionEmptyState} from '../ui-definition-empty-state/ui-definition-empty-state';
import {deepEquals} from '../../utils/equality-utils';

interface UiDefinitionInputFieldComponentProps {
    label: string;
    hint?: string | null;
    error?: string | null;
    required?: boolean | null;
    disabled?: boolean;
    value?: UiDefinitionInputFieldElementItem | null;
    expectedRootType?: ElementType | null;
    onChange: (value: UiDefinitionInputFieldElementItem | undefined) => void;
    displayContext: ElementDisplayContext;
    openOverride?: () => void;
}

function buildSummary(value?: UiDefinitionInputFieldElementItem | null): string {
    if (value == null) {
        return 'Keine UI-Definition konfiguriert';
    }

    const elementCount = flattenElements(value).length - 1; // Subtract 1 to compensate for the root element of the structure.
    const countLabel = `${humanizeNumberCapitalized(elementCount, {1: 'Ein'})} Element${elementCount === 1 ? '' : 'e'} enthalten`;

    return `${countLabel}`;
}

function cloneUiDefinitionValue(value: UiDefinitionInputFieldElementItem): UiDefinitionInputFieldElementItem {
    return JSON.parse(JSON.stringify(value)) as UiDefinitionInputFieldElementItem;
}

export function UiDefinitionInputFieldComponent(props: UiDefinitionInputFieldComponentProps) {
    const theme = useTheme();
    const confirm = useConfirm();
    const dispatch = useAppDispatch();

    const {
        label,
        hint,
        error,
        required,
        disabled,
        value,
        expectedRootType,
        onChange,
        displayContext,
        openOverride,
    } = props;

    const displayLabel = `${label}${required ? ' *' : ''}`;
    const [showDraftDialog, setShowDraftDialog] = useState<boolean>(false);
    const [draftValue, setDraftValue] = useState<UiDefinitionInputFieldElementItem | null>(null);
    const [initialDraftValue, setInitialDraftValue] = useState<UiDefinitionInputFieldElementItem | null>(null);
    const [inputData, setInputData] = useState<AuthoredElementValues>({});
    const [highlightElementId, setHighlightElementId] = useState<string | null>(null);
    const [highlightElementSignal, setHighlightElementSignal] = useState(0);
    const [hoveredTreeElementId, setHoveredTreeElementId] = useState<string | null>(null);
    const [openRootAddElementSignal, setOpenRootAddElementSignal] = useState(0);

    const {
        navigateToElementEditor,
    } = useElementEditorNavigation();

    const summary = useMemo(() => {
        return buildSummary(value);
    }, [value]);

    const expectedRootTypeLabel = useMemo(() => {
        if (expectedRootType == null) {
            return null;
        }

        return getElementNameForType(expectedRootType);
    }, [expectedRootType]);

    const defaultValue = useMemo(() => {
        return generateElementWithDefaultValues(expectedRootType ?? ElementType.GroupLayout) as UiDefinitionInputFieldElementItem;
    }, [expectedRootType]);
    const effectiveValue = draftValue ?? value ?? defaultValue;

    const hasUnsavedChanges = useMemo(() => {
        if (!showDraftDialog || draftValue == null || initialDraftValue == null) {
            return false;
        }

        return !deepEquals(initialDraftValue, draftValue);
    }, [draftValue, initialDraftValue, showDraftDialog]);

    const allowedRootChildTypes = useMemo(() => {
        return ElementChildOptions[displayContext][effectiveValue.type] ?? [];
    }, [displayContext, effectiveValue.type]);

    const canAddAtRoot = useMemo(() => {
        return (
            !disabled &&
            isAnyElementWithChildren(effectiveValue) &&
            allowedRootChildTypes.length > 0
        );
    }, [allowedRootChildTypes.length, disabled, effectiveValue]);

    const emptyStateTarget = useMemo(() => {
        if (
            !isAnyElementWithChildren(effectiveValue) ||
            (effectiveValue.children?.length ?? 0) > 0 ||
            allowedRootChildTypes.length === 0
        ) {
            return null;
        }

        return isRootElement(effectiveValue) ? 'section' : 'element';
    }, [allowedRootChildTypes.length, effectiveValue]);

    const handleClose = () => {
        setShowDraftDialog(false);
        setTimeout(() => {
            setDraftValue(null);
            setInitialDraftValue(null);
            setInputData({});
            setHoveredTreeElementId(null);
        }, 300);
    };

    const openDialog = useCallback(() => {
        if (openOverride != null) {
            openOverride();
        } else {
            const nextDraftValue = cloneUiDefinitionValue(value ?? defaultValue);
            setDraftValue(nextDraftValue);
            setInitialDraftValue(cloneUiDefinitionValue(nextDraftValue));
            setShowDraftDialog(true);
        }
    }, [defaultValue, openOverride, value]);

    const requestClose = useCallback(async () => {
        if (!hasUnsavedChanges) {
            handleClose();
            return;
        }

        const shouldDiscard = await confirm({
            title: 'Ungespeicherte Änderungen',
            children: (
                <Typography>
                    Sie haben ungespeicherte Änderungen an der UI-Definition. Möchten Sie den Dialog wirklich schließen? Dabei gehen alle ungespeicherten Änderungen verloren.
                </Typography>
            ),
        });

        if (!shouldDiscard) {
            return;
        }

        handleClose();
    }, [confirm, hasUnsavedChanges]);

    const handleDeleteElement = (element: AnyElement) => {
        if (draftValue == null) {
            return;
        }

        confirm({
            title: 'Element wirklich löschen',
            children: (
                <Typography>
                    Wollen Sie das Element <strong>{generateComponentTitle(element)}</strong> wirklich löschen?
                </Typography>
            ),
        })
            .then((conf) => {
                if (!conf) {
                    return;
                }

                function deleteElementRecursive<T extends AnyElement>(currentElement: T): T {
                    if (isAnyElementWithChildren(currentElement) && currentElement.children != null) {
                        return {
                            ...currentElement,
                            children: currentElement
                                .children
                                .filter(child => child.id !== element.id)
                                .map(child => deleteElementRecursive(child)),
                        };
                    } else {
                        return currentElement;
                    }
                }

                setDraftValue(deleteElementRecursive(draftValue));
            });
    };

    const handleCloneElement = (element: AnyElement) => {
        if (draftValue == null) {
            return;
        }

        function cloneElementRecursive<T extends AnyElement>(currentElement: T): T {
            if (isAnyElementWithChildren(currentElement) && currentElement.children != null) {
                const clonedChildIndex = currentElement
                    .children
                    .findIndex(child => child.id == element.id);

                if (clonedChildIndex !== -1) {
                    const clone = cloneElement(element);

                    const updatedChildren = [
                        ...currentElement.children,
                    ];
                    updatedChildren.splice(clonedChildIndex + 1, 0, clone);
                    dispatch(showSuccessSnackbar(`${generateComponentTitle(element)} wurde erfolgreich dupliziert.`));
                    return {
                        ...currentElement,
                        children: updatedChildren,
                    };
                } else {
                    return {
                        ...currentElement,
                        children: currentElement
                            .children
                            .map(child => cloneElementRecursive(child)),
                    };
                }
            } else {
                return currentElement;
            }
        }


        setDraftValue(cloneElementRecursive(draftValue));
    };

    const handleNavigateToElementEditor = (element: AnyElement, tab?: string | null) => {
        navigateToElementEditor(element.id, tab);
    };

    const handleHighlightElementInTree = (element: AnyElement) => {
        setHighlightElementId(element.id);
        setHighlightElementSignal((prev) => prev + 1);
        navigateToElementEditor(element.id, null);
    };

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    mb: 0.75,
                }}
            >
                <Typography variant="subtitle2">
                    {displayLabel}
                </Typography>

                <Button
                    variant="outlined"
                    size="small"
                    startIcon={<Edit/>}
                    sx={{
                        ml: 'auto',
                    }}
                    disabled={openOverride ? undefined : disabled}
                    onClick={openDialog}
                >
                    {
                        openOverride != null
                            ? 'Editor öffnen'
                            : 'Bearbeiten'
                    }
                </Button>
            </Box>

            <Box
                sx={{
                    border: '1px solid',
                    borderColor: error != null ? 'error.main' : 'divider',
                    borderRadius: 1,
                    px: 1.5,
                    py: 1.25,
                    minHeight: 52,
                    display: 'flex',
                    flexDirection: 'row',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: 0.5,
                }}
            >
                <Typography
                    variant="body2"
                    title={summary}
                    sx={{
                        color: value == null ? 'text.secondary' : 'text.primary',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                        lineHeight: 1.4,
                    }}
                >
                    {summary}
                </Typography>

                {
                    expectedRootTypeLabel != null &&
                    <Hint
                        summary={`Modellieren Sie eine UI-Struktur mit einem Element vom Typ ${expectedRootTypeLabel} als Basis.`}
                        detailsTitle="UI-Definition"
                        details={
                            <Typography>
                                Diese UI-Definition bildet ein Layout-Element vom
                                Typ <strong>{expectedRootTypeLabel}</strong> ab.
                                Über den Editor können Sie die Struktur der UI-Definition anpassen, um die
                                gewünschte Benutzeroberfläche zu erstellen.
                            </Typography>
                        }
                    />
                }
            </Box>

            {
                error == null &&
                hint != null &&
                <Typography
                    sx={{
                        mt: 1,
                        color: 'text.secondary',
                    }}
                    variant="caption"
                >
                    {hint}
                </Typography>
            }

            {
                error != null &&
                <Typography
                    sx={{
                        mt: 1,
                        color: 'error.main',
                    }}
                    variant="caption"
                >
                    {error}
                </Typography>
            }

            <Dialog
                open={showDraftDialog}
                onClose={() => {
                    void requestClose();
                }}
                fullWidth
                maxWidth="xl"
            >
                <DialogTitleWithClose
                    sx={{
                        boxShadow: '0px 1px 3px rgba(0, 0, 0, 0.2)',
                    }}
                    onClose={() => {
                        void requestClose();
                    }}
                >
                    {displayLabel}
                </DialogTitleWithClose>

                <DialogContent
                    sx={{
                        height: 'calc(100vh - 200px)',
                        overflowY: 'auto',
                        p: 0,
                    }}
                >
                    <Allotment vertical>
                        <Allotment>
                            <Allotment.Pane minSize={732}>
                                <Box
                                    sx={{
                                        p: 2,
                                        height: '100%',
                                        overflowY: 'auto',
                                    }}
                                >
                                    <ElementTreeInlineEditorContextProvider
                                        value={{
                                            deleteElement: handleDeleteElement,
                                            cloneElement: handleCloneElement,
                                            navigateToElementEditor: handleNavigateToElementEditor,
                                            highlightElementInTree: handleHighlightElementInTree,
                                            editable: !(disabled ?? false),
                                        }}
                                    >
                                        {
                                            emptyStateTarget != null ?
                                                <UiDefinitionEmptyState
                                                    target={emptyStateTarget}
                                                    onAdd={() => {
                                                        setOpenRootAddElementSignal((prev) => prev + 1);
                                                    }}
                                                    disabled={!canAddAtRoot}
                                                /> :
                                                <ElementDerivationContext
                                                    element={effectiveValue}
                                                    authoredElementValues={inputData}
                                                    onAuthoredElementValuesChange={setInputData}
                                                    highlightedElementId={hoveredTreeElementId}
                                                />
                                        }
                                    </ElementTreeInlineEditorContextProvider>
                                </Box>
                            </Allotment.Pane>
                            <Allotment.Pane
                                minSize={480}
                                preferredSize={480}
                            >
                                <ElementTree
                                    value={effectiveValue}
                                    onChange={setDraftValue}
                                    editable={!disabled}
                                    // The tree editor drawer needs to know the surrounding dialog layer.
                                    parentModalZIndex={theme.zIndex.modal}
                                    displayContext={displayContext}
                                    allowElementIdEditing={false}
                                    highlightElementId={highlightElementId}
                                    highlightElementSignal={highlightElementSignal}
                                    onHoveredElementIdChange={setHoveredTreeElementId}
                                    openRootAddElementSignal={openRootAddElementSignal}
                                />
                            </Allotment.Pane>
                        </Allotment>
                    </Allotment>
                </DialogContent>

                <DialogActions
                    sx={{
                        borderTop: '1px solid',
                        borderColor: 'divider',
                        display: 'flex',
                        width: '100%',
                        justifyContent: 'flex-start',
                        pt: 2,
                    }}
                >
                    <Button
                        variant="contained"
                        onClick={() => {
                            onChange(draftValue ?? undefined);
                            handleClose();
                        }}
                    >
                        Übernehmen
                    </Button>

                    <Button
                        onClick={() => {
                            // TODO
                        }}
                    >
                        Validierung testen
                    </Button>

                    <Button
                        sx={{
                            ml: 'auto',
                        }}
                        onClick={() => {
                            void requestClose();
                        }}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
