import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import {type ProcessEntity} from '../entities/process-entity';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import Stepper from '@mui/material/Stepper';
import {Box, Button, Grid, Step, StepLabel, type SvgIconProps, type SxProps, Tooltip} from '@mui/material';
import React, {type FC, type ReactNode, useEffect, useState} from 'react';
import Typography from '@mui/material/Typography';
import UploadFile from '@aivot/mui-material-symbols-400-outlined/dist/upload-file/UploadFile';
import {uploadObjectFile} from '../../../utils/download-utils';
import {type ProcessExport} from '../entities/process-export';
import {
    VDepartmentMembershipWithDetailsService,
} from '../../departments/services/v-department-membership-with-details-service';
import {
    SelectFieldComponent,
    type SelectFieldComponentOption,
} from '../../../components/select-field-2/select-field-component';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../departments/utils/department-utils';
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
import {ProcessTemplatesService, TemplateRegistryProcessItem} from '../services/process-templates-service';
import GridGuides from '@aivot/mui-material-symbols-400-outlined/dist/grid-guides/GridGuides';
import {useNavigate} from 'react-router-dom';
import {SHOW_ERRORS_ROUTER_STATE} from '../pages/details/process-details-page';

interface NewProcessDialogProps {
    open: boolean;
    onCancel: () => void;
}

