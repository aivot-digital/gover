import React, {useEffect, useMemo, useState} from 'react';
import {
    Alert,
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
import MoveGroup from '@aivot/mui-material-symbols-400-n25-outlined/MoveGroup';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {DepartmentEntity} from '../entities/department-entity';
import {VDepartmentShadowedEntity} from '../entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../services/v-department-shadowed-api-service';
import {getDepartmentPath, getDepartmentTypeIcons, getDepartmentTypeLabel, getMaxDepartmentDepth} from '../utils/department-utils';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {DepartmentApiService} from '../services/department-api-service';
import {setLoadingMessage} from '../../../slices/shell-slice';
import {normalizePhoneNumberForTelLink} from '../../../utils/phone-number-utils';

interface MoveDepartmentOption {
    value: number | null;
    label: string;
    subLabel?: string;
    icon?: React.ReactNode;
    disabled?: boolean;
    disabledReason?: string;
}

interface MoveDepartmentDialogProps {
    department: DepartmentEntity;
    onClose: () => void;
    onMoved: (updatedDepartment: DepartmentEntity) => void;
}

function createDepartmentUpdateForMove(
    latestDepartment: DepartmentEntity,
    parentDepartmentId: number | null,
    shadowedDepartment?: VDepartmentShadowedEntity,
): DepartmentEntity {
    if (parentDepartmentId != null || shadowedDepartment == null) {
        return {
            ...latestDepartment,
            parentDepartmentId,
        };
    }

    // Moving to root removes inheritance; materialize effective values so the department remains valid as a top-level organization.
    return {
        ...latestDepartment,
        parentDepartmentId,
        postalAddress: latestDepartment.postalAddress ?? shadowedDepartment.postalAddress,
        technicalSupportEmail: latestDepartment.technicalSupportEmail ?? shadowedDepartment.technicalSupportEmail,
        technicalSupportPhone: latestDepartment.technicalSupportPhone ?? normalizePhoneNumberForTelLink(shadowedDepartment.technicalSupportPhone),
        technicalSupportInfo: latestDepartment.technicalSupportInfo ?? shadowedDepartment.technicalSupportInfo,
        specialSupportEmail: latestDepartment.specialSupportEmail ?? shadowedDepartment.specialSupportEmail,
        specialSupportPhone: latestDepartment.specialSupportPhone ?? normalizePhoneNumberForTelLink(shadowedDepartment.specialSupportPhone),
        specialSupportInfo: latestDepartment.specialSupportInfo ?? shadowedDepartment.specialSupportInfo,
        imprint: latestDepartment.imprint ?? shadowedDepartment.imprint,
        commonPrivacy: latestDepartment.commonPrivacy ?? shadowedDepartment.commonPrivacy,
        commonAccessibility: latestDepartment.commonAccessibility ?? shadowedDepartment.commonAccessibility,
        defaultMailSignature: latestDepartment.defaultMailSignature ?? shadowedDepartment.defaultMailSignature,
        themeId: latestDepartment.themeId ?? shadowedDepartment.themeId,
    };
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
    const maxDepartmentDepth = getMaxDepartmentDepth();

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

    const departmentSubtreeHeight = useMemo(() => {
        if (availableDepartments == null) {
            return 0;
        }

        return availableDepartments.reduce((maxDepthOffset, candidate) => {
            const parentIds = candidate.parentIds ?? [];
            if (!parentIds.includes(department.id)) {
                return maxDepthOffset;
            }

            return Math.max(maxDepthOffset, candidate.depth - department.depth);
        }, 0);
    }, [availableDepartments, department.depth, department.id]);

    const parentOptions = useMemo<MoveDepartmentOption[]>(() => {
        const rootDisabledReason = department.parentDepartmentId == null ?
            'Bereits auf der höchsten Ebene.' :
            departmentSubtreeHeight > maxDepartmentDepth ?
                'Die Unterstruktur wäre für die höchste Ebene zu tief.' :
                undefined;
        const rootOption: MoveDepartmentOption = {
            value: null,
            label: 'Keine übergeordnete Organisationseinheit (höchste Ebene)',
            subLabel: rootDisabledReason ?? 'Die Organisationseinheit wird zur Wurzelebene verschoben.',
            disabled: rootDisabledReason != null,
            disabledReason: rootDisabledReason,
        };

        return [
            rootOption,
            ...(availableDepartments ?? []).map((candidate): MoveDepartmentOption => {
                const parentIds = candidate.parentIds ?? [];
                const disabledReason = candidate.id === department.id ?
                    'Diese Organisationseinheit kann nicht ihr eigenes Ziel sein.' :
                    parentIds.includes(department.id) ?
                        'Diese Untereinheit liegt innerhalb der zu verschiebenden Struktur.' :
                        candidate.depth + 1 + departmentSubtreeHeight > maxDepartmentDepth ?
                            'Die maximale Hierarchietiefe würde überschritten.' :
                            candidate.id === department.parentDepartmentId ?
                                'Bereits die aktuelle übergeordnete Organisationseinheit.' :
                                undefined;

                return {
                    value: candidate.id,
                    label: getDepartmentPath(candidate),
                    subLabel: disabledReason ?? getDepartmentTypeLabel(candidate.depth),
                    icon: getDepartmentTypeIcons(candidate.depth),
                    disabled: disabledReason != null,
                    disabledReason,
                };
            }),
        ];
    }, [availableDepartments, department.id, department.parentDepartmentId, departmentSubtreeHeight, maxDepartmentDepth]);

    const currentParentLabel = useMemo(() => {
        if (department.parentDepartmentId == null) {
            return 'Höchste Ebene (keine übergeordnete Organisationseinheit)';
        }

        const currentParent = availableDepartments?.find((candidate) => candidate.id === department.parentDepartmentId);
        return currentParent != null
            ? getDepartmentPath(currentParent)
            : `ID ${department.parentDepartmentId}`;
    }, [availableDepartments, department.parentDepartmentId]);

    const handleMove = async () => {
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

        try {
            const latestDepartment = await apiService.retrieve(department.id);
            const shadowedDepartment = resolvedParentId == null ?
                await new VDepartmentShadowedApiService().retrieve(department.id) :
                undefined;
            const departmentUpdate = createDepartmentUpdateForMove(latestDepartment, resolvedParentId, shadowedDepartment);
            const updatedDepartment = await apiService.update(department.id, departmentUpdate);

            dispatch(showSuccessSnackbar('Die Organisationseinheit wurde erfolgreich verschoben.'));
            onMoved(updatedDepartment);
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Die Organisationseinheit konnte nicht verschoben werden.'));
            console.error(err);
        } finally {
            dispatch(setLoadingMessage(undefined));
        }
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
                        Die Organisationseinheit wird mit allen Untereinheiten verschoben. Als Ziel sind nur Positionen
                        möglich, bei denen die maximale Hierarchietiefe eingehalten wird. Die Einheit selbst und ihre
                        Untereinheiten sind ausgeschlossen. Soll eine Untereinheit das neue Ziel werden, verschieben Sie
                        diese zuerst an eine andere Stelle.
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
                        renderOption={({key, ...props}, option) => (
                            <Box
                                key={key}
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

                    {
                        targetParentOption?.value === null &&
                        <Alert
                            severity="info"
                            sx={{mt: 2}}
                        >
                            Beim Verschieben auf die höchste Ebene werden bisher geerbte Pflichtangaben als eigene
                            Angaben übernommen.
                        </Alert>
                    }
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
