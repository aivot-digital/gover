import {Grid, Theme, ThemeProvider} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {RootState} from '../../../store';
import {
    clearAppModel,
    fetchApplicationById,
    selectApplicationLoadFailed,
    selectLoadedApplication,
    updateAppModel,
} from '../../../slices/app-slice';
import {
    LoadingPlaceholderComponentView
} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {useParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {
    resetAdminSettings,
    toggleComponentTree,
    toggleValidation,
    toggleVisibility
} from '../../../slices/admin-settings-slice';
import {resetUserInput} from '../../../slices/customer-input-slice';
import {UserInputDebugger} from '../../../components/user-input-debugger/user-input-debugger';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {
    faDisplay,
    faDisplaySlash,
    faExternalLink,
    faEye,
    faEyeSlash,
    faScrewdriverWrench,
    faText,
    faTextSlash
} from '@fortawesome/pro-light-svg-icons';
import {AdminToolsDialog} from '../../../dialogs/admin-tools/admin-tools-dialog';
import {Localization} from '../../../locale/localization';
import strings from './application-editor-page-strings.json';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {setCurrentStep} from '../../../slices/stepper-slice';

const _ = Localization(strings);

export function ApplicationEditorPage() {
    useAuthGuard();

    const params = useParams();
    const dispatch = useAppDispatch();

    const [showAdminTools, setShowAdminTools] = useState(false);

    const adminSettings = useAppSelector((state: RootState) => state.adminSettings);
    const application = useAppSelector(selectLoadedApplication);
    const failedToLoad = useAppSelector(selectApplicationLoadFailed);

    useEffect(() => {
        dispatch(clearAppModel());
        dispatch(resetAdminSettings());
        dispatch(resetUserInput());
        dispatch(setCurrentStep(0));
        if (params.id != null) {
            const id = parseInt(params.id)
            dispatch(fetchApplicationById(id));
        }
    }, [params.id, dispatch]);

    if (failedToLoad) {
        return (
            <>
                <AppToolbar
                    title={_.notFoundTitle}
                    parentPath={'/overview'}
                />
                <NotFoundPage/>
            </>
        )
    } else if (application == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        return (
            <ThemeProvider theme={(baseTheme: Theme) => createAppTheme(application?.root.theme, baseTheme)}>
                <MetaElement
                    title={`Editor - ${application.root.title ?? ''}`}
                />

                <AppToolbar
                    title={application.root.title ?? ''}
                    parentPath={'/overview'}
                    actions={[
                        {
                            tooltip: adminSettings.disableValidation ? _.enableValidations : _.disableValidations,
                            icon: adminSettings.disableValidation ? faText : faTextSlash,
                            onClick: () => {
                                dispatch(toggleValidation());
                            },
                        },
                        {
                            tooltip: adminSettings.disableVisibility ? _.enableVisibilities : _.disableVisibilities,
                            icon: adminSettings.disableVisibility ? faEye : faEyeSlash,
                            onClick: () => {
                                dispatch(toggleVisibility());
                            },
                        },
                        'separator',
                        {
                            tooltip: _.openAdminToolsTooltip,
                            icon: faScrewdriverWrench,
                            onClick: () => {
                                setShowAdminTools(true);
                            },
                        },
                        {
                            tooltip: adminSettings.hideComponentTree ? _.showElementTreeTooltip : _.hideElementTreeTooltip,
                            icon: adminSettings.hideComponentTree ? faDisplay : faDisplaySlash,
                            onClick: () => {
                                dispatch(toggleComponentTree());
                            },
                        },
                        {
                            tooltip: _.openAsCustomer,
                            icon: faExternalLink,
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
                                element={application.root}
                                onPatch={patch => {
                                    dispatch(updateAppModel({
                                        ...application,
                                        root: {
                                            ...application?.root,
                                            ...patch,
                                        }
                                    }));
                                }}
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
                            element={application.root}
                        />
                    </Grid>
                </Grid>

                <AdminToolsDialog
                    open={showAdminTools}
                    onClose={() => setShowAdminTools(false)}
                />

                <UserInputDebugger/>
            </ThemeProvider>
        );
    }
}

