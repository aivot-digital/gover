import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, Skeleton, Typography} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {isApiError} from '../../../models/api-error';
import MoveGroup from '@aivot/mui-material-symbols-400-outlined/dist/move-group/MoveGroup';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {setLoadingMessage} from '../../../slices/shell-slice';
import {VDepartmentShadowedEntity} from '../../departments/entities/v-department-shadowed-entity';
import {DepartmentSelectField} from '../../departments/components/department-select-field';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessDefinitionApiService} from '../services/process-definition-api-service';

interface MoveProcessToDepartmentDialogProps {
    processId: number;
    onClose: () => void;
    onMoved: (process: ProcessEntity) => void;
}

export function MoveProcessToDepartmentDialog(props: MoveProcessToDepartmentDialogProps) {
    const {
        processId,
        onClose,
        onMoved,
    } = props;

    const dispatch = useAppDispatch();

    const [targetDepartment, setTargetDepartment] = useState<VDepartmentShadowedEntity | null>(null);
    const [targetDepartmentError, setTargetDepartmentError] = useState<string | undefined>();

    const [process, setProcess] = useState<ProcessEntity>();
    useEffect(() => {
        setProcess(undefined);
        setTargetDepartment(null);
        setTargetDepartmentError(undefined);

        new ProcessDefinitionApiService()
            .retrieve(processId)
            .then(setProcess)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Prozess konnte nicht geladen werden.'));
            });
    }, [dispatch, processId]);

    const handleMove = () => {
        if (process == null) {
            return;
        }

        if (targetDepartment == null) {
            setTargetDepartmentError('Bitte wählen Sie eine neue verwaltende Organisationseinheit aus.');
            dispatch(showErrorSnackbar('Bitte wählen Sie eine Organisationseinheit aus, an die der Prozess übertragen werden soll.'));
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Prozess wird übertragen',
            estimatedTime: 500,
            blocking: true,
        }));

        new ProcessDefinitionApiService()
            .move(process.id, targetDepartment.id)
            .then((updatedProcess) => {
                dispatch(showSuccessSnackbar('Der Prozess wurde erfolgreich übertragen.'));
                onMoved(updatedProcess);
            })
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    dispatch(showErrorSnackbar(err.message));
                } else {
                    dispatch(showErrorSnackbar('Der Prozess konnte nicht übertragen werden.'));
                }
                console.error(err);
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    };

    return (
        <Dialog
            open={true}
            onClose={onClose}
            fullWidth={true}
        >
            <DialogTitleWithClose
                onClose={onClose}
            >
                Prozess an Organisationseinheit übertragen
            </DialogTitleWithClose>

            {
                process == null &&
                <DialogContent tabIndex={0}>
                    <Skeleton
                        variant="text"
                        width="90%"
                        height={56}
                        sx={{
                            fontSize: '1rem',
                        }}
                    />

                    <Skeleton
                        variant="text"
                        width="100%"
                        height={56}
                        sx={{
                            fontSize: '0.875rem',
                            mb: 2,
                        }}
                    />

                    <Skeleton
                        variant="rectangular"
                        width="100%"
                        height={56}
                    />
                </DialogContent>
            }

            {
                process != null &&
                <DialogContent tabIndex={0}>
                    <Typography
                        variant="body1"
                        gutterBottom={true}
                    >
                        Bitte wählen Sie die Organisationseinheit aus, an die der Prozess <strong>{process.internalTitle}</strong> übertragen werden soll.
                    </Typography>

                    <Typography
                        variant="body2"
                        gutterBottom={true}
                    >
                        Bitte beachten Sie, dass Sie möglicherweise nicht mehr auf den Prozess zugreifen können, wenn Sie ihn an eine andere Organisationseinheit übertragen.
                    </Typography>

                    <DepartmentSelectField
                        label="Neue verwaltende Organisationseinheit"
                        value={targetDepartment}
                        dialogTitle="Neue verwaltende Organisationseinheit auswählen"
                        onChange={(department) => {
                            if (department == null) {
                                setTargetDepartment(null);
                                setTargetDepartmentError(undefined);
                                return;
                            }

                            if (department.id === process.departmentId) {
                                setTargetDepartment(null);
                                setTargetDepartmentError('Bitte wählen Sie eine andere Organisationseinheit aus.');
                                return;
                            }

                            setTargetDepartment(department);
                            setTargetDepartmentError(undefined);
                        }}
                        error={targetDepartmentError}
                        hint="Wählen Sie die Organisationseinheit aus, die den Prozess künftig verwalten soll."
                        required
                    />
                </DialogContent>
            }

            <DialogActions>
                <Button
                    onClick={handleMove}
                    color="primary"
                    variant="contained"
                    startIcon={<MoveGroup />}
                    disabled={process == null || targetDepartment == null || targetDepartmentError != null}
                >
                    Ja, Prozess übertragen
                </Button>
                <Button
                    onClick={onClose}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
