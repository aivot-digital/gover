import React, {useEffect, useState} from 'react';
import {Container, Grid, ThemeProvider} from '@mui/material';
import {LoadingPlaceholderComponentView} from '../../../components/static-components/loading-placeholder/loading-placeholder.component.view';
import {useNavigate, useParams} from 'react-router-dom';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {createDefaultAppTheme} from '../../../theming/themes';
import {NotFoundPage} from '../../../components/static-components/not-found-page/not-found-page';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AppToolbar} from '../../../components/app-toolbar/app-toolbar';
import {PresetsService} from '../../../services/presets.service';
import {type Preset} from '../../../models/entities/preset';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {ElementTree} from '../../../components/element-tree/element-tree';
import {flattenElements} from '../../../utils/flatten-elements';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {PublishPresetDialog} from '../../../dialogs/publish-preset-dialog/publish-preset-dialog';
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import {isElementValid} from '../../../utils/is-element-valid';
import {useLogging} from '../../../hooks/use-logging';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectCustomerInput} from '../../../slices/customer-input-slice';
import {resetErrors} from '../../../slices/customer-input-errors-slice';

export function PresetEditPage(): JSX.Element {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const params = useParams();

    const [$debug] = useLogging();

    const [preset, setPreset] = useState<Preset>();
    const [failedToLoad, setFailedToLoad] = useState<boolean>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const [showPublishDialog, setShowPublishDialog] = useState(false);

    const customerData = useAppSelector(selectCustomerInput);

    useEffect(() => {
        if (params.id != null) {
            const id = parseInt(params.id);
            PresetsService.retrieve(id)
                .then((result) => {
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
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Vorlage konnte nicht gespeichert werden'));
                });
        }
    }, [preset]);

    const handleDelete = (): void => {
        if (preset == null) {
            return;
        }

        PresetsService
            .destroy(preset.id)
            .then(() => {
                navigate('/presets');
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Vorlage konnte nicht gelöscht werden'));
            });
    };

    if (failedToLoad === true) {
        return (
            <>
                <AppToolbar
                    title="Nicht gefunden"
                />
                <NotFoundPage
                    title="Vorlage nicht gefunden"
                    msg="Die von Ihnen aufgerufene Vorlage konnte nicht gefunden werden."
                />
            </>
        );
    } else if (preset == null) {
        return <LoadingPlaceholderComponentView/>;
    } else {
        const allElements = flattenElements(preset.root);

        const handleValidate = (): void => {
            dispatch(resetErrors());
            const isValid = isElementValid(
                $debug,
                undefined,
                allElements,
                dispatch,
                preset.root,
                customerData,
            );
            if (isValid) {
                dispatch(showSuccessSnackbar('Eingaben sind valide'));
            } else {
                dispatch(showErrorSnackbar('Eingaben sind nicht valide'));
            }
        };

        return (
            <ThemeProvider theme={createDefaultAppTheme}>
                <MetaElement
                    title={`Vorlagen-Editor - ${preset.root.name ?? ''}`}
                />

                <AppToolbar
                    title={preset.root.name ?? ''}
                    noPlaceholder={true}
                    actions={[
                        {
                            icon: <DoneAllOutlinedIcon/>,
                            tooltip: 'Validierung durchführen',
                            label: 'Validieren',
                            onClick: handleValidate,
                        },
                        {
                            icon: <PublishOutlinedIcon/>,
                            tooltip: 'Vorlage veröffentlichen',
                            label: 'Veröffentlichen',
                            onClick: () => {
                                setShowPublishDialog(true);
                            },
                        },
                        {
                            icon: <DeleteForeverOutlinedIcon/>,
                            tooltip: 'Vorlage löschen',
                            onClick: () => {
                                setConfirmDelete(() => handleDelete);
                            },
                        },
                    ]}
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
                            entity={preset}
                            onPatch={(patch) => {
                                setPreset({
                                    ...preset,
                                    ...patch,
                                });
                            }}
                            editable={true}
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
                    onCancel={() => {
                        setConfirmDelete(undefined);
                    }}
                >
                    Sind Sie sicher, dass Sie die Vorlage <strong>{preset.root.name}</strong> wirklich löschen wollen?
                    Bitte beachten Sie, dass dies <u>nicht rückgängig</u> gemacht werden kann!
                </ConfirmDialog>

                <PublishPresetDialog
                    preset={showPublishDialog ? preset : undefined}
                    onClose={() => {
                        setShowPublishDialog(false);
                    }}
                />
            </ThemeProvider>
        );
    }
}

