import React, {type FormEvent, useState} from 'react';
import {type PublishPresetDialogProps} from './publish-preset-dialog-props';
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
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {AlertComponent} from '../../../components/alert/alert-component';
import {cleanBeforeStoreUpload} from '../../../utils/clean-before-store-upload';


export function PublishPresetDialog(props: DialogProps & PublishPresetDialogProps): JSX.Element {
    const dispatch = useAppDispatch();
    const {
        preset,
        presetVersion,
        onClose,
        onPublish,

        ...passThroughProps
    } = props;

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    const [description, setDescription] = useState<string>();
    const [descriptionShort, setDescriptionShort] = useState<string>();
    const [datenfeldId, setDatenfeldId] = useState<string>();
    const [isPublic, setIsPublic] = useState<boolean>(true);

    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

    const resetDialog = (): void => {
        setDescription(undefined);
        setDescriptionShort(undefined);
        setDatenfeldId(undefined);
        setIsPublic(true);
    };

    const handleClose = (): void => {
        resetDialog();
        onClose();
    };

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (description != null && descriptionShort != null) {
            setIsSubmitting(true);

            GoverStoreService.publishModule(
                storeKey,
                {
                    gover_root: cleanBeforeStoreUpload(presetVersion.root),
                    version: presetVersion.version,
                    title: preset.title,
                    description,
                    description_short: descriptionShort,
                    is_public: isPublic,
                    datenfeld_id: datenfeldId == null ? '' : datenfeldId,
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
                        Vorlage als Baustein veröffentlichen
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
                            label="BOB-Baustein Schlüssel"
                            value={datenfeldId}
                            onChange={setDatenfeldId}
                            maxCharacters={64}
                            placeholder="F60000146V1.0"
                        />

                        <TextFieldComponent
                            label="Kurzbeschreibung"
                            value={descriptionShort}
                            onChange={setDescriptionShort}
                            multiline
                            required
                            maxCharacters={128}
                            minCharacters={1}
                            placeholder="Dieser Baustein wird genutzt für…"
                        />

                        <TextFieldComponent
                            label="Ausführliche Beschreibung"
                            value={description}
                            onChange={setDescription}
                            multiline
                            required
                            maxCharacters={2048}
                            minCharacters={1}
                            placeholder="Dieser Baustein wird genutzt für…"
                        />

                        <CheckboxFieldComponent
                            label="Öffentlicher Baustein"
                            value={isPublic}
                            onChange={setIsPublic}
                        />

                        {
                            !isPublic &&
                            <AlertComponent
                                title="Privater Baustein"
                                color="info"
                            >
                                Bitte beachten Sie, dass private Bausteine nur Ihren Mitarbeiter:innen zur Verfügung stehen.
                                Private Bausteine können nicht von fremden Dritten eingesehen werden.
                            </AlertComponent>
                        }
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