export function NewProcessDialog(props: NewProcessDialogProps): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        open,
        onCancel,
    } = props;

    const user = useAppSelector(selectUser);

    const [availableDepartments, setAvailableDepartments] = useState<Array<SelectFieldComponentOption<number>>>([]);
    const [nameOverride, setNameOverride] = useState<string | null>(null);
    const [departmentOverride, setDepartmentOverride] = useState<number | null>(null);
    const [nameError, setNameError] = useState<string | undefined>();
    const [departmentError, setDepartmentError] = useState<string | undefined>();

    const [isLoading, setIsLoading] = useState(false);

    const [templates, setTemplates] = useState<TemplateRegistryProcessItem[] | null>(null);

    useEffect(() => {
        if (user == null) {
            setAvailableDepartments([]);
            return;
        }

        new VDepartmentMembershipWithDetailsService()
            .listAll({
                userId: user.id,
            })
            .then(({content}) => {
                const options: Array<SelectFieldComponentOption<number>> = content
                    .map((membership) => ({
                        value: membership.departmentId,
                        label: membership.departmentName,
                        icon: getDepartmentTypeIcons(membership.departmentDepth),
                        subLabel: getDepartmentTypeLabel(membership.departmentDepth),
                    }));
                setAvailableDepartments(options);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Organisationseinheiten konnten nicht geladen werden. Bitte versuchen Sie es erneut.'));
            });
    }, [user]);

    useEffect(() => {
        new ProcessTemplatesService()
            .getProcessTemplates()
            .then(setTemplates)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Vorlagen konnten nicht geladen werden. Bitte versuchen Sie es später erneut.'));
            });
    }, []);

    const [activeStep, setActiveStep] = useState(0);
    const [selectedTemplateData, setSelectedTemplateData] = useState<ProcessExport | null>(null);

    const validateName = (value: string | null): string | undefined => {
        const trimmedValue = value?.trim() ?? '';

        if (!isStringNotNullOrEmpty(trimmedValue)) {
            return 'Bitte geben Sie einen Namen für das Verfahren an.';
        }

        if (trimmedValue.length < 3) {
            return 'Der Name des Verfahrens muss mindestens 3 Zeichen lang sein.';
        }

        if (trimmedValue.length > 96) {
            return 'Der Name des Verfahrens darf maximal 96 Zeichen lang sein.';
        }

        return undefined;
    };

    const validateDepartment = (value: number | null): string | undefined => {
        if (value == null) {
            return 'Bitte wählen Sie eine Organisationseinheit aus.';
        }

        return undefined;
    };

    const validateProcessConfiguration = (): boolean => {
        const nextNameError = validateName(nameOverride);
        const nextDepartmentError = validateDepartment(departmentOverride);

        setNameError(nextNameError);
        setDepartmentError(nextDepartmentError);

        return nextNameError == null && nextDepartmentError == null;
    };

    const handleClose = (): void => {
        onCancel();
        setTimeout(() => {
            setActiveStep(0);
            setNameOverride(null);
            setDepartmentOverride(null);
            setNameError(undefined);
            setDepartmentError(undefined);
        }, 300);
    };

    const handleImport = (): void => {
        uploadObjectFile<ProcessExport>('application/json')
            .then((importedProcessExport) => {
                if (importedProcessExport == null) {
                    return;
                }
                setSelectedTemplateData(importedProcessExport);
                setActiveStep(1);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die Datei konnte nicht importiert werden. Bitte versuchen Sie es erneut.'));
            });
    };

    const handleSave = (): void => {
        if (selectedTemplateData == null) {
            return;
        }

        if (!validateProcessConfiguration()) {
            setActiveStep(1);
            return;
        }

        if (nameOverride == null || departmentOverride == null) {
            return;
        }

        setIsLoading(true);

        const data: ProcessExport = {
            ...selectedTemplateData,
            process: {
                ...selectedTemplateData.process,
                internalTitle: nameOverride,
                departmentId: departmentOverride,
            },
        };

        new ProcessDefinitionApiService()
            .import(data)
            .then((createdProcess) => {
                setIsLoading(false);
                handleClose();

                setTimeout(() => {
                    navigate(`/processes/${createdProcess.id}/versions/1/`, {
                        state: SHOW_ERRORS_ROUTER_STATE,
                    });
                }, 1);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Das Verfahren konnte nicht erstellt werden, da der Datensatz fehlerhaft ist. Bitte probieren Sie eine andere Datei.'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitleWithClose onClose={handleClose}>
                Neues Verfahren erstellen
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    minHeight: '50vh',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Stepper
                    orientation="horizontal"
                    activeStep={activeStep}
                    sx={{
                        justifyContent: 'space-between',
                        '& .MuiStepLabel-label' : {
                            marginTop: 0,
                            marginLeft: 0,
                        },
                        '& .MuiStepConnector-root': {
                            display: 'block',
                            marginLeft: '0.5rem',
                            marginRight: '0.5rem',
                        },
                    }}
                >
                    <Step
                        completed={selectedTemplateData != null}
                    >
                        <StepLabel>
                            Vorlage
                        </StepLabel>
                    </Step>
                    <Step completed={validateName(nameOverride) == null && validateDepartment(departmentOverride) == null}>
                        <StepLabel>
                            Anpassung
                        </StepLabel>
                    </Step>
                    <Step>
                        <StepLabel>
                            Abschluss
                        </StepLabel>
                    </Step>
                </Stepper>

                <Box
                    sx={{
                        pt: 4,
                        display: 'flex',
                        flexDirection: 'column',
                        flex: 1,
                    }}
                >
                    {
                        activeStep === 0 &&
                        <>
                            <Typography>
                                Wählen Sie eine Vorlage für das neue Verfahren aus:
                            </Typography>

                            <Grid
                                container
                                spacing={2}
                                sx={{
                                    marginTop: 2,
                                }}
                            >
                                <ProcessTemplateCard
                                    Icon={AddBox}
                                    title="Leeres Verfahren"
                                    description="Ein leeres Verfahren ohne vordefinierte Schritte oder Logik."
                                    onClick={() => {
                                        setSelectedTemplateData(EmptyProcess);
                                        setActiveStep(1);
                                    }}
                                />

                                <ProcessTemplateCard
                                    Icon={UploadFile}
                                    title="Importieren"
                                    description="Importieren Sie ein Verfahren aus einer Datei."
                                    onClick={handleImport}
                                    sx={{
                                        borderStyle: 'dashed',
                                    }}
                                />

                                {
                                    templates != null &&
                                    templates.map((preset) => (
                                        <ProcessTemplateCard
                                            key={preset.path}
                                            Icon={GridGuides}
                                            title={preset.name}
                                            description={preset.description}
                                            onClick={() => {
                                                new ProcessTemplatesService()
                                                    .loadTemplate(preset)
                                                    .then((templateData) => {
                                                        if (templateData == null) {
                                                            return;
                                                        }
                                                        setSelectedTemplateData(templateData);
                                                        setActiveStep(1);
                                                    });
                                            }}
                                        />
                                    ))
                                }
                            </Grid>
                        </>
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
                            <TextFieldComponent
                                label="Name des Verfahrens"
                                value={nameOverride}
                                onChange={(val) => {
                                    const nextValue = val ?? null;
                                    setNameOverride(nextValue);

                                    if (nameError != null) {
                                        setNameError(validateName(nextValue));
                                    }
                                }}
                                onBlur={(val) => {
                                    const nextValue = val ?? null;
                                    setNameOverride(nextValue);
                                    setNameError(validateName(nextValue));
                                }}
                                required={true}
                                disabled={isLoading}
                                error={nameError}
                                minCharacters={3}
                                maxCharacters={96}
                            />

                            <SelectFieldComponent
                                label="Organisationseinheit"
                                value={departmentOverride}
                                onChange={(newValue) => {
                                    const nextValue = newValue ?? null;
                                    setDepartmentOverride(nextValue);

                                    if (departmentError != null || nextValue == null) {
                                        setDepartmentError(validateDepartment(nextValue));
                                    }
                                }}
                                options={availableDepartments}
                                required={true}
                                disabled={isLoading}
                                error={departmentError}
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
                                        setActiveStep(0);
                                    }}
                                    startIcon={<ArrowBack/>}
                                >
                                    Zurück
                                </Button>

                                <Button
                                    onClick={() => {
                                        if (!validateProcessConfiguration()) {
                                            return;
                                        }

                                        setActiveStep(2);
                                    }}
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
                            <Typography>
                                Klicken Sie auf {quoteString('Anlegen und Bearbeiten')}, um das neue Verfahren anzulegen.
                            </Typography>

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
                                        handleSave();
                                    }}
                                    endIcon={<Save/>}
                                    disabled={isLoading}
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
    );
}

