import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Box, Button, Chip, Dialog, DialogActions, DialogContent, IconButton, Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ResourceAccessControlsApiService} from '../resource-access-controls-api-service';
import {ResourceAccessControlResponseDto} from '../dtos/resource-access-control-response-dto';
import AddIcon from '@mui/icons-material/Add';
import {ResourceAccessControlRequestDto} from '../dtos/resource-access-control-request-dto';
import {TeamResponseDTO} from '../../teams/dtos/team-response-dto';
import {TeamsApiService} from '../../teams/teams-api-service';
import {SelectFieldComponentOption} from '../../../components/select-field/select-field-component-option';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {Actions} from '../../../components/actions/actions';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {useConfirm} from '../../../providers/confirm-provider';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {selectIsLoading, setLoadingMessage} from '../../../slices/shell-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {withDelay} from '../../../utils/with-delay';
import ApprovalDelegation from '@aivot/mui-material-symbols-400-outlined/dist/approval-delegation/ApprovalDelegation';
import {VDepartmentShadowedEntity} from '../../departments/entities/v-department-shadowed-entity';
import {getDepartmentPath, getDepartmentTypeLabel} from '../../departments/utils/department-utils';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {FormApiService} from '../../forms/services/form-api-service';
import {FormEntity} from '../../forms/entities/form-entity';

interface FormResourceAccessControlDialogProps {
    open: boolean;
    onClose: () => void;
    formId: number | null | undefined;
}

