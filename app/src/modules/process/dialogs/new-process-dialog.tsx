import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import Stepper from '@mui/material/Stepper';
import {
    Box,
    Button,
    Card,
    CardActionArea,
    CircularProgress,
    Divider,
    Grid,
    InputAdornment,
    Step,
    StepLabel,
    type SvgIconProps,
    type SxProps,
} from '@mui/material';
import {type StepIconProps} from '@mui/material/StepIcon';
import React, {type FC, type ReactNode, useEffect, useMemo, useState} from 'react';
import Typography from '@mui/material/Typography';
import UploadFile from '@aivot/mui-material-symbols-400-outlined/dist/upload-file/UploadFile';
import Check from '@aivot/mui-material-symbols-400-outlined/dist/check/Check';
import Draft from '@aivot/mui-material-symbols-400-outlined/dist/draft/Draft';
import LinkIcon from '@aivot/mui-material-symbols-400-outlined/dist/link/Link';
import {uploadObjectFile} from '../../../utils/download-utils';
import {type ProcessExport} from '../entities/process-export';
import {
    VDepartmentMembershipWithDetailsService,
} from '../../departments/services/v-department-membership-with-details-service';
import {getDepartmentPath, getDepartmentTypeIcons} from '../../departments/utils/department-utils';
import {showApiErrorSnackbar, showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import ArrowBack from '@aivot/mui-material-symbols-400-outlined/dist/arrow-back/ArrowBack';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';
import {isStringNotNullOrEmpty, quoteString} from '../../../utils/string-utils';
import Save from '@aivot/mui-material-symbols-400-outlined/dist/save/Save';
import {ProcessDefinitionApiService} from '../services/process-definition-api-service';
import AddBox from '@aivot/mui-material-symbols-400-outlined/dist/add-box/AddBox';
import {ProcessStatus} from '../enums/process-status';
import {ProcessTemplatesService, type TemplateRegistryProcessItem} from '../services/process-templates-service';
import Flowsheet from '@aivot/mui-material-symbols-400-outlined/dist/flowsheet/Flowsheet';
import {useNavigate} from 'react-router-dom';
import {SHOW_ERRORS_ROUTER_STATE} from '../pages/details/process-details-page';
import {AlertComponent} from '../../../components/alert/alert-component';
import {StatusTable} from '../../../components/status-table/status-table';
import {type StatusTablePropsItem} from '../../../components/status-table/status-table-props';
import Label from '@aivot/mui-material-symbols-400-outlined/dist/label/Label';
import {DepartmentSelectField} from '../../departments/components/department-select-field';
import {type VDepartmentShadowedEntityWithChildren} from '../../departments/entities/v-department-shadowed-entity';
import {normalizeProcessSlugInput, PROCESS_SLUG_MAX_LENGTH, validateProcessSlug} from '../utils/process-slug-utils';
import {useRefreshPermissionSet} from '../../permissions/hooks/use-permissions';

interface NewProcessDialogProps {
    open: boolean;
    onCancel: () => void;
    preselectedTemplate?: ProcessExport;
}

type StartPointType = 'empty' | 'import' | 'template';

interface SelectedStartPoint {
    label: string;
    type: StartPointType;
}

export function NewProcessDialog(props: NewProcessDialogProps): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const refreshPermissionSet = useRefreshPermissionSet();

    const {
        open,
        onCancel,
        preselectedTemplate = null,
    } = props;

    const user = useAppSelector(selectUser);

    const [availableDepartments, setAvailableDepartments] = useState<VDepartmentShadowedEntityWithChildren[]>();
    const [nameOverride, setNameOverride] = useState<string | null>(null);
    const [publicTitleOverride, setPublicTitleOverride] = useState<string | null>(preselectedTemplate?.version.publicTitle ?? null);
    const [publicTitleManuallyEdited, setPublicTitleManuallyEdited] = useState(preselectedTemplate != null && isStringNotNullOrEmpty(preselectedTemplate.version.publicTitle));
    const [slugOverride, setSlugOverride] = useState<string | null>(null);
    const [slugManuallyEdited, setSlugManuallyEdited] = useState(false);
    const [departmentOverride, setDepartmentOverride] = useState<number | null>(null);
    const [nameError, setNameError] = useState<string | undefined>();
    const [publicTitleError, setPublicTitleError] = useState<string | undefined>();
    const [slugError, setSlugError] = useState<string | undefined>();
    const [slugAvailabilityError, setSlugAvailabilityError] = useState<string | undefined>();
    const [isCheckingSlugAvailability, setIsCheckingSlugAvailability] = useState(false);
    const [departmentError, setDepartmentError] = useState<string | undefined>();

    const [isLoading, setIsLoading] = useState(false);

    const [templates, setTemplates] = useState<TemplateRegistryProcessItem[] | null>(null);

    const [activeStep, setActiveStep] = useState(preselectedTemplate != null ? 1 : 0);
    const [selectedTemplateData, setSelectedTemplateData] = useState<ProcessExport | null>(preselectedTemplate);
    const [selectedStartPoint, setSelectedStartPoint] = useState<SelectedStartPoint | null>(preselectedTemplate != null ? {
        type: 'template',
        label: preselectedTemplate.process.internalTitle,
    } : null);

    const selectedDepartment = useMemo(() => (
        availableDepartments?.find((department) => department.id === departmentOverride) ?? null
    ), [availableDepartments, departmentOverride]);
    const selectedDepartmentPath = selectedDepartment != null && (selectedDepartment.parentNames?.length ?? 0) > 0 ?
        getDepartmentPath(selectedDepartment) :
        undefined;

    const hasProcessConfigurationErrors = nameError != null || publicTitleError != null || slugError != null || slugAvailabilityError != null || departmentError != null;

    const summaryItems = useMemo<StatusTablePropsItem[]>(() => [
        {
            label: 'Startpunkt',
            icon: <SummaryIcon>{getStartPointIcon(selectedStartPoint?.type)}</SummaryIcon>,
            children: (
                <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>
                    {selectedStartPoint?.label ?? 'Nicht ausgewählt'}
                </Typography>
            ),
        },
        {
            label: 'Name',
            icon: <SummaryIcon><Label/></SummaryIcon>,
            children: (
                <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>
                    {nameOverride?.trim() ?? 'Nicht angegeben'}
                </Typography>
            ),
        },
        {
            label: 'Öffentliche Bezeichnung',
            icon: <SummaryIcon><Label/></SummaryIcon>,
            children: (
                <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>
                    {publicTitleOverride?.trim() ?? 'Nicht angegeben'}
                </Typography>
            ),
        },
        {
            label: 'URL-Namespace',
            icon: <SummaryIcon><LinkIcon/></SummaryIcon>,
            children: (
                <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>
                    {slugOverride != null ? `/${slugOverride.trim()}` : 'Nicht angegeben'}
                </Typography>
            ),
        },
        {
            label: 'Organisationseinheit',
            icon: <SummaryIcon>{selectedDepartment != null ? getDepartmentTypeIcons(selectedDepartment.depth) : undefined}</SummaryIcon>,
            children: (
                <Box>
                    <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>
                        {selectedDepartment?.name ?? 'Nicht ausgewählt'}
                        {
                            selectedDepartmentPath != null &&
                            <Typography component="span" color="text.secondary" sx={{ml: 0.5}}>
                                ({selectedDepartmentPath})
                            </Typography>
                        }
                    </Typography>

                </Box>
            ),
        },
        {
            label: 'Status',
            icon: <SummaryIcon><Draft/></SummaryIcon>,
            children: (
                <Typography variant="body2">
                    Neue Prozesse werden als Entwurf angelegt.
                </Typography>
            ),
        },
    ], [nameOverride, publicTitleOverride, selectedDepartment, selectedDepartmentPath, selectedStartPoint, slugOverride]);

    useEffect(() => {
        if (user == null) {
            setAvailableDepartments([]);
            return;
        }

        setAvailableDepartments(undefined);

        new VDepartmentMembershipWithDetailsService()
            .listAll({
                userId: user.id,
            })
            .then(({content}) => {
                const departments: VDepartmentShadowedEntityWithChildren[] = content
                    .map((membership) => ({
                        id: membership.departmentId,
                        name: membership.departmentName,
                        address: membership.departmentAddress,
                        depth: membership.departmentDepth,
                        parentNames: membership.departmentParentNames,
                        created: '',
                        updated: '',
                        children: [],
                    }));
                setAvailableDepartments(departments);
            })
            .catch((err) => {
                setAvailableDepartments([]);
                dispatch(showApiErrorSnackbar(err, 'Die Organisationseinheiten konnten nicht geladen werden. Bitte versuchen Sie es erneut.'));
            });
    }, [dispatch, user]);

    useEffect(() => {
        new ProcessTemplatesService()
            .getProcessTemplates()
            .then(setTemplates)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Vorlagen konnten nicht geladen werden. Bitte versuchen Sie es später erneut.'));
            });
    }, [dispatch]);

    const validateName = (value: string | null): string | undefined => {
        const trimmedValue = value?.trim() ?? '';

        if (!isStringNotNullOrEmpty(trimmedValue)) {
            return 'Bitte geben Sie einen Namen für den Prozess an.';
        }

        if (trimmedValue.length < 3) {
            return 'Der Name des Prozesses muss mindestens 3 Zeichen lang sein.';
        }

        if (trimmedValue.length > 96) {
            return 'Der Name des Prozesses darf maximal 96 Zeichen lang sein.';
        }

        return undefined;
    };

    const validatePublicTitle = (value: string | null): string | undefined => {
        const trimmedValue = value?.trim() ?? '';

        if (!isStringNotNullOrEmpty(trimmedValue)) {
            return 'Bitte geben Sie eine öffentliche Bezeichnung für den Prozess an.';
        }

        if (trimmedValue.length < 3) {
            return 'Die öffentliche Bezeichnung des Prozesses muss mindestens 3 Zeichen lang sein.';
        }

        if (trimmedValue.length > 96) {
            return 'Die öffentliche Bezeichnung des Prozesses darf maximal 96 Zeichen lang sein.';
        }

        return undefined;
    };

    const validateDepartment = (value: number | null): string | undefined => {
        if (value == null) {
            return 'Bitte wählen Sie eine Organisationseinheit aus.';
        }

        return undefined;
    };

    const checkSlugAvailability = async (slug: string | null): Promise<boolean> => {
        const slugFormatError = validateProcessSlug(slug);
        if (slugFormatError != null || slug == null) {
            return false;
        }

        setIsCheckingSlugAvailability(true);
        try {
            const isAvailable = await new ProcessDefinitionApiService()
                .checkSlugAvailability(slug);

            const nextAvailabilityError = isAvailable ? undefined : 'Dieser URL-Namespace ist bereits vergeben.';
            setSlugAvailabilityError(nextAvailabilityError);
            return isAvailable;
        } catch (error) {
            setSlugAvailabilityError('Die Verfügbarkeit des URL-Namespace konnte nicht geprüft werden.');
            dispatch(showApiErrorSnackbar(error, 'Die Verfügbarkeit des URL-Namespace konnte nicht geprüft werden.'));
            return false;
        } finally {
            setIsCheckingSlugAvailability(false);
        }
    };

    const validateProcessConfiguration = async (): Promise<boolean> => {
        const nextNameError = validateName(nameOverride);
        const nextPublicTitleError = validatePublicTitle(publicTitleOverride);
        const nextSlugError = validateProcessSlug(slugOverride);
        const nextDepartmentError = validateDepartment(departmentOverride);

        setNameError(nextNameError);
        setPublicTitleError(nextPublicTitleError);
        setSlugError(nextSlugError);
        setDepartmentError(nextDepartmentError);

        if (nextNameError != null || nextPublicTitleError != null || nextSlugError != null || nextDepartmentError != null) {
            return false;
        }

        return checkSlugAvailability(slugOverride);
    };

    useEffect(() => {
        const slugFormatError = validateProcessSlug(slugOverride);
        setSlugAvailabilityError(undefined);

        if (slugOverride == null || slugFormatError != null) {
            setIsCheckingSlugAvailability(false);
            return;
        }

        let cancelled = false;
        setIsCheckingSlugAvailability(true);

        const timeoutId = window.setTimeout(() => {
            new ProcessDefinitionApiService()
                .checkSlugAvailability(slugOverride)
                .then((isAvailable) => {
                    if (!cancelled) {
                        setSlugAvailabilityError(isAvailable ? undefined : 'Dieser URL-Namespace ist bereits vergeben.');
                    }
                })
                .catch((error) => {
                    if (!cancelled) {
                        setSlugAvailabilityError('Die Verfügbarkeit des URL-Namespace konnte nicht geprüft werden.');
                        dispatch(showApiErrorSnackbar(error, 'Die Verfügbarkeit des URL-Namespace konnte nicht geprüft werden.'));
                    }
                })
                .finally(() => {
                    if (!cancelled) {
                        setIsCheckingSlugAvailability(false);
                    }
                });
        }, 500);

        return () => {
            cancelled = true;
            window.clearTimeout(timeoutId);
        };
    }, [dispatch, slugOverride]);

    const handleSelectStartPoint = (templateData: ProcessExport, startPoint: SelectedStartPoint): void => {
        const defaultPublicTitle = startPoint.type === 'empty' || !isStringNotNullOrEmpty(templateData.version.publicTitle) ? null : templateData.version.publicTitle;

        setSelectedTemplateData(templateData);
        setSelectedStartPoint(startPoint);
        setPublicTitleOverride(defaultPublicTitle);
        setPublicTitleManuallyEdited(isStringNotNullOrEmpty(defaultPublicTitle));
        setNameError(undefined);
        setPublicTitleError(undefined);
        setSlugError(undefined);
        setSlugAvailabilityError(undefined);
        setSlugManuallyEdited(false);
        setDepartmentError(undefined);
        setActiveStep(1);
    };

    const resetDialogState = (): void => {
        setActiveStep(0);
        setSelectedTemplateData(null);
        setSelectedStartPoint(null);
        setNameOverride(null);
        setPublicTitleOverride(null);
        setPublicTitleManuallyEdited(false);
        setSlugOverride(null);
        setSlugManuallyEdited(false);
        setDepartmentOverride(null);
        setNameError(undefined);
        setPublicTitleError(undefined);
        setSlugError(undefined);
        setSlugAvailabilityError(undefined);
        setDepartmentError(undefined);
    };

    const handleClose = (): void => {
        onCancel();
    };

    const handleImport = (): void => {
        uploadObjectFile<ProcessExport>('application/json')
            .then((importedProcessExport) => {
                if (importedProcessExport == null) {
                    return;
                }
                handleSelectStartPoint(importedProcessExport, {
                    label: 'Importierte Datei',
                    type: 'import',
                });
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht importiert werden. Bitte versuchen Sie es erneut.'));
            });
    };

    const handleLoadTemplate = (template: TemplateRegistryProcessItem): void => {
        new ProcessTemplatesService()
            .loadTemplate(template)
            .then((templateData) => {
                if (templateData == null) {
                    return;
                }

                handleSelectStartPoint(templateData, {
                    label: template.name,
                    type: 'template',
                });
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Vorlage konnte nicht geladen werden. Bitte versuchen Sie es später erneut.'));
            });
    };

    const handleSave = async (): Promise<void> => {
        if (selectedTemplateData == null) {
            return;
        }

        if (!await validateProcessConfiguration()) {
            setActiveStep(1);
            return;
        }

        if (nameOverride == null || publicTitleOverride == null || slugOverride == null || departmentOverride == null) {
            return;
        }

        const processName = nameOverride.trim();
        const publicTitle = publicTitleOverride.trim();
        const processSlug = slugOverride.trim();

        setIsLoading(true);

        const data: ProcessExport = {
            ...selectedTemplateData,
            process: {
                ...selectedTemplateData.process,
                internalTitle: processName,
                departmentId: departmentOverride,
                slug: processSlug,
            },
            version: {
                ...selectedTemplateData.version,
                publicTitle,
            },
        };

        new ProcessDefinitionApiService()
            .import(data)
            .then(async (createdProcess) => {
                try {
                    // Newly created processes may receive permissions through database views immediately.
                    await refreshPermissionSet({broadcast: true});
                } catch (err) {
                    dispatch(showApiErrorSnackbar(err, 'Die Berechtigungen konnten nach dem Erstellen des Prozesses nicht aktualisiert werden.'));
                }

                setIsLoading(false);
                handleClose();

                setTimeout(() => {
                    navigate(`/processes/${createdProcess.id}/versions/1/`, {
                        state: data.nodes.length > 0 ? SHOW_ERRORS_ROUTER_STATE : undefined,
                    });
                }, 1);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Prozess konnte nicht erstellt werden, da der Datensatz fehlerhaft ist. Bitte probieren Sie eine andere Datei.'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    return (
        <>
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="md"
                TransitionProps={{
                    onExited: resetDialogState,
                }}
            >
                <DialogTitleWithClose onClose={handleClose}>
                    Neuen Prozess anlegen
                </DialogTitleWithClose>

                <DialogContent
                    sx={{
                        minHeight: 'min(620px, 74vh)',
                        display: 'flex',
                        flexDirection: 'column',
                        p: 0,
                    }}
                >
                    <Box
                        sx={{
                            px: 3,
                            py: 1.75,
                            borderTop: '1px solid',
                            borderBottom: '1px solid',
                            borderColor: 'divider',
                            bgcolor: 'rgba(15, 23, 42, 0.025)',
                        }}
                    >
                        <Stepper
                            orientation="horizontal"
                            activeStep={activeStep}
                            sx={{
                                '& .MuiStepConnector-root': {
                                    display: 'block',
                                    mx: 1.5,
                                },
                                '& .MuiStepConnector-line': {
                                    borderColor: 'divider',
                                },
                                '& .MuiStepLabel-root': {
                                    p: 0,
                                },
                                '& .MuiStepLabel-iconContainer': {
                                    p: 0,
                                },
                                '& .MuiStepLabel-label': {
                                    mt: 0,
                                    ml: 1,
                                    pt: 0,
                                    fontSize: '0.95rem',
                                    fontWeight: 500,
                                    color: 'text.secondary',
                                },
                                '& .MuiStepLabel-label.Mui-active': {
                                    color: 'primary.main',
                                    fontWeight: 700,
                                },
                                '& .MuiStepLabel-label.Mui-completed': {
                                    color: 'primary.main',
                                    fontWeight: 500,
                                },
                                '& .MuiStepIcon-root': {
                                    fontSize: 30,
                                },
                            }}
                        >
                            <Step completed={activeStep > 0 && selectedTemplateData != null}>
                                <StepLabel StepIconComponent={OutlinedStepIcon}>
                                    Startpunkt
                                </StepLabel>
                            </Step>
                            <Step completed={activeStep > 1}>
                                <StepLabel StepIconComponent={OutlinedStepIcon}>
                                    Angaben
                                </StepLabel>
                            </Step>
                            <Step>
                                <StepLabel StepIconComponent={OutlinedStepIcon}>
                                    Prüfen
                                </StepLabel>
                            </Step>
                        </Stepper>
                    </Box>

                <Box
                    sx={{
                        p: 3,
                        display: 'flex',
                        flexDirection: 'column',
                        flex: 1,
                        minHeight: 0,
                        overflowY: 'auto',
                    }}
                >
                    {
                        activeStep === 0 &&
                        <Box>
                            <Typography variant="subtitle1" fontWeight={700}>
                                Startpunkt wählen
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>
                                Wählen Sie, ob Sie leer beginnen, eine Datei importieren oder eine Vorlage übernehmen.
                            </Typography>

                            <ProcessTemplateSection>
                                <Grid
                                    container
                                    spacing={2}
                                >
                                    <ProcessTemplateCard
                                        Icon={AddBox}
                                        title="Leerer Prozess"
                                        description="Ein frischer Prozess ohne vordefinierte Schritte oder Logik."
                                        category="action"
                                        onClick={() => {
                                            handleSelectStartPoint(EmptyProcess, {
                                                label: 'Leerer Prozess',
                                                type: 'empty',
                                            });
                                        }}
                                    />

                                    <ProcessTemplateCard
                                        Icon={UploadFile}
                                        title="Prozess importieren (JSON)"
                                        description="Einen bestehenden Prozess aus einer Exportdatei importieren."
                                        category="action"
                                        onClick={handleImport}
                                        sx={{
                                            borderStyle: 'dashed',
                                        }}
                                    />
                                </Grid>
                            </ProcessTemplateSection>

                            <Divider sx={{my: 3}}/>

                            <ProcessTemplateSection title="Prozessvorlagen">
                                <Grid
                                    container
                                    spacing={2}
                                >
                                    {
                                        templates != null &&
                                        templates.map((preset) => (
                                            <ProcessTemplateCard
                                                key={preset.path}
                                                Icon={Flowsheet}
                                                title={preset.name}
                                                description={preset.description}
                                                category="template"
                                                onClick={() => {
                                                    handleLoadTemplate(preset);
                                                }}
                                            />
                                        ))
                                    }
                                </Grid>
                                {
                                    templates != null && templates.length === 0 &&
                                    <AlertComponent
                                        color="info"
                                        sx={{
                                            mt: 2,
                                        }}
                                    >
                                        Aktuell stehen keine Prozessvorlagen zur Verfügung.
                                    </AlertComponent>
                                }
                            </ProcessTemplateSection>
                        </Box>
                    }
                    {
                        activeStep === 1 &&
                        <Box
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                flex: 1,
                            }}
                        >
                            <Box>
                                <Typography variant="subtitle1" fontWeight={700}>
                                    Angaben zum Prozess
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{mt: 0.5, mb: 2}}>
                                    Diese Angaben werden beim Anlegen übernommen und können später bearbeitet werden.
                                </Typography>

                                <ProcessConfigurationErrorAlert show={hasProcessConfigurationErrors}/>

                                <TextFieldComponent
                                    label="Name des Prozesses (intern)"
                                    value={nameOverride}
                                    onChange={(val) => {
                                        const nextName = val ?? null;
                                        setNameOverride(nextName);
                                        setNameError(undefined);

                                        if (!publicTitleManuallyEdited) {
                                            setPublicTitleOverride(nextName);
                                            setPublicTitleError(undefined);
                                        }

                                        if (!slugManuallyEdited) {
                                            // Keep the URL namespace in sync until the user enters a custom value.
                                            setSlugOverride(normalizeProcessSlugInput(nextName));
                                            setSlugError(undefined);
                                            setSlugAvailabilityError(undefined);
                                        }
                                    }}
                                    required={true}
                                    disabled={isLoading}
                                    error={nameError}
                                    minCharacters={3}
                                    maxCharacters={96}
                                    hint="Nur intern sichtbar; dient zur Wiedererkennung des Prozesses in der Verwaltung."
                                />

                                <TextFieldComponent
                                    label="Öffentliche Bezeichnung"
                                    value={publicTitleOverride}
                                    onChange={(val) => {
                                        const nextPublicTitle = val ?? null;
                                        setPublicTitleOverride(nextPublicTitle);
                                        setPublicTitleManuallyEdited(nextPublicTitle != null && nextPublicTitle.length > 0);
                                        setPublicTitleError(undefined);
                                    }}
                                    required={true}
                                    disabled={isLoading}
                                    error={publicTitleError}
                                    minCharacters={3}
                                    maxCharacters={96}
                                    hint="Wird öffentlich im Kontext der Prozessversion verwendet (z. B. im Self-Service-Portal)."
                                    sx={{
                                        mt: 2,
                                    }}
                                />

                                <TextFieldComponent
                                    label="URL-Namespace des Prozesses"
                                    value={slugOverride}
                                    onChange={(val) => {
                                        const nextSlug = normalizeProcessSlugInput(val);
                                        setSlugOverride(nextSlug);
                                        setSlugManuallyEdited(nextSlug != null && nextSlug.length > 0);
                                        setSlugError(undefined);
                                        setSlugAvailabilityError(undefined);
                                    }}
                                    required={true}
                                    disabled={isLoading}
                                    error={slugError ?? slugAvailabilityError}
                                    minCharacters={3}
                                    maxCharacters={PROCESS_SLUG_MAX_LENGTH}
                                    hint="Der Namespace steht in öffentlichen URLs zwischen Elementtyp und Element-Slug, z. B. /form/hundesteuer/antrag."
                                    pattern={{
                                        regex: '^[a-z0-9-]+$',
                                        message: 'Der URL-Namespace darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.',
                                    }}
                                    muiPassTroughProps={{
                                        InputProps: {
                                            startAdornment: (
                                                <InputAdornment position="start" sx={{whiteSpace: 'nowrap', flexShrink: 0}}>
                                                    <Box component="span" sx={{whiteSpace: 'nowrap'}}>
                                                        /element-typ/
                                                    </Box>
                                                </InputAdornment>
                                            ),
                                            endAdornment: (
                                                <InputAdornment position="end" sx={{whiteSpace: 'nowrap', flexShrink: 0}}>
                                                    <Box
                                                        component="span"
                                                        sx={{
                                                            display: 'inline-flex',
                                                            alignItems: 'center',
                                                            gap: 0.75,
                                                            whiteSpace: 'nowrap',
                                                        }}
                                                    >
                                                        {
                                                            isCheckingSlugAvailability &&
                                                            <CircularProgress size={16} color="inherit"/>
                                                        }
                                                        /element-slug
                                                    </Box>
                                                </InputAdornment>
                                            ),
                                        },
                                    }}
                                    sx={{
                                        mt: 2,
                                    }}
                                />

                                <DepartmentSelectField
                                    label="Verwaltende Organisationseinheit"
                                    value={selectedDepartment}
                                    departments={availableDepartments}
                                    dialogTitle="Organisationseinheit auswählen"
                                    onChange={(department) => {
                                        setDepartmentOverride(department?.id ?? null);
                                        setDepartmentError(undefined);
                                    }}
                                    disabled={isLoading || availableDepartments == null}
                                    error={departmentError}
                                    required={true}
                                    hint="Die ausgewählte Einheit wird als verantwortlich für diesen Prozess festgelegt und kann z. B. Berechtigungen verwalten."
                                />
                            </Box>

                            <Box
                                sx={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    marginTop: 'auto',
                                }}
                            >

                                <Button
                                    onClick={() => {
                                        setActiveStep(0);
                                    }}
                                    startIcon={<ArrowBack/>}
                                    disabled={preselectedTemplate != null}
                                >
                                    Zurück
                                </Button>

                                <Button
                                    onClick={() => {
                                        void validateProcessConfiguration().then((isValid) => {
                                            if (isValid) {
                                                setActiveStep(2);
                                            }
                                        });
                                    }}
                                    disabled={isLoading || isCheckingSlugAvailability}
                                    endIcon={<ArrowForward/>}
                                    variant="contained"
                                >
                                    Weiter
                                </Button>
                            </Box>
                        </Box>
                    }
                    {
                        activeStep === 2 &&
                        <Box
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                flex: 1,
                            }}
                        >
                            <Typography variant="subtitle1" fontWeight={700}>
                                Angaben prüfen
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>
                                Klicken Sie auf {quoteString('Anlegen und Bearbeiten')}, um den neuen Prozess anzulegen.
                            </Typography>

                            <StatusTable
                                sx={{mt: 3}}
                                cardVariant="outlined"
                                dense
                                cardSx={{
                                    borderRadius: 1,
                                    boxShadow: 'none',
                                    '& .MuiTableCell-root': {
                                        py: 1.5,
                                    },
                                }}
                                items={summaryItems}
                            />

                            <Box
                                sx={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    marginTop: 'auto',
                                }}
                            >
                                <Button
                                    onClick={() => {
                                        setActiveStep(1);
                                    }}
                                    startIcon={<ArrowBack/>}
                                >
                                    Zurück
                                </Button>

                                <Button
                                    onClick={() => {
                                        void handleSave();
                                    }}
                                    endIcon={<Save/>}
                                    disabled={isLoading || isCheckingSlugAvailability}
                                    variant="contained"
                                >
                                    Anlegen und Bearbeiten
                                </Button>
                            </Box>
                        </Box>
                    }
                </Box>
                </DialogContent>
            </Dialog>

        </>
    );
}

