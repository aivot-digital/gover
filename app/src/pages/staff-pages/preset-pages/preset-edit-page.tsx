import React, {useEffect, useMemo, useState} from 'react';
import {Box, Button, Container, Grid, Typography} from '@mui/material';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {useNavigate, useParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {NotFoundPage} from '../../../components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {type Preset} from '../../../models/entities/preset';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {
    removeLoadingSnackbar,
    showErrorSnackbar,
    showLoadingSnackbar,
    showSuccessSnackbar
} from '../../../slices/snackbar-slice';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {flattenElements} from '../../../utils/flatten-elements';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import {type PresetVersion} from '../../../models/entities/preset-version';
import DriveFolderUploadOutlinedIcon from '@mui/icons-material/DriveFolderUploadOutlined';
import {VersionsPresetDialog} from '../../../dialogs/preset-dialogs/versions-preset-dialog/versions-preset-dialog';
import {determinePresetVersionDescriptor} from '../../../utils/determine-preset-version-descriptor';
import {useApi} from '../../../hooks/use-api';
import {clearErrors, hydrateFromDerivation, hydrateFromDerivationWithoutErrors} from '../../../slices/app-slice';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {PresetsApiService} from '../../../modules/presets/presets-api-service';
import {PresetVersionApiService} from '../../../modules/presets/preset-version-api-service';
import {usePrompt} from '../../../providers/prompt-provider';
import {CustomerInput} from '../../../models/customer-input';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {withAsyncWrapper} from "../../../utils/with-async-wrapper";
import {FormState} from "../../../models/dtos/form-state";

