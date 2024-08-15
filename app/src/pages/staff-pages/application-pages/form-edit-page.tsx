import {Alert, Grid, Snackbar, type Theme as MuiTheme, ThemeProvider} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {type RootState} from '../../../store';
import {
    clearCustomerInput,
    clearErrors,
    clearLoadedForm,
    selectLoadedForm,
    showDialog,
    updateLoadedForm,
} from '../../../slices/app-slice';
import {
    LoadingPlaceholder,
} from '../../../components/loading-placeholder/loading-placeholder';
import {useParams, useSearchParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {
    resetAdminSettings,
    toggleComponentTree,
    toggleValidation,
    toggleVisibility,
} from '../../../slices/admin-settings-slice';
import {UserInputDebugger} from '../../../components/user-input-debugger/user-input-debugger';
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
import {type Theme} from '../../../models/entities/theme';
import {ApplicationStatus} from '../../../data/application-status';
import {selectMemberships, selectUser} from '../../../slices/user-slice';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useThemesApi} from '../../../hooks/use-themes-api';
import {useApi} from '../../../hooks/use-api';
import {useFormsApi} from '../../../hooks/use-forms-api';
import {EntityLockDto} from '../../../models/dtos/entity-lock-dto';
import {EntityLockState} from '../../../data/entity-lock-state';
import {useUsersApi} from '../../../hooks/use-users-api';
import {getFullName, User} from '../../../models/entities/user';
import {CustomerInputService} from '../../../services/customer-input-service';
import {FormRevisionsDialog} from '../../../dialogs/form-revisions-dialog/form-revisions-dialog';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import {isAdmin} from '../../../utils/is-admin';

export const DialogSearchParam = 'dialog';

