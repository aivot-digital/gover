import React, {useCallback} from 'react';
import {
    Box,
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
import {AppDispatch, RootState} from '../../store';
import {
    AdminSettingsState, toggleShowDebugOutput,
    toggleShowUserInput,
    toggleValidation,
    toggleVisibility
} from '../../slices/admin-settings-slice';
import {faCode, faRefresh, faTrashAlt} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {resetUserInput} from '../../slices/customer-input-slice';
import {CodeService} from '../../services/code.service';
import {AdminToolsDialogProps} from './admin-tools-dialog-props';
import {selectLoadedApplication} from '../../slices/app-slice';
import {resetStepper} from '../../slices/stepper-slice';
import {downloadTextFile} from '../../utils/download-text-file';
import {showErrorSnackbar, showSuccessSnackbar} from "../../slices/snackbar-slice";
import {Localization} from "../../locale/localization";
import strings from "./admin-tools-dialog-strings.json";

const _ = Localization(strings);

const switches: {
    label: string;
    hint: string;
    onToggle: (dispatch: AppDispatch) => void;
    isActive: (settings: AdminSettingsState) => boolean;
}[] = [
    {
        label: _.validateSwitchLabel,
        hint: _.validateSwitchHint,
        onToggle: dispatch => dispatch(toggleValidation()),
        isActive: settings => !settings.disableValidation,
    },
    {
        label: _.visibilitySwitchLabel,
        hint: _.visibilitySwitchHint,
        onToggle: dispatch => dispatch(toggleVisibility()),
        isActive: settings => !settings.disableVisibility,
    },
    {
        label: _.debugSwitchLabel,
        hint: _.debugSwitchHint,
        onToggle: dispatch => dispatch(toggleShowDebugOutput()),
        isActive: settings => settings.showDebugOutput,
    },
    {
        label: _.userInputSwitchLabel,
        hint: _.userInputSwitchHint,
        onToggle: dispatch => dispatch(toggleShowUserInput()),
        isActive: settings => settings.showUserInput,
    },
];

export function AdminToolsDialog({open, onClose}: AdminToolsDialogProps) {
    const dispatch = useDispatch();

    const application = useSelector(selectLoadedApplication);
    const adminSettings = useSelector((state: RootState) => state.adminSettings);

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
                    closeTooltip={_.close}
                >
                    {_.title}
                </DialogTitleWithClose>

                <DialogContent>
                    <Typography variant="body1">
                        {_.hint}
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