export function FormResourceAccessControlDialog(props: FormResourceAccessControlDialogProps) {
    const {
        open,
        onClose,
        formId,
    } = props;

    const dispatch = useAppDispatch();
    const isGloballyLoading = useAppSelector(selectIsLoading);

    // The form to manage access for
    const [form, setForm] = useState<FormEntity>();

    // The list of all access controls for the form
    const [accessList, setAccessList] = useState<ResourceAccessControlWithSource[]>();

    // The list of all possible target options (teams and org units)
    const [targetOptions, setTargetOptions] = useState<SourceSelectionOption[]>();

    // The list of target options excluding those already in the access list
    const targetOptionsWithoutAlreadyInAccessList = useMemo(() => {
        if (accessList == null || form == null || targetOptions == null) {
            return [];
        }

        const usedIds: string[] = [
            createOrgUnitOptionValue(form.developingDepartmentId),
        ];
        for (const access of accessList) {
            if (access.sourceTeamId != null) {
                usedIds.push(`team_${access.sourceTeamId}`);
            } else if (access.sourceOrgUnitId != null) {
                usedIds.push(`org_${access.sourceOrgUnitId}`);
            }
        }

        return targetOptions
            .filter(o => !usedIds.includes(o.value));
    }, [form, accessList, targetOptions]);

    // Fetch the form details
    useEffect(() => {
        if (!open || formId == null) {
            setForm(undefined);
            return;
        }

        new FormApiService()
            .retrieve(formId)
            .then(setForm)
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Das Formular konnte nicht geladen werden.'));
            });
    }, [open, formId]);

    // Fetch all available teams and org units for target selection
    useEffect(() => {
        if (!open) {
            setTargetOptions(undefined);
            return;
        }

        Promise
            .all([
                new TeamsApiService()
                    .listAll(),
                new VDepartmentShadowedApiService()
                    .listAll(),
            ])
            .then(([teams, orgs]) => {
                const teamOptions: SourceSelectionOption[] = teams.content.map((t: TeamResponseDTO) => ({
                    value: createTeamOptionValue(t.id),
                    label: t.name ?? 'Unbenanntes Team',
                    subLabel: 'Team',
                    icon: ModuleIcons.teams,
                    team: t,
                }));

                const orgOptions: SourceSelectionOption[] = orgs.content.map((o: VDepartmentShadowedEntity) => ({
                    value: createOrgUnitOptionValue(o.id),
                    label: getDepartmentPath(o),
                    subLabel: getDepartmentTypeLabel(o.depth),
                    icon: ModuleIcons.departments,
                    orgUnit: o,
                }));

                setTargetOptions([
                    ...teamOptions,
                    ...orgOptions,
                ]);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Teams und Organisationseinheiten konnten nicht geladen werden.'));
            });
    }, [open]);

    // Fetch the list of all access controls for the form
    useEffect(() => {
        if (formId == null || targetOptions == null) {
            setAccessList(undefined);
            return;
        }

        new ResourceAccessControlsApiService()
            .listAll({
                targetFormId: formId,
            })
            .then(access => {
                const accessWithSource: ResourceAccessControlWithSource[] = access.content.map((a) => {
                    if (isResourceAccessControlWithTeam(a)) {
                        const team = targetOptions
                            .filter(isSourceSelectionOptionWithTeam)
                            .find(o => o.team.id === a.sourceTeamId)?.team;

                        if (team == null) {
                            throw new Error(`Team mit ID ${a.sourceTeamId} nicht gefunden für Zugriffskontrolle ${a.id}`);
                        }

                        return {
                            ...a,
                            sourceTeam: team,
                        };
                    } else if (a.sourceOrgUnitId != null) {
                        const orgUnit = targetOptions
                            .filter(isSourceSelectionOptionWithOrgUnit)
                            .find(o => o.orgUnit.id === a.sourceOrgUnitId)?.orgUnit;

                        if (orgUnit == null) {
                            throw new Error(`Organisationseinheit mit ID ${a.sourceOrgUnitId} nicht gefunden für Zugriffskontrolle ${a.id}`);
                        }

                        return {
                            ...a,
                            sourceOrgUnit: orgUnit,
                        };
                    } else {
                        throw new Error(`Zugriffskontrolle ${a.id} hat weder sourceTeamId noch sourceOrgUnitId gesetzt.`);
                    }
                });

                setAccessList(accessWithSource);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Zugriffskontrollen konnten nicht geladen werden.'));
            });
    }, [formId, targetOptions]);

    // Update access control in the list, shadow update to the server and rollback on failure
    const handleUpdateAccess = (updated: ResourceAccessControlWithSource) => {
        if (accessList == null) {
            return;
        }

        const originalList = [
            ...accessList,
        ];

        const updatedList = accessList
            .map(a => a.id === updated.id ? updated : a);

        setAccessList(updatedList);

        new ResourceAccessControlsApiService()
            .update(updated.id, updated)
            .catch((error) => {
                setAccessList(originalList);
                dispatch(showApiErrorSnackbar(error, 'Die Zugriffskontrolle konnte nicht aktualisiert werden.'));
            });
    };


    const handleDeleteAccess = async (deleted: ResourceAccessControlWithSource) => {
        if (accessList == null) {
            return;
        }

        const confirmed = await confirm({
            title: 'Zugriff entfernen',
            children: (
                <>
                    <Typography>
                        Möchten Sie den Zugriff für {'sourceTeam' in deleted ? deleted.sourceTeam.name : deleted.sourceOrgUnit.name} auf diese Ressource wirklich entfernen?
                    </Typography>
                </>
            ),
        });

        if (!confirmed) {
            return;
        }

        const originalList = [
            ...accessList,
        ];

        const updatedList = accessList
            .filter(a => a.id !== deleted.id);

        setAccessList(updatedList);

        new ResourceAccessControlsApiService()
            .destroy(deleted.id)
            .catch((error) => {
                setAccessList(originalList);
                dispatch(showApiErrorSnackbar(error, 'Die Zugriffskontrolle konnte nicht gelöscht werden.'));
            });
    };


    const [addId, setAddId] = useState<string>();

    const confirm = useConfirm();

    const handleAddAccess = async () => {
        if (addId == null || formId == null || accessList == null) {
            return;
        }

        let teamId: number | null = null;
        if (addId.startsWith('team_')) {
            teamId = parseInt(addId.substring('team_'.length), 10);
        }

        let orgUnitId: number | null = null;
        if (addId.startsWith('org_')) {
            orgUnitId = parseInt(addId.substring('org_'.length), 10);
        }

        const newAccessRequest: ResourceAccessControlRequestDto = {
            ...ResourceAccessControlsApiService.initialize(),
            targetFormId: formId,
            sourceTeamId: teamId,
            sourceOrgUnitId: orgUnitId,
        };

        dispatch(setLoadingMessage({
            message: 'Zugriffskontrolle wird erstellt',
            blocking: true,
            estimatedTime: 1000,
        }));

        withDelay(
            new ResourceAccessControlsApiService()
                .create(newAccessRequest),
            500,
        )
            .then((createdAccess) => {
                // Get the source entity for the created access
                let accessWithSource: ResourceAccessControlWithSource;
                if (isResourceAccessControlWithTeam(createdAccess)) {
                    const team = targetOptions
                        ?.filter(isSourceSelectionOptionWithTeam)
                        .find(o => o.team.id === createdAccess.sourceTeamId)?.team;

                    if (team == null) {
                        throw new Error(`Team mit ID ${createdAccess.sourceTeamId} nicht gefunden für Zugriffskontrolle ${createdAccess.id}`);
                    }

                    accessWithSource = {
                        ...createdAccess,
                        sourceTeam: team,
                    };
                } else if (createdAccess.sourceOrgUnitId != null) {
                    const orgUnit = targetOptions
                        ?.filter(isSourceSelectionOptionWithOrgUnit)
                        .find(o => o.orgUnit.id === createdAccess.sourceOrgUnitId)?.orgUnit;

                    if (orgUnit == null) {
                        throw new Error(`Organisationseinheit mit ID ${createdAccess.sourceOrgUnitId} nicht gefunden für Zugriffskontrolle ${createdAccess.id}`);
                    }

                    accessWithSource = {
                        ...createdAccess,
                        sourceOrgUnit: orgUnit,
                    };
                } else {
                    throw new Error(`Zugriffskontrolle ${createdAccess.id} hat weder sourceTeamId noch sourceOrgUnitId gesetzt.`);
                }

                setAccessList([
                    ...accessList,
                    accessWithSource,
                ]);
                setAddId(undefined);
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Zugriffskontrolle konnte nicht erstellt werden.'));
            })
            .finally(() => {
                dispatch(setLoadingMessage(undefined));
            });
    };

    // Handle the close action and cleaning up the state
    const handleClose = useCallback(() => {
        onClose();

        // Wait a little before clearing to allow for closing animation
        setTimeout(() => {
            setForm(undefined);
            setAccessList(undefined);
        }, 300);
    }, [onClose]);

    // Flags for ease of use
    const isLoading =
        form == null ||
        accessList == null ||
        targetOptions == null;

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            maxWidth="xl"
            fullWidth
        >
            <DialogTitleWithClose onClose={handleClose}>
                <ApprovalDelegation
                    sx={{
                        mr: 1,
                    }}
                />

                Zugriff für das Formular {
                isLoading ?
                    <Skeleton /> :
                    `„${form.internalTitle}“`
            }
            </DialogTitleWithClose>

            <DialogContent>
                <Typography
                    variant="h6"
                    gutterBottom
                >
                    Vorhandene Zugriffe
                </Typography>

                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell size="small">
                                    Team/Organisationseinheit
                                </TableCell>

                                {
                                    PermissionFields.map((field) => (
                                        <TableCell
                                            key={field}
                                            align="center"
                                            size="small"
                                        >
                                            {PermissionLabels[field] ?? ''}
                                        </TableCell>
                                    ))
                                }

                                <TableCell size="small">
                                    Aktionen
                                </TableCell>
                            </TableRow>
                        </TableHead>

                        <TableBody>
                            {
                                isLoading &&
                                <TableRow>
                                    <TableCell
                                        size="small"
                                    >
                                        <Skeleton />
                                    </TableCell>

                                    {
                                        PermissionFields.map((field) => (
                                            <TableCell
                                                key={field}
                                                size="small"
                                            >
                                                <Skeleton />
                                            </TableCell>
                                        ))
                                    }

                                    <TableCell
                                        size="small"
                                    >
                                        <Skeleton />
                                    </TableCell>
                                </TableRow>
                            }

                            {
                                !isLoading &&
                                <AccessRow
                                    isOwner={true}
                                    access={{
                                        ...DefaultAccess,
                                        sourceOrgUnitId: form.developingDepartmentId,
                                        sourceOrgUnit: targetOptions
                                            .filter(isSourceSelectionOptionWithOrgUnit)
                                            .find(o => o.orgUnit.id === form.developingDepartmentId)!
                                            .orgUnit,
                                        formPermissionAnnotate: true,
                                        formPermissionCreate: true,
                                        formPermissionDelete: true,
                                        formPermissionEdit: true,
                                        formPermissionPublish: true,
                                        formPermissionRead: true,
                                        id: -1,
                                    }}
                                />
                            }

                            {
                                accessList != null &&
                                accessList.length > 0 &&
                                accessList.map((access) => (
                                    <AccessRow
                                        key={access.id}
                                        isOwner={false}
                                        access={access}
                                        onUpdateAccess={handleUpdateAccess}
                                        onDeleteAccess={handleDeleteAccess}
                                    />
                                ))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>

                <Box mt={3}>
                    <Typography
                        variant="h6"
                        gutterBottom
                    >
                        Zugriff hinzufügen
                    </Typography>
                    <Box
                        display="flex"
                        alignItems="center"
                        gap={2}
                    >
                        {
                            targetOptions == null &&
                            <Skeleton />
                        }

                        {
                            targetOptions != null &&
                            <SelectFieldComponent
                                label="Hinzufügen"
                                value={addId}
                                onChange={(val) => {
                                    setAddId(val);
                                }}
                                options={targetOptions}
                                disabled={isGloballyLoading}
                            />
                        }

                        <IconButton
                            color="primary"
                            onClick={handleAddAccess}
                            disabled={!addId || isGloballyLoading}
                        >
                            <AddIcon />
                        </IconButton>
                    </Box>
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose}>
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