export function PresetEditPage(): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const showPrompt = usePrompt();

    const [isBusy, setIsBusy] = useState(false);
    const [isDeriving, setIsDeriving] = useState(false);

    const navigate = useNavigate();
    const {
        key: presetKey,
        version: versionNumber,
    } = useParams();

    const [preset, setPreset] = useState<Preset>();
    const [presetVersion, setPresetVersion] = useState<PresetVersion>();

    const presetsApiService = useMemo(() => {
        return new PresetsApiService(api);
    }, [api]);

    const [networkError, setNetworkError] = useState<{
        title: string;
        message: string;
    }>();

    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const [showPresetVersions, setShowPresetVersions] = useState(false);

    const [customerData, setCustomerData] = useState<CustomerInput>({});

    const [toolbarHeight, setToolbarHeight] = useState<number>(0);
    const updateToolbarHeight = (height: number) => {
        setToolbarHeight(height);
    };

    // Fetch the preset on key or version change.
    // The version change is needed to update the current version field in the preset.
    useEffect(() => {
        if (presetKey == null) {
            setPreset(undefined);
            return;
        }

        presetsApiService
            .retrieve(presetKey)
            .then(setPreset)
            .catch(() => {
                setNetworkError({
                    title: 'Fehler beim Laden der Vorlage',
                    message: 'Die Vorlage konnte nicht geladen werden',
                });
            });
    }, [presetKey, versionNumber]);

    // Fetch the version of the preset.
    useEffect(() => {
        if (preset == null || versionNumber == null) {
            setPresetVersion(undefined);
            return;
        }

        new PresetVersionApiService(api, preset.key)
            .retrieve(versionNumber)
            .then(setPresetVersion)
            .catch(() => setNetworkError({
                title: 'Fehler beim Laden der Vorlage-Version',
                message: 'Die Version der Vorlage konnte nicht geladen werden',
            }));

        setShowPresetVersions(false);
    }, [preset, versionNumber]);

    // Fetch the preset state and hydrate the form state.
    useEffect(() => {
        if (preset == null || presetVersion == null) {
            return;
        }

        presetsApiService
            .determinePresetState(preset.key, presetVersion.version, customerData, {
                disableVisibilities: false,
                disableValidation: true,
            })
            .then((presetState) => {
                dispatch(hydrateFromDerivationWithoutErrors(presetState));
            })
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Berechnen des Formularzustands'));
            });
    }, [preset, presetVersion]);

    useEffect(() => {
        if (preset == null) {
            return;
        }

        presetsApiService
            .update(preset.key, preset)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
            });
    }, [preset]);

    const handleDelete = () => {
        if (preset == null || presetVersion == null) {
            return;
        }

        const presetVersionApiService = new PresetVersionApiService(api, preset.key);
        presetVersionApiService.destroy(presetVersion.version)
            .then(() => navigate('/presets', {replace: true}))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Version konnte nicht gelöscht werden'));
            });
    };

    const handleAddNewVersion = async () => {
        if (!preset) return;

        const newVersion = await showPrompt({
            title: 'Neue Version anlegen',
            message: 'Bitte geben Sie eine Versionsnummer für die neue Vorlagen-Version ein:',
            inputLabel: 'Versionsnummer',
            inputPlaceholder: versionNumber ?? '1.0.0',
            confirmButtonText: 'Erstellen',
            cancelButtonText: 'Abbrechen',
            defaultValue: versionNumber ?? '1.0.0',
        });

        if (!newVersion) return;
        const presetVersionApiService = new PresetVersionApiService(api, preset.key);

        const newPresetVersion: PresetVersion = {
            preset: preset.key,
            version: newVersion,
            root: presetVersion ? presetVersion.root : generateElementWithDefaultValues(ElementType.Container) as GroupLayout,
            publishedAt: null,
            publishedStoreAt: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };

        presetVersionApiService.create(newPresetVersion)
            .then((createdVersion) => {
                if (!presetVersion) {
                    setPreset({...preset, currentVersion: createdVersion.version});
                    setPresetVersion(createdVersion);
                    setNetworkError(undefined);
                } else {
                    dispatch(showSuccessSnackbar('Neue Vorlagen-Version (' + createdVersion.version + ') wurde erfolgreich angelegt.'));
                    navigate(`/presets/edit/${preset.key}/${createdVersion.version}`, {replace: true});
                }
            })
            .catch((err) => {
                if (err.status === 409) {
                    dispatch(showErrorSnackbar('Diese Version existiert bereits'));
                } else {
                    dispatch(showErrorSnackbar('Version konnte nicht gespeichert werden'));
                }
            });
    };

    const handlePatch = (patch: Partial<PresetVersion>) => {
        if (preset == null || presetVersion == null) {
            return;
        }

        const updatedPreset = {
            ...presetVersion,
            ...patch,
        };


        dispatch(showLoadingOverlay('Speichern...'));

        new PresetVersionApiService(api, preset.key)
            .update(presetVersion.version, updatedPreset)
            .then((updatedPresetVersion) => {
                setPresetVersion(updatedPresetVersion);
                setIsDeriving(true);

                return presetsApiService
                    .determinePresetState(preset.key, presetVersion.version, customerData, {
                        disableVisibilities: false,
                        disableValidation: true,
                    });
            })
            .then((presetState) => {
                dispatch(hydrateFromDerivationWithoutErrors(presetState));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
            })
            .finally(() => {
                setIsDeriving(false);
                dispatch(hideLoadingOverlay());
            });
    };

    const handleValidate = () => {
        if (preset == null || presetVersion == null) {
            return;
        }

        if (isBusy) {
            return;
        }

        dispatch(clearErrors());
        setIsBusy(true);

        presetsApiService
            .determinePresetState(preset.key, presetVersion.version, customerData, {
                disableValidation: false,
                disableVisibilities: false,
            })
            .then((presetState) => {
                dispatch(hydrateFromDerivation(presetState));

                // errors always contains 3 base errors from the form (can change if form will extend in the future)
                if (presetState.errors && Object.keys(presetState.errors).length <= 3) {
                    dispatch(showSuccessSnackbar('Bei der Validierung sind keine Fehler aufgetreten.'));
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Validierung fehlgeschlagen'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handleValueChange = (key: string, value: any) => {
        if (preset == null || presetVersion == null) {
            return;
        }

        if (isBusy) {
            return;
        }

        const newData = {
            ...customerData,
            [key]: value,
        };
        setCustomerData(newData);

        setIsDeriving(true);
        dispatch(showLoadingSnackbar('Berechnungen werden durchgeführt…'));

        withAsyncWrapper<void, FormState>({
            desiredMinRuntime: 600,
            main: async () => {
                return await presetsApiService.determinePresetState(
                    preset.key,
                    presetVersion.version,
                    newData,
                    {
                        disableValidation: true,
                        disableVisibilities: false,
                    }
                );
            },
        }).then((presetState) => {
            dispatch(hydrateFromDerivationWithoutErrors(presetState));
        }).catch((err) => {
            console.error(err);
            dispatch(showErrorSnackbar('Fehler beim Berechnen des Formularzustands'));
        }).finally(() => {
            setIsDeriving(false);
            dispatch(removeLoadingSnackbar());
        });
    };

    if (networkError != null) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />

                <NotFoundPage
                    title={networkError.title}
                    msg={networkError.message}
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
    }

    if (preset == null || presetVersion == null) {
        return <LoadingPlaceholder />;
    }

    const allElements = flattenElements(presetVersion.root);

    return (
        <>
            <MetaElement
                title={`Vorlagen-Editor - ${preset.title} - ${versionNumber ?? ''} (${determinePresetVersionDescriptor(preset, presetVersion)})`}
            />

            <AppToolbar
                title={`${preset.title} - ${versionNumber ?? ''} (${determinePresetVersionDescriptor(preset, presetVersion)})`}
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
                        tooltip: isBusy ? 'Validierung läuft bereits' : 'Validierung durchführen',
                        onClick: handleValidate,
                        disabled: isBusy,
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
                        onPatch={handlePatch}
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
                            isBusy={isBusy}
                            isDeriving={isDeriving}
                            valueOverride={{
                                values: customerData,
                                onChange: handleValueChange,
                            }}
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
                Diese Aktion kann nicht rückgängig gemacht werden.
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

