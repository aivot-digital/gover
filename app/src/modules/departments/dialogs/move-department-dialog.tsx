import React, {useEffect, useMemo, useState} from 'react';
import {
    Autocomplete,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    Skeleton,
    TextField,
    Typography
} from '@mui/material';
import MoveGroup from '@aivot/mui-material-symbols-400-outlined/dist/move-group/MoveGroup';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {DepartmentEntity} from '../entities/department-entity';
import {VDepartmentShadowedEntity} from '../entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../services/v-department-shadowed-api-service';
import {getDepartmentPath, getDepartmentTypeIcons, getDepartmentTypeLabel} from '../utils/department-utils';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {DepartmentApiService} from '../services/department-api-service';
import {setLoadingMessage} from '../../../slices/shell-slice';

interface MoveDepartmentOption {
    value: number | null;
    label: string;
    subLabel?: string;
    icon?: React.ReactNode;
    disabled?: boolean;
}

interface MoveDepartmentDialogProps {
    department: DepartmentEntity;
    onClose: () => void;
    onMoved: (updatedDepartment: DepartmentEntity) => void;
}

export function MoveDepartmentDialog(props: MoveDepartmentDialogProps) {
    const {
        department,
        onClose,
        onMoved,
    } = props;

    const dispatch = useAppDispatch();

    const [availableDepartments, setAvailableDepartments] = useState<VDepartmentShadowedEntity[]>();
    const [targetParentOption, setTargetParentOption] = useState<MoveDepartmentOption | null>(null);

    useEffect(() => {
        new VDepartmentShadowedApiService()
            .listAllOrdered(['parentNames', 'name'], 'ASC')
            .then(({content}) => {
                setAvailableDepartments(content);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Liste der Organisationseinheiten konnte nicht geladen werden.'));
            });
    }, [dispatch]);

    const selectableParents = useMemo(() => {
        if (availableDepartments == null) {
            return [];
        }

        return availableDepartments
            .filter((candidate) => {
                if (candidate.id === department.id) {
                    return false;
                }

                const parentIds = candidate.parentIds ?? [];
                return !parentIds.includes(department.id);
            });
    }, [availableDepartments, department.id]);

    const parentOptions = useMemo<MoveDepartmentOption[]>(() => {
        const rootOption: MoveDepartmentOption = {
            value: null,
            label: 'Keine übergeordnete Organisationseinheit (höchste Ebene)',
            subLabel: 'Die Organisationseinheit wird zur Wurzelebene verschoben.',
            disabled: department.parentDepartmentId == null,
        };

        return [
            rootOption,
            ...selectableParents.map((candidate): MoveDepartmentOption => ({
                value: candidate.id,
                label: getDepartmentPath(candidate),
                subLabel: getDepartmentTypeLabel(candidate.depth),
                icon: getDepartmentTypeIcons(candidate.depth),
                disabled: candidate.id === department.parentDepartmentId,
            })),
        ];
    }, [department.parentDepartmentId, selectableParents]);

    const currentParentLabel = useMemo(() => {
        if (department.parentDepartmentId == null) {
            return 'Höchste Ebene (keine übergeordnete Organisationseinheit)';
        }

        const currentParent = selectableParents.find((candidate) => candidate.id === department.parentDepartmentId);
        return currentParent != null
            ? getDepartmentPath(currentParent)
            : `ID ${department.parentDepartmentId}`;
    }, [department.parentDepartmentId, selectableParents]);

    const handleMove = () => {
        if (targetParentOption == null) {
            dispatch(showErrorSnackbar('Bitte wählen Sie eine neue übergeordnete Organisationseinheit aus.'));
            return;
        }

        if (targetParentOption.disabled) {
            dispatch(showErrorSnackbar('Die aktuell zugewiesene Organisationseinheit kann nicht als Ziel ausgewählt werden.'));
            return;
        }

        const resolvedParentId = targetParentOption.value;

        if (resolvedParentId === department.parentDepartmentId) {
            dispatch(showErrorSnackbar('Die Organisationseinheit befindet sich bereits an der gewählten Position.'));
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Organisationseinheit wird verschoben',
            estimatedTime: 500,
            blocking: true,
        }));

        const apiService = new DepartmentApiService();

        apiService
            .retrieve(department.id)
            .then((latestDepartment) => apiService.update(department.id, {
                ...latestDepartment,
                parentDepartmentId: resolvedParentId,
            }))
            .then((updatedDepartment) => {
                dispatch(showSuccessSnackbar('Die Organisationseinheit wurde erfolgreich verschoben.'));
                onMoved(updatedDepartment);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Organisationseinheit konnte nicht verschoben werden.'));
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
            fullWidth
        >
            <DialogTitleWithClose onClose={onClose}>
                Organisationseinheit verschieben
            </DialogTitleWithClose>

            {
                availableDepartments == null &&
                <DialogContent tabIndex={0}>
                    <Skeleton variant="text" width="90%" height={52} />
                    <Skeleton variant="text" width="100%" height={52} sx={{mb: 1}} />
                    <Skeleton variant="rectangular" width="100%" height={56} />
                </DialogContent>
            }

            {
                availableDepartments != null &&
                <DialogContent tabIndex={0}>
                    <Typography variant="body1" gutterBottom>
                        Sie verschieben <strong>{department.name}</strong> in eine andere Hierarchieebene.
                    </Typography>

                    <Typography variant="body2" sx={{mb: 2}}>
                        Folgen der Verschiebung: Alle untergeordneten Organisationseinheiten werden mit verschoben,
                        die Ebenentiefe wird neu berechnet und bestehende Zuordnungen bleiben erhalten.
                    </Typography>

                    <Typography variant="body2" sx={{mb: 2}}>
                        Ungespeicherte Änderungen auf dieser Seite werden nicht automatisch mitgespeichert.
                    </Typography>

                    <Typography variant="body2" sx={{mb: 2}}>
                        Aktuelle übergeordnete Organisationseinheit: <br/>
                        <strong>{currentParentLabel}</strong>
                    </Typography>

                    <Autocomplete<MoveDepartmentOption, false, false, false>
                        options={parentOptions}
                        value={targetParentOption}
                        onChange={(_, value) => {
                            setTargetParentOption(value);
                        }}
                        getOptionLabel={(option) => option.label}
                        isOptionEqualToValue={(option, value) => option.value === value.value}
                        getOptionDisabled={(option) => option.disabled ?? false}
                        noOptionsText="Keine gültigen Ziel-Organisationseinheiten verfügbar"
                        renderOption={(props, option) => (
                            <Box
                                component="li"
                                {...props}
                                sx={{
                                    display: 'flex',
                                    alignItems: 'flex-start',
                                    py: 0.5,
                                    minHeight: 40,
                                }}
                            >
                                {
                                    option.icon != null &&
                                    <Box
                                        sx={{
                                            mr: 1,
                                            display: 'flex',
                                            alignItems: 'center',
                                        }}
                                    >
                                        {option.icon}
                                    </Box>
                                }
                                <Box
                                    sx={{
                                        minWidth: 0,
                                        display: 'flex',
                                        flexDirection: 'column',
                                        gap: 0.125,
                                        flex: 1,
                                    }}
                                >
                                    <Typography
                                        variant="body2"
                                        sx={{
                                            lineHeight: 1.2,
                                        }}
                                    >
                                        {option.label}
                                    </Typography>
                                    {
                                        option.subLabel != null &&
                                        <Typography
                                            variant="caption"
                                            color="textSecondary"
                                            sx={{
                                                lineHeight: 1.2,
                                            }}
                                        >
                                            {option.subLabel}
                                        </Typography>
                                    }
                                </Box>
                            </Box>
                        )}
                        renderInput={(params) => (
                            <TextField
                                {...params}
                                label="Neue übergeordnete Organisationseinheit"
                                placeholder="Organisationseinheit suchen…"
                            />
                        )}
                    />
                </DialogContent>
            }

            <DialogActions>
                <Button
                    onClick={handleMove}
                    color="primary"
                    variant="contained"
                    startIcon={<MoveGroup />}
                    disabled={availableDepartments == null || targetParentOption == null}
                >
                    Ja, verschieben
                </Button>
                <Button onClick={onClose}>
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
