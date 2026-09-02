import {ProcessEntity} from '../../entities/process-entity';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {Box, Button, Dialog, DialogActions, DialogContent, Tab, Tabs, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {useEffect, useRef, useState} from 'react';
import {VDepartmentShadowedApiService} from '../../../departments/services/v-department-shadowed-api-service';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessSettingsDialogProcessAccessTab, type ProcessSettingsDialogProcessAccessTabHandle} from './process-settings-dialog-process-access-tab';
import {TeamEntity} from '../../../teams/entities/team-entity';
import {TeamsApiService} from '../../../teams/services/teams-api-service';
import {
    ProcessSettingsDialogProcessInstanceAccessPresetTab,
    type ProcessSettingsDialogProcessInstanceAccessPresetTabHandle,
} from './process-settings-dialog-process-instance-access-preset-tab';
import {ProcessSettingsDialogGeneralTab, type ProcessSettingsDialogGeneralTabHandle} from './process-settings-dialog-general-tab';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useRetainedDialogValue} from '../../../../hooks/use-retained-dialog-value';
import {ProcessSettingsDialogVersionTab, type ProcessSettingsDialogVersionTabHandle} from './process-settings-dialog-version-tab';
import {useConfirm} from '../../../../providers/confirm-provider';
import {ThemesApiService} from '../../../themes/themes-api-service';
import {type ThemeResponseDTO} from '../../../themes/models/theme';
import {useApi} from '../../../../hooks/use-api';

interface ProcessSettingsDialogProps {
    open: boolean;
    onClose: () => void;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    onProcessChange: (process: ProcessEntity) => void;
    onVersionChange: (version: ProcessVersionEntity) => void;
    onDeleteProcess: () => void;
}

