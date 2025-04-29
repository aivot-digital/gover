import {Grid, type Theme as MuiTheme, ThemeProvider} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {type RootState} from '../../../store';
import {
    clearCustomerInput,
    clearErrors,
    clearLoadedForm,
    clearVisibilities,
    hydrateFromDerivationWithoutErrors,
    redoLoadedForm,
    selectFutureLoadedForm,
    selectLoadedForm,
    selectPastLoadedForm,
    setFormState,
    showDialog,
    undoLoadedForm,
    updateLoadedForm,
} from '../../../slices/app-slice';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {useParams, useSearchParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {resetAdminSettings, toggleComponentTree, toggleValidation, toggleVisibility} from '../../../slices/admin-settings-slice';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {AdminToolsDialog} from '../../../dialogs/admin-tools/admin-tools-dialog';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {setCurrentStep} from '../../../slices/stepper-slice';
import {flattenElements} from '../../../utils/flatten-elements';
import {HelpDialog, HelpDialogId} from '../../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../../dialogs/accessibility-dialog/accessibility-dialog';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import RemoveDoneOutlinedIcon from '@mui/icons-material/RemoveDoneOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import HandymanOutlinedIcon from '@mui/icons-material/HandymanOutlined';
import DesktopAccessDisabledOutlinedIcon from '@mui/icons-material/DesktopAccessDisabledOutlined';
import DesktopWindowsOutlinedIcon from '@mui/icons-material/DesktopWindowsOutlined';
import LaunchOutlinedIcon from '@mui/icons-material/LaunchOutlined';
import {type Theme} from '../../../modules/themes/models/theme';
import {ApplicationStatus} from '../../../data/application-status';
import {selectMemberships, selectUser} from '../../../slices/user-slice';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useApi} from '../../../hooks/use-api';
import {EntityLockDto} from '../../../models/dtos/entity-lock-dto';
import {EntityLockState} from '../../../data/entity-lock-state';
import {useUsersApi} from '../../../hooks/use-users-api';
import {getFullName, User} from '../../../models/entities/user';
import {CustomerInputService} from '../../../services/customer-input-service';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import {isAdmin} from '../../../utils/is-admin';
import UndoIcon from '@mui/icons-material/Undo';
import RedoIcon from '@mui/icons-material/Redo';
import {DeveloperTools} from '../../../components/developer-tools/developer-tools';
import {ThemesApiService} from '../../../modules/themes/themes-api-service';
import {FormsApiService} from '../../../modules/forms/forms-api-service';
import {enqueueSnackbar} from 'notistack';
import {FormRevisionsDialog} from '../../../modules/forms/dialogs/form-revisions-dialog';
import {Form} from '../../../models/entities/form';
import {ElementTreeEntity} from '../../../components/element-tree/element-tree-entity';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {FormState} from '../../../models/dtos/form-state';
import {useDidUpdateEffect} from '../../../hooks/use-did-update-effect';
import {IdentityProviderInfo} from '../../../modules/identity/models/identity-provider-info';

export const DialogSearchParam = 'dialog';

