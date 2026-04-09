import React, {useRef, useState} from 'react';
import {Box, Button} from '@mui/material';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';
import {type ElementTreeProps} from './element-tree-props';
import {ElementTreeHeader} from '../element-tree-header/element-tree-header';
import {ElementTreeItemList} from '../element-tree-item-list/element-tree-item-list';
import {ElementType} from '../../data/element-type/element-type';
import {AddElementDialog} from '../../dialogs/add-element-dialog/add-element-dialog';
import {ElementTreeItem} from '../element-tree-item/element-tree-item';
import {isRootElement, type RootElement} from '../../models/elements/root-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import {type ElementTreeEntity} from './element-tree-entity';
import {StepElement} from '../../models/elements/steps/step-element';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {isLoadedForm, LoadedForm} from '../../slices/app-slice';
import {AnyFormElement} from '../../models/elements/form/any-form-element';
import {PresetVersion} from '../../models/entities/preset-version';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';

export function ElementTree<T extends ElementTreeEntity>(props: ElementTreeProps<T>) {
    const [showAddDialog, setShowAddDialog] = useState(false);

    const scrollContainerRef = useRef<HTMLDivElement>(undefined);

    const handleAddElement = (element: StepElement | AnyFormElement): void => {
        if (isLoadedForm(props.entity)) {
            props.onPatch({
                ...props.entity,

                version: {
                    ...props.entity.version,

                    rootElement: {
                        ...props.entity.version.rootElement,

                        children: [
                            ...props.entity.version.rootElement.children ?? [],
                            element,
                        ],
                    },
                },
            });
        } else {
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
        }
    };

    const handleAdd = (): void => {
        if (isLoadedForm(props.entity)) {
            handleAddElement(generateElementWithDefaultValues(ElementType.Step) as StepElement);
        } else {
            setShowAddDialog(true);
        }
    };

    const handleRootPatch = (updatedElement: Partial<T extends LoadedForm ? RootElement : GroupLayout>, updatedEntity: Partial<T>): void => {
        if (isLoadedForm(props.entity)) {
            const updatedLoadedForm = updatedEntity as Partial<LoadedForm>;
            props.onPatch({
                ...props.entity,
                ...updatedLoadedForm,

                version: {
                    ...props.entity.version,
                    ...updatedLoadedForm.version,

                    rootElement: {
                        ...props.entity.version.rootElement,
                        ...updatedLoadedForm.version?.rootElement,

                        ...updatedElement,
                    },
                },
            });
        } else {
            const updatedPreset = updatedEntity as Partial<PresetVersion>;
            props.onPatch({
                ...props.entity,
                ...updatedPreset,

                rootElement: {
                    ...props.entity.rootElement,
                    ...updatedPreset.rootElement,

                    ...updatedElement,
                },
            });
        }
    };

    const root = isLoadedForm(props.entity) ? props.entity.version.rootElement : props.entity.rootElement;

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
                        element={root as any}
                        onPatch={handleRootPatch}
                        editable={props.editable}
                        scope={props.scope}
                        scrollContainerRef={scrollContainerRef}
                    />

                    <DndProvider
                        backend={HTML5Backend}
                    >
                        <ElementTreeItemList
                            parents={[]}
                            entity={props.entity}
                            element={isLoadedForm(props.entity) ? props.entity.version.rootElement : props.entity.rootElement}
                            onPatch={(updatedElement, entity) => {
                                if (isLoadedForm(props.entity)) {
                                    const updatedEntity = entity as Partial<LoadedForm>;
                                    props.onPatch({
                                        ...props.entity,
                                        ...updatedEntity,

                                        version: {
                                            ...props.entity.version,
                                            ...updatedEntity.version,

                                            rootElement: {
                                                ...props.entity.version.rootElement,
                                                ...updatedEntity.version?.rootElement,
                                                ...updatedElement,
                                            },
                                        },
                                    });
                                } else {
                                    const updatedEntity = entity as Partial<PresetVersion>;
                                    props.onPatch({
                                        ...props.entity,
                                        ...updatedEntity,
                                        rootElement: {
                                            ...props.entity.rootElement,
                                            ...updatedEntity.rootElement,
                                            ...updatedElement,
                                        },
                                    });
                                }
                            }}
                            onMove={() => {
                                // Nothing to do
                            }}
                            editable={props.editable}
                            isRootList={true}
                            scope={props.scope}
                            enabledIdentityProviderInfos={props.enabledIdentityProviderInfos}
                        />
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
                                isLoadedForm(props.entity) ?
                                    'Neuen Abschnitt hinzufügen' :
                                    'Neues Element hinzufügen'
                            }
                        </Button>
                    }
                </Box>
            </Box>

            <AddElementDialog
                show={showAddDialog}
                parentType={isLoadedForm(props.entity) ? props.entity.version.rootElement.type : props.entity.rootElement.type}
                parentElement={root}
                onAddElement={(element) => {
                    handleAddElement(element as AnyFormElement);
                    setShowAddDialog(false);
                }}
                onClose={() => {
                    setShowAddDialog(false);
                }}
                displayContext={ElementDisplayContext.CitizenFacing}
            />
        </>
    );
}
