import {Grid, type Theme as MuiTheme, ThemeProvider} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {type RootState} from '../../../store';
import {clearAppModel, fetchApplicationById, MetaDialog, selectApplicationLoadFailed, selectLoadedApplication, showMetaDialog, updateAppModel} from '../../../slices/app-slice';
import {LoadingPlaceholderComponentView} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {useParams, useSearchParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {resetAdminSettings, toggleComponentTree, toggleValidation, toggleVisibility} from '../../../slices/admin-settings-slice';
import {resetUserInput} from '../../../slices/customer-input-slice';
import {UserInputDebugger} from '../../../components/user-input-debugger/user-input-debugger';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {AdminToolsDialog} from '../../../dialogs/admin-tools/admin-tools-dialog';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {setCurrentStep} from '../../../slices/stepper-slice';
import {flattenElements} from '../../../utils/flatten-elements';
import {HelpDialog} from '../../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog} from '../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog} from '../../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog} from '../../../dialogs/accessibility-dialog/accessibility-dialog';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import RemoveDoneOutlinedIcon from '@mui/icons-material/RemoveDoneOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import HandymanOutlinedIcon from '@mui/icons-material/HandymanOutlined';
import DesktopAccessDisabledOutlinedIcon from '@mui/icons-material/DesktopAccessDisabledOutlined';
import DesktopWindowsOutlinedIcon from '@mui/icons-material/DesktopWindowsOutlined';
import LaunchOutlinedIcon from '@mui/icons-material/LaunchOutlined';
import {ThemesService} from '../../../services/themes-service';
import {type Theme} from '../../../models/entities/theme';
import {ApplicationStatus} from '../../../data/application-status/application-status';

export function ApplicationEditPage(): JSX.Element {
    const [searchParams, setSearchParams] = useSearchParams();
    const metaDialogName = searchParams.get('dialog');

    const params = useParams();
    const dispatch = useAppDispatch();

    const [showAdminTools, setShowAdminTools] = useState(false);

    const adminSettings = useAppSelector((state: RootState) => state.adminSettings);
    const application = useAppSelector(selectLoadedApplication);
    const failedToLoad = useAppSelector(selectApplicationLoadFailed);
    const metaDialog = useAppSelector((state) => state.app.showMetaDialog);

    const [theme, setTheme] = useState<Theme>();

    useEffect(() => {
        if (metaDialogName != null) {
            dispatch(showMetaDialog(metaDialogName as MetaDialog));
            setSearchParams({});
        }
    }, [metaDialogName]);

    useEffect(() => {
        dispatch(clearAppModel());
        dispatch(resetAdminSettings());
        dispatch(resetUserInput());
        dispatch(setCurrentStep(0));
        if (params.id != null) {
            const id = parseInt(params.id);
            dispatch(fetchApplicationById(id));
        }
    }, [params.id, dispatch]);

    useEffect(() => {
        if (application?.theme != null) {
            ThemesService
                .retrieve(application.theme)
                .then(setTheme)
                .catch((err) => {
                    console.error(err);
                });
        }
    }, [application]);

    if (failedToLoad === true) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />

                <NotFoundPage/>
            </>
        );
    } else if (application == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const allElements = flattenElements(application.root);

        return (
            <ThemeProvider theme={(baseTheme: MuiTheme) => createAppTheme(theme, baseTheme)}>
                <MetaElement
                    title={`Editor - ${application.title} - ${application.version}`}
                />

                <AppToolbar
                    title={`${application.title} - ${application.version}`}
                    actions={[
                        {
                            tooltip: adminSettings.disableValidation ? 'Validierungen aktivieren' : 'Validierungen deaktivieren',
                            icon: adminSettings.disableValidation ? <DoneAllOutlinedIcon/> : <RemoveDoneOutlinedIcon/>,
                            onClick: () => {
                                dispatch(toggleValidation());
                            },
                        },
                        {
                            tooltip: adminSettings.disableVisibility ? 'Sichtbarkeiten aktivieren' : 'Sichtbarkeiten deaktivieren',
                            icon: adminSettings.disableVisibility ? <VisibilityOutlinedIcon/> : <VisibilityOffOutlinedIcon/>,
                            onClick: () => {
                                dispatch(toggleVisibility());
                            },
                        },
                        'separator',
                        {
                            tooltip: 'Admin-Werkzeuge öffnen',
                            icon: <HandymanOutlinedIcon/>,
                            onClick: () => {
                                setShowAdminTools(true);
                            },
                        },
                        {
                            tooltip: adminSettings.hideComponentTree ? 'Formularstruktur einblenden' : 'Formularstruktur ausblenden',
                            icon: adminSettings.hideComponentTree ? <DesktopWindowsOutlinedIcon/> : <DesktopAccessDisabledOutlinedIcon/>,
                            onClick: () => {
                                dispatch(toggleComponentTree());
                            },
                        },
                        {
                            tooltip: 'Formular als Antragsteller:in öffnen (in neuem Tab)',
                            icon: <LaunchOutlinedIcon/>,
                            href: `/#/${application.slug ?? ''}/${application.version ?? ''}`,
                        },
                    ]}
                    noPlaceholder={true}
                />


                <Grid
                    container
                    sx={{
                        minHeight: '100vh',
                    }}
                >
                    {
                        !adminSettings.hideComponentTree &&
                        <Grid
                            item
                            xs={4}
                            sx={{
                                pt: 8,
                                px: 2,
                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                height: '100vh',
                                overflowY: 'scroll',
                                borderRight: '1px solid #E0E7E0',
                                position: 'relative',
                            }}
                        >
                            <ElementTree
                                entity={application}
                                onPatch={(patch) => {
                                    dispatch(updateAppModel({
                                        ...application,
                                        ...patch,
                                    }));
                                }}
                                editable={
                                    application.status !== ApplicationStatus.Published &&
                                    application.status !== ApplicationStatus.Revoked
                                }
                            />
                        </Grid>
                    }

                    <Grid
                        item
                        xs={adminSettings.hideComponentTree ? 12 : 8}
                        sx={{
                            pt: 8,
                            height: '100vh',
                            overflowY: 'scroll',
                        }}
                    >
                        <ViewDispatcherComponent
                            allElements={allElements}
                            element={application.root}
                        />
                    </Grid>
                </Grid>

                <AdminToolsDialog
                    open={showAdminTools}
                    onClose={() => {
                        setShowAdminTools(false);
                    }}
                />

                <UserInputDebugger/>

                <HelpDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Help}
                />

                <PrivacyDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Privacy}
                />

                <ImprintDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Imprint}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showMetaDialog(undefined))}
                    open={metaDialog === MetaDialog.Accessibility}
                />
            </ThemeProvider>
        );
    }
}

