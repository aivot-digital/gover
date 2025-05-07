import React, {useState} from 'react';
import {Box, Button, Dialog, DialogContent, FormControlLabel, FormGroup, FormHelperText, Grid, Switch, Typography} from '@mui/material';
import {useDispatch} from 'react-redux';
import {type AppDispatch, type RootState} from '../../store';
import {type AdminSettingsState, setDevToolsTab, toggleAutoScrollForSteps, toggleValidation, toggleVisibility} from '../../slices/admin-settings-slice';
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
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import BugReportIcon from '@mui/icons-material/BugReport';
import ExportApplicationDialog from "../application-dialogs/export-application-dialog/export-application-dialog";

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
];

export function AdminToolsDialog(props: AdminToolsDialogProps): JSX.Element {
    const dispatch = useDispatch();
    const api = useApi();

    const form = useAppSelector(selectLoadedForm);
    const adminSettings = useAppSelector((state: RootState) => state.adminSettings);
    const experimentalFeatureComplexity = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.experimentalFeatures.complexity));

    const [showMetrics, setShowMetrics] = useState(false);
    const [exportDialogOpen, setExportDialogOpen] = useState(false);

    const downloadPdfFile = (form: Form) => {
        dispatch(showLoadingOverlay('Vordruck wird generiert'));
        api
            .getBlob(`forms/${form.id}/print/`)
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

    const openExportDialog = () => {
        setExportDialogOpen(true);
    };

    const startExportForm = () => {
        try {
            downloadConfigFile(form);
            dispatch(showSuccessSnackbar('Formular wurde erfolgreich exportiert.'));
        } catch (error) {
            console.error(error);
            dispatch(showErrorSnackbar('Fehler beim Export des Formulars.'));
        }
        setExportDialogOpen(false);
    };

    const actions = [
        {
            label: 'Formular exportieren (.gov)',
            icon: <ImportExportOutlinedIcon />,
            onClick: () => {
                openExportDialog();
            },
        },
        {
            label: 'Vordruck exportieren (.pdf)',
            icon: <PictureAsPdfOutlinedIcon />,
            onClick: () => {
                if (form != null) {
                    downloadPdfFile(form);
                }
            },
        },
        {
            label: 'Entwicklerwerkzeuge öffnen',
            icon: <BugReportIcon />,
            onClick: () => {
                dispatch(setDevToolsTab(0));
                props.onClose();
            },
        },
    ];

    if (experimentalFeatureComplexity) {
        actions.push({
            label: 'Komplexitätseinschätzung',
            icon: <StackedLineChart />,
            onClick: () => {
                setShowMetrics(true);
            },
        });
    }

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
                    {/* TODO: Name sollte nur in "Werkzeuge" geändert werden */}
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


                    <Grid
                        container
                        spacing={2}
                        sx={{
                            mt: 3,
                        }}
                    >
                        {
                            actions.map(action => (
                                <Grid
                                    item
                                    key={action.label}
                                    xs={12}
                                    md={6}
                                >
                                    <Button
                                        fullWidth
                                        onClick={action.onClick}
                                        startIcon={action.icon}
                                        variant="outlined"
                                        sx={{
                                            height: '100%',
                                        }}
                                    >
                                        {action.label}
                                    </Button>
                                </Grid>
                            ))
                        }
                    </Grid>
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
                        form?.root != null &&
                        <FormMetrics root={form.root} />
                    }
                </DialogContent>
            </Dialog>

            <ExportApplicationDialog
                open={exportDialogOpen}
                onCancel={() => setExportDialogOpen(false)}
                onExport={startExportForm}
            />
        </>
    );
}
