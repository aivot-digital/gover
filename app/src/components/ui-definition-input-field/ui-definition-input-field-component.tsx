import React, {useMemo, useState} from 'react';
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
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
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
}

function buildSummary(value?: UiDefinitionInputFieldElementItem | null): string {
    if (value == null) {
        return 'Keine UI-Definition konfiguriert';
    }

    const elementCount = flattenElements(value).length - 1; // Subtract 1 to compensate for the root element of the structure.
    const countLabel = `${humanizeNumberCapitalized(elementCount)} Element${elementCount === 1 ? '' : 'e'} enthalten`;

    return `${countLabel}`;
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
    } = props;

    const displayLabel = `${label}${required ? ' *' : ''}`;
    const [showDraftDialog, setShowDraftDialog] = useState<boolean>(false);
    const [draftValue, setDraftValue] = useState<UiDefinitionInputFieldElementItem | null>(null);
    const [inputData, setInputData] = useState<AuthoredElementValues>({});

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

    const handleClose = () => {
        setShowDraftDialog(false);
        setTimeout(() => {
            setDraftValue(generateElementWithDefaultValues(expectedRootType ?? ElementType.GroupLayout) as UiDefinitionInputFieldElementItem);
            setInputData({});
        }, 300);
    };

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
                        ...currentElement.children
                    ];
                    updatedChildren.splice(clonedChildIndex, 0, clone);
                    dispatch(showSuccessSnackbar(`${generateComponentTitle(element)} wurde erfolgreich dupliziert.`));
                    return {
                        ...currentElement,
                        children: updatedChildren,
                    }
                } else {
                    return {
                        ...currentElement,
                        children: currentElement
                            .children
                            .map(child => cloneElementRecursive(child)),
                    }
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
                    disabled={disabled}
                    onClick={() => {
                        setDraftValue(value ?? defaultValue);
                        setShowDraftDialog(true);
                    }}
                >
                    Bearbeiten
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
                            <>
                                <Typography>
                                    Diese UI-Definition bildet ein Layout-Element vom
                                    Typ <strong>{expectedRootTypeLabel}</strong> ab.
                                    Über den Editor können Sie die Struktur der UI-Definition anpassen, um die
                                    gewünschte Benutzeroberfläche zu erstellen.
                                </Typography>
                                <Typography
                                    mt={1}
                                >
                                    Als Basis beziehungsweise Wurzel wird ein Element vom
                                    Typ <strong>{expectedRootTypeLabel}</strong> verwendet.
                                    Sie können beliebig viele weitere Elemente als Kind-Elemente der Basis hinzufüge um
                                    die UI-Definition zu erweitern.
                                </Typography>
                            </>
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
                onClose={handleClose}
                fullWidth
                maxWidth="xl"
            >
                <DialogTitleWithClose
                    sx={{
                        boxShadow: '0px 1px 3px rgba(0, 0, 0, 0.2)',
                    }}
                    onClose={handleClose}
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
                                            editable: !(disabled ?? false)
                                        }}
                                    >
                                        <ElementDerivationContext
                                            element={draftValue ?? value ?? defaultValue}
                                            authoredElementValues={inputData}
                                            onAuthoredElementValuesChange={setInputData}
                                        />
                                    </ElementTreeInlineEditorContextProvider>
                                </Box>
                            </Allotment.Pane>
                            <Allotment.Pane
                                minSize={480}
                                preferredSize={480}
                            >
                                <ElementTree
                                    value={draftValue ?? value ?? defaultValue}
                                    onChange={setDraftValue}
                                    editable={!disabled}
                                    // The tree editor drawer needs to know the surrounding dialog layer.
                                    parentModalZIndex={theme.zIndex.modal}
                                    displayContext={displayContext}
                                    allowElementIdEditing={false}
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
                        onClick={handleClose}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
