import {Alert, Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import React, { useEffect, useState } from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {type AddPresetDialogProps} from './add-preset-dialog-props';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {type Preset} from '../../../models/entities/preset';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {useApi} from '../../../hooks/use-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {PresetsApiService} from "../../../modules/presets/presets-api-service";
import {PresetVersionApiService} from "../../../modules/presets/preset-version-api-service";


type Errors = Partial<Record<keyof Preset, string>>;

function createEmptyPreset(): Preset {
    return {
        key: '',
        title: '',
        publishedVersion: null,
        draftedVersion: null,
        updated: new Date().toISOString(),
        created: new Date().toISOString(),
    };
}

function validate(preset: Preset): Errors | null {
    let errors: Errors | null = null;


    if (preset.title == null || preset.title.length < 3 || preset.title.length > 190) {
        errors = {
            ...(errors ?? {}),
            title: 'Bitte geben Sie einen Titel mit mindestens 3 und weniger als 190 Zeichen ein',
        };
    }

    return errors;
}

export function AddPresetDialog(props: AddPresetDialogProps) {
    const api = useApi();
    const dispatch = useAppDispatch();

    const {
        onClose,
        onAdded,
        root,

        ...passTroughProps
    } = props;

    const [preset, setPreset] = useState(createEmptyPreset());
    const [errors, setErrors] = useState<Errors>({});

    useEffect(() => {
        if (root?.name != null && root.name !== preset.title) {
            handlePatch({
                title: root.name,
            });
        }
    }, [root]);

    const handlePatch = (patch: Partial<Preset>): void => {
        setPreset({
            ...preset,
            ...patch,
        });
    };

    const handleSave = (): void => {
        setErrors({});

        const errors = validate(preset);

        if (errors != null) {
            setErrors(errors);
        } else {
            const presetsApiService = new PresetsApiService(api);

            presetsApiService
                .create(preset)
                .then((preset) => {
                    onAdded(preset);
                    handleClose();
                })
                .catch((err) => {
                    if (err.status === 409) {
                        setErrors({
                            title: 'Es existiert bereits eine Vorlage mit diesem Titel',
                        });
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Fehler beim Erstellen der Vorlage'));
                    }
                });
        }
    };

    const handleClose = (): void => {
        setPreset(createEmptyPreset());
        setErrors({});
        onClose();
    };

    return (
        <Dialog
            {...passTroughProps}
            fullWidth
            onClose={handleClose}
            maxWidth="lg"
        >
            <DialogTitleWithClose
                onClose={props.onClose}
                closeTooltip="Schließen"
            >
                Neue Vorlage erstellen
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                <Typography
                    variant="body2"
                    sx={{
                        mb: 2,
                    }}
                >
                    Vergeben Sie einen Titel für die Vorlage um es besser identifizieren zu können.
                    Diesen Titel können nur Sie und ihre Kolleg:innen einsehen.
                </Typography>

                <TextFieldComponent
                    label="Titel der Vorlage"
                    placeholder="Anschrift"
                    value={preset.title}
                    onChange={(val) => {
                        handlePatch({
                            title: val,
                        });
                    }}
                    required
                    error={errors.title}
                    maxCharacters={190}
                    minCharacters={3}
                />

                <Alert
                    severity="warning"
                    variant="outlined"
                    sx={{mt: 1}}
                >
                    <Box sx={{maxWidth: 900}}>
                        Bitte beachten Sie, dass diese Angaben später nicht mehr geändert werden können.
                        Wenn Sie die Vorlage erstellt haben, müssen Sie diese noch veröffentlichen, um sie zur Nutzung in Formularen zur Verfügung zu stellen.
                    </Box>
                </Alert>

                {
                    Object.keys(errors).length > 0 &&
                    <Alert
                        severity="error"
                        sx={{mt: 2}}
                    >
                        Bitte beheben Sie die markierten Fehler um fortzufahren.
                    </Alert>
                }
            </DialogContent>

            <DialogActions>
                <Button
                    variant="contained"
                    onClick={() => {
                        handleSave();
                    }}
                >
                    Vorlage erstellen
                </Button>
                <Button
                    onClick={handleClose}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
