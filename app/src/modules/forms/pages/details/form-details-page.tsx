import {Box, Paper, ThemeProvider, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {clearLoadedForm, LoadedForm, redoLoadedForm, selectFutureLoadedForm, selectLoadedForm, selectPastLoadedForm, showDialog, undoLoadedForm, updateLoadedForm} from '../../../../slices/app-slice';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {useParams, useSearchParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../../theming/themes';
import {resetAdminSettings, selectDevToolsTab, toggleComponentTree, toggleValidation, toggleVisibility} from '../../../../slices/admin-settings-slice';
import {AdminToolsDialog} from '../../../../dialogs/admin-tools/admin-tools-dialog';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../../components/element-tree/element-tree';
import {setCurrentStep} from '../../../../slices/stepper-slice';
import {flattenElements} from '../../../../utils/flatten-elements';
import {HelpDialog, HelpDialogId} from '../../../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../../../dialogs/accessibility-dialog/accessibility-dialog';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import RemoveDoneOutlinedIcon from '@mui/icons-material/RemoveDoneOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import HandymanOutlinedIcon from '@mui/icons-material/HandymanOutlined';
import DesktopAccessDisabledOutlinedIcon from '@mui/icons-material/DesktopAccessDisabledOutlined';
import DesktopWindowsOutlinedIcon from '@mui/icons-material/DesktopWindowsOutlined';
import LaunchOutlinedIcon from '@mui/icons-material/LaunchOutlined';
import {type Theme} from '../../../themes/models/theme';
import {selectMemberships, selectUser} from '../../../../slices/user-slice';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {useApi} from '../../../../hooks/use-api';
import {EntityLockDto} from '../../../../models/dtos/entity-lock-dto';
import {EntityLockState} from '../../../../data/entity-lock-state';
import {useUsersApi} from '../../../../hooks/use-users-api';
import {getFullName, User} from '../../../../models/entities/user';
import {CustomerInputService} from '../../../../services/customer-input-service';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import {isAdmin} from '../../../../utils/is-admin';
import UndoIcon from '@mui/icons-material/Undo';
import RedoIcon from '@mui/icons-material/Redo';
import {DeveloperTools} from '../../../../components/developer-tools/developer-tools';
import {enqueueSnackbar} from 'notistack';
import {FormRevisionsDialog} from '../../dialogs/form-revisions-dialog';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../slices/loading-overlay-slice';
import {withAsyncWrapper} from '../../../../utils/with-async-wrapper';
import {useDidUpdateEffect} from '../../../../hooks/use-did-update-effect';
import {IdentityProviderInfo} from '../../../identity/models/identity-provider-info';
import {setIdentityId} from '../../../../slices/identity-slice';
import {ElementData, ElementDerivationResponse} from '../../../../models/element-data';
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

export const DialogSearchParam = 'dialog';

const formService = new FormApiService();
const versionService = new FormVersionApiService();

export function FormDetailsPage() {
    const baseTheme = useTheme();

    const [searchParams, setSearchParams] = useSearchParams();
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

    const [showAdminTools, setShowAdminTools] = useState(false);
    const [showRevisions, setShowRevisions] = useState(false);
    const showDeveloperTools = useAppSelector(selectDevToolsTab);

    const [elementData, setElementData] = useState<ElementData>({});

    const {
        disableVisibility,
        disableValidation,
        hideComponentTree,
    } = useAppSelector((state: RootState) => state.adminSettings);

    const loadedForm = useAppSelector(selectLoadedForm);

    const pastLoadedForm = useAppSelector(selectPastLoadedForm);
    const hasPastLoadedForm = useMemo(() => pastLoadedForm.length > 0, [pastLoadedForm]);

    const futureLoadedForm = useAppSelector(selectFutureLoadedForm);
    const hasFutureLoadedForm = useMemo(() => futureLoadedForm.length > 0, [futureLoadedForm]);

    const [lockState, setLockState] = useState<EntityLockDto & { user?: User }>();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
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
                dispatch(showApiErrorSnackbar(err, 'Das Farbschema des Formulars konnte nicht geladen werden.'));
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
                        elementData,
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
                setElementData(newState.elementData);
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
                        elementData,
                        {
                            skipErrorsFor: ['ALL'],
                            skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                            skipValuesFor: [],
                            skipOverridesFor: [],
                        },
                    )
                    .then((state) => {
                        setElementData(state.elementData);
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
                        elementData,
                        {
                            skipErrorsFor: ['ALL'],
                            skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                            skipValuesFor: [],
                            skipOverridesFor: [],
                        },
                    )
                    .then((state) => {
                        setElementData(state.elementData);
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

    if (loadedForm == null) {
        return <LoadingPlaceholder />;
    } else {
        const allElements = flattenElements(loadedForm.version.rootElement);

        const isEditable = (
            loadedForm.version.status == FormStatus.Drafted &&
            (memberships ?? []).some((mem) => mem.departmentId === loadedForm.form.developingDepartmentId) &&
            (lockState == null || lockState.state === EntityLockState.Free || lockState.state === EntityLockState.LockedSelf)
        );
        const canViewHistory = isAdmin(user) || (memberships ?? []).some((mem) => mem.departmentId === loadedForm.form.developingDepartmentId);

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
            const originalLoadedForm: LoadedForm = {
                ...loadedForm,
            };

            dispatch(setLoadingMessage({
                message: 'Speichere',
                blocking: false,
                estimatedTime: 800,
            }));


            try {
                const [updatedForm, updatedVersion] = await withDelay(Promise.all([
                    formService.update(loadedForm.form.id, formToUpdate),
                    versionService.update({formId: loadedForm.form.id, version: loadedForm.version.version}, versionToUpdate),
                ]), 600);

                dispatch(updateLoadedForm({
                    ...loadedForm,
                    form: updatedForm,
                    version: updatedVersion,
                }));
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
                    elementData,
                    {
                        skipErrorsFor: ['ALL'],
                        skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                        skipValuesFor: [],
                        skipOverridesFor: [],
                    },
                );
            setElementData(newState.elementData);
            dispatch(addDerivationLogItems(newState.logItems));

            dispatch(clearLoadingMessage());
        };

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
                                        actions={[
                                            {
                                                tooltip: 'Änderung rückgängig machen',
                                                icon: <UndoIcon />,
                                                onClick: handleUndo,
                                                disabled: !hasPastLoadedForm,
                                                visible: loadedForm.version.status === FormStatus.Drafted,
                                            },
                                            {
                                                tooltip: 'Änderung wiederherstellen',
                                                icon: <RedoIcon />,
                                                onClick: handleRedo,
                                                disabled: !hasFutureLoadedForm,
                                                visible: loadedForm.version.status === FormStatus.Drafted,
                                            },
                                            'separator',
                                            {
                                                tooltip: 'Historie anzeigen',
                                                icon: <AccessTimeIcon />,
                                                onClick: () => {
                                                    setShowRevisions(true);
                                                },
                                                visible: canViewHistory,
                                            },
                                            {
                                                tooltip: disableValidation ? 'Validierungen aktivieren' : 'Validierungen deaktivieren',
                                                icon: disableValidation ? <DoneAllOutlinedIcon /> :
                                                    <RemoveDoneOutlinedIcon />,
                                                onClick: () => {
                                                    dispatch(toggleValidation());
                                                },
                                            },
                                            {
                                                tooltip: disableVisibility ? 'Sichtbarkeiten aktivieren' : 'Sichtbarkeiten deaktivieren',
                                                icon: disableVisibility ? <VisibilityOutlinedIcon /> :
                                                    <VisibilityOffOutlinedIcon />,
                                                onClick: () => {
                                                    dispatch(toggleVisibility());
                                                },
                                            },
                                            'separator',
                                            {
                                                tooltip: 'Admin-Werkzeuge öffnen',
                                                icon: <HandymanOutlinedIcon />,
                                                onClick: () => {
                                                    setShowAdminTools(true);
                                                },
                                            },
                                            {
                                                tooltip: hideComponentTree ? 'Formularstruktur einblenden' : 'Formularstruktur ausblenden',
                                                icon: hideComponentTree ? <DesktopWindowsOutlinedIcon /> :
                                                    <DesktopAccessDisabledOutlinedIcon />,
                                                onClick: () => {
                                                    dispatch(toggleComponentTree());
                                                },
                                            },
                                            {
                                                tooltip: 'Formular als antragstellende Person öffnen (in neuem Tab)',
                                                icon: <LaunchOutlinedIcon />,
                                                href: `/${loadedForm.form.slug ?? ''}/${loadedForm.version.version ?? ''}`,
                                            },
                                        ]}
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
                                                elementData={elementData}
                                                onElementDataChange={setElementData}
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
                                                px: 2,
                                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                                borderLeft: '1px solid #E0E7E0',
                                                borderRadius: 0,
                                                position: 'relative',
                                                height: '100%',
                                                overflow: 'hidden',
                                            }}
                                        >
                                            <ElementTree
                                                entity={loadedForm}
                                                onPatch={handlePatch}
                                                editable={isEditable}
                                                scope="application"
                                                enabledIdentityProviderInfos={identityProviderInfos}
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
                                        elementData={elementData}
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
                                                    setElementData(state.elementData);
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

                <AdminToolsDialog
                    open={showAdminTools}
                    onClose={() => {
                        setShowAdminTools(false);
                    }}
                />

                <FormRevisionsDialog
                    open={showRevisions}
                    onClose={() => setShowRevisions(false)}
                />
            </PageWrapper>
        );
    }
}

