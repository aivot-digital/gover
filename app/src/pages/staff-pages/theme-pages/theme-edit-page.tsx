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
import {PresetColor} from 'react-color/lib/components/sketch/Sketch';

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
                                                smallTitle={true}
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
                        label="Primärfarbe"
                        value={editedTheme?.main}
                        onChange={(color) => {
                            handlePatch({
                                main: color,
                            });
                        }}
                    />

                    <ColorPicker
                        label="Primärfarbe (Dunkel)"
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
                    Dieses Farbschema kann nicht gelöscht werden, da es aktuell von einem oder mehreren Formularen verwendet wird.
                    Sie müssen diese Anträge erst löschen oder ein anderes Farbschema einrichten, bevor Sie dieses Farbschema löschen können.
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
                Sind Sie sicher, dass Sie dieses Farbschema wirklich löschen wollen?
                Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
            </ConfirmDialog>
        </>
    );
}

const colors: PresetColor[] = [
    {
        title: 'Standard-Blau',
        color: '#253e63',
    },
    {
        title: 'Standard-Blau (Dunkel)',
        color: '#142638',
    },
    {
        title: 'Standard-Gelb',
        color: '#ffd481',
    },
    {
        title: 'Standard-Fehler',
        color: '#BF261D',
    },
    {
        title: 'Standard-Warnung',
        color: '#D18D23',
    },
    {
        title: 'Standard-Info',
        color: '#1D7C9C',
    },
    {
        title: 'Standard-Erfolg',
        color: '#449456',
    },


    {
        title: 'Rot',
        color: '#f44336',
    },
    {
        title: 'Rot (Dunkel)',
        color: '#aa2e25',
    },
    {
        title: 'Rot (Akzentuiert)',
        color: '#ff1744',
    },

    {
        title: 'Pink',
        color: '#e91e63',
    },
    {
        title: 'Pink (Dunkel)',
        color: '#a31545',
    },
    {
        title: 'Pink (Akzentuiert)',
        color: '#f50057',
    },

    {
        title: 'Flieder',
        color: '#9c27b0',
    },
    {
        title: 'Flieder (Dunkel)',
        color: '#6d1b7b',
    },
    {
        title: 'Flieder (Akzentuiert)',
        color: '#d500f9',
    },

    {
        title: 'Violett',
        color: '#673ab7',
    },
    {
        title: 'Violett (Dunkel)',
        color: '#482880',
    },
    {
        title: 'Violett (Akzentuiert)',
        color: '#651fff',
    },

    {
        title: 'Indigo',
        color: '#3f51b5',
    },
    {
        title: 'Indigo (Dunkel)',
        color: '#2c387e',
    },
    {
        title: 'Indigo (Akzentuiert)',
        color: '#3d5afe',
    },

    {
        title: 'Blau',
        color: '#2196f3',
    },
    {
        title: 'Blau (Dunkel)',
        color: '#1769aa',
    },
    {
        title: 'Blau (Akzentuiert)',
        color: '#2979ff',
    },

    {
        title: 'Cyan',
        color: '#00bcd4',
    },
    {
        title: 'Cyan (Dunkel)',
        color: '#008394',
    },
    {
        title: 'Cyan (Akzentuiert)',
        color: '#00e5ff',
    },

    {
        title: 'Blaugrün',
        color: '#009688',
    },
    {
        title: 'Blaugrün (Dunkel)',
        color: '#00695f',
    },
    {
        title: 'Blaugrün (Akzentuiert)',
        color: '#1de9b6',
    },

    {
        title: 'Grün',
        color: '#4caf50',
    },
    {
        title: 'Grün (Dunkel)',
        color: '#357a38',
    },
    {
        title: 'Grün (Akzentuiert)',
        color: '#00e676',
    },

    {
        title: 'Limette',
        color: '#cddc39',
    },
    {
        title: 'Limette (Dunkel)',
        color: '#8f9a27',
    },
    {
        title: 'Limette (Akzentuiert)',
        color: '#c6ff00',
    },

    {
        title: 'Gelb',
        color: '#ffeb3b',
    },
    {
        title: 'Gelb (Dunkel)',
        color: '#b2a429',
    },
    {
        title: 'Gelb (Akzentuiert)',
        color: '#ffea00',
    },

    {
        title: 'Bernstein',
        color: '#ffc107',
    },
    {
        title: 'Bernstein (Dunkel)',
        color: '#b28704',
    },
    {
        title: 'Bernstein (Akzentuiert)',
        color: '#ffc400',
    },

    {
        title: 'Orange',
        color: '#ff5722',
    },
    {
        title: 'Orange (Dunkel)',
        color: '#b23c17',
    },
    {
        title: 'Orange (Dunkel)',
        color: '#ff3d00',
    },
];

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
                styles={{
                    default: {
                        picker: {
                            boxShadow: 'none',
                            border: '1px solid #ccc',
                        },
                    },
                }}
                presetColors={colors}
            />
        </Grid>
    );
}
