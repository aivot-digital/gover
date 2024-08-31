import React from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogContent,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    Switch,
    Typography,
} from '@mui/material';
import {useDispatch} from 'react-redux';
import {type AppDispatch, type RootState} from '../../store';
import {
    type AdminSettingsState,
    toggleAutoScrollForSteps,
    toggleShowDebugOutput,
    toggleShowUserInput,
    toggleValidation,
    toggleVisibility,
} from '../../slices/admin-settings-slice';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type AdminToolsDialogProps} from './admin-tools-dialog-props';
import {selectLoadedForm} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {downloadBlobFile, downloadConfigFile} from '../../utils/download-utils';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import StackedLineChart from '@mui/icons-material/StackedLineChart';
import {FormMetrics} from '../../components/form-metrics/form-metrics';
import {selectBooleanSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';
import {Form} from '../../models/entities/form';
import {useApi} from '../../hooks/use-api';
import {hideLoadingOverlay, hideLoadingOverlayWithTimeout, showLoadingOverlay} from '../../slices/loading-overlay-slice';
import {showErrorSnackbar} from '../../slices/snackbar-slice';

const switches: Array<{
    label: string;
    hint: string;
    onToggle: (dispatch: AppDispatch) => void;
    isActive: (settings: AdminSettingsState) => boolean;
}> = [
    {
        label: 'Validierungen berücksichtigen',
        hint: 'Deaktivieren Sie die Validierungen von Eingaben um schnell im Formular navigieren zu können ohne fehlerhafte Eingaben korrigieren zu müssen.',
        onToggle: (dispatch) => dispatch(toggleValidation()),
        isActive: (settings) => !settings.disableValidation,
    },
    {
        label: 'Sichtbarkeiten berücksichtigen',
        hint: 'Deaktivieren Sie die Sichtbarkeiten um alle Abschnitte und Elemente des Formulars jederzeit einsehen zu können.',
        onToggle: (dispatch) => dispatch(toggleVisibility()),
        isActive: (settings) => !settings.disableVisibility,
    },
    {
        label: 'Automatisches Scrollen',
        hint: 'Deaktivieren Sie das automatische Scrollen um die aktuelle Scroll-Position im Formular beizubehalten',
        onToggle: (dispatch) => dispatch(toggleAutoScrollForSteps()),
        isActive: (settings) => !settings.disableAutoScrollForSteps,
    },
    {
        label: 'Debug-Ausgabe in der Konsole erlauben',
        hint: 'Lassen Sie sich die Debug-Ausgaben in der JavaScript-Konsole anzeigen. Entwickler können so mögliche Probleme besser nachvollziehen.',
        onToggle: (dispatch) => dispatch(toggleShowDebugOutput()),
        isActive: (settings) => settings.showDebugOutput,
    },
    {
        label: 'Nutzereingaben im Speicher anzeigen',
        hint: 'Lassen Sie sich die aktuellen Nutzereingaben im Speicher anzeigen. Entwickler können so besser nachvollziehen, welche Eingaben getätigt wurden.',
        onToggle: (dispatch) => dispatch(toggleShowUserInput()),
        isActive: (settings) => settings.showUserInput,
    },
];

export function AdminToolsDialog(props: AdminToolsDialogProps): JSX.Element {
    const dispatch = useDispatch();
    const api = useApi();

    const adminSettings = useAppSelector((state: RootState) => state.adminSettings);
    const application = useAppSelector(selectLoadedForm);
    const experimentalFeatureComplexity = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.experimentalFeatures.complexity));

    const [showMetrics, setShowMetrics] = React.useState(false);

    const downloadPdfFile = (form: Form) => {
        dispatch(showLoadingOverlay('Vordruck wird generiert'));
        api
            .getBlob(`forms/${form.id}/print`)
            .then((blob) => {
                downloadBlobFile(`vordruck - ${form.slug} - ${form.version}.pdf`, blob);
                dispatch(hideLoadingOverlayWithTimeout(1000));
            })
            .catch((error) => {
                console.error(error);
                dispatch(hideLoadingOverlay());
                dispatch(showErrorSnackbar('Fehler beim Generieren des Vordrucks'));
            });
    };

    return (
        <>
            <Dialog
                open={props.open}
                onClose={props.onClose}
                fullWidth
            >
                <DialogTitleWithClose
                    onClose={props.onClose}
                    closeTooltip="Schließen"
                >
                    Admin-Werkzeuge für die Bearbeitung des Formulars
                </DialogTitleWithClose>

                <DialogContent tabIndex={0}>
                    <Typography variant="body1">
                        Aktivieren oder Deaktivieren Sie ausgewählte Funktionen, um Ihnen die Bearbeitung Ihres
                        Formulars einfacher zu gestalten.
                    </Typography>

                    <Box sx={{mt: 3}}>
                        {
                            switches.map((swtch) => (
                                <FormGroup key={swtch.label}>
                                    <FormControlLabel
                                        control={<Switch
                                            checked={swtch.isActive(adminSettings)}
                                            onChange={() => {
                                                swtch.onToggle(dispatch);
                                            }}
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

                    {
                        experimentalFeatureComplexity &&
                        <Box sx={{mt: 3}}>
                            <Button
                                fullWidth
                                onClick={() => {
                                    setShowMetrics(true);
                                }}
                                endIcon={
                                    <StackedLineChart />
                                }
                            >
                                Komplexitätseinschätzung anzeigen
                            </Button>
                        </Box>
                    }

                    <Box sx={{mt: 3}}>
                        <Button
                            fullWidth
                            onClick={() => {
                                downloadConfigFile(application);
                            }}
                            endIcon={
                                <ImportExportOutlinedIcon />
                            }
                        >
                            Formular als .gov-Datei exportieren
                        </Button>
                    </Box>

                    <Box sx={{mt: 3}}>
                        <Button
                            fullWidth
                            onClick={() => {
                                if (application != null) {
                                    downloadPdfFile(application);
                                }
                            }}
                            endIcon={
                                <PictureAsPdfOutlinedIcon />
                            }
                        >
                            Formular als Vordruck exportieren
                        </Button>
                    </Box>
                </DialogContent>
            </Dialog>

            <Dialog
                open={showMetrics}
                onClose={() => {
                    setShowMetrics(false);
                }}
                fullWidth
                maxWidth="md"
            >
                <DialogTitleWithClose
                    onClose={() => {
                        setShowMetrics(false);
                    }}
                >
                    Komplexitätseinschätzung
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    {
                        application?.root != null &&
                        <FormMetrics root={application.root} />
                    }
                </DialogContent>
            </Dialog>
        </>
    );
}
