import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import {type ProcessEntity} from '../entities/process-entity';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import Stepper from '@mui/material/Stepper';
import {Box, Button, Grid, Step, StepLabel, type SvgIconProps, type SxProps} from '@mui/material';
import React, {type FC, type ReactNode, useEffect, useState} from 'react';
import Typography from '@mui/material/Typography';
import UploadFile from '@aivot/mui-material-symbols-400-outlined/dist/upload-file/UploadFile';
import {ProcessTemplates} from '../data/templates';
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
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import ArrowBack from '@aivot/mui-material-symbols-400-outlined/dist/arrow-back/ArrowBack';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';
import {isStringNotNullOrEmpty, quoteString} from '../../../utils/string-utils';
import Save from '@aivot/mui-material-symbols-400-outlined/dist/save/Save';
import {ProcessDefinitionApiService} from '../services/process-definition-api-service';

interface NewProcessDialogProps {
    open: boolean;
    onNew: (process: ProcessEntity) => void;
    onCancel: () => void;
}

export function NewProcessDialog(props: NewProcessDialogProps): ReactNode {
    const dispatch = useAppDispatch();

    const {
        open,
        onNew,
        onCancel,
    } = props;

    const user = useAppSelector(selectUser);

    const [availableDepartments, setAvailableDepartments] = useState<Array<SelectFieldComponentOption<number>>>([]);
    const [nameOverride, setNameOverride] = useState<string | null>(null);
    const [departmentOverride, setDepartmentOverride] = useState<number | null>(null);

    const [isLoading, setIsLoading] = useState(false);

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

    const [activeStep, setActiveStep] = useState(0);
    const [selectedTemplateData, setSelectedTemplateData] = useState<ProcessExport | null>(null);

    const handleClose = (): void => {
        onCancel();
        setTimeout(() => {
            setActiveStep(0);
            setNameOverride(null);
            setDepartmentOverride(null);
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
                onNew(createdProcess);
                handleClose();
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Das Verfahren konnte nicht erstellt werden. Bitte versuchen Sie es erneut.'));
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
            maxWidth="lg"
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
                    }}
                >
                    <Step
                        completed={selectedTemplateData != null}
                    >
                        <StepLabel>
                            Vorlage
                        </StepLabel>
                    </Step>
                    <Step completed={isStringNotNullOrEmpty(nameOverride) && departmentOverride != null}>
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
                                {
                                    ProcessTemplates
                                        .map((preset, index) => (
                                            <ProcessTemplateCard
                                                key={index}
                                                Icon={preset.Icon}
                                                title={preset.name}
                                                description={preset.description}
                                                onClick={() => {
                                                    setSelectedTemplateData(preset.data);
                                                    setActiveStep(1);
                                                }}
                                            />
                                        ))
                                }

                                <ProcessTemplateCard
                                    Icon={UploadFile}
                                    title="Importieren"
                                    description="Importieren Sie ein Verfahren aus einer Datei."
                                    onClick={handleImport}
                                    sx={{
                                        borderStyle: 'dashed',
                                    }}
                                />
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
                                    setNameOverride(val ?? null);
                                }}
                                required={true}
                            />

                            <SelectFieldComponent
                                label="Organisationseinheit"
                                value={departmentOverride}
                                onChange={(newValue) => {
                                    setDepartmentOverride(newValue ?? null);
                                }}
                                options={availableDepartments}
                                required={true}
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
                                        setActiveStep(2);
                                    }}
                                    endIcon={<ArrowForward/>}
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
                                Klicken Sie auf {quoteString('Erstellen')}, um das neue Verfahren anzulegen.
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
                                >
                                    Erstellen
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
                sm: 6,
                md: 4,
                lg: 3,
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

                <Typography
                    variant="body2"
                    color="textSecondary"
                    sx={{
                        mb: 'auto',
                    }}
                >
                    {description}
                </Typography>
            </Button>
        </Grid>
    );
}