interface AccessRowProps {
    isOwner: boolean;
    access: ResourceAccessControlWithSource;
    onUpdateAccess?: (updated: ResourceAccessControlWithSource) => void;
    onDeleteAccess?: (deleted: ResourceAccessControlWithSource) => void;
}

function AccessRow(props: AccessRowProps) {
    const {
        isOwner,
        access,
        onUpdateAccess,
        onDeleteAccess,
    } = props;

    const isTeam = isResourceAccessControlWithTeam(access);

    return (
        <TableRow
            key={access.id}
        >
            <TableCell
                size="small"
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                    }}
                >
                    {
                        isTeam ?
                            ModuleIcons.teams :
                            ModuleIcons.departments
                    }

                    <Typography
                        sx={{
                            ml: 1,
                        }}
                    >
                        {
                            isTeam ?
                                access.sourceTeam.name :
                                access.sourceOrgUnit.name
                        }
                    </Typography>

                    {
                        isOwner &&
                        <Chip
                            label="Eigentümer:in"
                            size="small"
                            sx={{
                                ml: 1,
                            }}
                            variant="outlined"
                        />
                    }
                </Box>
            </TableCell>
            {
                PermissionFields
                    .map((field) => (
                        <TableCell
                            key={field}
                            align="center"
                            size="small"
                        >
                            <CheckboxFieldComponent
                                label={PermissionLabels[field] ?? ''}
                                onChange={(val) => {
                                    if (onUpdateAccess == null) {
                                        return;
                                    }

                                    const updatedAccess = {
                                        ...access,
                                        [field]: val,
                                    };

                                    if (val) {
                                        updatedAccess.formPermissionRead = true;
                                    }

                                    onUpdateAccess(updatedAccess);
                                }}
                                value={Boolean(access[field]) || false}
                                variant="switch"
                                sx={{
                                    p: 0,
                                    m: 0,
                                    width: 'unset',
                                }}
                                invisibleLabel={true}
                                disabled={onUpdateAccess == null || (field === 'formPermissionRead' && PermissionFields.some(f => f !== 'formPermissionRead' && Boolean(access[f])))}
                            />
                        </TableCell>
                    ))
            }
            <TableCell>
                <Actions
                    actions={[{
                        icon: <Delete />,
                        tooltip: onDeleteAccess == null ? 'Zugriff kann nicht entfernt werden' : 'Zugriff entfernen',
                        onClick: () => {
                            if (onDeleteAccess != null) {
                                onDeleteAccess(access);
                            }
                        },
                        disabled: onDeleteAccess == null,
                    }]}
                    size="small"
                />
            </TableCell>
        </TableRow>
    );
}


