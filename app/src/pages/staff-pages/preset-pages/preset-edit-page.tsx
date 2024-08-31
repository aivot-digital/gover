import React, {useEffect, useState} from 'react';
import {Box, Button, Container, Grid, Typography} from '@mui/material';
import {
    LoadingPlaceholder,
} from '../../../components/loading-placeholder/loading-placeholder';
import {useNavigate, useParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {NotFoundPage} from '../../../components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {type Preset} from '../../../models/entities/preset';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {flattenElements} from '../../../utils/flatten-elements';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import {isElementValid} from '../../../utils/is-element-valid';
import {useLogging} from '../../../hooks/use-logging';
import {useAppSelector} from '../../../hooks/use-app-selector';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import {type PresetVersion} from '../../../models/entities/preset-version';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import {VersionsPresetDialog} from '../../../dialogs/preset-dialogs/versions-preset-dialog/versions-preset-dialog';
import {determinePresetVersionDescriptor} from '../../../utils/determine-preset-version-descriptor';
import {useApi} from '../../../hooks/use-api';
import {usePresetsApi} from '../../../hooks/use-presets-api';
import {clearErrors} from '../../../slices/app-slice';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';

export function PresetEditPage(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const {
        key,
        version,
    } = useParams();

    const [$debug] = useLogging();

    const [preset, setPreset] = useState<Preset>();
    const [presetVersion, setPresetVersion] = useState<PresetVersion>();

    const [failedToLoadPreset, setFailedToLoadPreset] = useState<boolean>();
    const [failedToLoadVersion, setFailedToLoadVersion] = useState<boolean>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();


    const [showPresetVersions, setShowPresetVersions] = useState(false);

    const customerData = useAppSelector(state => state.app.inputs);

    const [toolbarHeight, setToolbarHeight] = useState<number>(0);
    const updateToolbarHeight = (height: number) => {
        setToolbarHeight(height);
    };

    useEffect(() => {
        if (key != null) {
            usePresetsApi(api)
                .retrieve(key)
                .then(setPreset)
                .catch(() => {
                    setFailedToLoadPreset(true);
                });
        }
    }, [key, version]);

    useEffect(() => {
        if (preset != null && version != null) {
            usePresetsApi(api)
                .retrieveVersion(preset.key, version)
                .then(setPresetVersion)
                .catch(() => {
                    setFailedToLoadVersion(true);
                });
            setShowPresetVersions(false);
        }
    }, [preset, version]);

    useEffect(() => {
        if (preset != null) {
            usePresetsApi(api)
                .save(preset)
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
                });
        }
    }, [preset]);

    useEffect(() => {
        if (preset != null && presetVersion != null) {
            usePresetsApi(api)
                .updateVersion(preset.key, presetVersion)
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
                });
        }
    }, [preset, presetVersion]);

    const handleDelete = (): void => {
        if (preset == null || presetVersion == null) {
            return;
        }

        usePresetsApi(api)
            .destroyVersion(preset.key, presetVersion.version)
            .then(() => {
                navigate('/presets');
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Vorlage konnte nicht gelöscht werden'));
            });
    };

    const handleAddNewVersion = () => {
        if (preset != null) {
            const newVersion = prompt('Bitte geben Sie die neue Versionsnummer ein', version ?? '1.0.0');
            if (newVersion != null) {
                usePresetsApi(api)
                    .createVersion(preset.key, presetVersion != null ? {
                        ...presetVersion,
                        version: newVersion,
                        publishedAt: null,
                        publishedStoreAt: null,
                        created: new Date().toISOString(),
                        updated: new Date().toISOString(),
                    } : {
                        preset: preset.key,
                        root: generateElementWithDefaultValues(ElementType.Container) as GroupLayout,
                        version: newVersion,
                        publishedAt: null,
                        publishedStoreAt: null,
                        created: new Date().toISOString(),
                        updated: new Date().toISOString(),
                    })
                    .then((createdPresetVersion) => {
                        if (presetVersion == null) {
                            setPreset({
                                ...preset,
                                currentVersion: createdPresetVersion.version,
                            });
                            setPresetVersion(createdPresetVersion);
                            setFailedToLoadVersion(false);
                        } else {
                            navigate(`/presets/edit/${preset.key}/${createdPresetVersion.version}`, {replace: true});
                        }
                    })
                    .catch((err) => {
                        if (err.status === 409) {
                            dispatch(showErrorSnackbar('Diese Version existiert bereits'));
                        } else {
                            console.error(err);
                            dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
                        }
                    });
            }
        }
    };

    const failedToLoad = failedToLoadPreset || failedToLoadVersion;

    if (failedToLoad) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />

                <NotFoundPage
                    title={failedToLoadPreset ? 'Vorlage nicht gefunden' : 'Version der Vorlage nicht gefunden'}
                    msg={failedToLoadPreset ? 'Die von Ihnen aufgerufene Vorlage konnte nicht gefunden werden.' : 'Die von Ihnen aufgerufene Version der Vorlage konnte nicht gefunden werden.'}
                >
                    {
                        preset != null &&
                        <Box>
                            <Typography>
                                Möchten Sie für die Vorlage <strong>{preset.title}</strong> eine neue Version anlegen?
                            </Typography>

                            <Button
                                onClick={handleAddNewVersion}
                                sx={{mt: 2}}
                            >
                                Jetzt neue Version anlegen
                            </Button>
                        </Box>
                    }
                </NotFoundPage>


            </>
        );
    } else if (preset == null || presetVersion == null) {
        return <LoadingPlaceholder />;
    } else {
        const allElements = flattenElements(presetVersion.root);

        const handleValidate = (): void => {
            dispatch(clearErrors());
            const isValid = isElementValid(
                $debug,
                undefined,
                allElements,
                dispatch,
                presetVersion.root,
                customerData,
            );
            if (isValid) {
                dispatch(showSuccessSnackbar('Eingaben sind valide'));
            } else {
                dispatch(showErrorSnackbar('Eingaben sind nicht valide'));
            }
        };

        return (
            <>
                <MetaElement
                    title={`Vorlagen-Editor - ${preset.title} - ${version ?? ''} (${determinePresetVersionDescriptor(preset, presetVersion)})`}
                />

                <AppToolbar
                    title={`${preset.title} - ${version ?? ''} (${determinePresetVersionDescriptor(preset, presetVersion)})`}
                    updateToolbarHeight={updateToolbarHeight}
                    actions={[
                        {
                            icon: <DriveFolderUploadOutlinedIcon />,
                            tooltip: 'Neue Version anlegen',
                            onClick: handleAddNewVersion,
                        },
                        {
                            icon: <HistoryOutlinedIcon />,
                            tooltip: 'Versionen anzeigen',
                            onClick: () => {
                                setShowPresetVersions(true);
                            },
                        },
                        {
                            icon: <DoneAllOutlinedIcon />,
                            tooltip: 'Validierung durchführen',
                            onClick: handleValidate,
                        },
                    ].concat(presetVersion.publishedAt == null ? [
                        {
                            icon: <DeleteForeverOutlinedIcon />,
                            tooltip: 'Version der Vorlage löschen',
                            onClick: () => {
                                setConfirmDelete(() => handleDelete);
                            },
                        },
                    ] : [])}
                />

                <Grid
                    container
                    sx={{
                        minHeight: 'calc(100vh - ' + toolbarHeight + 'px)',
                    }}
                >
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
                            entity={presetVersion}
                            onPatch={(patch) => {
                                setPresetVersion({
                                    ...presetVersion,
                                    ...patch,
                                });
                            }}
                            editable={presetVersion.publishedAt == null && presetVersion.publishedStoreAt == null}
                            scope="preset"
                        />
                    </Grid>

                    <Grid
                        item
                        xs={8}
                        sx={{
                            height: 'calc(100vh - ' + toolbarHeight + 'px)',
                            overflowY: 'scroll',
                        }}
                    >
                        <Container>
                            <ViewDispatcherComponent
                                allElements={allElements}
                                element={presetVersion.root}
                            />
                        </Container>
                    </Grid>
                </Grid>

                <ConfirmDialog
                    title="Vorlage wirklich löschen"
                    onConfirm={confirmDelete}
                    onCancel={() => {
                        setConfirmDelete(undefined);
                    }}
                >
                    Sind Sie sicher, dass Sie die Version <strong>{presetVersion.version}</strong> der
                    Vorlage <strong>{preset.title}</strong> wirklich löschen wollen?
                    Bitte beachten Sie, dass dies <u>nicht rückgängig</u> gemacht werden kann!
                </ConfirmDialog>

                <VersionsPresetDialog
                    open={showPresetVersions}
                    onClose={() => {
                        setShowPresetVersions(false);
                    }}
                    preset={preset}
                />
            </>
        );
    }
}

