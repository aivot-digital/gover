import React, {useState} from 'react';
import {Box, Button, Divider, IconButton, Tooltip, Typography} from '@mui/material';
import ProjectPackage from '../../../package.json';
import GitInfo from '../../git-info.json';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';
import {type ElementTreeProps} from './element-tree-props';
import {ElementTreeHeader} from './components/element-tree-header/element-tree-header';
import {ElementTreeItemList} from './components/element-tree-item-list/element-tree-item-list';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faClose} from '@fortawesome/pro-light-svg-icons';
import {ElementType} from '../../data/element-type/element-type';
import {AddElementDialog} from '../../dialogs/add-element-dialog/add-element-dialog';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementTreeItem} from './components/element-tree-item/element-tree-item';
import {type RootElement} from '../../models/elements/root-element';
import {generateElementIdForType} from '../../utils/id-utils';
import {SearchInput} from '../search-input/search-input';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectTreeElementSearch, setExpandElementTree, setTreeElementSearch} from '../../slices/admin-settings-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {type Application, isApplication} from '../../models/entities/application';
import {type Preset} from '../../models/entities/preset';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';

export function ElementTree<T extends Application | Preset>(props: ElementTreeProps<T>): JSX.Element {
    const dispatch = useAppDispatch();

    const [showAddDialog, setShowAddDialog] = useState(false);
    const [showSearch, setShowSearch] = useState(false);

    const treeElementSearch = useAppSelector(selectTreeElementSearch);

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
        if (isApplication(props.entity)) {
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
                        onToggleSearch={handleToggleSearch}
                        editable={props.editable}
                    />

                    {
                        showSearch &&
                        <Box
                            sx={{
                                display: 'flex',
                            }}
                        >
                            <Tooltip title="Suche schließen">
                                <IconButton
                                    onClick={handleToggleSearch}
                                    size="small"
                                >
                                    <FontAwesomeIcon icon={faClose}/>
                                </IconButton>
                            </Tooltip>

                            <Box
                                sx={{
                                    flex: 1,
                                }}
                            >
                                <SearchInput
                                    value={treeElementSearch ?? ''}
                                    onChange={(val) => {
                                        dispatch(setTreeElementSearch(val));
                                    }}
                                    placeholder="Element Suchen"
                                />
                            </Box>
                        </Box>
                    }

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
                            isApplication(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.root]}
                                entity={props.entity}
                                element={props.entity.root.introductionStep}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isApplication(props.entity)) {
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
                        />

                        {
                            isApplication(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.root]}
                                entity={props.entity}
                                element={props.entity.root.summaryStep}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isApplication(props.entity)) {
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
                            />
                        }

                        {
                            isApplication(props.entity) &&
                            <ElementTreeItem
                                parents={[props.entity.root]}
                                entity={props.entity}
                                element={props.entity.root.submitStep}
                                onPatch={(updatedElement, updatedEntity) => {
                                    if (isApplication(props.entity)) {
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
                                isApplication(props.entity) ?
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
                            &copy; {new Date(GitInfo.date).getFullYear()} Aivot
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
