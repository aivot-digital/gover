import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import React, {FormEvent, useState} from 'react';
import {Preset} from "../../models/entities/preset";
import {TextFieldComponent} from "../../components/text-field/text-field-component";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from "../../utils/string-utils";
import {Link} from "react-router-dom";
import {CheckboxFieldComponent} from "../../components/checkbox-field/checkbox-field-component";
import {AlertComponent} from "../../components/alert/alert-component";
import {GoverStoreService} from "../../services/gover-store.service";
import {useAppDispatch} from "../../hooks/use-app-dispatch";
import {showErrorSnackbar, showSuccessSnackbar} from "../../slices/snackbar-slice";
import {withTimeout} from "../../utils/with-timeout";

const submissionTimeoutMinMs = 3000;

interface PublishPresetDialogProps {
    preset?: Preset;
    onClose: () => void;
}

export function PublishPresetDialog(props: PublishPresetDialogProps) {
    const dispatch = useAppDispatch();
    const {preset, onClose, ...passTroughProps} = props;

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    const [version, setVersion] = useState<string>();
    const [title, setTitle] = useState<string>();
    const [description, setDescription] = useState<string>();
    const [descriptionShort, setDescriptionShort] = useState<string>();
    const [datenfeldId, setDatenfeldId] = useState<string>();
    const [isPublic, setIsPublic] = useState<boolean>(true);

    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

    const handleClose = () => {
        setVersion(undefined);
        setTitle(undefined);
        setDescription(undefined);
        setDescriptionShort(undefined);
        setDatenfeldId(undefined);
        setIsPublic(true);

        onClose();
    };

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();
        event.stopPropagation();

        if (preset != null && preset.root != null && version != null && title != null && description != null && descriptionShort != null) {
            withTimeout(
                () => setIsSubmitting(true),
                () => {
                    return GoverStoreService.publishModule(
                        storeKey,
                        {
                            gover_root: preset.root,
                            version: version,
                            title: title,
                            description: description,
                            description_short: descriptionShort,
                            is_public: isPublic,
                            datenfeld_id: datenfeldId == null ? '' : datenfeldId,
                        }
                    )
                        .then(res => {
                            // TODO: Lokale Vorlage mit GoverStoreId updaten
                            dispatch(showSuccessSnackbar('Vorlage erfolgreich als Baustein veröffentlicht.'));
                            return true;
                        })
                        .catch(err => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Vorlage konnte nicht als Baustein veröffentlicht werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                            return true;
                        });
                },
                (res) => {
                    setIsSubmitting(false);
                    if (res) {
                        handleClose();
                    }
                },
                submissionTimeoutMinMs,
            );
        }

        return false;
    };

    return (
        <Dialog
            open={preset != null}
            onClose={handleClose}
        >
            <form
                onSubmit={handleSubmit}
            >
                <DialogTitle>
                    Vorlage als Baustein veröffentlichen
                </DialogTitle>

                {
                    isStringNullOrEmpty(storeKey) &&
                    <DialogContent>
                        <Typography>
                            Um Vorlagen als Bausteine im Gover Store veröffentlichen zu können,
                            müssen Sie zuerst Ihren Gover Store Schlüssel hinterlegen.
                            Gehen Sie dazu in die <Link to="/settings">Anwendungseinstellungen</Link> und geben Sie
                            Ihren
                            Schlüssel im entsprechenden Feld ein. Mehr Informationen zum Gover Store finden Sie unter <a
                            href="https://store.gover.digital/contribute"
                            target="_blank"
                        >
                            https://store.gover.digital/contribute</a>.
                        </Typography>
                    </DialogContent>
                }

                {
                    isStringNotNullOrEmpty(storeKey) &&
                    <DialogContent>
                        <TextFieldComponent
                            label="Titel des Bausteins"
                            value={title}
                            onChange={setTitle}
                            required
                            maxCharacters={96}
                            placeholder="Neuer Baustein"
                        />

                        <TextFieldComponent
                            label="Version des Bausteins"
                            value={version}
                            onChange={setVersion}
                            required
                            maxCharacters={11}
                            placeholder="1.0.0"
                            hint="Bitte geben Sie die Version im Muster X.Y.Z ein, wobei Sie nur Zahlen und Punkte verwenden"
                        />

                        <TextFieldComponent
                            label="FIM-Datenfeld Schlüssel"
                            value={datenfeldId}
                            onChange={setDatenfeldId}
                            maxCharacters={64}
                            placeholder="D00000146V1.0"
                        />

                        <Typography variant="caption">
                            Mehr Informationen zu FIM-Datenfeldern finden Sie unter <a
                            href="https://fimportal.de/"
                            target="_blank"
                        >https://fimportal.de/</a>
                        </Typography>


                        <TextFieldComponent
                            label="Kurzbeschreibung"
                            value={descriptionShort}
                            onChange={setDescriptionShort}
                            multiline
                            required
                            maxCharacters={128}
                            placeholder="Dieser Baustein wird genutzt für..."
                        />

                        <TextFieldComponent
                            label="Ausführliche Beschreibung"
                            value={description}
                            onChange={setDescription}
                            multiline
                            required
                            maxCharacters={2048}
                            placeholder="Dieser Baustein wird genutzt für..."
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
                                text="Bitte beachten Sie, dass private Bausteine nur Ihren Mitarbeiter:innen zur Verfügung stehen und nicht im Gover Store eingesehen werden können."
                                color="info"
                            />
                        }
                    </DialogContent>
                }
                <DialogActions>
                    <Button onClick={handleClose}>
                        Abbrechen
                    </Button>
                    <Button
                        type="submit"
                        disabled={isStringNullOrEmpty(storeKey)}
                    >
                        Veröffentlichen
                    </Button>
                </DialogActions>
            </form>
        </Dialog>
    );
}