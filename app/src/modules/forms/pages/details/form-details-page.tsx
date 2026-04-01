import {Box, Paper, ThemeProvider, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {
    clearLoadedForm,
    LoadedForm,
    redoLoadedForm,
    selectFutureLoadedForm,
    selectLoadedForm,
    selectPastLoadedForm,
    showDialog,
    undoLoadedForm,
    updateLoadedForm,
} from '../../../../slices/app-slice';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../../theming/themes';
import {
    resetAdminSettings,
    selectDevToolsTab,
    setDevToolsTab,
    toggleAutoScrollForSteps,
    toggleComponentTree,
    toggleElementContextMenu,
    toggleValidation,
    toggleVisibility,
} from '../../../../slices/admin-settings-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../../components/element-tree-2/element-tree';
import {setCurrentStep} from '../../../../slices/stepper-slice';
import {flattenElements} from '../../../../utils/flatten-elements';
import {HelpDialog, HelpDialogId} from '../../../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../../../dialogs/imprint-dialog/imprint-dialog';
import {
    AccessibilityDialog,
    AccessibilityDialogId,
} from '../../../../dialogs/accessibility-dialog/accessibility-dialog';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import RemoveDoneOutlinedIcon from '@mui/icons-material/RemoveDoneOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import {type Theme} from '../../../themes/models/theme';
import {selectUser} from '../../../../slices/user-slice';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useApi} from '../../../../hooks/use-api';
import {EntityLockDto} from '../../../../models/dtos/entity-lock-dto';
import {EntityLockState} from '../../../../data/entity-lock-state';
import {useUsersApi} from '../../../../hooks/use-users-api';
import {getFullName, User} from '../../../../models/entities/user';
import {CustomerInputService} from '../../../../services/customer-input-service';
import {isAdmin} from '../../../../utils/is-admin';
import UndoIcon from '@mui/icons-material/Undo';
import RedoIcon from '@mui/icons-material/Redo';
import {DeveloperTools} from '../../../../components/developer-tools/developer-tools';
import {enqueueSnackbar} from 'notistack';
import {FormRevisionsDialog} from '../../dialogs/form-revisions-dialog';
import {
    hideLoadingOverlay,
    hideLoadingOverlayWithTimeout,
    showLoadingOverlay,
} from '../../../../slices/loading-overlay-slice';
import {withAsyncWrapper} from '../../../../utils/with-async-wrapper';
import {useDidUpdateEffect} from '../../../../hooks/use-did-update-effect';
import {IdentityProviderInfo} from '../../../identity/models/identity-provider-info';
import {setIdentityId} from '../../../../slices/identity-slice';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    ElementDerivationResponse,
} from '../../../../models/element-data';
import {FormStatus} from '../../enums/form-status';
import {addDerivationLogItems} from '../../../../slices/logging-slice';
import {RootState} from '../../../../store.staff';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';
import {Allotment} from 'allotment';
import {useElementSize} from '../../../../utils/element-size';
import {addEntityHistoryItem} from '../../../../slices/entity-history-slice';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {isApiError} from '../../../../models/api-error';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {withDelay} from '../../../../utils/with-delay';
import {FormApiService} from '../../services/form-api-service';
import {FormVersionApiService} from '../../services/form-version-api-service';
import {VFormWithPermissionsApiService} from '../../services/v-form-with-permissions-api-service';
import {FormEntity} from '../../entities/form-entity';
import {FormVersionEntity} from '../../entities/form-version-entity';
import {RootElement} from '../../../../models/elements/root-element';
import {ExportFormDialog} from '../../dialogs/export-form-dialog';
import {PrefillFormDialog} from '../../../../dialogs/prefill-form-dialog/prefill-form-dialog';
import {DeleteFormDialog} from '../../dialogs/delete-form-dialog';
import {downloadBlobFile, downloadFormExportFile} from '../../../../utils/download-utils';
import {downloadQrCode} from '../../../../utils/download-qrcode';
import {copyToClipboardText} from '../../../../utils/copy-to-clipboard';
import {createCustomerPath} from '../../../../utils/url-path-utils';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import QrCode from '@aivot/mui-material-symbols-400-outlined/dist/qr-code/QrCode';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import Settings from '@aivot/mui-material-symbols-400-outlined/dist/settings/Settings';
import History from '@aivot/mui-material-symbols-400-outlined/dist/history/History';
import {FormDetailsPageMoreMenu, type FormDetailsPageMoreMenuItem} from './components/form-details-page-more-menu';
import {type Action} from '../../../../components/actions/actions-props';
import {useElementEditorNavigation} from '../../../../hooks/use-element-editor-navigation';
import Preview from '@aivot/mui-material-symbols-400-outlined/dist/preview/Preview';
import Link from '@aivot/mui-material-symbols-400-outlined/dist/link/Link';
import FileExport from '@aivot/mui-material-symbols-400-outlined/dist/file-export/FileExport';
import Contract from '@aivot/mui-material-symbols-400-outlined/dist/contract/Contract';
import Draw from '@aivot/mui-material-symbols-400-outlined/dist/draw/Draw';
import AccountTree from '@aivot/mui-material-symbols-400-outlined/dist/account-tree/AccountTree';
import SwipeVertical from '@aivot/mui-material-symbols-400-outlined/dist/swipe-vertical/SwipeVertical';
import TouchApp from '@aivot/mui-material-symbols-400-outlined/dist/touch-app/TouchApp';
import BugReport from '@aivot/mui-material-symbols-400-outlined/dist/bug-report/BugReport';
import {ElementEditor} from '../../../../components/element-editor/element-editor';
import {ElementDisplayContext} from '../../../../data/element-type/element-child-options';

