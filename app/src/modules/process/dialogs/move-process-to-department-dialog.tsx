import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, Skeleton, Typography} from '@mui/material';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {isApiError} from '../../../models/api-error';
import MoveGroup from '@aivot/mui-material-symbols-400-outlined/dist/move-group/MoveGroup';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {setLoadingMessage} from '../../../slices/shell-slice';
import {withDelay} from '../../../utils/with-delay';
import {VDepartmentShadowedEntity} from '../../departments/entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {getDepartmentPath} from '../../departments/utils/department-utils';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessDefinitionApiService} from '../services/process-definition-api-service';

interface MoveFormToDepartmentDialogProps {
    processId: number;
    onClose: () => void;
    onMoved: () => void;
}

export function MoveProcessToDepartmentDialog(props: MoveFormToDepartmentDialogProps) {
    const {
        processId,
        onClose,
        onMoved,
    } = props;

    const dispatch = useAppDispatch();

    const [targetDepartmentId, setTargetDepartmentId] = useState<number | null>(null);

    const [process, setProcess] = useState<ProcessEntity>();
    useEffect(() => {
        new ProcessDefinitionApiService()
            .retrieve(processId)
            .then(setProcess)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Prozess konnte nicht geladen werden.'));
            });
    }, [processId]);

    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[]>();

    useEffect(() => {
        withDelay(
            new VDepartmentShadowedApiService()
                .listAllOrdered(['parentNames', 'name'], 'ASC'),
            600,
        )
            .then(({content}) => {
                setDepartments(content);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Liste der Organisationseinheiten konnte nicht geladen werden.'));
            });
    }, []);

    const handleMove = () => {
        if (process == null) {
            return;
        }

        if (targetDepartmentId == null) {
            dispatch(showErrorSnackbar('Bitte wählen Sie eine Organisationseinheit aus, an die das Formular übertragen werden soll.'));
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Formular wird übertragen',
            estimatedTime: 500,
            blocking: true,
        }));

        new ProcessDefinitionApiService()
            .move(process.id, targetDepartmentId)
            .then(() => {
                dispatch(showSuccessSnackbar('Das Formular wurde erfolgreich übertragen.'));
                onMoved();
            })
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    dispatch(showErrorSnackbar(err.message));
                } else {
                    dispatch(showErrorSnackbar('Das Formular konnte nicht übertragen werden.'));
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
                Formular an Organisationseinheit übertragen
            </DialogTitleWithClose>

            {
                (process == null || departments == null) &&
                <DialogContent tabIndex={0}>
                    {/* Skeleton for the first Typography */}
                    <Skeleton
                        variant="text"
                        width="90%"
                        height={56}
                        sx={{
                            fontSize: '1rem',
                        }}
                    />

                    {/* Skeleton for the second Typography */}
                    <Skeleton
                        variant="text"
                        width="100%"
                        height={56}
                        sx={{
                            fontSize: '0.875rem',
                            mb: 2,
                        }}
                    />

                    {/* Skeleton for the SelectFieldComponent */}
                    <Skeleton
                        variant="rectangular"
                        width="100%"
                        height={56}
                    />
                </DialogContent>
            }

            {
                process != null &&
                departments != null &&
                <DialogContent tabIndex={0}>
                    <Typography
                        variant="body1"
                        gutterBottom={true}
                    >
                        Bitte wählen Sie die Organisationseinheit aus, an die das Formular <strong>{process.internalTitle}</strong> übertragen werden soll.
                    </Typography>

                    <Typography
                        variant="body2"
                        gutterBottom={true}
                    >
                        Bitte beachten Sie, dass Sie möglicherweise nicht mehr auf das Formular zugreifen können, wenn Sie es an eine andere Organisationseinheit übertragen.
                    </Typography>

                    <SelectFieldComponent
                        label="Organisation"
                        value={targetDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            const valNumber = val != null ? parseInt(val.toString(), 10) : null;
                            setTargetDepartmentId(valNumber);
                        }}
                        options={
                            departments
                                .filter(dep => dep.id !== process.departmentId)
                                .map(dep => ({
                                    label: getDepartmentPath(dep),
                                    value: dep.id.toString(),
                                }))
                        }
                    />
                </DialogContent>
            }

            <DialogActions>
                <Button
                    onClick={handleMove}
                    color="primary"
                    variant="contained"
                    startIcon={<MoveGroup />}
                    disabled={process == null || departments == null}
                >
                    Ja, Formular übertragen
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
