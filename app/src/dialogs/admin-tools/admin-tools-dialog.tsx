import React from 'react';
import {Box, Button, Dialog, DialogContent, FormControlLabel, FormGroup, FormHelperText, Switch, Typography} from '@mui/material';
import {useDispatch} from 'react-redux';
import {AppDispatch, RootState} from '../../store';
import {AdminSettingsState, toggleShowDebugOutput, toggleShowUserInput, toggleValidation, toggleVisibility} from '../../slices/admin-settings-slice';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {AdminToolsDialogProps} from './admin-tools-dialog-props';
import {selectLoadedApplication} from "../../slices/app-slice";
import {useAppSelector} from "../../hooks/use-app-selector";
import {downloadConfigFile} from "../../utils/download-utils";
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';

const switches: {
    label: string;
    hint: string;
    onToggle: (dispatch: AppDispatch) => void;
    isActive: (settings: AdminSettingsState) => boolean;
}[] = [
    {
        label: 'Validierungen berücksichtigen',
        hint: 'Deaktivieren Sie die Validierungen von Eingaben um schnell im Formular navigieren zu können ohne fehlerhafte Eingaben korrigieren zu müssen.',
        onToggle: dispatch => dispatch(toggleValidation()),
        isActive: settings => !settings.disableValidation,
    },
    {
        label: 'Sichtbarkeiten berücksichtigen',
        hint: 'Deaktivieren Sie die Sichtbarkeiten um alle Abschnitte und Elemente des Formulars jederzeit einsehen zu können.',
        onToggle: dispatch => dispatch(toggleVisibility()),
        isActive: settings => !settings.disableVisibility,
    },
    {
        label: 'Debug-Ausgabe in der Konsole erlauben',
        hint: 'Lassen Sie sich die Debug-Ausgaben in der JavaScript-Konsole anzeigen. Entwickler können so mögliche Probleme besser nachvollziehen.',
        onToggle: dispatch => dispatch(toggleShowDebugOutput()),
        isActive: settings => settings.showDebugOutput,
    },
    {
        label: 'Nutzereingaben im Speicher anzeigen',
        hint: 'Lassen Sie sich die aktuellen Nutzereingaben im Speicher anzeigen. Entwickler können so besser nachvollziehen, welche Eingaben getätigt wurden.',
        onToggle: dispatch => dispatch(toggleShowUserInput()),
        isActive: settings => settings.showUserInput,
    },
];

export function AdminToolsDialog({
                                     open,
                                     onClose,
                                 }: AdminToolsDialogProps) {
    const dispatch = useDispatch();

    const adminSettings = useAppSelector((state: RootState) => state.adminSettings);
    const application = useAppSelector(selectLoadedApplication);

    return (
        <>
            <Dialog
                open={open}
                onClose={onClose}
                fullWidth
            >
                <DialogTitleWithClose
                    onClose={onClose}
                    closeTooltip="Schließen"
                >
                    Admin-Werkzeuge für die Bearbeitung des Formulars
                </DialogTitleWithClose>

                <DialogContent>
                    <Typography variant="body1">
                        Aktivieren oder Deaktivieren Sie ausgewählte Funktionen, um Ihnen die Bearbeitung Ihres
                        Formulars einfacher zu gestalten.
                    </Typography>

                    <Box sx={{mt: 3}}>
                        {
                            switches.map(swtch => (
                                <FormGroup key={swtch.label}>
                                    <FormControlLabel
                                        control={<Switch
                                            checked={swtch.isActive(adminSettings)}
                                            onChange={() => swtch.onToggle(dispatch)}
                                        />}
                                        label={swtch.label}
                                    />

                                    <FormHelperText>
                                        {swtch.hint}
                                    </FormHelperText>
                                </FormGroup>
                            ))
                        }
                    </Box>

                    <Box sx={{mt: 3}}>
                        <Button
                            fullWidth
                            onClick={() => {
                                downloadConfigFile(application);
                            }}
                            endIcon={
                                <ImportExportOutlinedIcon/>
                            }
                        >
                            Formular als .gov-Datei exportieren
                        </Button>
                    </Box>
                </DialogContent>
            </Dialog>
        </>
    );
}
