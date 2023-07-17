import React, {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {EditDepartmentPageMembersTab} from './tabs/edit-department-page-members-tab';
import {type Department} from '../../../models/entities/department';
import {DepartmentsService} from '../../../services/departments-service';
import {delayPromise} from '../../../utils/with-delay';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useMembershipGuard} from '../../../hooks/use-membership-guard';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {EditDepartmentPageFormsTab} from './tabs/edit-department-page-forms-tab';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {type ListApplication} from '../../../models/entities/list-application';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Box, Divider, List, ListItem, Typography} from '@mui/material';
import {RichTextEditorComponentView} from '../../../components/richt-text-editor/rich-text-editor.component.view';
import {validateEmail} from '../../../utils/validate-email';
import {InfoDialog} from '../../../dialogs/info-dialog/info-dialog';

type Errors = {
    [key in keyof Department]?: string;
};

export function DepartmentEditPage(): JSX.Element {
    const departmentId = useParams().id;

    useMembershipGuard(parseInt(departmentId ?? ''));

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [originalDepartment, setOriginalDepartment] = useState<Department>();
    const [editedDepartment, setEditedDepartment] = useState<Department>();

    const [relatedApplications, setRelatedApplications] = useState<ListApplication[]>();

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
                created: '',
                updated: '',
            };
            setOriginalDepartment(newDepartment);
            setEditedDepartment(newDepartment);
            setIsBusy(false);
        } else {
            delayPromise(DepartmentsService.retrieve(parseInt(departmentId)))
                .then((user) => {
                    setOriginalDepartment(user);
                    setEditedDepartment(user);
                })
                .catch((err) => {
                    console.error(err);
                    setIs404(true);
                })
                .finally(() => {
                    setIsBusy(false);
                });

            DepartmentsService
                .listApplications(parseInt(departmentId))
                .then(setRelatedApplications)
                .catch((err) => {
                    console.error(err);
                    setRelatedApplications([]);
                    dispatch(showErrorSnackbar('Zugehörige Formulare konnten nicht geladen werden'));
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

            if (editedDepartment.imprint.length === 0) {
                errors.imprint = 'Bitte geben Sie ein Impressum an';
            }

            if (editedDepartment.privacy.length === 0) {
                errors.privacy = 'Bitte geben Sie eine Datenschutzerklärung an';
            }

            if (editedDepartment.accessibility.length === 0) {
                errors.accessibility = 'Bitte geben Sie eine Barrierefreiheitserklärung an';
            }

            if (Object.keys(errors).length > 0) {
                setErrors(errors);
            } else {
                setIsBusy(true);

                if (editedDepartment.id === 0) {
                    DepartmentsService
                        .create(editedDepartment)
                        .then((createdDepartment) => {
                            dispatch(showSuccessSnackbar('Fachbereich erfolgreich erstellt!'));
                            navigate(`/departments/${createdDepartment.id}`, {replace: true});
                        })
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fachbereich konnte nicht gespeichert werden!'));
                            setIsBusy(false);
                        });
                } else {
                    DepartmentsService
                        .update(editedDepartment.id, editedDepartment)
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
                        });
                }
            }
        }
    };

    const handleReset = (): void => {
        if (originalDepartment != null) {
            setEditedDepartment(originalDepartment);
        }
    };

    const handlePrepareDelete = (): void => {
        if (relatedApplications != null && relatedApplications.length > 0) {
            setShowNotDeletableDialog(true);
        } else {
            setConfirmDelete(() => handleDelete);
        }
    };

    const handleDelete = (): void => {
        if (editedDepartment != null && editedDepartment.id !== 0) {
            setIsBusy(true);

            DepartmentsService
                .destroy(editedDepartment.id)
                .then(() => {
                    navigate('/departments');
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Fachbereich konnte nicht gelöscht werden!'));
                    setIsBusy(false);
                });
        }
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
                onDelete={editedDepartment?.id !== 0 ? handlePrepareDelete : undefined}

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
                                label: 'Formulare',
                                content: (
                                    <EditDepartmentPageFormsTab
                                        applications={relatedApplications ?? []}
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

                <Divider sx={{my: 4}}>
                    Kontakt-Email-Adressen
                </Divider>

                <TextFieldComponent
                    label="Kontakt-Email-Adresse fachliche Unterstützung"
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
                    label="Kontakt-Email-Adresse technische Unterstützung"
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

                <Divider sx={{my: 4}}>
                    Rechtliche Informationen
                </Divider>

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
                    Sie müssen diese Anträge erst löschen oder in einen anderen Fachbereich überschreiben, bevor Sie den Fachbereich löschen können.
                </Typography>

                <List>
                    {
                        relatedApplications?.map((app) => (
                            <ListItem
                                key={app.id}
                            >
                                <Link to={`/edit/${app.id}`}>
                                    {app.title} {app.version}
                                </Link>
                            </ListItem>
                        ))
                    }
                </List>
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
