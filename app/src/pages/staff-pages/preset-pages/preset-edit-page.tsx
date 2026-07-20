import React, {useEffect, useMemo, useRef, useState} from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {useNavigate, useParams} from 'react-router-dom';
import {NotFoundPage} from '../../../components/not-found-page/not-found-page';
import {type Preset} from '../../../models/entities/preset';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {
    removeLoadingSnackbar,
    showErrorSnackbar,
    showLoadingSnackbar,
    showSuccessSnackbar,
} from '../../../slices/snackbar-slice';
import {flattenElements} from '../../../utils/flatten-elements';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import DoneAllOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/DoneAll';
import {type PresetVersion} from '../../../models/entities/preset-version';
import {VersionsPresetDialog} from '../../../dialogs/preset-dialogs/versions-preset-dialog/versions-preset-dialog';
import {determinePresetVersionDescriptor} from '../../../utils/determine-preset-version-descriptor';
import {useApi} from '../../../hooks/use-api';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {PresetsApiService} from '../../../modules/presets/presets-api-service';
import {PresetVersionApiService} from '../../../modules/presets/preset-version-api-service';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {IdentityProviderInfo} from '../../../modules/identity/models/identity-provider-info';
import {IdentityProvidersApiService} from '../../../modules/identity/identity-providers-api-service';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    ElementDerivationResponse,
} from '../../../models/element-data';
import {FormStatus} from '../../../modules/forms/enums/form-status';
import {useConfirm} from '../../../providers/confirm-provider';
import {addDerivationLogItems} from '../../../slices/logging-slice';
import {addEntityHistoryItem} from '../../../slices/entity-history-slice';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {useElementSize} from '../../../utils/element-size';
import {Allotment} from 'allotment';
import {Paper} from '@mui/material';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import NewWindow from '@aivot/mui-material-symbols-400-n25-outlined/NewWindow';
import HomeStorage from '@aivot/mui-material-symbols-400-n25-outlined/HomeStorage';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {collectErrors} from '../../../components/error-alert/error-alert';
import {RootStructureActionsContextProvider} from '../../../components/form/root-structure-actions-context';
import {ElementTree} from '../../../components/element-tree-2/element-tree';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';

