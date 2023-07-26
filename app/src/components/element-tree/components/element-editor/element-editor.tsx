import {Box, Drawer} from '@mui/material';
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
import {type ElementEditorProps} from './element-editor-props';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {type AnyElement} from '../../../../models/elements/any-element';
import {ElementType} from '../../../../data/element-type/element-type';
import ProjectPackage from '../../../../../package.json';
import Editors from '../../../../editors';
import {type Application} from '../../../../models/entities/application';
import {type Preset} from '../../../../models/entities/preset';

export function ElementEditor<T extends AnyElement, E extends Application | Preset>(props: ElementEditorProps<T, E>): JSX.Element | null {
    const dispatch = useAppDispatch();

    const testMode = useAppSelector(selectUseTestMode);

    const [updatedElement, setUpdatedElement] = useState<T>();
    const [updatedEntity, setUpdatedEntity] = useState<E>();
    const [currentTab, setCurrentTab] = useState(testMode ? DefaultTabs.test : DefaultTabs.properties);
    const [showCreatePresetDialog, setShowCreatePresetDialog] = useState(false);

    const handleSave = (): void => {
        let elementToSave: Partial<T> = {};
        if (updatedElement != null) {
            elementToSave = {
                ...updatedElement,
                appVersion: ProjectPackage.version,
            };

            if (props.element.testProtocolSet != null) {
                elementToSave.testProtocolSet = undefined;
            }
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

    const handleSavePreset = (presetName: string): void => {
        if (props.element.type === ElementType.Container) {
            PresetsService.create({
                id: 0,
                root: {
                    ...props.element,
                    name: presetName,
                },
                created: '',
                updated: '',
            })
                .then(() => {
                    dispatch(showSuccessSnackbar('Vorlage erfolgreich gespeichert'));
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
                });
        }
        handleClosePresetDialog();
    };

    const handleClose = (): void => {
        if (updatedElement != null) {
            const conf = window.confirm('Sollen die Änderungen am Element wirklich verworfen werden?');
            if (conf) {
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
                    />
                </Box>

                {
                    props.editable &&
                    <ElementEditorActions
                        onSave={handleSave}
                        onCancel={handleClose}
                        onDelete={props.onDelete}
                        onSaveAsPreset={(updatedElement ?? props.element).type === ElementType.Container ? handleShowPresetDialog : undefined}
                        onClone={props.onClone}
                    />
                }

                <AddPresetDialog
                    open={showCreatePresetDialog}
                    onSavePreset={handleSavePreset}
                    onClose={handleClosePresetDialog}
                />
            </Box>
        </Drawer>
    );
}
