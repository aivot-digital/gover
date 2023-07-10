import { useAuthGuard } from '../../../hooks/use-auth-guard';
import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { TextFieldComponent } from '../../../components/text-field/text-field-component';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { showErrorSnackbar, showSuccessSnackbar } from '../../../slices/snackbar-slice';
import { useUserGuard } from '../../../hooks/use-user-guard';
import { useChangeBlocker } from '../../../hooks/use-change-blocker';
import { FormPageWrapper } from '../../../components/form-page-wrapper/form-page-wrapper';
import { delayPromise } from '../../../utils/with-delay';
import { type Theme } from '../../../models/entities/theme';
import { ThemesService } from '../../../services/themes-service';
import { Box, Grid, Typography } from '@mui/material';
import { SketchPicker } from 'react-color';

export function ThemeEditPage(): JSX.Element {
    useAuthGuard();
    useUserGuard((user) => user?.admin ?? false);

    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const themeId = useParams().id;

    const [originalTheme, setOriginalTheme] = useState<Theme>();
    const [editedTheme, setEditedTheme] = useState<Theme>();

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
            .then((responseLink) => {
                setOriginalTheme(responseLink);
                setEditedTheme(responseLink);
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
                console.error(err);
                dispatch(showErrorSnackbar('Theme konnte nicht gelöscht werden'));
                setIsLoading(false);
            });
    };

    return (
        <FormPageWrapper
            title="Farbpalette bearbeiten"
            isLoading={ isLoading }
            is404={ isNotFound }
            hasChanged={ hasChanged }
            onSave={ handleSave }
            onReset={ (editedTheme?.id ?? 0) !== 0 ? handleReset : undefined }
            onDelete={ (editedTheme?.id ?? 0) !== 0 ? handleDelete : undefined }
        >
            <TextFieldComponent
                label="Name der Farbpalette"
                placeholder="Neue Farbpalette"
                hint="Der Name der Farbpalette. Wird nur Ihren Mitarbeiter:innen angezeigt."
                value={ editedTheme?.name }
                onChange={ (val) => {
                    handlePatch({
                        name: val,
                    });
                } }
                required
                maxCharacters={ 96 }
            />

            <Grid
                container
                columnSpacing={ 2 }
                rowSpacing={ 4 }
                sx={ {
                    mt: 2,
                } }
            >
                <ColorPicker
                    label="Pimärfarbe"
                    value={ editedTheme?.main }
                    onChange={ (color) => {
                        handlePatch({
                            main: color,
                        });
                    } }
                />

                <ColorPicker
                    label="Pimärfarbe (Dunkel)"
                    value={ editedTheme?.mainDark }
                    onChange={ (color) => {
                        handlePatch({
                            mainDark: color,
                        });
                    } }
                />

                <ColorPicker
                    label="Akzentfarbe"
                    value={ editedTheme?.accent }
                    onChange={ (color) => {
                        handlePatch({
                            accent: color,
                        });
                    } }
                />

                <ColorPicker
                    label="Fehlerfarbe"
                    value={ editedTheme?.error }
                    onChange={ (color) => {
                        handlePatch({
                            error: color,
                        });
                    } }
                />

                <ColorPicker
                    label="Warnungsfarbe"
                    value={ editedTheme?.warning }
                    onChange={ (color) => {
                        handlePatch({
                            warning: color,
                        });
                    } }
                />

                <ColorPicker
                    label="Informationsfarbe"
                    value={ editedTheme?.info }
                    onChange={ (color) => {
                        handlePatch({
                            info: color,
                        });
                    } }
                />

                <ColorPicker
                    label="Erfolgsfarbe"
                    value={ editedTheme?.success }
                    onChange={ (color) => {
                        handlePatch({
                            success: color,
                        });
                    } }
                />
            </Grid>
        </FormPageWrapper>
    );
}

function ColorPicker({label, value, onChange}: {
    label: string;
    value?: string;
    onChange: (val: string) => void;
}): JSX.Element {
    return (
        <Grid
            item
            xs={ 12 }
            md={ 6 }
            lg={ 4 }
        >
            <Box
                sx={ {
                    mb: 1,
                    display: 'flex',
                    alignItems: 'center',
                } }
            >
                <Box
                    sx={ {
                        mr: 1,
                        width: '1em',
                        height: '1em',
                        borderRadius: '100%',
                        backgroundColor: value,
                    } }
                />

                <Typography
                    variant="subtitle1"
                    component="h2"
                >
                    { label }
                </Typography>
            </Box>

            <SketchPicker
                color={ value }
                onChange={ (color) => {
                    onChange(color.hex);
                } }
                disableAlpha={ true }
                width="256px"
                presetColors={ [
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
                ] }
            />
        </Grid>
    );
}