interface ProcessTemplateCardProps {
    Icon: FC<SvgIconProps>;
    title: string;
    description: string;
    onClick: () => void;
    sx?: SxProps;
}

function ProcessTemplateCard(props: ProcessTemplateCardProps): ReactNode {
    const {
        Icon,
        title,
        description,
        onClick,
        sx,
    } = props;

    return (
        <Grid
            size={{
                xs: 12,
                md: 6,
                lg: 4,
            }}
        >
            <Button
                variant="outlined"
                fullWidth
                sx={{
                    ...sx,
                    display: 'flex',
                    flexDirection: 'column',
                    px: 1,
                    py: 2,
                    height: '12em',
                    width: '100%',
                }}
                onClick={onClick}
            >
                <Icon
                    fontSize="large"
                    sx={{
                        mb: 2,
                    }}
                />

                <Typography
                    variant="h6"
                    sx={{
                        marginTop: 0.5,
                        marginBottom: 0.25,
                    }}
                >
                    {title}
                </Typography>

                <Tooltip title={description}>
                    <Typography
                        variant="body2"
                        color="textSecondary"
                        sx={{
                            mb: 'auto',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            display: '-webkit-box',
                            WebkitLineClamp: '2',
                            WebkitBoxOrient: 'vertical',
                        }}
                    >
                        {description}
                    </Typography>
                </Tooltip>
            </Button>
        </Grid>
    );
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
        internalTitle: 'Neues Verfahren',
        departmentId: 0,
        accessKey: '',
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
        publicTitle: 'Neues Verfahren',
        crated: '',
        updated: '',
        published: null,
        revoked: null,
    },
};