import React, {useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {EditDepartmentPageMembersTab} from './tabs/edit-department-page-members-tab';
import {type Department} from '../../../models/entities/department';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {EditDepartmentPageDevelopingFormsTab} from './tabs/edit-department-page-developing-forms-tab';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Box, Typography} from '@mui/material';
import {RichTextEditorComponentView} from '../../../components/richt-text-editor/rich-text-editor.component.view';
import {validateEmail} from '../../../utils/validate-email';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';
import {useApi} from '../../../hooks/use-api';
import {useDepartmentsApi} from '../../../hooks/use-departments-api';
import {useAdminMembershipGuard} from '../../../hooks/use-admin-membership-guard';
import {useFormsApi} from '../../../hooks/use-forms-api';
import {FormListProjection} from '../../../models/entities/form';
import {EditDepartmentPageManagingFormsTab} from './tabs/edit-department-page-managing-forms-tab';
import {EditDepartmentPageResponsibleFormsTab} from './tabs/edit-department-page-responsible-forms-tab';
import {AlertComponent} from '../../../components/alert/alert-component';

type Errors = {
    [key in keyof Department]?: string;
};

export function DepartmentEditPage(): JSX.Element {
    const api = useApi();
    const departmentId = useParams().id;

    useAdminMembershipGuard(parseInt(departmentId ?? ''));

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [originalDepartment, setOriginalDepartment] = useState<Department>();
    const [editedDepartment, setEditedDepartment] = useState<Department>();

    const [developedForms, setDevelopedForms] = useState<FormListProjection[]>();
    const [managedForms, setManagedForms] = useState<FormListProjection[]>();
    const [responsibleForms, setResponsibleForms] = useState<FormListProjection[]>();

    const [showNotDeletableDialog, setShowNotDeletableDialog] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const [isBusy, setIsBusy] = useState(true);
    const [is404, setIs404] = useState(false);

    const hasChanged = useChangeBlocker(originalDepartment, editedDepartment);

    const [errors, setErrors] = useState<Errors>({});

    useEffect(() => {
        setIsBusy(true);
        setIs404(false);
        if (departmentId == null || departmentId === 'new') {
            const newDepartment: Department = {
                id: 0,
                name: '',
                address: '',
                accessibility: '',
                imprint: '',
                privacy: '',
                specialSupportAddress: '',
                technicalSupportAddress: '',
                created: new Date().toISOString(),
                updated: new Date().toISOString(),
            };
            setOriginalDepartment(newDepartment);
            setEditedDepartment(newDepartment);
            setIsBusy(false);
        } else {
            useDepartmentsApi(api).retrieve(parseInt(departmentId))
                .then((department) => {
                    setOriginalDepartment(department);
                    setEditedDepartment(department);
                })
                .catch((err) => {
                    console.error(err);
                    setIs404(true);
                })
                .finally(() => {
                    setIsBusy(false);
                });

            useFormsApi(api)
                .list({department: parseInt(departmentId)})
                .then(setDevelopedForms)
                .catch((err) => {
                    console.error(err);
                    setDevelopedForms([]);
                    dispatch(showErrorSnackbar('Zugehörige entwickelte Formulare konnten nicht geladen werden'));
                });

            useFormsApi(api)
                .list({managing: parseInt(departmentId)})
                .then(setManagedForms)
                .catch((err) => {
                    console.error(err);
                    setManagedForms([]);
                    dispatch(showErrorSnackbar('Zugehörige verwaltete Formulare konnten nicht geladen werden'));
                });

            useFormsApi(api)
                .list({responsible: parseInt(departmentId)})
                .then(setResponsibleForms)
                .catch((err) => {
                    console.error(err);
                    setResponsibleForms([]);
                    dispatch(showErrorSnackbar('Zugehörige bewirtschaftete Formulare konnten nicht geladen werden'));
                });
        }
    }, [departmentId]);

    const handleChange = (patch: Partial<Department>): void => {
        if (editedDepartment != null) {
            setEditedDepartment({
                ...editedDepartment,
                ...patch,
            });
        }
    };

    const handleSave = (): void => {
        if (editedDepartment != null) {
            const errors: Errors = {};

            if (editedDepartment.name.length < 3) {
                errors.name = 'Bitte geben Sie mindestens 3 Zeichen ein';
            }

            if (editedDepartment.address.length === 0) {
                errors.address = 'Bitte geben Sie eine Adresse ein';
            }

            if (!validateEmail(editedDepartment.specialSupportAddress)) {
                errors.specialSupportAddress = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
            }

            if (!validateEmail(editedDepartment.technicalSupportAddress)) {
                errors.technicalSupportAddress = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
            }

            if (!editedDepartment.imprint || editedDepartment.imprint?.length === 0) {
                errors.imprint = 'Bitte geben Sie ein Impressum an';
            }

            if (!editedDepartment.privacy || editedDepartment.privacy?.length === 0) {
                errors.privacy = 'Bitte geben Sie eine Datenschutzerklärung an';
            }

            if (!editedDepartment.accessibility || editedDepartment.accessibility?.length === 0) {
                errors.accessibility = 'Bitte geben Sie eine Barrierefreiheitserklärung an';
            }

            if (editedDepartment.departmentMail != null && !editedDepartment.departmentMail.split(',').map(s => s.trim()).every(validateEmail)) {
                errors.departmentMail = 'Bitte geben Sie nur gültige E-Mail-Adressen ein und trennen Sie diese durch ein Komma';
            }

            if (Object.keys(errors).length > 0) {
                setErrors(errors);
                dispatch(showErrorSnackbar('Bitte beheben Sie die markierten Fehler zum Speichern.'));
            } else {
                setIsBusy(true);

                if (editedDepartment.id === 0) {
                    useDepartmentsApi(api)
                        .save(editedDepartment)
                        .then((createdDepartment) => {
                            setOriginalDepartment(createdDepartment);
                            setEditedDepartment(createdDepartment);
                            dispatch(showSuccessSnackbar('Fachbereich erfolgreich erstellt!'));
                        })
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fachbereich konnte nicht gespeichert werden!'));
                        })
                        .finally(() => {
                            setIsBusy(false);
                            setErrors({});
                        });
                } else {
                    useDepartmentsApi(api)
                        .save(editedDepartment)
                        .then((updatedDepartment) => {
                            setOriginalDepartment(updatedDepartment);
                            setEditedDepartment(updatedDepartment);
                            dispatch(showSuccessSnackbar('Fachbereich erfolgreich gespeichert!'));
                        })
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fachbereich konnte nicht gespeichert werden!'));
                        })
                        .finally(() => {
                            setIsBusy(false);
                            setErrors({});
                        });
                }
            }
        }
    };

    const handleReset = (): void => {
        if (originalDepartment != null) {
            setEditedDepartment(originalDepartment);
            setErrors({});
        }
    };

    const handleDelete = (): void => {
        setConfirmDelete(() => () => {
            if (editedDepartment != null && editedDepartment.id !== 0) {
                setIsBusy(true);

                useDepartmentsApi(api)
                    .destroy(editedDepartment.id)
                    .then(() => {
                        navigate('/departments');
                    })
                    .catch((err) => {
                        if (err.status === 409) {
                            setShowNotDeletableDialog(true);
                            setConfirmDelete(undefined);
                        } else {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fachbereich konnte nicht gelöscht werden!'));
                        }
                        setIsBusy(false);
                    });
            }
        });
    };

    return (
        <>
            <FormPageWrapper
                title="Fachbereich bearbeiten"
                isLoading={isBusy}
                is404={is404}

                hasChanged={hasChanged}
                onSave={handleSave}
                onReset={editedDepartment?.id !== 0 ? handleReset : undefined}
                onDelete={editedDepartment?.id !== 0 ? handleDelete : undefined}

                tabs={
                    editedDepartment != null &&
                    editedDepartment.id !== 0 ?
                        [
                            {
                                label: 'Mitarbeiter:innen',
                                content: (
                                    <EditDepartmentPageMembersTab
                                        department={editedDepartment}
                                    />
                                ),
                            },
                            {
                                label: 'Entwickelte Formulare',
                                content: (
                                    <EditDepartmentPageDevelopingFormsTab
                                        applications={developedForms ?? []}
                                    />
                                ),
                            },
                            {
                                label: 'Bewirtschaftet Formulare',
                                content: (
                                    <EditDepartmentPageManagingFormsTab
                                        applications={managedForms ?? []}
                                    />
                                ),
                            },
                            {
                                label: 'Zuständige Formulare',
                                content: (
                                    <EditDepartmentPageResponsibleFormsTab
                                        applications={responsibleForms ?? []}
                                    />
                                ),
                            },
                        ] :
                        undefined
                }
            >
                <TextFieldComponent
                    label="Name des Fachbereichs"
                    value={editedDepartment?.name}
                    onChange={(val) => {
                        handleChange({
                            name: val ?? '',
                        });
                    }}
                    required
                    maxCharacters={96}
                    minCharacters={3}
                    error={errors.name}
                />

                <TextFieldComponent
                    label="Adresse des Fachbereichs"
                    value={editedDepartment?.address}
                    onChange={(val) => {
                        handleChange({
                            address: val ?? '',
                        });
                    }}
                    required
                    maxCharacters={255}
                    multiline
                    rows={3}
                    error={errors.address}
                />

                <Typography
                    variant="h5"
                    sx={{
                        mt: 6,
                        mb: 2,
                    }}
                >
                    Kontakt-E-Mail-Adressen für antragstellende Personen
                </Typography>

                <TextFieldComponent
                    label="Kontakt-E-Mail-Adresse fachliche Unterstützung"
                    type="email"
                    value={editedDepartment?.specialSupportAddress}
                    onChange={(val) => {
                        handleChange({
                            specialSupportAddress: val ?? '',
                        });
                    }}
                    required
                    maxCharacters={255}
                    error={errors.specialSupportAddress}
                />

                <TextFieldComponent
                    label="Kontakt-E-Mail-Adresse technische Unterstützung"
                    type="email"
                    value={editedDepartment?.technicalSupportAddress}
                    onChange={(val) => {
                        handleChange({
                            technicalSupportAddress: val ?? '',
                        });
                    }}
                    required
                    maxCharacters={255}
                    error={errors.technicalSupportAddress}
                />

                <Typography
                    variant="h5"
                    sx={{
                        mt: 6,
                        mb: 2,
                    }}
                >
                    Rechtliche Informationen
                </Typography>

                <Box sx={{mb: 3}}>
                    <RichTextEditorComponentView
                        label="Impressum"
                        value={editedDepartment?.imprint ?? ''}
                        onChange={(val) => {
                            handleChange({
                                imprint: val,
                            });
                        }}
                        required
                        error={errors.imprint}
                    />
                </Box>

                <Box sx={{mb: 3}}>
                    <RichTextEditorComponentView
                        label="Datenschutzerklärung"
                        value={editedDepartment?.privacy ?? ''}
                        onChange={(val) => {
                            handleChange({
                                privacy: val,
                            });
                        }}
                        required
                        error={errors.privacy}
                    />
                </Box>

                <Box sx={{mb: 3}}>
                    <RichTextEditorComponentView
                        label="Barrierefreiheitserklärung"
                        value={editedDepartment?.accessibility ?? ''}
                        onChange={(val) => {
                            handleChange({
                                accessibility: val,
                            });
                        }}
                        required
                        error={errors.accessibility}
                    />
                </Box>

                <Typography
                    variant="h5"
                    sx={{
                        mt: 6,
                        mb: 2,
                    }}
                >
                    Zentrale E-Mail-Adressen für Systembenachrichtigungen
                </Typography>

                <AlertComponent
                    color="info"
                    sx={{my: 2}}
                >
                    Systembenachrichtigungen (wie z.B. Eingang eines neuen Antrags) werden standardmäßig an jede Mitarbeiter:in im Fachbereich gesendet.
                    Sobald Sie hier zentrale E-Mail-Adressen hinterlegen, erhalten nur noch diese die Systembenachrichtigungen.
                </AlertComponent>

                <TextFieldComponent
                    label="Zentrale E-Mail-Adressen für Systembenachrichtigungen"
                    value={editedDepartment?.departmentMail ?? undefined}
                    onChange={(val) => {
                        handleChange({
                            departmentMail: val,
                        });
                    }}
                    maxCharacters={255}
                    error={errors.departmentMail}
                    hint="Sie können mehrere E-Mail-Adressen durch ein Komma getrennt eingeben."
                />
            </FormPageWrapper>

            <InfoDialog
                title="Fachbereich kann nicht gelöscht werden"
                severity="warning"
                open={showNotDeletableDialog}
                onClose={() => {
                    setShowNotDeletableDialog(false);
                }}
            >
                <Typography
                    variant="body1"
                    component="p"
                >
                    Der Fachbereich kann nicht gelöscht werden, da er aktuell noch ein oder mehrere Formulare enthält.
                    Sie müssen diese Anträge erst löschen oder in einen anderen Fachbereich überschreiben, bevor Sie den
                    Fachbereich löschen können.
                </Typography>
            </InfoDialog>

            <ConfirmDialog
                title="Fachbereich löschen"
                onConfirm={confirmDelete}
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
            >
                Sind Sie sicher, dass Sie den Fachbereich wirklich löschen wollen?
                Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
            </ConfirmDialog>
        </>
    );
}