const PermissionFields: Array<keyof ResourceAccessControlRequestDto> = [
    'formPermissionRead',
    'formPermissionEdit',
    'formPermissionDelete',
    'formPermissionAnnotate',
    'formPermissionPublish',
];

const PermissionLabels: Partial<Record<keyof ResourceAccessControlRequestDto, string>> = {
    'formPermissionRead': 'Lesen',
    'formPermissionEdit': 'Bearbeiten',
    'formPermissionDelete': 'Löschen',
    'formPermissionAnnotate': 'Prüfen',
    'formPermissionPublish': 'Veröffentlichen/Zurückziehen',
};

const DefaultAccess = ResourceAccessControlsApiService.initialize();

type ResourceAccessControlWithTeam = ResourceAccessControlResponseDto & {
    sourceTeam: TeamResponseDTO;
};

function isResourceAccessControlWithTeam(access: ResourceAccessControlResponseDto): access is ResourceAccessControlWithTeam {
    return access.sourceTeamId != null;
}

type ResourceAccessControlWithOrgUnit = ResourceAccessControlResponseDto & {
    sourceOrgUnit: VDepartmentShadowedEntity;
};

type ResourceAccessControlWithSource = ResourceAccessControlWithTeam | ResourceAccessControlWithOrgUnit;

type SourceSelectionOptionWithTeam = SelectFieldComponentOption & {
    team: TeamResponseDTO;
};

function isSourceSelectionOptionWithTeam(option: SourceSelectionOption): option is SourceSelectionOptionWithTeam {
    return 'team' in option;
}

type SourceSelectionOptionWithOrgUnit = SelectFieldComponentOption & {
    orgUnit: VDepartmentShadowedEntity;
};

function isSourceSelectionOptionWithOrgUnit(option: SourceSelectionOption): option is SourceSelectionOptionWithOrgUnit {
    return 'orgUnit' in option;
}

type SourceSelectionOption = SourceSelectionOptionWithTeam | SourceSelectionOptionWithOrgUnit;

function createTeamOptionValue(teamId: number): string {
    return `team_${teamId}`;
}

function createOrgUnitOptionValue(orgUnitId: number): string {
    return `org_${orgUnitId}`;
}