interface ProcessTemplateCardProps {
    Icon: FC<SvgIconProps>;
    title: string;
    description: string;
    onClick: () => void;
    category: 'action' | 'template';
    sx?: SxProps;
}

function ProcessTemplateCard(props: ProcessTemplateCardProps): ReactNode {
    const {
        Icon,
        title,
        description,
        onClick,
        category,
        sx,
    } = props;
    const isAction = category === 'action';

    return (
        <Grid
            size={{
                xs: 12,
                sm: 6,
                lg: 6,
            }}
        >
            <Card
                variant="outlined"
                sx={{
                    height: '100%',
                    borderRadius: 1,
                    borderColor: isAction ? 'primary.light' : 'divider',
                    bgcolor: isAction ? 'rgba(25, 118, 210, 0.025)' : 'background.paper',
                    transition: 'border-color 120ms ease, box-shadow 120ms ease, background-color 120ms ease',
                    '&:hover': {
                        borderColor: 'primary.main',
                        boxShadow: 1,
                    },
                    ...sx,
                }}
            >
                <CardActionArea
                    onClick={onClick}
                    sx={{
                        height: '100%',
                        minHeight: 136,
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: 1.5,
                        p: 2,
                        textAlign: 'left',
                    }}
                >
                    <Box
                        sx={{
                            width: 42,
                            height: 42,
                            borderRadius: '50%',
                            bgcolor: isAction ? 'primary.main' : 'grey.100',
                            color: isAction ? 'primary.contrastText' : 'text.secondary',
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                        }}
                    >
                        <Icon sx={{fontSize: 24}}/>
                    </Box>

                    <Box
                        sx={{
                            minWidth: 0,
                            flex: 1,
                        }}
                    >
                        <Typography
                            variant="subtitle1"
                            fontWeight={700}
                            sx={{
                                lineHeight: 1.25,
                            }}
                        >
                            {title}
                        </Typography>

                        <Typography
                            variant="body2"
                            color="text.secondary"
                            title={description}
                            sx={{
                                mt: 0.75,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                display: '-webkit-box',
                                WebkitLineClamp: '4',
                                WebkitBoxOrient: 'vertical',
                            }}
                        >
                            {description}
                        </Typography>
                    </Box>
                </CardActionArea>
            </Card>
        </Grid>
    );
}

