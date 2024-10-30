import {Alert, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {type AddPresetDialogProps} from './add-preset-dialog-props';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {type Preset} from '../../../models/entities/preset';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {useApi} from '../../../hooks/use-api';
import {usePresetsApi} from '../../../hooks/use-presets-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';


type Errors = Partial<Record<keyof Preset, string>>;

function createEmptyPreset(): Preset {
    return {
        key: '',
        title: '',
        currentVersion: '',
        currentStoreVersion: null,
        currentPublishedVersion: null,
        updated: new Date().toISOString(),
        created: new Date().toISOString(),
        storeId: '',
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

    if (preset.currentVersion == null || preset.currentVersion.length < 5 || preset.currentVersion.length > 11) {
        errors = {
            ...(errors ?? {}),
            currentVersion: 'Bitte geben Sie einen Version mit mindestens 5 weniger als 12 Zeichen ein',
        };
    }

    const versionRegex = /^[0-9]+(\.[0-9]+){2}$/;
    if (!versionRegex.test(preset.currentVersion)) {
        errors = {
            ...(errors ?? {}),
            currentVersion: 'Bitte geben Sie eine gültige Version nach dem Schema X.Y.Z ein',
        };
    }

    return errors;
}

export function AddPresetDialog(props: AddPresetDialogProps): JSX.Element {
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
            usePresetsApi(api)
                .save(preset)
                .then((createdPreset) => {
                    return usePresetsApi(api).createVersion(createdPreset.key, {
                        preset: createdPreset.key,
                        version: preset.currentVersion,
                        root: root ?? generateElementWithDefaultValues(ElementType.Container) as GroupLayout,
                        publishedAt: null,
                        publishedStoreAt: null,
                        created: new Date().toISOString(),
                        updated: new Date().toISOString(),
                    })
                        .then((createdVersion) => {
                            createdPreset.currentVersion = createdVersion.version;
                            return {
                                preset: createdPreset,
                                version: createdVersion,
                            };
                        });
                })
                .then(({preset, version}) => {
                    onAdded(preset, version);
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
                        mt: 4,
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

                <Typography
                    variant="body2"
                    sx={{
                        mt: 4,
                        mb: 2,
                    }}
                >
                    Vergeben Sie die Version der Vorlage. Unter dieser wird die Vorlage verfügbar
                    sein. Achten Sie darauf, dass Sie dem Schema der semantischen Versionierung folgen.
                    Die Version besteht aus drei Zahlen, die jeweils durch einen Punkt getrennt werden. Die erste Zahl
                    gibt die Hauptversion (Major) an und sollte nur bei tiefgreifenden Änderungen erhöht werden. Die
                    zweite Zahl gibt die Nebenversion (Minor) an und sollte bei kleineren Änderungen erhöht werden. Die
                    dritte Zahl gibt die Fehlerkorrekturen (Fix) an und sollte nur bei solchen erhöht werden.
                </Typography>

                <TextFieldComponent
                    label="Version der Vorlage"
                    placeholder="1.0.0"
                    value={preset.currentVersion}
                    onChange={(val) => {
                        handlePatch({
                            currentVersion: val,
                        });
                    }}
                    required
                    error={errors.currentVersion}
                    maxCharacters={11}
                    minCharacters={5}
                />

                <Alert
                    severity="warning"
                    variant="outlined"
                    sx={{mt: 1}}
                >
                    Bitte beachten Sie, dass diese Angaben später nicht mehr geändert werden können.
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