export function FormEditPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const params = useParams();

    const {
        id: formIdStr,
    } = useParams();

    const dispatch = useAppDispatch();
    const api = useApi();

    const [showAdminTools, setShowAdminTools] = useState(false);
    const [showRevisions, setShowRevisions] = useState(false);

    const {
        disableVisibility,
        disableValidation,
        hideComponentTree,
    } = useAppSelector((state: RootState) => state.adminSettings);

    const loadedForm = useAppSelector(selectLoadedForm);
    const customerInput = useAppSelector((state) => state.app.inputs);

    const pastLoadedForm = useAppSelector(selectPastLoadedForm);
    const hasPastLoadedForm = useMemo(() => pastLoadedForm.length > 0, [pastLoadedForm]);

    const futureLoadedForm = useAppSelector(selectFutureLoadedForm);
    const hasFutureLoadedForm = useMemo(() => futureLoadedForm.length > 0, [futureLoadedForm]);

    const [failedToLoad, setFailedToLoad] = useState(false);
    const [lockState, setLockState] = useState<EntityLockDto & { user?: User }>();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const [identityProviderInfos, setIdentityProviderInfos] = useState<IdentityProviderInfo[]>([]);

    const [theme, setTheme] = useState<Theme>();

    const [toolbarHeight, setToolbarHeight] = useState<number>(0);
    const updateToolbarHeight = (height: number) => {
        setToolbarHeight(height);
    };

    // Cleanup lock state on unload
    useEffect(() => {
        function handleCleanup() {
            // Use the formId here and not the loaded form to prevent the cleanup function being triggered on each form change
            const formId = parseInt(formIdStr ?? '');
            if (isNaN(formId)) {
                return;
            }

            new FormsApiService(api)
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
        dispatch(clearLoadedForm());
        dispatch(resetAdminSettings());
        dispatch(clearCustomerInput());
        dispatch(clearErrors());
        dispatch(setCurrentStep(0));
        setFailedToLoad(false);
        if (formIdStr != null) {
            const id = parseInt(formIdStr);
            new FormsApiService(api)
                .retrieve(id)
                .then((app) => {
                    CustomerInputService.cleanCustomerInput(app);
                    dispatch(updateLoadedForm(app));
                })
                .catch((err) => {
                    console.error(err);
                    setFailedToLoad(true);
                });
            fetchLockState(id);
        }
    }, [formIdStr, dispatch]);

    useEffect(() => {
        if (loadedForm?.themeId != null) {
            new ThemesApiService(api)
                .retrieve(loadedForm.themeId)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        } else {
            setTheme(undefined);
        }

        if (loadedForm != null) {
            FormsApiService
                .getIdentityProviders(loadedForm.id)
                .then(res => setIdentityProviderInfos(res.content));
        }
    }, [loadedForm]);

    useDidUpdateEffect(() => {
        if (loadedForm == null) {
            return;
        }

        dispatch(showLoadingOverlay('Sichtbarkeiten berechnen…'));

        withAsyncWrapper<any, FormState>({
            desiredMinRuntime: 600,
            main: () => {
                return new FormsApiService(api)
                    .determineFormState(
                        loadedForm.id,
                        customerInput,
                        {
                            stepsToValidate: ['NONE'],
                            stepsToCalculateVisibilities: disableVisibility ? ['NONE'] : ['ALL'],
                            stepsToCalculateValues: ['ALL'],
                            stepsToCalculateOverrides: ['ALL'],
                        },
                    );
            },
        })
            .then((newState) => {
                dispatch(setFormState({
                    formState: newState,
                    options: {
                        freshVisibilities: true,
                        ignoreErrors: true,
                    },
                }));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    }, [disableVisibility]);

    function fetchLockState(id: number) {
        new FormsApiService(api)
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
                setFailedToLoad(true);
            });
    }

    const handleUndo = () => {
        dispatch(undoLoadedForm());
        dispatch(showLoadingOverlay('Speichern…'));
        new FormsApiService(api)
            .update(loadedForm!.id, pastLoadedForm[pastLoadedForm.length - 1])
            .then((loadedForm) => {
                new FormsApiService(api)
                    .determineFormState(
                        loadedForm.id,
                        customerInput,
                        {
                            stepsToValidate: ['NONE'],
                            stepsToCalculateVisibilities: disableVisibility ? ['NONE'] : ['ALL'],
                            stepsToCalculateValues: ['ALL'],
                            stepsToCalculateOverrides: ['ALL'],
                        },
                    )
                    .then((state) => {
                        dispatch(clearVisibilities());
                        dispatch(clearErrors());
                        dispatch(hydrateFromDerivationWithoutErrors(state));
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
        dispatch(redoLoadedForm());
        dispatch(showLoadingOverlay('Speichern…'));
        new FormsApiService(api)
            .update(loadedForm!.id, futureLoadedForm[futureLoadedForm.length - 1])
            .then((loadedForm) => {
                new FormsApiService(api)
                    .determineFormState(
                        loadedForm.id,
                        customerInput,
                        {
                            stepsToValidate: ['NONE'],
                            stepsToCalculateVisibilities: disableVisibility ? ['NONE'] : ['ALL'],
                            stepsToCalculateValues: ['ALL'],
                            stepsToCalculateOverrides: ['ALL'],
                        },
                    )
                    .then((state) => {
                        dispatch(clearVisibilities());
                        dispatch(clearErrors());
                        dispatch(hydrateFromDerivationWithoutErrors(state));
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

    if (Boolean(failedToLoad)) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />

                <NotFoundPage />
            </>
        );
    } else if (loadedForm == null) {
        return <LoadingPlaceholder />;
    } else {
        const allElements = flattenElements(loadedForm.root);

        const isEditable = (
            loadedForm.status !== ApplicationStatus.Published &&
            loadedForm.status !== ApplicationStatus.Revoked &&
            (memberships ?? []).some((mem) => mem.departmentId === loadedForm.developingDepartmentId) &&
            (lockState == null || lockState.state === EntityLockState.Free || lockState.state === EntityLockState.LockedSelf)
        );
        const canViewHistory = isAdmin(user) || (memberships ?? []).some((mem) => mem.departmentId === loadedForm.developingDepartmentId);

        const handlePatch = async (patch: Partial<ElementTreeEntity>) => {
            if (loadedForm == null) {
                return;
            }

            if (!isEditable) {
                dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                return;
            }

            const updatedAppModel = {
                ...loadedForm,
                ...patch,
            };

            const originalApplication = {
                ...loadedForm,
            };

            dispatch(showLoadingOverlay('Speichern…'));

            const apiService = new FormsApiService(api);

            await withAsyncWrapper({
                desiredMinRuntime: 600,
                main: async () => {
                    try {
                        const updatedForm = await apiService
                            .update(
                                updatedAppModel.id,
                                updatedAppModel as Form,
                            );
                        dispatch(updateLoadedForm(updatedForm));
                    } catch (err: any) {
                        if (err.status === 403) {
                            dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                        } else if (err.status === 423) {
                            dispatch(showErrorSnackbar('Das Formular wird aktuell durch eine andere Mitarbeiter:in bearbeitet.'));
                            fetchLockState(loadedForm.id);
                        } else {
                            console.error(err);
                            dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                        }

                        dispatch(updateLoadedForm(originalApplication));
                    }

                    try {
                        const newState = await apiService
                            .determineFormState(
                                updatedAppModel.id,
                                customerInput,
                                {
                                    stepsToValidate: ['NONE'],
                                    stepsToCalculateVisibilities: disableVisibility ? ['NONE'] : ['ALL'],
                                    stepsToCalculateValues: ['ALL'],
                                    stepsToCalculateOverrides: ['ALL'],
                                },
                            );
                        dispatch(setFormState({
                            formState: newState,
                            options: {
                                freshVisibilities: true,
                                ignoreErrors: true,
                            },
                        }));
                    } catch (err: any) {
                        console.error(err);
                        dispatch(showErrorSnackbar('Die Formularzustände konnten nicht berechnet werden.'));
                    }
                },
                after: async () => {
                    dispatch(hideLoadingOverlay());
                },
            });
        };

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={`Editor - ${loadedForm.title} - ${loadedForm.version}`}
                />

                <AppToolbar
                    title={`${loadedForm.title} - ${loadedForm.version}`}
                    updateToolbarHeight={updateToolbarHeight}
                    actions={[
                        {
                            tooltip: 'Änderung rückgängig machen',
                            icon: <UndoIcon />,
                            onClick: handleUndo,
                            disabled: !hasPastLoadedForm,
                            visible: loadedForm.status === ApplicationStatus.Drafted || loadedForm.status === ApplicationStatus.InReview,
                        },
                        {
                            tooltip: 'Änderung wiederherstellen',
                            icon: <RedoIcon />,
                            onClick: handleRedo,
                            disabled: !hasFutureLoadedForm,
                            visible: loadedForm.status === ApplicationStatus.Drafted || loadedForm.status === ApplicationStatus.InReview,
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
                            href: `/${loadedForm.slug ?? ''}/${loadedForm.version ?? ''}`,
                        },
                    ]}
                />


                <Grid
                    container
                    sx={{
                        minHeight: 'calc(100vh - ' + toolbarHeight + 'px)',
                    }}
                >
                    {
                        !hideComponentTree &&
                        <Grid
                            item
                            xs={4}
                            sx={{
                                px: 2,
                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                height: 'calc(100vh - ' + toolbarHeight + 'px)',
                                overflowY: 'scroll',
                                borderRight: '1px solid #E0E7E0',
                                position: 'relative',
                            }}
                        >
                            <ElementTree
                                entity={loadedForm}
                                onPatch={handlePatch}
                                editable={isEditable}
                                scope="application"
                                enabledIdentityProviderInfos={identityProviderInfos}
                            />
                        </Grid>
                    }

                    <Grid
                        item
                        xs={hideComponentTree ? 12 : 8}
                        sx={{
                            height: 'calc(100vh - ' + toolbarHeight + 'px)',
                            overflowY: 'scroll',
                        }}
                        ref={scrollContainerRef}
                    >
                        <ViewDispatcherComponent
                            allElements={allElements}
                            element={loadedForm.root}
                            scrollContainerRef={scrollContainerRef}
                            isBusy={false}
                            isDeriving={false}
                            mode="editor"
                        />
                    </Grid>
                </Grid>

                <AdminToolsDialog
                    open={showAdminTools}
                    onClose={() => {
                        setShowAdminTools(false);
                    }}
                />

                <DeveloperTools />

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

                <FormRevisionsDialog
                    open={showRevisions}
                    onClose={() => setShowRevisions(false)}
                />
            </ThemeProvider>
        );
    }
}

