import React, {useState} from 'react';
import {Box, Button, Divider, Typography} from '@mui/material';
import ProjectPackage from '../../../package.json';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';
import {ElementTreeProps} from './element-tree-props';
import {ElementTreeHeader} from './components/element-tree-header/element-tree-header';
import {ElementTreeItemList} from './components/element-tree-item-list/element-tree-item-list';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPlusCircle} from '@fortawesome/pro-light-svg-icons';
import {ElementType} from '../../data/element-type/element-type';
import {AddElementDialog} from '../../dialogs/add-element-dialog/add-element-dialog';
import {AnyElement} from '../../models/elements/any-element';
import {AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {ElementTreeItem} from './components/element-tree-item/element-tree-item';
import {isRootElement, RootElement} from '../../models/elements/root-element';
import {generateElementIdForType} from "../../utils/id-utils";

export function ElementTree<T extends AnyElementWithChildren>(props: ElementTreeProps<T>) {
    const [showAddDialog, setShowAddDialog] = useState(false);

    const handleAddElement = (element: AnyElement) => {
        props.onPatch({
            children: [
                ...(props.element as any).children,
                element,
            ]
        } as any);
    };

    const handleAdd = () => {
        if (isRootElement(props.element)) {
            handleAddElement({
                id: generateElementIdForType(ElementType.Step),
                type: ElementType.Step,
                children: [],
            });
        } else {
            setShowAddDialog(true);
        }
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
                        element={props.element}
                        onPatch={props.onPatch}
                    />

                    <Divider
                        sx={{
                            mb: 2,
                            borderColor: '#16191F',
                        }}
                    />

                    <DndProvider
                        backend={HTML5Backend}
                    >
                        {
                            isRootElement(props.element) &&
                            <ElementTreeItem
                                element={props.element.introductionStep}
                                onPatch={patch => {
                                    if (isRootElement(props.element)) {
                                        const rootPatch: Partial<RootElement> = {
                                            introductionStep: {
                                                ...props.element.introductionStep,
                                                ...patch,
                                            }
                                        }
                                        props.onPatch(rootPatch as Partial<T>);
                                    }
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                            />
                        }

                        <ElementTreeItemList
                            element={props.element}
                            onPatch={props.onPatch}
                            isRootList
                        />

                        {
                            isRootElement(props.element) &&
                            <ElementTreeItem
                                element={props.element.summaryStep}
                                onPatch={patch => {
                                    if (isRootElement(props.element)) {
                                        const rootPatch: Partial<RootElement> = {
                                            summaryStep: {
                                                ...props.element.summaryStep,
                                                ...patch,
                                            }
                                        }
                                        props.onPatch(rootPatch as Partial<T>);
                                    }
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                            />
                        }

                        {
                            isRootElement(props.element) &&
                            <ElementTreeItem
                                element={props.element.submitStep}
                                onPatch={patch => {
                                    if (isRootElement(props.element)) {
                                        const rootPatch: Partial<RootElement> = {
                                            submitStep: {
                                                ...props.element.submitStep,
                                                ...patch,
                                            }
                                        }
                                        props.onPatch(rootPatch as Partial<T>);
                                    }
                                }}
                                onDelete={() => {
                                    // Ignore delete
                                }}
                                onClone={() => {
                                    // Ignore clone
                                }}
                            />
                        }
                    </DndProvider>
                </Box>

                <Box>
                    <Button
                        onClick={handleAdd}
                        variant="outlined"
                        size="small"
                        fullWidth
                        endIcon={<span
                            style={{
                                fontSize: '16px',
                                transform: 'translateY(1px)'
                            }}
                        ><FontAwesomeIcon icon={faPlusCircle}/></span>}
                        sx={{mt: 4}}
                    >
                        {
                            isRootElement(props.element) ? 'Neuen Abschnitt hinzufügen' : 'Neues Element hinzufügen'
                        }
                    </Button>

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
                            marginBottom: '20px'
                        }}
                    >
                        <Typography
                            variant="body1"
                            sx={{
                                color: '#BFBFBF',
                            }}
                        >
                            &copy; 2022 Aivot
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

            {
                showAddDialog &&
                <AddElementDialog
                    parentType={props.element.type}
                    onAddElement={element => {
                        handleAddElement(element);
                        setShowAddDialog(false);
                    }}
                    onClose={() => {
                        setShowAddDialog(false);
                    }}
                />
            }
        </>
    );
}