interface ProcessTemplateSectionProps {
    title?: string;
    children: ReactNode;
}

function ProcessTemplateSection(props: ProcessTemplateSectionProps): ReactNode {
    return (
        <Box sx={{mt: 2.5}}>
            {
                props.title != null &&
                <Typography
                    variant="subtitle2"
                    sx={{
                        mb: 1.25,
                        fontWeight: 700,
                    }}
                >
                    {props.title}
                </Typography>
            }

            {props.children}
        </Box>
    );
}

interface ProcessConfigurationErrorAlertProps {
    show: boolean;
}

function ProcessConfigurationErrorAlert(props: ProcessConfigurationErrorAlertProps): ReactNode {
    const {
        show,
    } = props;

    if (!show) {
        return null;
    }

    return (
        <AlertComponent
            title="Dieser Schritt enthält fehlerhafte oder fehlende Angaben"
            color="error"
            sx={{
                mt: 2,
                mb: 1,
            }}
        >
            <Typography>
                Bitte korrigieren Sie die markierten Angaben und klicken Sie erneut auf Weiter.
            </Typography>
        </AlertComponent>
    );
}

function SummaryIcon(props: {children?: ReactNode}): ReactNode {
    return (
        <Box
            sx={{
                width: 34,
                height: 34,
                borderRadius: '50%',
                bgcolor: 'grey.100',
                color: 'text.secondary',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                '& svg': {
                    fontSize: 19,
                    color: 'text.secondary',
                },
            }}
        >
            {props.children}
        </Box>
    );
}

