import {ProcessEntity} from '../../entities/process-entity';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {Alert, Box, Button, Dialog, DialogActions, DialogContent, Tab, Tabs, Typography} from '@mui/material';
import {Permission} from '../../../../data/permissions/permission';
import {useHasDepartmentPermission, useHasProcessPermission} from '../../../permissions/hooks/use-permissions';
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
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';

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
    const canUpdateProcess = useHasProcessPermission(renderProcess.id, Permission.PROCESS_DEFINITION_UPDATE);
    const canDeleteProcess = useHasProcessPermission(renderProcess.id, Permission.PROCESS_DEFINITION_DELETE);
    // Delegated process grants do not count: only the managing department decides who has access to the process.
    const canManageProcessAccess = useHasDepartmentPermission(renderProcess.departmentId, Permission.PROCESS_DEFINITION_UPDATE);
    const missingUpdatePermissionTooltip = formatMissingPermissionTooltip(Permission.PROCESS_DEFINITION_UPDATE);
    // Only users with a delegated update grant reach this tab without canManageProcessAccess, so do not claim the permission is missing.
    const managingDepartmentRequiredTooltip = `Zum Ändern der Prozessberechtigungen benötigen Sie die Berechtigung „Prozess bearbeiten“ (${Permission.PROCESS_DEFINITION_UPDATE}) in der verwaltenden Organisationseinheit des Prozesses oder systemweit.`;
    const accessTabDisabledSx = !canUpdateProcess ? {
        color: 'text.disabled',
        cursor: 'not-allowed',
        '&:hover': {color: 'text.disabled'},
    } : undefined;
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
            confirmButtonColor: 'error',
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
        // Access tabs use aria-disabled to keep permission tooltips reachable on hover.
        // ARIA does not prevent activation, so reject unauthorized tab changes here.
        if (newValue === currentTab || isSavingSettings || ((newValue === 2 || newValue === 3) && !canUpdateProcess)) {
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
        new ThemesApiService()
            .listAll()
            .then(({content}) => {
                setThemes(content);
            })
            .catch((error) => {
                setThemes([]);
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Erscheinungsbilder'));
            });
    }, [dispatch]);

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
                        aria-label="Prozessberechtigungen"
                        label={(
                            <DisabledTooltip disabled={!canUpdateProcess} title={missingUpdatePermissionTooltip}>
                                Prozessberechtigungen
                            </DisabledTooltip>
                        )}
                        aria-disabled={!canUpdateProcess}
                        tabIndex={!canUpdateProcess ? -1 : undefined}
                        sx={accessTabDisabledSx}
                    />
                    <Tab
                        value={3}
                        aria-label="Standardrechte für neue Vorgänge"
                        label={(
                            <DisabledTooltip disabled={!canUpdateProcess} title={missingUpdatePermissionTooltip}>
                                Standardrechte für neue Vorgänge
                            </DisabledTooltip>
                        )}
                        aria-disabled={!canUpdateProcess}
                        tabIndex={!canUpdateProcess ? -1 : undefined}
                        sx={accessTabDisabledSx}
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
                        // The tooltips only appear on hover; this alert gives keyboard and screen-reader users the same reason.
                        !canUpdateProcess &&
                        <Alert
                            severity="info"
                            sx={{mb: 2}}
                        >
                            Sie können die Einstellungen einsehen. Zum Bearbeiten benötigen Sie die Berechtigung „Prozess bearbeiten“. Prozessberechtigungen und Standardrechte für neue Vorgänge sind nur mit dieser Berechtigung verfügbar.
                        </Alert>
                    }
                    {
                        currentTab === 0 &&
                        <ProcessSettingsDialogGeneralTab
                            ref={generalTabRef}
                            open={open}
                            readOnly={!canUpdateProcess}
                            canDeleteProcess={canDeleteProcess}
                            canMoveProcess={canManageProcessAccess}
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
                            readOnly={!canUpdateProcess}
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
                            readOnly={!canManageProcessAccess}
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
                    <DisabledTooltip
                        disabled={!canUpdateProcess}
                        title={missingUpdatePermissionTooltip}
                    >
                        <Button
                            variant="contained"
                            startIcon={<Save/>}
                            disabled={!canUpdateProcess || !hasUnsavedGeneralChanges || hasGeneralValidationError || isSavingGeneralSettings}
                            onClick={() => {
                                generalTabRef.current?.save();
                            }}
                        >
                            Speichern
                        </Button>
                    </DisabledTooltip>
                }
                {
                    currentTab === 1 &&
                    <DisabledTooltip
                        disabled={!canUpdateProcess}
                        title={missingUpdatePermissionTooltip}
                    >
                        <Button
                            variant="contained"
                            startIcon={<Save/>}
                            disabled={!canUpdateProcess || !hasUnsavedVersionChanges || hasVersionValidationError || isSavingVersionSettings}
                            onClick={() => {
                                versionTabRef.current?.save();
                            }}
                        >
                            Speichern
                        </Button>
                    </DisabledTooltip>
                }
                {
                    currentTab === 2 &&
                    <DisabledTooltip
                        disabled={!canManageProcessAccess}
                        title={managingDepartmentRequiredTooltip}
                    >
                        <Button
                            variant="contained"
                            startIcon={<Save/>}
                            disabled={!canManageProcessAccess || !hasUnsavedProcessAccessChanges || isSavingProcessAccess}
                            onClick={() => {
                                processAccessTabRef.current?.save();
                            }}
                        >
                            Speichern
                        </Button>
                    </DisabledTooltip>
                }
                {
                    currentTab === 3 &&
                    <DisabledTooltip
                        disabled={!canUpdateProcess}
                        title={missingUpdatePermissionTooltip}
                    >
                        <Button
                            variant="contained"
                            startIcon={<Save/>}
                            disabled={!canUpdateProcess || !hasUnsavedProcessInstanceAccessPresetChanges || isSavingProcessInstanceAccessPreset}
                            onClick={() => {
                                processInstanceAccessPresetTabRef.current?.save();
                            }}
                        >
                            Speichern
                        </Button>
                    </DisabledTooltip>
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