export function FormEditPage(): JSX.Element {
    const [searchParams, setSearchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const params = useParams();
    const dispatch = useAppDispatch();
    const api = useApi();

    const [showAdminTools, setShowAdminTools] = useState(false);
    const [showRevisions, setShowRevisions] = useState(false);

    const adminSettings = useAppSelector((state: RootState) => state.adminSettings);
    const application = useAppSelector(selectLoadedForm);
    const [failedToLoad, setFailedToLoad] = useState(false);
    const [lockState, setLockState] = useState<EntityLockDto & { user?: User }>();

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);
    const metaDialog = useAppSelector((state) => state.app.showDialog);

    const [theme, setTheme] = useState<Theme>();

    const [toolbarHeight, setToolbarHeight] = useState<number>(0);
    const updateToolbarHeight = (height: number) => {
        setToolbarHeight(height);
    };

    useEffect(() => {
        if (application != null) {
            return () => {
                useFormsApi(api)
                    .deleteLockState(application.id)
                    .catch((err) => {
                        console.error(err);
                    });
            };
        }
    }, [application]);

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
        if (params.id != null) {
            const id = parseInt(params.id);
            useFormsApi(api)
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
    }, [params.id, dispatch]);

    useEffect(() => {
        if (application?.themeId != null) {
            useThemesApi(api)
                .retrieveTheme(application.themeId)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        } else {
            setTheme(undefined);
        }
    }, [application]);

    function fetchLockState(id: number) {
        useFormsApi(api)
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

    const scrollContainerRef = useRef<HTMLDivElement>(null);

    if (Boolean(failedToLoad)) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />

                <NotFoundPage />
            </>
        );
    } else if (application == null) {
        return <LoadingPlaceholder />;
    } else {
        const allElements = flattenElements(application.root);

        const isEditable = (
            application.status !== ApplicationStatus.Published &&
            application.status !== ApplicationStatus.Revoked &&
            (memberships ?? []).some((mem) => mem.departmentId === application.developingDepartmentId) &&
            (lockState == null || lockState.state === EntityLockState.Free || lockState.state === EntityLockState.LockedSelf)
        );

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={`Editor - ${application.title} - ${application.version}`}
                />

                <AppToolbar
                    title={`${application.title} - ${application.version}`}
                    updateToolbarHeight={updateToolbarHeight}
                    actions={[
                        ...(isAdmin(user) ? [
                                {
                                    tooltip: 'Historie anzeigen',
                                    icon: <AccessTimeIcon />,
                                    onClick: () => {
                                        setShowRevisions(true);
                                    },
                                },
                            ] : []
                        ),
                        {
                            tooltip: adminSettings.disableValidation ? 'Validierungen aktivieren' : 'Validierungen deaktivieren',
                            icon: adminSettings.disableValidation ? <DoneAllOutlinedIcon /> :
                                <RemoveDoneOutlinedIcon />,
                            onClick: () => {
                                dispatch(toggleValidation());
                            },
                        },
                        {
                            tooltip: adminSettings.disableVisibility ? 'Sichtbarkeiten aktivieren' : 'Sichtbarkeiten deaktivieren',
                            icon: adminSettings.disableVisibility ? <VisibilityOutlinedIcon /> :
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
                            tooltip: adminSettings.hideComponentTree ? 'Formularstruktur einblenden' : 'Formularstruktur ausblenden',
                            icon: adminSettings.hideComponentTree ? <DesktopWindowsOutlinedIcon /> :
                                <DesktopAccessDisabledOutlinedIcon />,
                            onClick: () => {
                                dispatch(toggleComponentTree());
                            },
                        },
                        {
                            tooltip: 'Formular als antragstellende Person öffnen (in neuem Tab)',
                            icon: <LaunchOutlinedIcon />,
                            href: `/${application.slug ?? ''}/${application.version ?? ''}`,
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
                        !adminSettings.hideComponentTree &&
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
                                entity={application}
                                onPatch={(patch) => {
                                    if (application != null) {
                                        if (isEditable) {
                                            const updatedAppModel = {
                                                ...application,
                                                ...patch,
                                            };
                                            const orignalApplication = {
                                                ...application,
                                            };
                                            dispatch(updateLoadedForm(updatedAppModel));
                                            useFormsApi(api)
                                                .save(updatedAppModel)
                                                .catch((err) => {
                                                    if (err.status === 403) {
                                                        dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                                                    } else if (err.status === 423) {
                                                        dispatch(showErrorSnackbar('Das Formular wird aktuell durch eine andere Mitarbeiter:in bearbeitet.'));
                                                        fetchLockState(application.id);
                                                    } else {
                                                        console.error(err);
                                                        dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                                                    }
                                                    dispatch(updateLoadedForm(orignalApplication));
                                                });
                                        } else {
                                            dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                                        }
                                    }
                                }}
                                editable={isEditable}
                                scope="application"
                            />
                        </Grid>
                    }

                    <Grid
                        item
                        xs={adminSettings.hideComponentTree ? 12 : 8}
                        sx={{
                            height: 'calc(100vh - ' + toolbarHeight + 'px)',
                            overflowY: 'scroll',
                        }}
                        ref={scrollContainerRef}
                    >
                        <ViewDispatcherComponent
                            allElements={allElements}
                            element={application.root}
                            scrollContainerRef={scrollContainerRef}
                        />
                    </Grid>
                </Grid>

                <AdminToolsDialog
                    open={showAdminTools}
                    onClose={() => {
                        setShowAdminTools(false);
                    }}
                />

                <UserInputDebugger />

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

                {
                    lockState != null &&
                    lockState.state === EntityLockState.LockedOther &&
                    <Snackbar
                        color="warning"
                        open={true}
                        anchorOrigin={{
                            vertical: 'bottom',
                            horizontal: 'center',
                        }}
                    >
                        <Alert
                            severity="error"
                            sx={{width: '100%'}}
                            variant="filled"
                        >
                            Dieses Formular wird aktuell von {getFullName(lockState.user)} bearbeitet.
                        </Alert>
                    </Snackbar>
                }

                <FormRevisionsDialog
                    open={showRevisions}
                    onClose={() => setShowRevisions(false)}
                />
            </ThemeProvider>
        );
    }
}