function OutlinedStepIcon(props: StepIconProps): ReactNode {
    const isHighlighted = props.active || props.completed;
    const content = props.completed ? <Check sx={{fontSize: 18}}/> : props.icon;
    const backgroundColor = props.completed ? 'primary.main' : 'background.paper';
    const color = props.completed ? 'primary.contrastText' : isHighlighted ? 'primary.main' : 'text.secondary';

    return (
        <Box
            sx={{
                width: 30,
                height: 30,
                borderRadius: '50%',
                border: '2px solid',
                borderColor: isHighlighted ? 'primary.main' : 'text.disabled',
                bgcolor: backgroundColor,
                color: color,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '0.875rem',
                fontWeight: 800,
                lineHeight: 1,
                '& svg': {
                    fontSize: 18,
                    color: 'inherit',
                },
            }}
        >
            {content}
        </Box>
    );
}

function getStartPointIcon(type: StartPointType | undefined): ReactNode {
    switch (type) {
        case 'empty':
            return <AddBox sx={{fontSize: 20, color: 'text.secondary'}}/>;
        case 'import':
            return <UploadFile sx={{fontSize: 20, color: 'text.secondary'}}/>;
        case 'template':
            return <Flowsheet sx={{fontSize: 20, color: 'text.secondary'}}/>;
        default:
            return undefined;
    }
}

const EmptyProcess: ProcessExport = {
    appBuildNumber: '',
    appVersion: '',
    createdByVendor: '',
    edges: [],
    exportTimestamp: '',
    nodes: [],
    process: {
        id: 0,
        internalTitle: 'Neuer Prozess',
        departmentId: 0,
        accessKey: '',
        slug: '',
        versionCount: 0,
        draftedVersion: null,
        publishedVersion: null,
        created: '',
        updated: '',
    },
    version: {
        processId: 0,
        processVersion: 0,
        status: ProcessStatus.Drafted,
        publicTitle: 'Neuer Prozess',
        caseNumberTemplate: null,
        crated: '',
        updated: '',
        published: null,
        revoked: null,
    },
};
