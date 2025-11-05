import {Box, Paper, ThemeProvider, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {clearLoadedForm, redoLoadedForm, selectFutureLoadedForm, selectLoadedForm, selectPastLoadedForm, showDialog, undoLoadedForm, updateLoadedForm} from '../../../../slices/app-slice';
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
import {FormsApiService} from '../../forms-api-service';
import {FormsApiService as FormsApiServiceV2} from '../../forms-api-service-v2';
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
import {setErrorMessage, setLoadingMessage} from '../../../../slices/shell-slice';

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

    const formApiService = useMemo(() => new FormsApiServiceV2(), []);

    const [theme, setTheme] = useState<Theme>();

    const {ref: containerRef, size: containerSize} = useElementSize<HTMLDivElement>();
    const developerToolsMinHeight = 280;
    const developerToolsMaxHeight = containerSize.height > 0 ? Math.max(developerToolsMinHeight, Math.floor(containerSize.height * 0.5)) : undefined;

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

        new FormsApiService(api)
            .retrieve({
                id: formId,
                version: formVersion,
            })
            .then((app) => {
                CustomerInputService.cleanCustomerInput(app);
                dispatch(updateLoadedForm(app));
                dispatch(addEntityHistoryItem({
                    title: app.internalTitle,
                    link: `/forms/${app.id}/${app.version}`,
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

        if (loadedForm.themeId === theme?.id) {
            return;
        }

        formApiService
            .getFormTheme(loadedForm.slug, loadedForm.version)
            .then((theme) => {
                setTheme(theme);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Das Farbschema des Formulars konnte nicht geladen werden.'));
            });
    }, [loadedForm]);

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
                return formApiService
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
                formApiService
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
                formApiService
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

    if (loadedForm == null) {
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
                        const newState = await formApiService
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
                                <Box sx={{height: '100%', overflow: 'auto'}}>
                                    <DeveloperTools
                                        rootElement={loadedForm.rootElement}
                                        elementData={elementData}
                                        onElementDataChange={setElementData}
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