export function PresetEditPage() {
    const api = useApi();
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const {
        ref: containerRef,
        size: containerSize,
    } = useElementSize<HTMLDivElement>();
    const scrollContainerRef = useRef<HTMLDivElement>(null);

    const [isBusy, setIsBusy] = useState(false);
    const [isDeriving, setIsDeriving] = useState(false);

    const navigate = useNavigate();
    const {
        key: presetKey,
        version: versionNumberStr,
    } = useParams();

    const versionNumber = useMemo(() => {
        if (versionNumberStr == null) {
            return 0;
        }
        const v = parseInt(versionNumberStr);
        return isNaN(v) ? 0 : v;
    }, [versionNumberStr]);

    const [preset, setPreset] = useState<Preset>();
    const [presetVersion, setPresetVersion] = useState<PresetVersion>();
    const [identityProviders, setIdentityProviders] = useState<IdentityProviderInfo[]>([]);

    const presetsApiService = useMemo(() => {
        return new PresetsApiService(api);
    }, [api]);

    const [networkError, setNetworkError] = useState<{
        title: string;
        message: string;
    }>();

    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const [showPresetVersions, setShowPresetVersions] = useState(false);

    const [authoredElementValues, setAuthoredElementValues] = useState<AuthoredElementValues>({});
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(createDerivedRuntimeElementData());
    const [openAddSectionSignal, setOpenAddSectionSignal] = useState(0);
    const rootStructureActions = useMemo(() => ({
        canAddAtRoot: presetVersion?.status === FormStatus.Drafted,
        openAddAtRootDialog: () => {
            setOpenAddSectionSignal((prev) => prev + 1);
        },
    }), [presetVersion?.status]);

    const [toolbarHeight, setToolbarHeight] = useState<number>(0);
    const updateToolbarHeight = (height: number) => {
        setToolbarHeight(height);
    };

    // Fetch all available identity providers
    useEffect(() => {
        new IdentityProvidersApiService()
            .listAll()
            .then(res => setIdentityProviders(res.content.map(idp => ({
                key: idp.key,
                name: idp.name,
                type: idp.type,
                iconAssetKey: '',
                metadataIdentifier: idp.metadataIdentifier,
            }))));
    }, [api]);

    // Fetch the preset on key or version change.
    // The version change is needed to update the current version field in the preset.
    useEffect(() => {
        if (presetKey == null) {
            setPreset(undefined);
            return;
        }

        presetsApiService
            .retrieve(presetKey)
            .then((preset) => {
                setPreset(preset);
                dispatch(addEntityHistoryItem({
                    type: ServerEntityType.Presets,
                    link: `/presets/edit/${preset.key}/${versionNumber}`,
                    title: preset.title,
                }));
            })
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
            .determinePresetState(preset.key, presetVersion.version, authoredElementValues, {
                disableVisibilities: false,
                disableValidation: true,
            })
            .then(({elementData, logItems}) => {
                setDerivedData(elementData);
                dispatch(addDerivationLogItems(logItems));
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
            .update(preset.key, {
                title: preset.title,
                rootElement: {} as any,
            })
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
        if (!preset) {
            return;
        }

        const conf = await showConfirm({
            title: 'Neuen Entwurf anlegen',
            children: (
                <Typography>
                    Möchten Sie wirklich eine neuen Entwurf (Arbeitsversion) der Vorlage {preset.title} anlegen?
                </Typography>
            ),
            confirmButtonText: 'Ja, Entwurf anlegen',
            isDestructive: false,
        });

        if (!conf) {
            return;
        }

        const presetVersionApiService = new PresetVersionApiService(api, preset.key);

        const newPresetVersion: PresetVersion = {
            presetKey: preset.key,
            version: 0,
            status: FormStatus.Drafted,
            rootElement: presetVersion ? presetVersion.rootElement : generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
            published: null,
            revoked: null,
        };

        presetVersionApiService.create(newPresetVersion)
            .then((createdVersion) => {
                if (!presetVersion) {
                    setPreset({
                        ...preset,
                        draftedVersion: createdVersion.version,
                    });
                    setPresetVersion(createdVersion);
                    setNetworkError(undefined);
                } else {
                    dispatch(showSuccessSnackbar('Neue Version (' + createdVersion.version + ') wurde erfolgreich angelegt.'));
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


        dispatch(showLoadingOverlay('Speichern'));

        new PresetVersionApiService(api, preset.key)
            .update(presetVersion.version, updatedPreset)
            .then((updatedPresetVersion) => {
                setPresetVersion(updatedPresetVersion);
                setIsDeriving(true);

                return presetsApiService
                    .determinePresetState(preset.key, presetVersion.version, authoredElementValues, {
                        disableVisibilities: false,
                        disableValidation: true,
                    });
            })
            .then(({elementData, logItems}) => {
                setDerivedData(elementData);
                dispatch(addDerivationLogItems(logItems));
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

        setIsBusy(true);

        presetsApiService
            .determinePresetState(preset.key, presetVersion.version, authoredElementValues, {
                disableValidation: false,
                disableVisibilities: false,
            })
            .then(({elementData, logItems}) => {
                setDerivedData(elementData);

                dispatch(addDerivationLogItems(logItems));

                if (collectErrors(presetVersion.rootElement, authoredElementValues, elementData).length === 0) {
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

    const handleValueChange = (elementData: AuthoredElementValues) => {
        if (preset == null || presetVersion == null) {
            return;
        }

        if (isBusy) {
            return;
        }

        setAuthoredElementValues(elementData);

        setIsDeriving(true);
        dispatch(showLoadingSnackbar('Berechnungen werden durchgeführt…'));

        withAsyncWrapper<void, ElementDerivationResponse>({
            desiredMinRuntime: 600,
            main: async () => {
                return await presetsApiService.determinePresetState(
                    preset.key,
                    presetVersion.version,
                    elementData,
                    {
                        disableValidation: true,
                        disableVisibilities: false,
                    },
                );
            },
        }).then(({elementData, logItems}) => {
            setDerivedData(elementData);
            dispatch(addDerivationLogItems(logItems));
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
        return <LoadingPlaceholder/>;
    }

    const allElements = flattenElements(presetVersion.rootElement);

    return (
        <PageWrapper
            title={`Vorlage - ${preset.title} - ${versionNumber ?? ''} (${determinePresetVersionDescriptor(preset, presetVersion)})`}
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
                    <RootStructureActionsContextProvider
                        value={rootStructureActions}
                    >
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
                                        title={`Vorlage: ${preset.title} - ${versionNumber ?? ''} (${determinePresetVersionDescriptor(preset, presetVersion)})`}
                                        badge={{
                                            color: 'default',
                                            label: `Version ${presetVersion.version}`,
                                        }}
                                        icon={ModuleIcons.presets}
                                        actions={[
                                            {
                                                icon: <NewWindow/>,
                                                tooltip: 'Neuen Entwurf anlegen',
                                                onClick: handleAddNewVersion,
                                            },
                                            {
                                                icon: <HomeStorage/>,
                                                tooltip: 'Versionen anzeigen',
                                                onClick: () => {
                                                    setShowPresetVersions(true);
                                                },
                                            },
                                            {
                                                icon: <DoneAllOutlinedIcon/>,
                                                tooltip: isBusy ? 'Validierung läuft bereits' : 'Validierung durchführen',
                                                onClick: handleValidate,
                                                disabled: isBusy,
                                            },
                                            {
                                                icon: <Delete color={'error'}/>,
                                                tooltip: 'Version der Vorlage löschen',
                                                onClick: () => {
                                                    setConfirmDelete(() => handleDelete);
                                                },
                                                visible: presetVersion.status != FormStatus.Published,
                                            },
                                        ]}
                                    />

                                    <Paper
                                        sx={{
                                            overflowY: 'auto',
                                            flex: 1,
                                            mt: 2,
                                            p: 4,
                                            minHeight: 0,
                                            borderTopLeftRadius: 10,
                                            borderTopRightRadius: 10,
                                            borderBottomLeftRadius: 0,
                                            borderBottomRightRadius: 0,
                                        }}
                                    >
                                        {
                                            /* TODO: use ViewDispatcherContext
                                        <ViewDispatcherComponent
                                            rootElement={presetVersion.rootElement}
                                            allElements={allElements}
                                            element={presetVersion.rootElement}
                                            scrollContainerRef={scrollContainerRef}
                                            isBusy={false}
                                            isDeriving={false}
                                            mode="editor"
                                            authoredElementValues={authoredElementValues}
                                            derivedData={derivedData}
                                            onAuthoredElementValuesChange={handleValueChange}
                                            onDerivedDataChange={setDerivedData}
                                            onElementBlur={undefined}
                                            derivationTriggerIdQueue={[] /* Not necessary because this is kept internally by the root component view }
                                            disableVisibility={false}
                                            onDerive={() => {

                                            }}
                                        />
                                        */
                                        }
                                    </Paper>
                                </Box>
                            </Allotment.Pane>

                            <Allotment.Pane
                                minSize={480}
                                preferredSize={480}
                            >
                                {/* Element Tree */}
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
                                        value={presetVersion.rootElement}
                                        onChange={(p) => {
                                            handlePatch({
                                                ...presetVersion,
                                                rootElement: p as any,
                                            });
                                        }}
                                        editable={presetVersion.status == FormStatus.Drafted}
                                        openRootAddElementSignal={openAddSectionSignal}
                                        displayContext={ElementDisplayContext.CitizenFacing}
                                        allowElementIdEditing={false}
                                    />
                                </Paper>
                            </Allotment.Pane>
                        </Allotment>
                    </RootStructureActionsContextProvider>
                </Allotment>
            </Box>

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
        </PageWrapper>
    );
}
