import React, {FormEvent, useState} from 'react';
import {type PublishPresetVersionDialogProps} from './publish-preset-version-dialog-props';
import Dialog, {type DialogProps} from '@mui/material/Dialog/Dialog';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {GoverStoreService} from '../../../services/gover-store.service';
import {LoadingWrapper} from '../../../components/loading-wrapper/loading-wrapper';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {Box, Button, DialogActions, DialogContent, Typography} from '@mui/material';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';


export function PublishPresetVersionDialog(props: DialogProps & PublishPresetVersionDialogProps): JSX.Element {
    const dispatch = useAppDispatch();
    const {
        preset,
        presetVersion,
        onClose,
        onPublish,

        ...passThroughProps
    } = props;

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    const [changes, setChanges] = useState<string>();

    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

    const resetDialog = (): void => {
        setChanges(undefined);
    };

    const handleClose = (): void => {
        resetDialog();
        onClose();
    };

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (preset.storeId != null && changes != null) {
            setIsSubmitting(true);

            GoverStoreService.publishModuleVersion(
                storeKey,
                preset.storeId,
                {
                    gover_root: presetVersion.root,
                    version: presetVersion.version,
                    changes,
                },
            )
                .then((res) => {
                    dispatch(showSuccessSnackbar('Vorlage erfolgreich als Baustein veröffentlicht.'));
                    resetDialog();
                    onPublish(res);
                })
                .catch((err) => {
                    if (err.status === 403) {
                        dispatch(showErrorSnackbar('Sie haben keine Berechtigung, um Vorlagen als Bausteine zu veröffentlichen. Bitte prüfen Sie Ihren Store-Schlüssel.'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Vorlage konnte nicht als Baustein veröffentlicht werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                    }
                })
                .finally(() => {
                    setIsSubmitting(false);
                });
        }
    };

    if (storeKey == null || isStringNullOrEmpty(storeKey)) {
        return (
            <></>
        );
    }

    return (
        <Dialog
            {...passThroughProps}
            onClose={handleClose}
            maxWidth="md"
            fullWidth
        >
            <LoadingWrapper
                isLoading={isSubmitting}
            >
                <form
                    onSubmit={handleSubmit}
                >
                    <DialogTitleWithClose onClose={handleClose}>
                        Neue Version veröffentlichen
                    </DialogTitleWithClose>

                    <DialogContent tabIndex={0}>
                        <Box
                            sx={{
                                mb: 2,
                            }}
                        >
                            <Typography variant="subtitle1">
                                Titel des Bausteins
                            </Typography>
                            <Typography>
                                {preset.title}
                            </Typography>
                        </Box>

                        <Box
                            sx={{
                                mb: 2,
                            }}
                        >
                            <Typography variant="subtitle1">
                                Version des Bausteins
                            </Typography>
                            <Typography>
                                {presetVersion.version}
                            </Typography>
                        </Box>

                        <TextFieldComponent
                            label="Änderungen"
                            value={changes}
                            onChange={setChanges}
                            multiline
                            required
                            maxCharacters={2048}
                            minCharacters={1}
                            placeholder="Der Baustein hat folgende neue Funktionen..."
                        />
                    </DialogContent>

                    <DialogActions>
                        <Button
                            type="submit"
                            disabled={isStringNullOrEmpty(storeKey)}
                            variant="contained"
                        >
                            Veröffentlichen
                        </Button>
                        <Button onClick={handleClose}>
                            Abbrechen
                        </Button>
                    </DialogActions>
                </form>
            </LoadingWrapper>
        </Dialog>
    );
}
