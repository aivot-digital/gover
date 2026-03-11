import {HTML5Backend} from 'react-dnd-html5-backend';
import {ElementTreeItem} from '../element-tree-item/element-tree-item';
import {ElementTreeItemList} from '../element-tree-item-list/element-tree-item-list';
import {DndProvider} from 'react-dnd';
import React, {useState} from 'react';
import {AnyElement} from '../../models/elements/any-element';
import {ElementTreeEntity} from './element-tree-entity';
import {isRootElement} from '../../models/elements/root-element';
import type {ElementTreeScope} from './element-tree-scope';
import {AnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {IdentityProviderInfo} from '../../modules/identity/models/identity-provider-info';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {Box, Paper, Typography} from '@mui/material';
import {AddElementDialog} from '../../dialogs/add-element-dialog/add-element-dialog';
import {Actions} from '../actions/actions';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';

interface ElementTreeTreeProps<T extends AnyElementWithChildren> {
    label: string;
    hint?: string;

    value: T;
    onChange: (value: T) => void;

    limitElementTypes?: ElementType[];

    entity: ElementTreeEntity;
    editable: boolean;
    scope: ElementTreeScope;
    enabledIdentityProviderInfos: IdentityProviderInfo[];
}

export function ElementTreeTree<T extends AnyElementWithChildren>(props: ElementTreeTreeProps<T>) {
    const {
        label,
        hint,

        value,
        onChange,

        limitElementTypes,

        entity,
        editable,
        scope,
        enabledIdentityProviderInfos,
    } = props;

    const [showAddDialog, setShowAddDialog] = useState(false);

    return (
        <Paper
            variant="outlined"
            sx={{
                p: 2,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    mb: 2,
                }}
            >
                <Box>
                    <Typography
                        variant="h6"
                        component="div"
                    >
                        {label}
                    </Typography>
                    {
                        hint != null &&
                        <Typography
                            variant="caption"
                            component="div"
                        >
                            {hint}
                        </Typography>
                    }
                </Box>

                <Actions
                    actions={[
                        {
                            icon: <AddCircleOutlineOutlinedIcon />,
                            tooltip: 'Element hinzufügen',
                            onClick: () => setShowAddDialog(true),
                        },
                    ]}
                />
            </Box>

            <AddElementDialog
                show={showAddDialog}
                parentType={ElementType.GroupLayout}
                onAddElement={elem => {
                    onChange({
                        ...value,
                        children: [
                            ...(value.children ?? []),
                            elem,
                        ],
                    });
                    setShowAddDialog(false);
                }}
                onClose={() => {
                    setShowAddDialog(false);
                }}
                hidePresets={true}
                hideGoverStore={true}
                limitElementTypes={limitElementTypes}
            />

            <DndProvider
                backend={HTML5Backend}
            >
                <ElementTreeItemList
                    parents={[]}
                    entity={entity}
                    element={value}
                    onPatch={(updatedElement) => {
                        onChange({
                            ...value,
                            ...updatedElement,
                        });
                    }}
                    onMove={() => {
                        // Nothing to do
                    }}
                    editable={editable}
                    isRootList={true}
                    scope={scope}
                    enabledIdentityProviderInfos={enabledIdentityProviderInfos}
                    limitElementTypes={limitElementTypes}
                />
            </DndProvider>
        </Paper>
    );
}