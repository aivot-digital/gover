import React, {useCallback} from 'react';
import {
    Button,
    Dialog,
    DialogContent,
    Divider,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    Switch,
    Typography
} from '@mui/material';
import {useDispatch, useSelector} from 'react-redux';
import {RootState} from '../../store';
import {toggleShowUserInput, toggleValidation, toggleVisibility} from '../../slices/admin-settings-slice';
import {faCode, faFileExport, faFilePdf, faRefresh, faTrashAlt} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {resetUserInput} from '../../slices/customer-input-slice';
import {CodeService} from '../../services/code.service';
import {AdminToolsDialogProps} from './admin-tools-dialog-props';
import {selectLoadedApplication} from '../../slices/app-slice';
import {resetStepper} from '../../slices/stepper-slice';
import {downloadTextFile} from '../../utils/download-text-file';
import {showErrorSnackbar, showSuccessSnackbar} from "../../slices/snackbar-slice";

// TODO: Localize

export function AdminToolsDialog({open, onClose}: AdminToolsDialogProps) {
    const dispatch = useDispatch();

    const application = useSelector(selectLoadedApplication);
    const adminSettings = useSelector((state: RootState) => state.adminSettings);

    const downloadConfig = useCallback(() => {
        if (application) {
            // TODO: Implement downloadConfigFile(rootModel);
        }
    }, [application]);

    return (
        <>
            <Dialog
                open={open}
                onClose={onClose}
                fullWidth
            >
                <DialogTitleWithClose
                    id="admin-tools-dialog-title"
                    onClose={onClose}
                    closeTooltip={'Close' /* TODO: Localize */}
                >
                    Admin-Werkzeuge für die Bearbeitung des Antrages
                </DialogTitleWithClose>

                <DialogContent>
                    <Typography variant="body1">
                        Aktivieren oder Deaktivieren Sie ausgewählte Funktionen, um Ihnen die Bearbeitung Ihres Antrages
                        einfacher zu gestalten.
                    </Typography>

                    <FormGroup sx={{mt: 3}}>
                        <FormControlLabel
                            control={<Switch
                                checked={!adminSettings.disableValidation}
                                onChange={() => {
                                    dispatch(toggleValidation());
                                }}
                            />}
                            label="Validierungen berücksichtigen"
                        />

                        <FormHelperText>
                            Deaktivieren Sie die Validierungen von Eingaben um schnell im Antrag navigieren zu können
                            ohne fehlerhafte Eingaben korrigieren zu müssen.
                        </FormHelperText>

                        <FormControlLabel
                            sx={{mt: 2}}
                            control={<Switch
                                checked={!adminSettings.disableVisibility}
                                onChange={() => {
                                    dispatch(toggleVisibility());
                                }}
                            />}
                            label="Sichtbarkeiten berücksichtigen"
                        />

                        <FormHelperText>
                            Deaktivieren Sie die Sichtbarkeiten um alle Abschnitte und Elemente des Antrages jederzeit
                            einsehen zu können.
                        </FormHelperText>

                        <FormControlLabel
                            sx={{mt: 2}}
                            control={<Switch
                                checked={adminSettings.showUserInput}
                                onChange={() => {
                                    dispatch(toggleShowUserInput());
                                }}
                            />}
                            label="Nutzereingaben im Speicher anzeigen"
                        />

                        <FormHelperText>
                            Lassen Sie sich die aktuellen Nutzereingaben im Speicher anzeigen.
                            Entwickler können so besser nachvollziehen, welche Eingaben getätigt wurden.
                        </FormHelperText>
                    </FormGroup>

                    <Divider sx={{my: 2, mt: 5}}/>

                    <Typography variant="body1">
                        Sie können die Eingaben in diesem Antrag zurücksetzen.
                    </Typography>
                    <Button
                        sx={{my: 2}}
                        startIcon={<FontAwesomeIcon
                            icon={faTrashAlt}
                            fixedWidth
                            style={{marginTop: '-2px'}}
                        />}
                        size="large"
                        variant="outlined"
                        onClick={() => {
                            dispatch(resetUserInput());
                            dispatch(resetStepper());
                            onClose();
                        }}
                    >
                        Eingaben zurücksetzen
                    </Button>

                    <Divider sx={{my: 2}}/>

                    <Typography variant="body1">
                        Erzeugen Sie den Code neu, wenn Funktionen (z.B. für Validierungen) hinzugefügt oder geändert
                        wurden.
                    </Typography>
                    <Button
                        sx={{my: 2}}
                        startIcon={<FontAwesomeIcon
                            icon={faCode}
                            fixedWidth
                            style={{marginTop: '-2px'}}
                        />}
                        size="large"
                        variant="outlined"
                        onClick={() => {
                            if (application) {
                                const codeStubs = CodeService.createCodeStubs(application);
                                downloadTextFile(
                                    application.slug + '.js',
                                    codeStubs,
                                    'text/javascript'
                                );
                                onClose();
                            }
                        }}
                    >
                        Erzeuge Code-Vorlage
                    </Button>

                    <Divider sx={{my: 2}}/>

                    <Typography variant="body1">
                        Laden Sie den Code für den Antrag neu.
                        Dies ist nützlich, falls Entwickler den Code aktualisieren, während Sie den Antrag bearbeiten.
                    </Typography>
                    <Button
                        sx={{my: 2}}
                        startIcon={<FontAwesomeIcon
                            icon={faRefresh}
                            fixedWidth
                            style={{marginTop: '-2px'}}
                        />}
                        size="large"
                        variant="outlined"
                        onClick={() => {
                            if (application != null) {
                                CodeService.loadCode(application.id)
                                    .then(() => {
                                        dispatch(showSuccessSnackbar('Code erfolgreich neu geladen.'));
                                    })
                                    .catch(err => {
                                        console.error(err);
                                        dispatch(showErrorSnackbar('Fehler beim Code neu laden.'));
                                    });
                            }
                        }}
                    >
                        Code neu laden
                    </Button>
                </DialogContent>
            </Dialog>
        </>
    );
}
