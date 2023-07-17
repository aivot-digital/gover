import React, {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {delayPromise} from '../../../utils/with-delay';
import {type Theme} from '../../../models/entities/theme';
import {ThemesService} from '../../../services/themes-service';
import {Box, Divider, Grid, List, ListItem, Typography} from '@mui/material';
import {SketchPicker} from 'react-color';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {type GridColDef} from '@mui/x-data-grid';
import {type ListApplication} from '../../../models/entities/list-application';
import {AlertComponent} from '../../../components/alert/alert-component';
import {TableWrapper} from '../../../components/table-wrapper/table-wrapper';
import {filterItems} from '../../../utils/filter-items';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';

const columns: Array<GridColDef<ListApplication>> = [
    {
        field: 'title',
        headerName: 'Title',
        flex: 1,
    },
    {
        field: 'version',
        headerName: 'Version',
        flex: 1,
    },
];

export function ThemeEditPage(): JSX.Element {
    useAdminGuard();

    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const themeId = useParams().id;

    const [originalTheme, setOriginalTheme] = useState<Theme>();
    const [editedTheme, setEditedTheme] = useState<Theme>();

    const [relatedApplications, setRelatedApplications] = useState<ListApplication[]>();
    const [searchRelatedApplication, setSearchRelatedApplication] = useState('');

    const [showNotDeletableDialog, setShowNotDeletableDialog] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const [isLoading, setIsLoading] = useState(false);
    const [isNotFound, setIsNotFound] = useState(false);

    const hasChanged = useChangeBlocker(originalTheme, editedTheme);

    useEffect(() => {
        if (themeId == null || themeId === 'new') {
            const newTheme: Theme = {
                id: 0,
                name: '',
                main: '#113a8d',
                mainDark: '#113a8d',
                accent: '#b6f1dc',
                error: '#BF261D',
                info: '#1D7C9C',
                warning: '#D18D23',
                success: '#449456',
            };
            setOriginalTheme(newTheme);
            setEditedTheme(newTheme);
        } else {
            setIsLoading(true);
            delayPromise(ThemesService.retrieve(parseInt(themeId)))
                .then((theme) => {
                    setOriginalTheme(theme);
                    setEditedTheme(theme);
                })
                .catch((err) => {
                    console.error(err);
                    setIsNotFound(true);
                })
                .finally(() => {
                    setIsLoading(false);
                });

            ThemesService
                .listApplications(parseInt(themeId))
                .then(setRelatedApplications)
                .catch((err) => {
                    console.error(err);
                    setRelatedApplications([]);
                    dispatch(showErrorSnackbar('Zugehörige Formulare konnten nicht geladen werden'));
                });
        }
    }, [themeId]);

    const handlePatch = (patch: Partial<Theme>): void => {
        if (editedTheme != null) {
            setEditedTheme({
                ...editedTheme,
                ...patch,
            });
        }
    };

    const handleSave = (): void => {
        if (editedTheme == null) {
            return;
        }

        setIsLoading(true);
        ThemesService
            .save(
                editedTheme.id !== 0 ? editedTheme.id : undefined,
                editedTheme,
            )
            .then((createdTheme) => {
                if (editedTheme.id === 0) {
                    setRelatedApplications([]);
                }
                setOriginalTheme(createdTheme);
                setEditedTheme(createdTheme);
                dispatch(showSuccessSnackbar('Theme erfolgreich gespeichert'));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Theme konnte nicht gespeichert werden'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    const handleReset = (): void => {
        if (originalTheme == null) {
            return;
        }
        setEditedTheme(originalTheme);
    };

    const handlePrepareDelete = (): void => {
        if (relatedApplications != null && relatedApplications.length > 0) {
            setShowNotDeletableDialog(true);
        } else {
            setConfirmDelete(() => handleDelete);
        }
    };

    const handleDelete = (): void => {
        if (editedTheme == null || editedTheme.id === 0) {
            return;
        }

        setIsLoading(true);
        ThemesService
            .destroy(editedTheme.id)
            .then(() => {
                navigate('/themes');
            })
            .catch((err) => {
                if (err.response?.status === 409) {
                    dispatch(showErrorSnackbar('Farbschema wird noch verwendet und kann nicht gelöscht werden'));
                } else {
                    console.error(err);
                    dispatch(showErrorSnackbar('Farbschema konnte nicht gelöscht werden'));
                }
                setIsLoading(false);
            });
    };

    return (
        <>
            <FormPageWrapper
                title="Farbschema bearbeiten"
                isLoading={isLoading}
                is404={isNotFound}
                hasChanged={hasChanged}
                onSave={handleSave}
                onReset={(editedTheme?.id ?? 0) !== 0 ? handleReset : undefined}
                onDelete={(editedTheme?.id ?? 0) !== 0 ? handlePrepareDelete : undefined}

                tabs={
                    editedTheme?.id !== 0 ?
                        [
                            {
                                label: 'Formulare',
                                content: (
                                    <>
                                        {
                                            relatedApplications != null &&
                                            relatedApplications.length === 0 &&
                                            <AlertComponent
                                                color="info"
                                            >
                                                Dieses Farbschema wird aktuell von keinem Formular verwendet.
                                            </AlertComponent>
                                        }

                                        {
                                            relatedApplications != null &&
                                            relatedApplications.length > 0 &&
                                            <TableWrapper
                                                columns={columns}
                                                rows={filterItems(relatedApplications, 'title', searchRelatedApplication)}
                                                onRowClick={(app) => {
                                                    navigate(`/edit/${app.id}`);
                                                }}
                                                title="Die folgenden Formulare verwenden dieses Farbschema"
                                                search={searchRelatedApplication}
                                                searchPlaceholder="Formular suchen"
                                                onSearchChange={setSearchRelatedApplication}
                                                actions={[]}
                                            />
                                        }
                                    </>
                                ),
                            },
                        ] :
                        undefined
                }
            >
                <TextFieldComponent
                    label="Name des Farbschemas"
                    placeholder="Neues Farbschema"
                    hint="Der Name des Farbschemas. Dieser wird nur Ihren Mitarbeiter:innen angezeigt."
                    value={editedTheme?.name}
                    onChange={(val) => {
                        handlePatch({
                            name: val,
                        });
                    }}
                    required
                    maxCharacters={96}
                />

                <Grid
                    container
                    columnSpacing={2}
                    rowSpacing={4}
                    sx={{
                        mt: 2,
                    }}
                >
                    <ColorPicker
                        label="Pimärfarbe"
                        value={editedTheme?.main}
                        onChange={(color) => {
                            handlePatch({
                                main: color,
                            });
                        }}
                    />

                    <ColorPicker
                        label="Pimärfarbe (Dunkel)"
                        value={editedTheme?.mainDark}
                        onChange={(color) => {
                            handlePatch({
                                mainDark: color,
                            });
                        }}
                    />

                    <ColorPicker
                        label="Akzentfarbe"
                        value={editedTheme?.accent}
                        onChange={(color) => {
                            handlePatch({
                                accent: color,
                            });
                        }}
                    />
                </Grid>

                <Divider
                    sx={{
                        my: 8,
                    }}
                />

                <Grid
                    container
                    columnSpacing={2}
                    rowSpacing={4}
                >
                    <ColorPicker
                        label="Fehlerfarbe"
                        value={editedTheme?.error}
                        onChange={(color) => {
                            handlePatch({
                                error: color,
                            });
                        }}
                    />

                    <ColorPicker
                        label="Warnungsfarbe"
                        value={editedTheme?.warning}
                        onChange={(color) => {
                            handlePatch({
                                warning: color,
                            });
                        }}
                    />

                    <ColorPicker
                        label="Informationsfarbe"
                        value={editedTheme?.info}
                        onChange={(color) => {
                            handlePatch({
                                info: color,
                            });
                        }}
                    />

                    <ColorPicker
                        label="Erfolgsfarbe"
                        value={editedTheme?.success}
                        onChange={(color) => {
                            handlePatch({
                                success: color,
                            });
                        }}
                    />
                </Grid>
            </FormPageWrapper>

            <InfoDialog
                title="Farbschema kann nicht gelöscht werden"
                severity="warning"
                open={showNotDeletableDialog}
                onClose={() => {
                    setShowNotDeletableDialog(false);
                }}
            >
                <Typography
                    variant="body1"
                    component="p"
                >
                    Dieses Farbschema kann nicht gelöscht werden, da es aktuell von einem oder mehreren Formularen
                    verwendet wird.
                </Typography>

                <List>
                    {
                        relatedApplications?.map((app) => (
                            <ListItem
                                key={app.id}
                            >
                                <Link to={`/edit/${app.id}`}>
                                    {app.title} {app.version}
                                </Link>
                            </ListItem>
                        ))
                    }
                </List>
            </InfoDialog>

            <ConfirmDialog
                title="Farbschema löschen"
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
                onConfirm={confirmDelete}
            >
                Soll dieses Farbschema wirklich gelöscht werden?
            </ConfirmDialog>
        </>
    );
}

function ColorPicker({
                         label,
                         value,
                         onChange,
                     }: {
    label: string;
    value?: string;
    onChange: (val: string) => void;
}): JSX.Element {
    return (
        <Grid
            item
            xs={12}
            md={6}
            lg={4}
        >
            <Box
                sx={{
                    mb: 1,
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                <Box
                    sx={{
                        mr: 1,
                        width: '1em',
                        height: '1em',
                        borderRadius: '100%',
                        backgroundColor: value,
                    }}
                />

                <Typography
                    variant="subtitle1"
                    component="h2"
                >
                    {label}
                </Typography>
            </Box>

            <SketchPicker
                color={value}
                onChange={(color) => {
                    onChange(color.hex);
                }}
                disableAlpha={true}
                width="256px"
                presetColors={[
                    '#113a8d',
                    '#d73234',
                    '#60865e',
                    '#fdc022',
                    '#bc457c',
                    '#2e3d45',
                    '#BF261D',
                    '#D18D23',
                    '#1D7C9C',
                    '#449456',
                ]}
            />
        </Grid>
    );
}
