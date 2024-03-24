import React, {useState} from 'react';
import {Box, Button, Divider, Typography} from '@mui/material';
import ProjectPackage from '../../../package.json';
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
import {generateElementIdForType} from '../../utils/id-utils';
import {isForm} from '../../models/entities/form';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import {type ElementTreeEntity} from './element-tree-entity';
import {AppConfig} from "../../app-config";

export function ElementTree<T extends ElementTreeEntity>(props: ElementTreeProps<T>): JSX.Element {
    const [showAddDialog, setShowAddDialog] = useState(false);

    const handleAddElement = (element: AnyElement): void => {
        props.onPatch({
            ...props.entity,
            root: {
                ...props.entity.root,
                children: [
                    ...props.entity.root.children,
                    element,
                ],
            },
        });
    };

    const handleAdd = (): void => {
        if (isForm(props.entity)) {
            handleAddElement({
                id: generateElementIdForType(ElementType.Step),
                type: ElementType.Step,
                appVersion: ProjectPackage.version,
                children: [],
            });
        } else {
            setShowAddDialog(true);
        }
    };

    const handleRootPatch = (updatedElement: Partial<RootElement | GroupLayout>, updatedEntity: Partial<T>): void => {
        props.onPatch({
            ...props.entity,
            ...updatedEntity,
            root: {
                ...props.entity.root,
                ...updatedEntity.root,
                ...updatedElement,
            },
        });
    };



    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    height: '100%',
                }}
            >
                <Box>
                    <ElementTreeHeader
                        entity={props.entity}
                        element={props.entity.root}
                        onPatch={handleRootPatch}
                        editable={props.editable}
                        scope={props.scope}
                    />



                    <DndProvider
                        backend={HTML5Backend}
                    >
                        {
                            isForm(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.root]}
                                entity={props.entity}
                                element={props.entity.root.introductionStep}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isForm(props.entity)) {
                                        props.onPatch({
                                            ...props.entity,
                                            ...updatedEntity,
                                            root: {
                                                ...props.entity.root,
                                                ...updatedEntity.root,
                                                introductionStep: {
                                                    ...props.entity.root.introductionStep,
                                                    ...updatedEntity.root?.introductionStep,
                                                    ...updatedElement,
                                                },
                                            },
                                        });
                                    }
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                                editable={props.editable}
                                scope={props.scope}
                            />
                        }

                        <ElementTreeItemList
                            parents={[]}
                            entity={props.entity}
                            element={props.entity.root}
                            onPatch={(updatedElement, updatedEntity) => {
                                props.onPatch({
                                    ...props.entity,
                                    ...updatedEntity,
                                    root: {
                                        ...props.entity.root,
                                        ...updatedEntity.root,
                                        ...updatedElement,
                                    },
                                });
                            }}
                            editable={props.editable}
                            isRootList={true}
                            scope={props.scope}
                        />

                        {
                            isForm(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.root]}
                                entity={props.entity}
                                element={props.entity.root.summaryStep}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isForm(props.entity)) {
                                        props.onPatch({
                                            ...props.entity,
                                            ...updatedEntity,
                                            root: {
                                                ...props.entity.root,
                                                ...updatedEntity.root,
                                                summaryStep: {
                                                    ...props.entity.root.summaryStep,
                                                    ...updatedEntity.root?.summaryStep,
                                                    ...updatedElement,
                                                },
                                            },
                                        });
                                    }
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                                editable={props.editable}
                                scope={props.scope}
                            />
                        }

                        {
                            isForm(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.root]}
                                entity={props.entity}
                                element={props.entity.root.submitStep}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isForm(props.entity)) {
                                        props.onPatch({
                                            ...props.entity,
                                            ...updatedEntity,
                                            root: {
                                                ...props.entity.root,
                                                ...updatedEntity.root,
                                                submitStep: {
                                                    ...props.entity.root.submitStep,
                                                    ...updatedEntity.root?.submitStep,
                                                    ...updatedElement,
                                                },
                                            },
                                        });
                                    }
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                                editable={props.editable}
                                scope={props.scope}
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
                            endIcon={<AddCircleOutlineOutlinedIcon sx={{transform: 'translateY(-1px)'}}/>}
                            sx={{
                                mt: 4,
                            }}
                        >
                            {
                                isForm(props.entity) ?
                                    'Neuen Abschnitt hinzufügen' :
                                    'Neues Element hinzufügen'
                            }
                        </Button>
                    }

                    <Divider
                        sx={{
                            mt: 3,
                            mb: 2,
                            borderColor: '#16191F',
                        }}
                    />

                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '20px',
                        }}
                    >
                        <Typography
                            variant="body1"
                            sx={{
                                color: '#BFBFBF',
                            }}
                        >
                            &copy; {new Date(AppConfig.date).getFullYear()} Aivot
                        </Typography>

                        <Typography
                            variant="body1"
                            sx={{
                                color: '#BFBFBF',
                            }}
                        >
                            Gover Version {ProjectPackage.version}
                        </Typography>
                    </Box>
                </Box>
            </Box>

            <AddElementDialog
                show={showAddDialog}
                parentType={props.entity.root.type}
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
