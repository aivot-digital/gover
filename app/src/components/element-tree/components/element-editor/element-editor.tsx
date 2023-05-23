import {Box, Drawer,} from '@mui/material';
import React, {useState} from 'react';
import {PresetsService} from '../../../../services/presets.service';
import {AddPresetDialog} from '../../../../dialogs/add-preset/add-preset.dialog';
import {DefaultTabs} from './data/default-tabs';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectUseTestMode} from '../../../../slices/admin-settings-slice';
import {ElementEditorTabs} from './components/element-editor-tabs';
import {ElementEditorContent} from './components/element-editor-content/element-editor-content';
import {ElementEditorActions} from './components/element-editor-actions/element-editor-actions';
import {ElementEditorProps} from './element-editor-props';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {AnyElement} from '../../../../models/elements/any-element';
import {ElementType} from '../../../../data/element-type/element-type';
import ProjectPackage from '../../../../../package.json';
import Editors from "../../../../editors";

export function ElementEditor<T extends AnyElement>(props: ElementEditorProps<T>) {
    const dispatch = useAppDispatch();

    const testMode = useAppSelector(selectUseTestMode);

    const [updatedElement, setUpdatedElement] = useState<T>();
    const [currentTab, setCurrentTab] = useState(testMode ? DefaultTabs.test : DefaultTabs.properties);
    const [showCreatePresetDialog, setShowCreatePresetDialog] = useState(false);

    const handleSave = () => {
        if (updatedElement != null) {
            props.onSave({
                ...updatedElement,
                appVersion: ProjectPackage.version,
            });
        } else {
            props.onSave(props.element);
        }
    };

    const handleSetCurrentTab = (newTab: string) => {
        setCurrentTab(newTab);
    };

    const handleShowPresetDialog = () => {
        setShowCreatePresetDialog(true);
    };

    const handleClosePresetDialog = () => {
        setShowCreatePresetDialog(false);
    };

    const handleChange = (update: T) => {
        setUpdatedElement(update);
    };

    const handleSavePreset = (presetName: string) => {
        if (props.element.type === ElementType.Container) {
            PresetsService.create({
                root: {
                    ...props.element,
                    name: presetName,
                },
            })
                .then(() => {
                    dispatch(showSuccessSnackbar('Preset Saved!'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Failed to save preset...'));
                });
        }
        handleClosePresetDialog();
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
            onClose={props.onCancel}
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
                        element={updatedElement ?? props.element}
                        currentTab={currentTab}
                        additionalTabs={additionalTabs}
                        onChange={handleChange}
                    />
                </Box>

                <ElementEditorActions
                    onSave={handleSave}
                    onCancel={props.onCancel}
                    onDelete={props.onDelete}
                    onSaveAsPreset={(updatedElement ?? props.element).type === ElementType.Container ? handleShowPresetDialog : undefined}
                    onClone={props.onClone}
                />

                <AddPresetDialog
                    open={showCreatePresetDialog}
                    onSavePreset={handleSavePreset}
                    onClose={handleClosePresetDialog}
                />
            </Box>
        </Drawer>
    );
}
