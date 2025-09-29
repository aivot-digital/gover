import {Box, Grid, Paper, ThemeProvider, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {clearLoadedForm, redoLoadedForm, selectFutureLoadedForm, selectLoadedForm, selectPastLoadedForm, showDialog, undoLoadedForm, updateLoadedForm} from '../../../../slices/app-slice';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {useParams, useSearchParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../../theming/themes';
import {NotFoundPage} from '../../../../components/not-found-page/not-found-page';
import {resetAdminSettings, toggleComponentTree, toggleValidation, toggleVisibility} from '../../../../slices/admin-settings-slice';
import {AppToolbar} from '../../../../components/app-toolbar/app-toolbar';
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
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
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
import {ThemesApiService} from '../../../themes/themes-api-service';
import {FormsApiService} from '../../forms-api-service';
import {enqueueSnackbar} from 'notistack';
import {FormRevisionsDialog} from '../../dialogs/form-revisions-dialog';
import {ElementTreeEntity} from '../../../../components/element-tree/element-tree-entity';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../slices/loading-overlay-slice';
import {withAsyncWrapper} from '../../../../utils/with-async-wrapper';
import {useDidUpdateEffect} from '../../../../hooks/use-did-update-effect';
import {IdentityProviderInfo} from '../../../identity/models/identity-provider-info';
import {setIdentityId} from '../../../../slices/identity-slice';
import {ElementData, ElementDerivationResponse} from '../../../../models/element-data';
import {asFormRequestDTO, FormDetailsResponseDTO} from '../../dtos/form-details-response-dto';
import {FormStatus} from '../../enums/form-status';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {addDerivationLogItems} from '../../../../slices/logging-slice';
import {RootState} from '../../../../store';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {GenericPageHeader} from '../../../../components/generic-page-header/generic-page-header';

export const DialogSearchParam = 'dialog';

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

    const [elementData, setElementData] = useState<ElementData>({});

    const {
        disableVisibility,
        disableValidation,
        hideComponentTree,
    } = useAppSelector((state: RootState) => state.adminSettings);

    const loadedForm = useAppSelector(selectLoadedForm);
    const systemThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

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
            if (formId == null) {
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
        dispatch(setCurrentStep(0));
        dispatch(setIdentityId(undefined));
        setFailedToLoad(false);
        if (formId != null && formVersion != null) {
            new FormsApiService(api)
                .retrieve({
                    id: formId,
                    version: formVersion,
                })
                .then((app) => {
                    CustomerInputService.cleanCustomerInput(app);
                    dispatch(updateLoadedForm(app));
                })
                .catch((err) => {
                    console.error(err);
                    setFailedToLoad(true);
                });
            fetchLockState(formId);
        }
    }, [formId, formVersion, dispatch]);

    useEffect(() => {
        if (loadedForm == null) {
            return;
        }

        if (loadedForm.themeId === theme?.id) {
            return;
        }

        if (loadedForm.themeId != null && !isNaN(loadedForm.themeId)) {
            new ThemesApiService(api)
                .retrieve(loadedForm.themeId)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
            return;
        }

        if (parseInt(systemThemeId) === theme?.id) {
            return;
        }

        if (systemThemeId != null && !isNaN(parseInt(systemThemeId))) {
            new ThemesApiService(api)
                .retrieve(parseInt(systemThemeId))
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [loadedForm, systemThemeId]);

    useEffect(() => {
        if (loadedForm == null) {
            return;
        }

        if (identityProviderInfos != null) {
            return;
        }

        new FormsApiService(api)
            .getIdentityProviders(loadedForm.slug, loadedForm.version)
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
                return new FormsApiService(api)
                    .determineFormState(
                        loadedForm.slug,
                        loadedForm.version,
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
        if (formId == null || formVersion == null) {
            return;
        }

        dispatch(undoLoadedForm());
        dispatch(showLoadingOverlay('Speichern…'));
        new FormsApiService(api)
            .update({
                id: formId,
                version: formVersion,
            }, pastLoadedForm[pastLoadedForm.length - 1])
            .then((loadedForm) => {
                new FormsApiService(api)
                    .determineFormState(
                        loadedForm.slug,
                        loadedForm.version,
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
        new FormsApiService(api)
            .update({
                id: formId,
                version: formVersion,
            }, futureLoadedForm[futureLoadedForm.length - 1])
            .then((loadedForm) => {
                new FormsApiService(api)
                    .determineFormState(
                        loadedForm.slug,
                        loadedForm.version,
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
        const allElements = flattenElements(loadedForm.rootElement);

        const isEditable = (
            loadedForm.status == FormStatus.Drafted &&
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
                                {
                                    id: loadedForm.id,
                                    version: loadedForm.version,
                                },
                                asFormRequestDTO(updatedAppModel as FormDetailsResponseDTO),
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
                        console.log('Determening Form State');
                        const newState = await apiService
                            .determineFormState(
                                loadedForm.slug,
                                loadedForm.version,
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
            <PageWrapper
                title={`Editor - ${loadedForm.internalTitle} — Version ${loadedForm.version}`}
                fullWidth={true}
                fullHeight={true}
            >
                <Box
                    sx={{
                        height: '100vh',
                        display: 'flex',
                    }}
                >
                    {/* Working Area */}
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            flex: 1,
                            px: 2,
                            py: 2,
                        }}
                    >
                        <GenericPageHeader
                            title={'Formular: ' + loadedForm.internalTitle}
                            badge={{
                                color: 'default',
                                label: `Version ${loadedForm.version}`,
                            }}
                            icon={ModuleIcons.forms}
                            actions={[
                                {
                                    tooltip: 'Änderung rückgängig machen',
                                    icon: <UndoIcon />,
                                    onClick: handleUndo,
                                    disabled: !hasPastLoadedForm,
                                    visible: loadedForm.status === FormStatus.Drafted,
                                },
                                {
                                    tooltip: 'Änderung wiederherstellen',
                                    icon: <RedoIcon />,
                                    onClick: handleRedo,
                                    disabled: !hasFutureLoadedForm,
                                    visible: loadedForm.status === FormStatus.Drafted,
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

                        <Paper
                            sx={{
                                overflowY: 'scroll',
                                flex: 1,
                                mt: 2,
                            }}
                            ref={scrollContainerRef}
                        >
                            <ThemeProvider theme={_theme}>
                                <ViewDispatcherComponent
                                    rootElement={loadedForm.rootElement}
                                    allElements={allElements}
                                    element={loadedForm.rootElement}
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
                            </ThemeProvider>
                        </Paper>
                    </Box>

                    {/* Element Tree */}
                    {
                        !hideComponentTree &&
                        <Paper
                            sx={{
                                px: 2,
                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                borderLeft: '1px solid #E0E7E0',
                                borderRadius: 0,
                                position: 'relative',
                                width: '24rem',
                                height: '100vh',
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
                    }
                </Box>

                <AdminToolsDialog
                    open={showAdminTools}
                    onClose={() => {
                        setShowAdminTools(false);
                    }}
                />
                <DeveloperTools
                    rootElement={loadedForm.rootElement}
                    elementData={elementData}
                    onElementDataChange={setElementData}
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
                <FormRevisionsDialog
                    open={showRevisions}
                    onClose={() => setShowRevisions(false)}
                />
            </PageWrapper>
        );
    }
}

