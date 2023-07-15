import React, { useState } from 'react';
import { Box, Button, Divider, IconButton, Tooltip, Typography } from '@mui/material';
import ProjectPackage from '../../../package.json';
import GitInfo from '../../git-info.json';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { type ElementTreeProps } from './element-tree-props';
import { ElementTreeHeader } from './components/element-tree-header/element-tree-header';
import { ElementTreeItemList } from './components/element-tree-item-list/element-tree-item-list';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faClose, faPlusCircle } from '@fortawesome/pro-light-svg-icons';
import { ElementType } from '../../data/element-type/element-type';
import { AddElementDialog } from '../../dialogs/add-element-dialog/add-element-dialog';
import { type AnyElement } from '../../models/elements/any-element';
import { type AnyElementWithChildren } from '../../models/elements/any-element-with-children';
import { ElementTreeItem } from './components/element-tree-item/element-tree-item';
import { isRootElement, type RootElement } from '../../models/elements/root-element';
import { generateElementIdForType } from '../../utils/id-utils';
import { SearchInput } from '../search-input/search-input';
import { useAppSelector } from '../../hooks/use-app-selector';
import { selectTreeElementSearch, setExpandElementTree, setTreeElementSearch } from '../../slices/admin-settings-slice';
import { useAppDispatch } from '../../hooks/use-app-dispatch';

export function ElementTree<T extends AnyElementWithChildren>(props: ElementTreeProps<T>): JSX.Element {
    const dispatch = useAppDispatch();

    const [showAddDialog, setShowAddDialog] = useState(false);
    const [showSearch, setShowSearch] = useState(false);

    const treeElementSearch = useAppSelector(selectTreeElementSearch);

    const handleAddElement = (element: AnyElement): void => {
        props.onPatch({
            children: [
                ...(props.element as any).children,
                element,
            ],
        } as any);
    };

    const handleAdd = (): void => {
        if (isRootElement(props.element)) {
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

    const handleToggleSearch = (): void => {
        if (showSearch) {
            dispatch(setTreeElementSearch(undefined));
        } else {
            dispatch(setExpandElementTree('expanded'));
        }
        setShowSearch(!showSearch);
    };

    return (
        <>
            <Box
                sx={ {
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    height: '100%',
                } }
            >
                <Box>
                    <ElementTreeHeader
                        element={ props.element }
                        onPatch={ props.onPatch }
                        onToggleSearch={ handleToggleSearch }
                        editable={ props.editable }
                    />

                    {
                        showSearch &&
                        <Box
                            sx={ {
                                display: 'flex',
                            } }
                        >
                            <Tooltip title="Suche schließen">
                                <IconButton
                                    onClick={ handleToggleSearch }
                                    size="small"
                                >
                                    <FontAwesomeIcon icon={ faClose }/>
                                </IconButton>
                            </Tooltip>

                            <Box
                                sx={ {
                                    flex: 1,
                                } }
                            >
                                <SearchInput
                                    value={ treeElementSearch ?? '' }
                                    onChange={ (val) => {
                                        dispatch(setTreeElementSearch(val));
                                    } }
                                    placeholder="Element Suchen"
                                />
                            </Box>
                        </Box>
                    }

                    <Divider
                        sx={ {
                            mb: 2,
                            borderColor: '#16191F',
                        } }
                    />

                    <DndProvider
                        backend={ HTML5Backend }
                    >
                        {
                            isRootElement(props.element) &&
                            <ElementTreeItem
                                parents={ [props.element] }
                                element={ props.element.introductionStep }
                                onPatch={ (patch) => {
                                    if (isRootElement(props.element)) {
                                        const rootPatch: Partial<RootElement> = {
                                            introductionStep: {
                                                ...props.element.introductionStep,
                                                ...patch,
                                            },
                                        };
                                        props.onPatch(rootPatch as Partial<T>);
                                    }
                                } }
                                onDelete={ () => {
                                    // Ignore delete
                                } }
                                onClone={ () => {
                                    // Ignore clone
                                } }
                                editable={ props.editable }
                            />
                        }

                        <ElementTreeItemList
                            parents={ [] }
                            element={ props.element }
                            onPatch={ props.onPatch }
                            isRootList
                            editable={ props.editable }
                        />

                        {
                            isRootElement(props.element) &&
                            <ElementTreeItem
                                parents={ [props.element] }
                                element={ props.element.summaryStep }
                                onPatch={ (patch) => {
                                    if (isRootElement(props.element)) {
                                        const rootPatch: Partial<RootElement> = {
                                            summaryStep: {
                                                ...props.element.summaryStep,
                                                ...patch,
                                            },
                                        };
                                        props.onPatch(rootPatch as Partial<T>);
                                    }
                                } }
                                onDelete={ () => {
                                    // Ignore delete
                                } }
                                onClone={ () => {
                                    // Ignore clone
                                } }
                                editable={ props.editable }
                            />
                        }

                        {
                            isRootElement(props.element) &&
                            <ElementTreeItem
                                parents={ [props.element] }
                                element={ props.element.submitStep }
                                onPatch={ (patch) => {
                                    if (isRootElement(props.element)) {
                                        const rootPatch: Partial<RootElement> = {
                                            submitStep: {
                                                ...props.element.submitStep,
                                                ...patch,
                                            },
                                        };
                                        props.onPatch(rootPatch as Partial<T>);
                                    }
                                } }
                                onDelete={ () => {
                                    // Ignore delete
                                } }
                                onClone={ () => {
                                    // Ignore clone
                                } }
                                editable={ props.editable }
                            />
                        }
                    </DndProvider>
                </Box>

                <Box>
                    {
                        props.editable &&
                        <Button
                            onClick={ handleAdd }
                            variant="outlined"
                            size="small"
                            fullWidth
                            endIcon={ <span
                                style={ {
                                    fontSize: '16px',
                                    transform: 'translateY(1px)',
                                } }
                            ><FontAwesomeIcon icon={ faPlusCircle }/></span> }
                            sx={ {
                                mt: 4,
                            } }
                        >
                            {
                                isRootElement(props.element) ? 'Neuen Abschnitt hinzufügen' : 'Neues Element hinzufügen'
                            }
                        </Button>
                    }

                    <Divider
                        sx={ {
                            mt: 3,
                            mb: 2,
                            borderColor: '#16191F',
                        } }
                    />

                    <Box
                        sx={ {
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '20px',
                        } }
                    >
                        <Typography
                            variant="body1"
                            sx={ {
                                color: '#BFBFBF',
                            } }
                        >
                            &copy; { new Date(GitInfo.date).getFullYear() } Aivot
                        </Typography>

                        <Typography
                            variant="body1"
                            sx={ {
                                color: '#BFBFBF',
                            } }
                        >
                            Gover Version { ProjectPackage.version }
                        </Typography>
                    </Box>
                </Box>
            </Box>

            {
                showAddDialog &&
                <AddElementDialog
                    parentType={ props.element.type }
                    onAddElement={ (element) => {
                        handleAddElement(element);
                        setShowAddDialog(false);
                    } }
                    onClose={ () => {
                        setShowAddDialog(false);
                    } }
                />
            }
        </>
    );
}