export function ProcessSettingsDialog(props: ProcessSettingsDialogProps) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const api = useApi();

    const {
        open,
        onClose,
        process,
        version,
        onProcessChange,
        onVersionChange,
        onDeleteProcess,
    } = props;

    const [currentTab, setCurrentTab] = useState(0);
    const [hasUnsavedGeneralChanges, setHasUnsavedGeneralChanges] = useState(false);
    const [isSavingGeneralSettings, setIsSavingGeneralSettings] = useState(false);
    const [hasGeneralValidationError, setHasGeneralValidationError] = useState(false);
    const [hasUnsavedVersionChanges, setHasUnsavedVersionChanges] = useState(false);
    const [isSavingVersionSettings, setIsSavingVersionSettings] = useState(false);
    const [hasVersionValidationError, setHasVersionValidationError] = useState(false);
    const [hasUnsavedProcessAccessChanges, setHasUnsavedProcessAccessChanges] = useState(false);
    const [isSavingProcessAccess, setIsSavingProcessAccess] = useState(false);
    const [hasUnsavedProcessInstanceAccessPresetChanges, setHasUnsavedProcessInstanceAccessPresetChanges] = useState(false);
    const [isSavingProcessInstanceAccessPreset, setIsSavingProcessInstanceAccessPreset] = useState(false);
    const generalTabRef = useRef<ProcessSettingsDialogGeneralTabHandle | null>(null);
    const versionTabRef = useRef<ProcessSettingsDialogVersionTabHandle | null>(null);
    const processAccessTabRef = useRef<ProcessSettingsDialogProcessAccessTabHandle | null>(null);
    const processInstanceAccessPresetTabRef = useRef<ProcessSettingsDialogProcessInstanceAccessPresetTabHandle | null>(null);
    const renderProcess = useRetainedDialogValue(open, process);
    const renderVersion = useRetainedDialogValue(open, version);
    const renderOnProcessChange = useRetainedDialogValue(open, onProcessChange);
    const renderOnVersionChange = useRetainedDialogValue(open, onVersionChange);
    const isSavingSettings = isSavingGeneralSettings || isSavingVersionSettings || isSavingProcessAccess || isSavingProcessInstanceAccessPreset;

    const getUnsavedChangesTabLabel = (tab: number): string | undefined => {
        if (tab === 0 && hasUnsavedGeneralChanges) {
            return 'Allgemeine Einstellungen';
        }

        if (tab === 1 && hasUnsavedVersionChanges) {
            return 'Versionsspezifische Einstellungen';
        }

        if (tab === 2 && hasUnsavedProcessAccessChanges) {
            return 'Prozessberechtigungen';
        }

        if (tab === 3 && hasUnsavedProcessInstanceAccessPresetChanges) {
            return 'Standardrechte für neue Vorgänge';
        }

        return undefined;
    };
    const hasCurrentTabUnsavedChanges = getUnsavedChangesTabLabel(currentTab) != null;

    const clearUnsavedChangesForTab = (tab: number): void => {
        if (tab === 0) {
            setHasUnsavedGeneralChanges(false);
        }

        if (tab === 1) {
            setHasUnsavedVersionChanges(false);
        }

        if (tab === 2) {
            setHasUnsavedProcessAccessChanges(false);
        }

        if (tab === 3) {
            setHasUnsavedProcessInstanceAccessPresetChanges(false);
        }
    };

    const confirmDiscardUnsavedChanges = async (actionText: string): Promise<boolean> => {
        const unsavedChangesTabLabel = getUnsavedChangesTabLabel(currentTab);

        if (unsavedChangesTabLabel == null) {
            return true;
        }

        return await confirm({
            title: 'Ungespeicherte Änderungen',
            confirmButtonText: 'Änderungen verwerfen',
            children: (
                <Typography>
                    Im Tab <strong>{unsavedChangesTabLabel}</strong> gibt es ungespeicherte Änderungen.
                    Möchten Sie diese Änderungen verwerfen und {actionText}?
                </Typography>
            ),
        });
    };

    const handleTabChange = async (newValue: number): Promise<void> => {
        if (newValue === currentTab || isSavingSettings) {
            return;
        }

        const shouldContinue = await confirmDiscardUnsavedChanges('zum ausgewählten Tab wechseln');
        if (!shouldContinue) {
            return;
        }

        clearUnsavedChangesForTab(currentTab);
        setCurrentTab(newValue);
    };

    const handleClose = async (): Promise<void> => {
        if (isSavingSettings) {
            return;
        }

        const shouldClose = await confirmDiscardUnsavedChanges('den Dialog schließen');
        if (!shouldClose) {
            return;
        }

        clearUnsavedChangesForTab(currentTab);
        onClose();
    };

    const handleResetCurrentTab = (): void => {
        if (!hasCurrentTabUnsavedChanges || isSavingSettings) {
            return;
        }

        if (currentTab === 0) {
            generalTabRef.current?.reset();
        }

        if (currentTab === 1) {
            versionTabRef.current?.reset();
        }

        if (currentTab === 2) {
            processAccessTabRef.current?.reset();
        }

        if (currentTab === 3) {
            processInstanceAccessPresetTabRef.current?.reset();
        }

        clearUnsavedChangesForTab(currentTab);
    };

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
            setIsSavingProcessAccess(false);
            setHasUnsavedProcessInstanceAccessPresetChanges(false);
            setIsSavingProcessInstanceAccessPreset(false);
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
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Liste der Organisationseinheiten'));
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

    const [themes, setThemes] = useState<ThemeResponseDTO[] | null>(null);
    useEffect(() => {
        new ThemesApiService(api)
            .listAll()
            .then(({content}) => {
                setThemes(content);
            })
            .catch((error) => {
                setThemes([]);
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Erscheinungsbilder'));
            });
    }, [api, dispatch]);

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            maxWidth="lg"
            fullWidth={true}
            sx={{
                '& .MuiDialog-container': {
                    alignItems: 'flex-start',
                },
            }}
        >
            <DialogTitleWithClose onClose={handleClose}>
                Einstellungen für diesen Prozess
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    mt: -1,
                    p: 0,
                    minWidth: 0,
                }}
            >
                <Tabs
                    variant="scrollable"
                    scrollButtons="auto"
                    allowScrollButtonsMobile
                    sx={{
                        borderBottom: '1px solid',
                        borderBottomColor: 'divider',
                        minWidth: 0,
                        maxWidth: '100%',
                    }}
                    value={currentTab}
                    onChange={(_, newValue) => {
                        void handleTabChange(newValue);
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
                        label="Prozessberechtigungen"
                    />
                    <Tab
                        value={3}
                        label="Standardrechte für neue Vorgänge"
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
                        minWidth: 0,
                        maxWidth: '100%',
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
                            onDeleteProcess={onDeleteProcess}
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
                            departments={departments}
                            themes={themes}
                            onVersionChange={renderOnVersionChange}
                            onUnsavedChangesChange={setHasUnsavedVersionChanges}
                            onSavingChange={setIsSavingVersionSettings}
                            onValidationErrorChange={setHasVersionValidationError}
                        />
                    }
                    {
                        currentTab === 2 &&
                        <ProcessSettingsDialogProcessAccessTab
                            ref={processAccessTabRef}
                            open={open}
                            process={renderProcess}
                            departments={departments}
                            teams={teams}
                            onUnsavedChangesChange={setHasUnsavedProcessAccessChanges}
                            onSavingChange={setIsSavingProcessAccess}
                        />
                    }
                    {
                        currentTab === 3 &&
                        <ProcessSettingsDialogProcessInstanceAccessPresetTab
                            ref={processInstanceAccessPresetTabRef}
                            open={open}
                            process={renderProcess}
                            version={renderVersion}
                            departments={departments}
                            teams={teams}
                            onUnsavedChangesChange={setHasUnsavedProcessInstanceAccessPresetChanges}
                            onSavingChange={setIsSavingProcessInstanceAccessPreset}
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
                {
                    currentTab === 2 &&
                    <Button
                        variant="contained"
                        startIcon={<Save/>}
                        disabled={!hasUnsavedProcessAccessChanges || isSavingProcessAccess}
                        onClick={() => {
                            processAccessTabRef.current?.save();
                        }}
                    >
                        Speichern
                    </Button>
                }
                {
                    currentTab === 3 &&
                    <Button
                        variant="contained"
                        startIcon={<Save/>}
                        disabled={!hasUnsavedProcessInstanceAccessPresetChanges || isSavingProcessInstanceAccessPreset}
                        onClick={() => {
                            processInstanceAccessPresetTabRef.current?.save();
                        }}
                    >
                        Speichern
                    </Button>
                }
                {
                    currentTab <= 3 &&
                    <Button
                        color="error"
                        disabled={!hasCurrentTabUnsavedChanges || isSavingSettings}
                        onClick={handleResetCurrentTab}
                    >
                        Zurücksetzen
                    </Button>
                }
                <Box sx={{flexGrow: 1}}/>
                <Button
                    onClick={handleClose}
                    disabled={isSavingSettings}
                >
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
