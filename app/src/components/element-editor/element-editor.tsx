import {Box, Drawer} from '@mui/material';
import React, {useMemo, useState} from 'react';
import {DefaultTabs} from './default-tabs';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectUseTestMode} from '../../slices/admin-settings-slice';
import {ElementEditorTabs} from '../element-editor-tabs/element-editor-tabs';
import {ElementEditorContent} from '../element-editor-content/element-editor-content';
import {ElementEditorActions} from '../element-editor-actions/element-editor-actions';
import {type ElementEditorProps} from './element-editor-props';

import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {type AnyElement} from '../../models/elements/any-element';
import ProjectPackage from '../../../package.json';
import Editors from '../../editors';
import {AddPresetDialog} from '../../dialogs/preset-dialogs/add-preset-dialog/add-preset-dialog';
import {type GroupLayout, isGroupLayout, isPresetGroupLayout} from '../../models/elements/form/layout/group-layout';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {useChangeBlocker} from "../../hooks/use-change-blocker";
import {useConfirm} from "../../providers/confirm-provider";

export function ElementEditor<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorProps<T, E>): JSX.Element | null {
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const testMode = useAppSelector(selectUseTestMode);

    const [updatedElement, setUpdatedElement] = useState<T>();
    const [updatedEntity, setUpdatedEntity] = useState<E>();
    const [currentTab, setCurrentTab] = useState(testMode ? DefaultTabs.test : DefaultTabs.properties);
    const [showCreatePresetDialog, setShowCreatePresetDialog] = useState(false);

    const initialState = useMemo(() => ({ element: props.element, entity: props.entity }), [props.element, props.entity]);
    const currentState = useMemo(() => ({ element: updatedElement ?? props.element, entity: updatedEntity ?? props.entity }), [updatedElement, updatedEntity, props.element, props.entity]);

    const changeBlocker = useChangeBlocker(initialState, currentState);

    const handleSave = (): void => {
        let elementToSave: Partial<T> = {};
        if (updatedElement != null) {
            elementToSave = {
                ...updatedElement,
                appVersion: ProjectPackage.version,
            };
        }

        let entityToSave: Partial<E> = {};
        if (updatedEntity != null) {
            entityToSave = {
                ...updatedEntity,
            };

            if (props.element.testProtocolSet != null) {
                elementToSave.testProtocolSet = undefined;
            }
        }

        props.onSave(elementToSave, entityToSave);
    };

    const handleSetCurrentTab = (newTab: string): void => {
        setCurrentTab(newTab);
    };

    const handleShowPresetDialog = (): void => {
        setShowCreatePresetDialog(true);
    };

    const handleClosePresetDialog = (): void => {
        setShowCreatePresetDialog(false);
    };

    const handleChange = (update: Partial<T>): void => {
        setUpdatedElement({
            ...(updatedElement ?? props.element),
            ...update,
        });
    };

    const handleEntityChange = (update: Partial<E>): void => {
        setUpdatedEntity({
            ...(updatedEntity ?? props.entity),
            ...update,
        });
    };

    const handleClose = async (): Promise<void> => {
        if (updatedElement != null || updatedEntity != null) {

            const confirmed = await showConfirm({
                title: "Änderungen verwerfen?",
                confirmButtonText: "Ja, verwerfen",
                children: (
                    <div>
                        Möchten Sie die Änderungen an diesem{" "}
                        {updatedElement != null ? "Element" : "Formular"} wirklich verwerfen? Diese Aktion kann nicht rückgängig gemacht werden.
                    </div>
                ),
            });

            if (confirmed) {
                props.onCancel();
            }
        } else {
            props.onCancel();
        }
    };

    const editor = Editors[(updatedElement ?? props.element).type];
    const additionalTabs = editor?.additionalTabs ?? [];

    return (
        <Drawer
            anchor="right"
            open={true}
            PaperProps={{
                sx: {
                    width: '66%',
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

                <ElementEditorTabs
                    component={updatedElement ?? props.element}
                    additionalTabs={additionalTabs}
                    currentTab={currentTab}
                    onTabChange={handleSetCurrentTab}
                    scope={props.scope}
                    rootEditor={props.rootEditor}
                />

                <Box
                    sx={{
                        flex: '1',
                        overflowY: 'scroll',
                        borderTop: '1px solid #E0E0E0',
                        pb: 4,
                    }}
                >
                    <ElementEditorContent
                        parents={props.parents}
                        element={updatedElement ?? props.element}
                        entity={updatedEntity ?? props.entity}
                        currentTab={currentTab}
                        additionalTabs={additionalTabs}
                        onChange={handleChange}
                        onChangeEntity={handleEntityChange}
                        editable={props.editable}
                        scope={props.scope}
                        rootEditor={props.rootEditor}
                        hasChanges={updatedElement != null || updatedEntity != null}
                    />
                </Box>

                <ElementEditorActions
                    onSave={handleSave}
                    onCancel={handleClose}
                    onDelete={props.onDelete}
                    onSaveAsPreset={isGroupLayout(updatedElement ?? props.element) && !isPresetGroupLayout(updatedElement ?? props.element) ? handleShowPresetDialog : undefined}
                    onClone={props.onClone}
                    editable={props.editable}
                    hasChanges={updatedElement != null || updatedEntity != null}
                />

                <AddPresetDialog
                    open={showCreatePresetDialog}
                    onAdded={() => {
                        handleClosePresetDialog();
                        dispatch(showSuccessSnackbar('Vorlage erfolgreich gespeichert'));
                    }}
                    onClose={handleClosePresetDialog}
                    root={{
                        ...props.element,
                        storeLink: null,
                    } as GroupLayout}
                />
            </Box>

            {changeBlocker.dialog}
        </Drawer>
    );
}
