import {Container, Grid, Theme, ThemeProvider} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {
    LoadingPlaceholderComponentView
} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {useParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {useAuthGuard} from '../../../hooks/use-auth-guard';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {Localization} from '../../../locale/localization';
import strings from './preset-editor-page-strings.json';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {PresetsService} from '../../../services/presets.service';
import {Preset} from '../../../models/entities/preset';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {ElementTree} from '../../../components/element-tree/element-tree';

const _ = Localization(strings);

export function PresetEditorPage() {
    useAuthGuard();

    const dispatch = useAppDispatch();
    const params = useParams();

    const defaultTheme = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [preset, setPreset] = useState<Preset>();
    const [failedToLoad, setFailedToLoad] = useState<boolean>();

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
                    title={_.notFoundTitle}
                    parentPath={'/presets'}
                />
                <NotFoundPage/>
            </>
        )
    } else if (preset == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        return (
            <ThemeProvider theme={(baseTheme: Theme) => createAppTheme(defaultTheme, baseTheme)}>
                <MetaElement
                    title={`Preset-Editor - ${preset.root.name ?? ''}`}
                />

                <AppToolbar
                    title={preset.root.name ?? ''}
                    parentPath={'/presets'}
                    noPlaceholder={true}
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
                                element={preset.root}
                            />
                        </Container>
                    </Grid>
                </Grid>
            </ThemeProvider>
        );
    }
}

