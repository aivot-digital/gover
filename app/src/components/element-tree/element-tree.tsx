import React, {useRef, useState} from 'react';
import {Box, Button} from '@mui/material';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';
import {type ElementTreeProps} from './element-tree-props';
import {ElementTreeHeader} from '../element-tree-header/element-tree-header';
import {ElementTreeItemList} from '../element-tree-item-list/element-tree-item-list';
import {ElementType} from '../../data/element-type/element-type';
import {AddElementDialog} from '../../dialogs/add-element-dialog/add-element-dialog';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementTreeItem} from '../element-tree-item/element-tree-item';
import {type RootElement} from '../../models/elements/root-element';
import {isForm} from '../../models/entities/form';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import {type ElementTreeEntity} from './element-tree-entity';
import {StepElement} from '../../models/elements/steps/step-element';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';

export function ElementTree<T extends ElementTreeEntity>(props: ElementTreeProps<T>) {
    const [showAddDialog, setShowAddDialog] = useState(false);

    const scrollContainerRef = useRef<HTMLDivElement>(undefined);

    const handleAddElement = (element: AnyElement): void => {
        props.onPatch({
            ...props.entity,
            rootElement: {
                ...props.entity.rootElement,
                children: [
                    ...props.entity.rootElement.children ?? [],
                    element,
                ],
            },
        });
    };

    const handleAdd = (): void => {
        if (isForm(props.entity)) {
            handleAddElement(generateElementWithDefaultValues(ElementType.Step) as StepElement);
        } else {
            setShowAddDialog(true);
        }
    };

    const handleRootPatch = (updatedElement: Partial<RootElement | GroupLayout>, updatedEntity: Partial<T>): void => {
        props.onPatch({
            ...props.entity,
            ...updatedEntity,
            rootElement: {
                ...props.entity.rootElement,
                ...updatedEntity.rootElement,
                ...updatedElement,
            },
        });
    };

    return (
        <>
            <Box
                ref={scrollContainerRef}
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    height: '100%',
                    overflowY: 'auto',
                }}
            >
                <Box>
                    <ElementTreeHeader
                        entity={props.entity}
                        element={props.entity.rootElement}
                        onPatch={handleRootPatch}
                        editable={props.editable}
                        scope={props.scope}
                        scrollContainerRef={scrollContainerRef}
                    />

                    <DndProvider
                        backend={HTML5Backend}
                    >
                        {
                            isForm(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.rootElement]}
                                entity={props.entity}
                                element={props.entity.rootElement.introductionStep!}
                                disableDrag={true}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isForm(props.entity)) {
                                        props.onPatch({
                                            ...props.entity,
                                            ...updatedEntity,
                                            rootElement: {
                                                ...props.entity.rootElement,
                                                ...updatedEntity.rootElement,
                                                introductionStep: {
                                                    ...props.entity.rootElement.introductionStep,
                                                    ...updatedEntity.rootElement?.introductionStep,
                                                    ...updatedElement,
                                                },
                                            },
                                        });
                                    }
                                }}
                                onMove={() => {
                                    // Ignore move
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                                editable={props.editable}
                                scope={props.scope}
                                enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
                            />
                        }

                        <ElementTreeItemList
                            parents={[]}
                            entity={props.entity}
                            element={props.entity.rootElement}
                            onPatch={(updatedElement, updatedEntity) => {
                                props.onPatch({
                                    ...props.entity,
                                    ...updatedEntity,
                                    rootElement: {
                                        ...props.entity.rootElement,
                                        ...updatedEntity.rootElement,
                                        ...updatedElement,
                                    },
                                });
                            }}
                            onMove={() => {
                                // Nothing to do
                            }}
                            editable={props.editable}
                            isRootList={true}
                            scope={props.scope}
                            enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
                        />

                        {
                            isForm(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.rootElement]}
                                entity={props.entity}
                                element={props.entity.rootElement.summaryStep!}
                                disableDrag={true}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isForm(props.entity)) {
                                        props.onPatch({
                                            ...props.entity,
                                            ...updatedEntity,
                                            rootElement: {
                                                ...props.entity.rootElement,
                                                ...updatedEntity.rootElement,
                                                summaryStep: {
                                                    ...props.entity.rootElement.summaryStep,
                                                    ...updatedEntity.rootElement?.summaryStep,
                                                    ...updatedElement,
                                                },
                                            },
                                        });
                                    }
                                }}
                                onMove={() => {
                                    // Ignore move
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                                editable={props.editable}
                                scope={props.scope}
                                enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
                            />
                        }

                        {
                            isForm(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.rootElement]}
                                entity={props.entity}
                                element={props.entity.rootElement.submitStep!}
                                disableDrag={true}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isForm(props.entity)) {
                                        props.onPatch({
                                            ...props.entity,
                                            ...updatedEntity,
                                            rootElement: {
                                                ...props.entity.rootElement,
                                                ...updatedEntity.rootElement,
                                                submitStep: {
                                                    ...props.entity.rootElement.submitStep,
                                                    ...updatedEntity.rootElement?.submitStep,
                                                    ...updatedElement,
                                                },
                                            },
                                        });
                                    }
                                }}
                                onMove={() => {
                                    // Ignore move
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                                editable={props.editable}
                                scope={props.scope}
                                enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
                            />
                        }
                    </DndProvider>
                </Box>

                <Box>
                    {
                        props.editable &&
                        <Button
                            onClick={handleAdd}
                            variant="outlined"
                            size="small"
                            fullWidth
                            endIcon={<AddCircleOutlineOutlinedIcon sx={{transform: 'translateY(-1px)'}} />}
                            sx={{
                                mt: 4,
                                mb: 3,
                            }}
                        >
                            {
                                isForm(props.entity) ?
                                    'Neuen Abschnitt hinzufügen' :
                                    'Neues Element hinzufügen'
                            }
                        </Button>
                    }
                </Box>
            </Box>

            <AddElementDialog
                show={showAddDialog}
                parentType={props.entity.rootElement.type}
                onAddElement={(element) => {
                    handleAddElement(element);
                    setShowAddDialog(false);
                }}
                onClose={() => {
                    setShowAddDialog(false);
                }}
            />
        </>
    );
}
