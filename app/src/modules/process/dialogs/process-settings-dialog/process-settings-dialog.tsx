import {ProcessEntity} from '../../entities/process-entity';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {Box, Button, Dialog, DialogActions, DialogContent, Tab, Tabs} from '@mui/material';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {useEffect, useRef, useState} from 'react';
import {VDepartmentShadowedApiService} from '../../../departments/services/v-department-shadowed-api-service';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showWarningSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessSettingsDialogProcessAccessTab} from './process-settings-dialog-process-access-tab';
import {TeamEntity} from '../../../teams/entities/team-entity';
import {TeamsApiService} from '../../../teams/services/teams-api-service';
import {ProcessSettingsDialogProcessInstanceAccessPresetTab} from './process-settings-dialog-process-instance-access-preset-tab';
import {ProcessSettingsDialogGeneralTab, type ProcessSettingsDialogGeneralTabHandle} from './process-settings-dialog-general-tab';
import Save from '@aivot/mui-material-symbols-400-outlined/dist/save/Save';
import {useRetainedDialogValue} from '../../../../hooks/use-retained-dialog-value';
import {ProcessSettingsDialogVersionTab, type ProcessSettingsDialogVersionTabHandle} from './process-settings-dialog-version-tab';

interface ProcessSettingsDialogProps {
    open: boolean;
    onClose: () => void;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    onProcessChange: (process: ProcessEntity) => void;
    onVersionChange: (version: ProcessVersionEntity) => void;
}

