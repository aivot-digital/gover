import {Box, Breadcrumbs, Drawer, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {ElementEditorTabs} from '../element-editor-tabs/element-editor-tabs';
import {ElementEditorContent} from '../element-editor-content/element-editor-content';
import {ElementEditorActions} from '../element-editor-actions/element-editor-actions';
import {type ElementEditorProps} from './element-editor-props';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {type AnyElement} from '../../models/elements/any-element';
import {editors as Editors} from '../../editors';
import {AddPresetDialog} from '../../dialogs/preset-dialogs/add-preset-dialog/add-preset-dialog';
import {type GroupLayout, isGroupLayout, isPresetGroupLayout} from '../../models/elements/form/layout/group-layout';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {useChangeBlocker} from '../../hooks/use-change-blocker';
import {useConfirm} from '../../providers/confirm-provider';
import {AppInfo} from '../../app-info';
import {createElementEditorNavigationLink, useElementEditorNavigation} from '../../hooks/use-element-editor-navigation';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {getElementNameForType} from '../../data/element-type/element-names';
import {Link} from 'react-router-dom';
import {addSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../slices/shell-slice';

export function ElementEditor<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorProps<T, E>): React.ReactNode | null {
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const {
        currentEditorTab,
        navigateToEditorTab,
    } = useElementEditorNavigation();

    const {
        open,
        lockMessage,
        element,
        entity,
    } = props;

    const [updatedElement, setUpdatedElement] = useState<T>();
    const [updatedEntity, setUpdatedEntity] = useState<E>();
    const [showCreatePresetDialog, setShowCreatePresetDialog] = useState(false);

    // Reset the updated states when opening the editor
    useEffect(() => {
        if (open) {
            setUpdatedElement(undefined);
            setUpdatedEntity(undefined);
        }
    }, [open]);

    const initialState = useMemo(() => ({
        element: element,
        entity: entity,
    }), [element, entity]);

    const currentState = useMemo(() => ({
        element: updatedElement ?? element,
        entity: updatedEntity ?? entity,
    }), [updatedElement, updatedEntity, element, entity]);

    const changeBlocker = useChangeBlocker(initialState, currentState);

    useEffect(() => {
        if (open && lockMessage != null) {
            dispatch(addSnackbarMessage({
                key: 'element-editor-lock-message',
                type: SnackbarType.Dismissable,
                severity: SnackbarSeverity.Warning,
                message: lockMessage,
            }));
        }
    }, [open, lockMessage]);

    const handleSave = (): void => {
        let elementToSave: Partial<T> = {};
        if (updatedElement != null) {
            elementToSave = {
                ...updatedElement,
                appVersion: AppInfo.version,
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
        navigateToEditorTab(newTab);
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
                title: 'Änderungen verwerfen?',
                confirmButtonText: 'Ja, verwerfen',
                children: (
                    <div>
                        Möchten Sie die Änderungen an diesem{' '}
                        {updatedElement != null ? 'Element' : 'Formular'} wirklich verwerfen? Diese Aktion kann nicht rückgängig gemacht werden.
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
            open={props.open}
            PaperProps={{
                sx: {
                    width: {
                        xs: '100%',
                        sm: '100%',
                        md: '90%',
                        lg: '85%',
                        xl: '75%',
                    },
                    maxWidth: '1720px',
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
                <Box
                    sx={{
                        display: 'flex',
                        px: 2,
                        py: 1,
                    }}
                >
                    <Typography
                        variant="h6"
                        component="div"
                        sx={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            mr: 2,
                        }}
                    >
                        {generateComponentTitle(props.element)} ({getElementNameForType(props.element.type)})
                    </Typography>

                    <Breadcrumbs
                        sx={{
                            ml: 'auto',
                            color: 'text.secondary',
                            '& a': {
                                color: 'text.secondary',
                                textDecoration: 'none',
                                '&:hover': {
                                    textDecoration: 'underline',
                                },
                            },
                        }}
                        maxItems={3}
                    >
                        {
                            props
                                .parents
                                .map((element) => (
                                    <Link
                                        to={createElementEditorNavigationLink(element.id)}
                                        replace={true}
                                    >
                                        {generateComponentTitle(element)}
                                    </Link>
                                ))
                        }
                    </Breadcrumbs>
                </Box>

                <ElementEditorTabs
                    component={updatedElement ?? props.element}
                    additionalTabs={additionalTabs}
                    currentTab={currentEditorTab}
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
                        currentTab={currentEditorTab}
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
