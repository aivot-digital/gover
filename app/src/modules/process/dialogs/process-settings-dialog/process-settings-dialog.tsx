import {ProcessEntity} from '../../entities/process-entity';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {Box, Dialog, DialogContent, Tab, Tabs} from '@mui/material';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {useEffect, useState} from 'react';
import {VDepartmentShadowedApiService} from '../../../departments/services/v-department-shadowed-api-service';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessSettingsDialogProcessAccessTab} from './process-settings-dialog-process-access-tab';
import {TeamEntity} from '../../../teams/entities/team-entity';
import {TeamsApiService} from '../../../teams/services/teams-api-service';

interface ProcessSettingsDialogProps {
    open: boolean;
    onClose: () => void;
    process: ProcessEntity;
    version: ProcessVersionEntity;
}

export function ProcessSettingsDialog(props: ProcessSettingsDialogProps) {
    const dispatch = useAppDispatch();

    const {
        open,
        onClose,
        process,
    } = props;

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
            maxWidth="xl"
            fullWidth={true}
        >
            <DialogTitleWithClose onClose={onClose}>
                Einstellungen für diesen Prozess
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    p: 0,
                }}
            >
                <Tabs
                    sx={{
                        borderBottom: '1px solid rgba(0, 0, 0, 0.12)',
                    }}
                    value={1}
                >
                    <Tab
                        value={0}
                        label="Allgemeine Einstellungen"
                        disabled={true}
                    />
                    <Tab
                        value={1}
                        label="Berechtigungen des Prozesses"
                    />
                    <Tab
                        value={2}
                        label="Berechtigungen für neue Vorgänge"
                        disabled={true}
                    />
                    <Tab
                        value={3}
                        label="Testprofile"
                        disabled={true}
                    />
                </Tabs>

                <Box
                    sx={{
                        p: 2,
                    }}
                >
                    <ProcessSettingsDialogProcessAccessTab
                        process={process}
                        departments={departments}
                        teams={teams}
                    />
                </Box>
            </DialogContent>
        </Dialog>
    );
}