export function ProcessSettingsDialog(props: ProcessSettingsDialogProps) {
    const dispatch = useAppDispatch();

    const {
        open,
        onClose,
        process,
        version,
        onProcessChange,
        onVersionChange,
    } = props;

    const [currentTab, setCurrentTab] = useState(0);
    const [hasUnsavedGeneralChanges, setHasUnsavedGeneralChanges] = useState(false);
    const [isSavingGeneralSettings, setIsSavingGeneralSettings] = useState(false);
    const [hasGeneralValidationError, setHasGeneralValidationError] = useState(false);
    const [hasUnsavedVersionChanges, setHasUnsavedVersionChanges] = useState(false);
    const [isSavingVersionSettings, setIsSavingVersionSettings] = useState(false);
    const [hasVersionValidationError, setHasVersionValidationError] = useState(false);
    const [hasUnsavedProcessAccessChanges, setHasUnsavedProcessAccessChanges] = useState(false);
    const [hasUnsavedProcessInstanceAccessPresetChanges, setHasUnsavedProcessInstanceAccessPresetChanges] = useState(false);
    const generalTabRef = useRef<ProcessSettingsDialogGeneralTabHandle | null>(null);
    const versionTabRef = useRef<ProcessSettingsDialogVersionTabHandle | null>(null);
    const renderProcess = useRetainedDialogValue(open, process);
    const renderVersion = useRetainedDialogValue(open, version);
    const renderOnProcessChange = useRetainedDialogValue(open, onProcessChange);
    const renderOnVersionChange = useRetainedDialogValue(open, onVersionChange);

    useEffect(() => {
        if (open) {
            setCurrentTab(0);
            setHasUnsavedGeneralChanges(false);
            setIsSavingGeneralSettings(false);
            setHasGeneralValidationError(false);
            setHasUnsavedVersionChanges(false);
            setIsSavingVersionSettings(false);
            setHasVersionValidationError(false);
            setHasUnsavedProcessAccessChanges(false);
            setHasUnsavedProcessInstanceAccessPresetChanges(false);
        }
    }, [open]);

    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[]>([]);
    useEffect(() => {
        new VDepartmentShadowedApiService()
            .listAll()
            .then(({content}) => {
                setDepartments(content);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der List der Organisationseinheiten'));
            });
    }, []);

    const [teams, setTeams] = useState<TeamEntity[]>([]);
    useEffect(() => {
        new TeamsApiService()
            .listAll()
            .then(({content}) => {
                setTeams(content);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Liste der Teams'));
            });
    }, []);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="lg"
            fullWidth={true}
        >
            <DialogTitleWithClose onClose={onClose}>
                Einstellungen für diesen Prozess
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    mt: -1,
                    p: 0,
                }}
            >
                <Tabs
                    sx={{
                        borderBottom: '1px solid rgba(0, 0, 0, 0.12)',
                    }}
                    value={currentTab}
                    onChange={(_, newValue) => {
                        if (currentTab === 0 && newValue !== currentTab && hasUnsavedGeneralChanges) {
                            dispatch(showWarningSnackbar('Bitte speichern Sie zuerst die Änderungen in den allgemeinen Einstellungen.'));
                            return;
                        }
                        if (currentTab === 1 && newValue !== currentTab && hasUnsavedVersionChanges) {
                            dispatch(showWarningSnackbar('Bitte speichern Sie zuerst die Änderungen in den versionsspezifischen Einstellungen.'));
                            return;
                        }
                        if (currentTab === 2 && newValue !== currentTab && hasUnsavedProcessAccessChanges) {
                            dispatch(showWarningSnackbar('Bitte speichern Sie zuerst die Änderungen in den Prozessberechtigungen.'));
                            return;
                        }
                        if (currentTab === 3 && newValue !== currentTab && hasUnsavedProcessInstanceAccessPresetChanges) {
                            dispatch(showWarningSnackbar('Bitte speichern Sie zuerst die Änderungen in den Berechtigungen für neue Vorgänge.'));
                            return;
                        }

                        setCurrentTab(newValue);
                    }}
                >
                    <Tab
                        value={0}
                        label="Allgemeine Einstellungen"
                    />
                    <Tab
                        value={1}
                        label="Versionsspezifische Einstellungen"
                    />
                    <Tab
                        value={2}
                        label="Berechtigungen des Prozesses"
                    />
                    <Tab
                        value={3}
                        label="Berechtigungen für neue Vorgänge"
                    />
                    <Tab
                        value={4}
                        label="Testprofile"
                        disabled={true}
                    />
                </Tabs>

                <Box
                    sx={{
                        p: 2,
                    }}
                >
                    {
                        currentTab === 0 &&
                        <ProcessSettingsDialogGeneralTab
                            ref={generalTabRef}
                            open={open}
                            process={renderProcess}
                            departments={departments}
                            onProcessChange={renderOnProcessChange}
                            onUnsavedChangesChange={setHasUnsavedGeneralChanges}
                            onSavingChange={setIsSavingGeneralSettings}
                            onValidationErrorChange={setHasGeneralValidationError}
                        />
                    }
                    {
                        currentTab === 1 &&
                        <ProcessSettingsDialogVersionTab
                            ref={versionTabRef}
                            open={open}
                            version={renderVersion}
                            onVersionChange={renderOnVersionChange}
                            onUnsavedChangesChange={setHasUnsavedVersionChanges}
                            onSavingChange={setIsSavingVersionSettings}
                            onValidationErrorChange={setHasVersionValidationError}
                        />
                    }
                    {
                        currentTab === 2 &&
                        <ProcessSettingsDialogProcessAccessTab
                            open={open}
                            process={renderProcess}
                            departments={departments}
                            teams={teams}
                            onUnsavedChangesChange={setHasUnsavedProcessAccessChanges}
                        />
                    }
                    {
                        currentTab === 3 &&
                        <ProcessSettingsDialogProcessInstanceAccessPresetTab
                            open={open}
                            process={renderProcess}
                            version={renderVersion}
                            departments={departments}
                            teams={teams}
                            onUnsavedChangesChange={setHasUnsavedProcessInstanceAccessPresetChanges}
                        />
                    }
                </Box>
            </DialogContent>
            <DialogActions sx={{px: 2}}>
                {
                    currentTab === 0 &&
                    <Button
                        variant="contained"
                        startIcon={<Save/>}
                        disabled={!hasUnsavedGeneralChanges || hasGeneralValidationError || isSavingGeneralSettings}
                        onClick={() => {
                            generalTabRef.current?.save();
                        }}
                    >
                        Speichern
                    </Button>
                }
                {
                    currentTab === 1 &&
                    <Button
                        variant="contained"
                        startIcon={<Save/>}
                        disabled={!hasUnsavedVersionChanges || hasVersionValidationError || isSavingVersionSettings}
                        onClick={() => {
                            versionTabRef.current?.save();
                        }}
                    >
                        Speichern
                    </Button>
                }
                <Box sx={{flexGrow: 1}}/>
                <Button
                    onClick={onClose}
                    disabled={isSavingGeneralSettings || isSavingVersionSettings}
                >
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
