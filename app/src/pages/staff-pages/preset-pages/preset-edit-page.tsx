import {Container, Grid, Theme, ThemeProvider} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {
    LoadingPlaceholderComponentView
} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {useNavigate, useParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {PresetsService} from '../../../services/presets.service';
import {Preset} from '../../../models/entities/preset';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {flattenElements} from "../../../utils/flatten-elements";
import {faTrashAlt} from "@fortawesome/pro-light-svg-icons";
import {ConfirmDialog} from "../../../dialogs/confirm-dialog/confirm-dialog";

export function PresetEditPage() {
    useAuthGuard();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const params = useParams();

    const defaultTheme = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [preset, setPreset] = useState<Preset>();
    const [failedToLoad, setFailedToLoad] = useState<boolean>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    useEffect(() => {
        if (params.id != null) {
            const id = parseInt(params.id)
            PresetsService.retrieve(id)
                .then(result => {
                    setPreset(result);
                })
                .catch(() => {
                    setFailedToLoad(true);
                });
        }
    }, [params]);

    useEffect(() => {
        if (preset != null) {
            PresetsService.update(preset.id, preset)
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Failed to save preset'));
                });
        }
    }, [preset]);

    if (failedToLoad) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />
                <NotFoundPage/>
            </>
        )
    } else if (preset == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const allElements = flattenElements(preset.root);

        return (
            <ThemeProvider theme={(baseTheme: Theme) => createAppTheme(defaultTheme, baseTheme)}>
                <MetaElement
                    title={`Vorlagen-Editor - ${preset.root.name ?? ''}`}
                />

                <AppToolbar
                    title={preset.root.name ?? ''}
                    noPlaceholder={true}
                    actions={[{
                        icon: faTrashAlt,
                        tooltip: 'Vorlage löschen',
                        onClick: () => setConfirmDelete(() => () => {
                            PresetsService.destroy(preset.id);
                            navigate('/presets');
                        })
                    }]}
                />

                <Grid
                    container
                    sx={{
                        minHeight: '100vh',
                    }}
                >
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
                            element={preset.root}
                            onPatch={patch => setPreset({
                                ...preset,
                                root: {
                                    ...preset.root,
                                    ...patch,
                                },
                            })}
                        />
                    </Grid>

                    <Grid
                        item
                        xs={8}
                        sx={{
                            pt: 8,
                            height: '100vh',
                            overflowY: 'scroll',
                        }}
                    >
                        <Container>
                            <ViewDispatcherComponent
                                allElements={allElements}
                                element={preset.root}
                            />
                        </Container>
                    </Grid>
                </Grid>

                <ConfirmDialog
                    title="Vorlage wirklich löschen"
                    onConfirm={confirmDelete}
                    onCancel={() => setConfirmDelete(undefined)}
                >
                    Sind Sie sicher, dass Sie die Vorlage <strong>{preset.root.name}</strong> wirklich löschen wollen?
                    Bitte beachten Sie, dass dies <u>nicht rückgängig</u> gemacht werden kann!
                </ConfirmDialog>
            </ThemeProvider>
        );
    }
}