export const DialogSearchParam = 'dialog';

const formService = new FormApiService();
const versionService = new FormVersionApiService();

export function FormDetailsPage() {
    const baseTheme = useTheme();

    const [searchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const {
        id: formIdStr,
        version: formVersionStr,
    } = useParams();

    const formId = useMemo(() => {
        if (formIdStr == null) {
            return undefined;
        }

        const id = parseInt(formIdStr);
        if (isNaN(id)) {
            return undefined;
        }

        return id;
    }, [formIdStr]);

    const formVersion = useMemo(() => {
        if (formVersionStr == null) {
            return undefined;
        }

        const version = parseInt(formVersionStr);
        if (isNaN(version)) {
            return undefined;
        }

        return version;
    }, [formVersionStr]);

    const dispatch = useAppDispatch();
    const api = useApi();
    const navigate = useNavigate();
    const {
        closeElementEditor,
        currentEditedElementId,
        navigateToElementEditor,
    } = useElementEditorNavigation();

    const [showExportFormDialog, setShowExportFormDialog] = useState(false);
    const [showPrefillDialog, setShowPrefillDialog] = useState(false);
    const [showRevisions, setShowRevisions] = useState(false);
    const [showMoreMenuAtEl, setShowMoreMenuAtEl] = useState<HTMLElement | null>(null);
    const [formToDelete, setFormToDelete] = useState<FormEntity>();
    const showDeveloperTools = useAppSelector(selectDevToolsTab);

    const [authoredElementValues, setAuthoredElementValues] = useState<AuthoredElementValues>({});
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(createDerivedRuntimeElementData());

    const {
        disableVisibility,
        disableValidation,
        disableAutoScrollForSteps,
        disableElementContextMenu,
        hideComponentTree,
    } = useAppSelector((state: RootState) => state.adminSettings);

    const loadedForm = useAppSelector(selectLoadedForm);

    const pastLoadedForm = useAppSelector(selectPastLoadedForm);
    const hasPastLoadedForm = useMemo(() => pastLoadedForm.length > 0, [pastLoadedForm]);

    const futureLoadedForm = useAppSelector(selectFutureLoadedForm);
    const hasFutureLoadedForm = useMemo(() => futureLoadedForm.length > 0, [futureLoadedForm]);

    const [lockState, setLockState] = useState<EntityLockDto & { user?: User }>();

    const user = useAppSelector(selectUser);
    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const [identityProviderInfos, setIdentityProviderInfos] = useState<IdentityProviderInfo[]>([]);

    const [theme, setTheme] = useState<Theme>();

    const {
        ref: containerRef,
        size: containerSize,
    } = useElementSize<HTMLDivElement>();
    const developerToolsMinHeight = 280;
    const developerToolsMaxHeight = containerSize.height > 0 ? Math.max(developerToolsMinHeight, Math.floor(containerSize.height * 0.5)) : undefined;

    // Cleanup lock state on unload
    useEffect(() => {
        function handleCleanup() {
            if (formId == null) {
                return;
            }

            formService
                .deleteLockState(formId)
                .catch((err) => {
                    console.error(err);
                });
        }

        window.addEventListener('beforeunload', handleCleanup);

        return () => {
            handleCleanup();
            window.removeEventListener('beforeunload', handleCleanup);
        };
    }, [api, formIdStr]);

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [metaDialogName]);

    useEffect(() => {
        if (formId == null || formVersion == null) {
            return;
        }

        dispatch(clearLoadedForm());
        dispatch(resetAdminSettings());
        dispatch(setCurrentStep(0));
        dispatch(setIdentityId(undefined));

        dispatch(setLoadingMessage({
            blocking: false,
            message: 'Formular wird geladen',
            estimatedTime: 2000,
        }));

        Promise.all([
            formService
                .retrieve(formId),
            versionService
                .retrieve({
                    formId: formId,
                    version: formVersion,
                }),
            new VFormWithPermissionsApiService()
                .retrieve({
                    formId: formId,
                    userId: user?.id ?? '',
                }),
        ])
            .then(([form, version, permissions]) => {
                CustomerInputService.cleanCustomerInput(form.slug, version.version);
                dispatch(updateLoadedForm({
                    form: form,
                    version: version,
                    permissions: permissions,
                }));
                dispatch(addEntityHistoryItem({
                    title: form.internalTitle,
                    link: `/forms/${form.id}/${version.version}`,
                    type: ServerEntityType.Forms,
                }));
            })
            .catch((err) => {
                if (isApiError(err)) {
                    if (err.status === 403) {
                        dispatch(setErrorMessage({
                            status: 403,
                            message: err.displayableToUser ? err.message : 'Sie verfügen nicht über die notwendigen Rechte, um dieses Formular anzuzeigen.',
                        }));
                    } else if (err.status === 404) {
                        dispatch(setErrorMessage({
                            status: 404,
                            message: err.displayableToUser ? err.message : 'Das angeforderte Formular wurde nicht gefunden.',
                        }));
                    } else if (err.displayableToUser) {
                        dispatch(showErrorSnackbar(err.message));
                    } else {
                        dispatch(showErrorSnackbar('Das Formular konnte nicht geladen werden.'));
                    }
                } else {
                    dispatch(showErrorSnackbar('Das Formular konnte nicht geladen werden.'));
                }
                console.error(err);
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
        fetchLockState(formId);
    }, [formId, formVersion, dispatch]);

    useEffect(() => {
        if (loadedForm == null) {
            return;
        }

        if (loadedForm.version.themeId === theme?.id) {
            return;
        }

        formService
            .getFormTheme(loadedForm.form.slug, loadedForm.version.version)
            .then(setTheme)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Das Erscheinungsbild des Formulars konnte nicht geladen werden.'));
            });
    }, [loadedForm]);

    useEffect(() => {
        if (loadedForm == null) {
            return;
        }

        if (identityProviderInfos != null) {
            return;
        }

        formService
            .getIdentityProviders(loadedForm.form.slug, loadedForm.version.version)
            .then(res => setIdentityProviderInfos(res.content));
    }, [loadedForm]);


    useDidUpdateEffect(() => {
        if (loadedForm == null) {
            return;
        }

        dispatch(showLoadingOverlay('Sichtbarkeiten berechnen'));

        withAsyncWrapper<any, ElementDerivationResponse>({
            desiredMinRuntime: 600,
            main: () => {
                return formService
                    .deriveForm(
                        loadedForm.form.slug,
                        loadedForm.version.version,
                        authoredElementValues,
                        {
                            skipErrorsFor: [],
                            skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                            skipValuesFor: [],
                            skipOverridesFor: [],
                        },
                    );
            },
        })
            .then((newState) => {
                setDerivedData(newState.elementData);
                dispatch(addDerivationLogItems(newState.logItems));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    }, [disableVisibility]);

    const _theme = useMemo(() => {
        return createAppTheme(theme, baseTheme);
    }, [theme, baseTheme]);

    function fetchLockState(id: number) {
        formService
            .getLockState(id)
            .then((lockState) => {
                if (lockState.state === EntityLockState.LockedOther && lockState.lockedBy != null) {
                    useUsersApi(api)
                        .retrieve(lockState.lockedBy)
                        .then((user) => {
                            setLockState({
                                ...lockState,
                                user: user,
                            });
                        })
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Das Formular wird aktuell durch eine andere Person bearbeitet.'));
                            setLockState(lockState);
                        });
                } else {
                    setLockState(lockState);
                }
            })
            .catch((err) => {
                console.error(err);
            });
    }

    const handleUndo = () => {
        if (formId == null || formVersion == null) {
            return;
        }

        dispatch(undoLoadedForm());
        dispatch(showLoadingOverlay('Speichern…'));
        Promise.all([
            formService
                .update(formId, pastLoadedForm[pastLoadedForm.length - 1].form),
            versionService
                .update({
                    formId: formId,
                    version: formVersion,
                }, pastLoadedForm[pastLoadedForm.length - 1].version),
        ])
            .then(([form, version]) => {
                formService
                    .deriveForm(
                        form.slug,
                        version.version,
                        authoredElementValues,
                        {
                            skipErrorsFor: ['ALL'],
                            skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                            skipValuesFor: [],
                            skipOverridesFor: [],
                        },
                    )
                    .then((state) => {
                        setDerivedData(state.elementData);
                        dispatch(addDerivationLogItems(state.logItems));
                    });
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    const handleRedo = () => {
        if (formId == null || formVersion == null) {
            return;
        }

        dispatch(redoLoadedForm());
        dispatch(showLoadingOverlay('Speichern…'));
        Promise.all([
            formService
                .update(formId, futureLoadedForm[futureLoadedForm.length - 1].form),
            versionService
                .update({
                    formId: formId,
                    version: formVersion,
                }, futureLoadedForm[futureLoadedForm.length - 1].version),
        ])
            .then(([form, version]) => {
                formService
                    .deriveForm(
                        form.slug,
                        version.version,
                        authoredElementValues,
                        {
                            skipErrorsFor: ['ALL'],
                            skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                            skipValuesFor: [],
                            skipOverridesFor: [],
                        },
                    )
                    .then((state) => {
                        setDerivedData(state.elementData);
                        dispatch(addDerivationLogItems(state.logItems));
                    });
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    const scrollContainerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (lockState?.state === EntityLockState.LockedOther && lockState.user) {
            enqueueSnackbar(`Dieses Formular wird aktuell von ${getFullName(lockState.user)} bearbeitet.`, {
                variant: 'error',
                autoHideDuration: 6000,
            });
        }
    }, [lockState, enqueueSnackbar]);

    const isEditable = useMemo(() => {
        if (loadedForm == null) {
            return false;
        }

        if (lockState != null && lockState.state === EntityLockState.LockedOther) {
            return false;
        }

        return (
            isAdmin(user) ||
            loadedForm.permissions.formPermissionEdit
        );
    }, [user, lockState, loadedForm]);

    const canViewHistory = useMemo(() => {
        if (loadedForm == null) {
            return false;
        }

        if (lockState != null && lockState.state === EntityLockState.LockedOther) {
            return false;
        }

        return (
            isAdmin(user) ||
            loadedForm.permissions.formPermissionEdit ||
            loadedForm.permissions.formPermissionRead
        );
    }, [loadedForm, lockState, user]);

    if (loadedForm == null) {
        return <LoadingPlaceholder/>;
    } else {
        const allElements = flattenElements(loadedForm.version.rootElement);
        const publicFormLink = createCustomerPath(loadedForm.form.slug);
        const previewFormLink = createCustomerPath(`${loadedForm.form.slug}/${loadedForm.version.version}`);
        const canDeleteForm = isAdmin(user) || loadedForm.permissions.formPermissionDelete;

        const handleOpenPreview = () => {
            window.open(previewFormLink, '_blank', 'noopener,noreferrer');
        };

        const handleCopyPublicFormLink = async () => {
            try {
                const success = await copyToClipboardText(publicFormLink);
                if (!success) {
                    throw new Error('copy failed');
                }
                dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
            } catch (err) {
                console.error(err);
                dispatch(showErrorSnackbar('Formularlink konnte nicht kopiert werden'));
            }
        };

        const handleDownloadPublicQrCode = async () => {
            try {
                await downloadQrCode(publicFormLink, `qr-code-${loadedForm.form.slug}.png`);
                dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
            } catch (err) {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
            }
        };

        const handleDownloadPdfFile = () => {
            dispatch(showLoadingOverlay('Vordruck wird generiert'));
            api
                .getBlob(`forms/${loadedForm.form.id}/${loadedForm.version.version}/print/`)
                .then((blob) => {
                    downloadBlobFile(`vordruck - ${loadedForm.form.slug} - ${loadedForm.version.version}.pdf`, blob);
                    dispatch(hideLoadingOverlayWithTimeout(1000));
                })
                .catch((error) => {
                    console.error(error);
                    dispatch(hideLoadingOverlay());
                    dispatch(showErrorSnackbar('Fehler beim Generieren des Vordrucks'));
                });
        };

        const handleExportForm = async () => {
            try {
                const exportFile = await formService.export(loadedForm.form.id, loadedForm.version.version);
                downloadFormExportFile(exportFile);
                dispatch(showSuccessSnackbar('Formular wurde erfolgreich exportiert.'));
            } catch (error) {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Export des Formulars.'));
            } finally {
                setShowExportFormDialog(false);
            }
        };

        const handleDeleteCurrentForm = (form: FormEntity) => {
            setFormToDelete(undefined);
            dispatch(setLoadingMessage({
                message: 'Formular wird gelöscht',
                blocking: true,
                estimatedTime: 500,
            }));

            formService
                .destroy(form.id)
                .then(() => {
                    dispatch(showSuccessSnackbar('Das Formular wurde erfolgreich gelöscht.'));
                    navigate('/forms', {
                        replace: true,
                    });
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Fehler beim Löschen des Formulars'));
                })
                .finally(() => {
                    dispatch(clearLoadingMessage());
                });
        };

        const handlePatch = async (patchedLoadedForm: Partial<LoadedForm>) => {
            if (loadedForm == null) {
                return;
            }

            if (!isEditable) {
                dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                return;
            }

            const formToUpdate: FormEntity = {
                ...loadedForm.form,
                ...patchedLoadedForm.form,
            };
            const versionToUpdate: FormVersionEntity = {
                ...loadedForm.version,
                ...patchedLoadedForm.version,
            };
            dispatch(updateLoadedForm({
                ...loadedForm,
                form: formToUpdate,
                version: versionToUpdate,
            }));

            const originalLoadedForm: LoadedForm = {
                ...loadedForm,
            };

            dispatch(setLoadingMessage({
                message: 'Speichere',
                blocking: false,
                estimatedTime: 800,
            }));


            try {
                await withDelay(Promise.all([
                    formService.update(loadedForm.form.id, formToUpdate),
                    versionService.update({
                        formId: loadedForm.form.id,
                        version: loadedForm.version.version,
                    }, versionToUpdate),
                ]), 600);
            } catch (err: any) {
                if (err.status === 403) {
                    dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                } else if (err.status === 423) {
                    dispatch(showErrorSnackbar('Das Formular wird aktuell durch eine andere Mitarbeiter:in bearbeitet.'));
                    fetchLockState(loadedForm.form.id);
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Das Formular konnte nicht gespeichert werden.'));
                }
                dispatch(updateLoadedForm(originalLoadedForm));
            }

            const newState = await formService
                .deriveForm(
                    loadedForm.form.slug,
                    loadedForm.version.version,
                    authoredElementValues,
                    {
                        skipErrorsFor: ['ALL'],
                        skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                        skipValuesFor: [],
                        skipOverridesFor: [],
                    },
                );
            setDerivedData(newState.elementData);
            dispatch(addDerivationLogItems(newState.logItems));

            dispatch(clearLoadingMessage());
        };

        const handleRootEditorSave = (
            updatedElement: Partial<RootElement>,
            updatedLoadedForm: Partial<LoadedForm>,
        ) => {
            closeElementEditor();

            void handlePatch({
                ...updatedLoadedForm,
                version: {
                    ...loadedForm.version,
                    ...updatedLoadedForm.version,
                    rootElement: {
                        ...loadedForm.version.rootElement,
                        ...updatedLoadedForm.version?.rootElement,
                        ...updatedElement,
                    },
                },
            });
        };

        const moreMenuItems: FormDetailsPageMoreMenuItem[] = [
            {
                label: 'Vorschau in neuem Tab öffnen',
                icon: <Preview/>,
                endIcon: <OpenInNew fontSize="small"/>,
                onClick: handleOpenPreview,
            },
            {
                label: 'Änderungsverlauf ansehen',
                icon: <History/>,
                onClick: () => {
                    setShowRevisions(true);
                },
                visible: canViewHistory,
            },
            'separator',
            {
                label: 'Öffentl. Link in Zwischenablage kopieren',
                icon: <Link/>,
                onClick: () => {
                    void handleCopyPublicFormLink();
                },
            },
            {
                label: 'QR-Code mit öffentl. Link herunterladen',
                icon: <QrCode/>,
                onClick: () => {
                    void handleDownloadPublicQrCode();
                },
            },
            'separator',
            {
                label: 'Formular exportieren (.gov)',
                icon: <FileExport/>,
                onClick: () => {
                    setShowExportFormDialog(true);
                },
            },
            {
                label: 'Vordruck exportieren (.pdf)',
                icon: <Contract/>,
                onClick: handleDownloadPdfFile,
            },
            {
                label: 'Formular vorbefüllen',
                icon: <Draw/>,
                onClick: () => {
                    setShowPrefillDialog(true);
                },
            },
            'separator',
            {
                type: 'toggle',
                label: 'Formularstruktur anzeigen',
                icon: <AccountTree/>,
                checked: !hideComponentTree,
                onToggle: () => {
                    dispatch(toggleComponentTree());
                },
            },
            {
                type: 'toggle',
                label: 'Autom. Scrollen aktivieren',
                icon: <SwipeVertical/>,
                checked: !disableAutoScrollForSteps,
                onToggle: () => {
                    dispatch(toggleAutoScrollForSteps());
                },
            },
            {
                type: 'toggle',
                label: 'Element-Kontextmenü aktivieren',
                icon: <TouchApp/>,
                checked: !disableElementContextMenu,
                onToggle: () => {
                    dispatch(toggleElementContextMenu());
                },
            },
            {
                label: 'Entwicklerwerkzeuge öffnen',
                icon: <BugReport/>,
                onClick: () => {
                    dispatch(setDevToolsTab(showDeveloperTools ?? 0));
                },
            },
            'separator',
            {
                label: 'Formular löschen',
                icon: <Delete/>,
                onClick: () => {
                    setFormToDelete(loadedForm.form);
                },
                isDangerous: true,
                visible: canDeleteForm,
            },
        ];

        const headerActions: Action[] = [
            {
                tooltip: 'Änderung rückgängig machen',
                icon: <UndoIcon/>,
                onClick: handleUndo,
                disabled: !hasPastLoadedForm,
                visible: loadedForm.version.status === FormStatus.Drafted,
            },
            {
                tooltip: 'Änderung wiederherstellen',
                icon: <RedoIcon/>,
                onClick: handleRedo,
                disabled: !hasFutureLoadedForm,
                visible: loadedForm.version.status === FormStatus.Drafted,
            },
            'separator' as const,
            {
                tooltip: disableValidation ? 'Validierungen aktivieren' : 'Validierungen deaktivieren',
                icon: disableValidation ? <DoneAllOutlinedIcon/> : <RemoveDoneOutlinedIcon/>,
                onClick: () => {
                    dispatch(toggleValidation());
                },
            },
            {
                tooltip: disableVisibility ? 'Sichtbarkeiten aktivieren' : 'Sichtbarkeiten deaktivieren',
                icon: disableVisibility ? <VisibilityOutlinedIcon/> : <VisibilityOffOutlinedIcon/>,
                onClick: () => {
                    dispatch(toggleVisibility());
                },
            },
            'separator' as const,
            {
                tooltip: 'Formular-Einstellungen öffnen',
                icon: <Settings/>,
                onClick: () => {
                    navigateToElementEditor(loadedForm.version.rootElement.id);
                },
            },
            {
                tooltip: 'Weitere Optionen',
                icon: <MoreVert/>,
                onClick: (event: React.MouseEvent<HTMLButtonElement>) => {
                    setShowMoreMenuAtEl(event.currentTarget);
                },
            },
            'separator' as const,
            {
                label: 'Zurück zum Prozess',
                tooltip: 'Zurück zum Prozess',
                icon: null,
                onClick: () => undefined,
                variant: 'contained' as const,
                activeStyle: {ml: 1},
            },
        ];

        return (
            <PageWrapper
                title={`Editor - ${loadedForm.form.internalTitle} — Version ${loadedForm.version.version}`}
                fullWidth={true}
                fullHeight={true}
            >
                <Box
                    ref={containerRef}
                    sx={{
                        height: '100vh',
                        '--focus-border': (theme) => theme.palette.secondary.main,
                    }}
                >
                    <Allotment vertical>
                        <Allotment>
                            <Allotment.Pane minSize={732}>
                                {/* Working Area */}
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        height: '100%',
                                        px: 2,
                                        pt: 2,
                                        overflow: 'hidden',
                                    }}
                                >
                                    <GenericPageHeader
                                        title={'Formular: ' + loadedForm.form.internalTitle}
                                        badge={{
                                            color: 'default',
                                            label: `Version ${loadedForm.version.version}`,
                                        }}
                                        icon={ModuleIcons.forms}
                                        actions={headerActions}
                                    />

                                    <Paper
                                        sx={{
                                            overflowY: 'auto',
                                            flex: 1,
                                            mt: 2,
                                            minHeight: 0,
                                            borderTopLeftRadius: 10,
                                            borderTopRightRadius: 10,
                                            borderBottomLeftRadius: 0,
                                            borderBottomRightRadius: 0,
                                        }}
                                        ref={scrollContainerRef}
                                    >
                                        <ThemeProvider theme={_theme}>
                                            <ViewDispatcherComponent
                                                rootElement={loadedForm.version.rootElement}
                                                allElements={allElements}
                                                element={loadedForm.version.rootElement}
                                                scrollContainerRef={scrollContainerRef}
                                                isBusy={false}
                                                isDeriving={false}
                                                mode="editor"
                                                authoredElementValues={authoredElementValues}
                                                derivedData={derivedData}
                                                onAuthoredElementValuesChange={setAuthoredElementValues}
                                                onDerivedDataChange={setDerivedData}
                                                onElementBlur={undefined}
                                                derivationTriggerIdQueue={[] /* Not necessary because this is kept internally by the root component view */}
                                                disableVisibility={disableVisibility}
                                            />

                                            <HelpDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === HelpDialogId}
                                            />

                                            <PrivacyDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === PrivacyDialogId}
                                            />

                                            <ImprintDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === ImprintDialogId}
                                            />

                                            <AccessibilityDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === AccessibilityDialogId}
                                            />
                                        </ThemeProvider>
                                    </Paper>
                                </Box>
                            </Allotment.Pane>
                            {/* Element Tree */}
                            {
                                !hideComponentTree &&
                                (
                                    <Allotment.Pane
                                        minSize={480}
                                        preferredSize={480}
                                    >
                                        <Paper
                                            sx={{
                                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                                borderLeft: '1px solid #E0E7E0',
                                                borderRadius: 0,
                                                position: 'relative',
                                                height: '100%',
                                                overflow: 'hidden',
                                            }}
                                        >
                                            <ElementTree
                                                value={loadedForm.version.rootElement}
                                                onChange={(changed) => {
                                                    handlePatch({
                                                        ...loadedForm,
                                                        version: {
                                                            ...loadedForm.version,
                                                            rootElement: changed as RootElement,
                                                        },
                                                    });
                                                }}
                                                editable={isEditable}
                                                displayContext={ElementDisplayContext.CitizenFacing}
                                                allowElementIdEditing={false}
                                            />
                                        </Paper>
                                    </Allotment.Pane>
                                )}

                        </Allotment>

                        {showDeveloperTools !== undefined && (
                            <Allotment.Pane
                                minSize={developerToolsMinHeight}
                                maxSize={developerToolsMaxHeight}
                                preferredSize={420}
                            >
                                <Box
                                    sx={{
                                        height: '100%',
                                        overflow: 'auto',
                                    }}
                                >
                                    <DeveloperTools
                                        dataLabel={loadedForm.form.internalTitle}
                                        rootElement={loadedForm.version.rootElement}
                                        elementData={authoredElementValues}
                                        onElementDataChange={(elementData) => {
                                            dispatch(setLoadingMessage({
                                                message: 'Element-Daten werden importiert',
                                                blocking: true,
                                                estimatedTime: 500,
                                            }));

                                            withDelay(
                                                formService
                                                    .deriveForm(
                                                        loadedForm.form.slug,
                                                        loadedForm.version.version,
                                                        elementData,
                                                        {
                                                            skipErrorsFor: ['ALL'],
                                                            skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                                                            skipValuesFor: [],
                                                            skipOverridesFor: [],
                                                        },
                                                    ), 500)
                                                .then((state) => {
                                                    setAuthoredElementValues(elementData);
                                                    setDerivedData(state.elementData);
                                                    dispatch(addDerivationLogItems(state.logItems));
                                                })
                                                .finally(() => {
                                                    dispatch(setLoadingMessage(undefined));
                                                });
                                        }}
                                    />
                                </Box>

                            </Allotment.Pane>
                        )}

                    </Allotment>
                </Box>

                <FormDetailsPageMoreMenu
                    anchorEl={showMoreMenuAtEl}
                    onClose={() => {
                        setShowMoreMenuAtEl(null);
                    }}
                    items={moreMenuItems}
                />

                <ExportFormDialog
                    open={showExportFormDialog}
                    onCancel={() => {
                        setShowExportFormDialog(false);
                    }}
                    onExport={() => {
                        void handleExportForm();
                    }}
                />

                <PrefillFormDialog
                    open={showPrefillDialog}
                    onClose={() => {
                        setShowPrefillDialog(false);
                    }}
                />

                <FormRevisionsDialog
                    open={showRevisions}
                    onClose={() => setShowRevisions(false)}
                />

                <DeleteFormDialog
                    form={formToDelete}
                    onDelete={handleDeleteCurrentForm}
                    onCancel={() => {
                        setFormToDelete(undefined);
                    }}
                />

                {
                    currentEditedElementId === loadedForm.version.rootElement.id &&
                    <ElementEditor
                        open={true}
                        parents={[]}
                        entity={loadedForm}
                        element={loadedForm.version.rootElement}
                        onSave={handleRootEditorSave}
                        onCancel={closeElementEditor}
                        editable={isEditable}
                        scope="application"
                        rootEditor={true}
                    />
                }
            </PageWrapper>
        );
    }
}
