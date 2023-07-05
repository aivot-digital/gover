import React, {FormEvent, useEffect, useState} from "react";
import {Box, Button, Divider, Skeleton} from "@mui/material";
import {TextFieldComponent} from "../../../../components/text-field/text-field-component";
import {validateEmail} from "../../../../utils/validate-email";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {showSuccessSnackbar} from "../../../../slices/snackbar-slice";
import {Department} from "../../../../models/entities/department";
import {DepartmentsService} from "../../../../services/departments-service";
import {RichTextEditorComponentView} from "../../../../components/richt-text-editor/rich-text-editor.component.view";
import {useNavigate} from "react-router-dom";
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";

type Errors = {
    [key in keyof Department]?: string;
}

export function EditDepartmentPageCommonTab({id}: { id: string }) {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [originalDepartment, setOriginalDepartment] = useState<Department>();
    const [department, setDepartment] = useState<Department>();
    const [errors, setErrors] = useState<Errors>({});

    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    useEffect(() => {
        if (id === 'new') {
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
            setDepartment(newDepartment);
        } else {
            DepartmentsService
                .retrieve(parseInt(id))
                .then(user => {
                    setOriginalDepartment(user);
                    setDepartment(user);
                });
        }
    }, [id]);

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();
        event.stopPropagation();

        if (department != null) {
            const errors: Errors = {};

            if (department.name.length < 3) {
                errors.name = 'Bitte geben Sie mindestens 3 Zeichen ein';
            }

            if (department.address.length === 0) {
                errors.address = 'Bitte geben Sie eine Adresse ein';
            }

            if (!validateEmail(department.specialSupportAddress)) {
                errors.specialSupportAddress = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
            }

            if (!validateEmail(department.technicalSupportAddress)) {
                errors.technicalSupportAddress = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
            }

            if (department.imprint.length === 0) {
                errors.imprint = 'Bitte geben Sie ein Impressum an';
            }

            if (department.privacy.length === 0) {
                errors.privacy = 'Bitte geben Sie eine Datenschutzerklärung an';
            }

            if (department.accessibility.length === 0) {
                errors.accessibility = 'Bitte geben Sie eine Barrierefreiheitserklärung an';
            }

            if (Object.keys(errors).length > 0) {
                setErrors(errors);
            } else {
                setErrors({});

                if (department.id === 0) {
                    DepartmentsService
                        .create(department)
                        .then(createdDepartment => {
                            dispatch(showSuccessSnackbar('Fachbereich erfolgreich erstellt!'));
                            navigate(`/departments/${createdDepartment.id}`);
                        });
                } else {
                    DepartmentsService
                        .update(department.id, department)
                        .then(updatedDepartment => {
                            setOriginalDepartment(updatedDepartment);
                            setDepartment(updatedDepartment);
                            dispatch(showSuccessSnackbar('Fachbereich erfolgreich gespeichert!'));
                        });
                }
            }
        }


        return false;
    };

    return (
        <>

            {
                department == null &&
                <Skeleton variant="rectangular"/>
            }

            {
                department != null &&
                <form onSubmit={handleSubmit}>
                    <TextFieldComponent
                        label="Name des Fachbereichs"
                        value={department.name}
                        onChange={val => setDepartment({
                            ...department,
                            name: val ?? '',
                        })}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        error={errors.name}
                    />

                    <TextFieldComponent
                        label="Adresse des Fachbereichs"
                        value={department.address}
                        onChange={val => setDepartment({
                            ...department,
                            address: val ?? '',
                        })}
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
                        value={department.specialSupportAddress}
                        onChange={val => setDepartment({
                            ...department,
                            specialSupportAddress: val ?? '',
                        })}
                        required
                        maxCharacters={255}
                        error={errors.specialSupportAddress}
                    />

                    <TextFieldComponent
                        label="Kontakt-Email-Adresse technische Unterstützung"
                        type="email"
                        value={department.technicalSupportAddress}
                        onChange={val => setDepartment({
                            ...department,
                            technicalSupportAddress: val ?? '',
                        })}
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
                            value={department.imprint}
                            onChange={val => setDepartment({
                                ...department,
                                imprint: val,
                            })}
                            required
                            error={errors.imprint}
                        />
                    </Box>

                    <Box sx={{mb: 3}}>
                        <RichTextEditorComponentView
                            label="Datenschutzerklärung"
                            value={department.privacy}
                            onChange={val => setDepartment({
                                ...department,
                                privacy: val,
                            })}
                            required
                            error={errors.privacy}
                        />
                    </Box>

                    <Box sx={{mb: 3}}>
                        <RichTextEditorComponentView
                            label="Barrierefreiheitserklärung"
                            value={department.accessibility}
                            onChange={val => setDepartment({
                                ...department,
                                accessibility: val,
                            })}
                            required
                            error={errors.accessibility}
                        />
                    </Box>

                    <Box sx={{mt: 4, display: 'flex'}}>
                        <Button
                            type="submit"
                        >
                            Speichern
                        </Button>

                        <Button
                            sx={{ml: 2}}
                            type="reset"
                            color="error"
                            onClick={() => {
                                setDepartment(originalDepartment!);
                                setErrors({});
                            }}
                        >
                            Zurücksetzen
                        </Button>

                        {
                            id !== 'new' &&
                            <Button
                                sx={{ml: 'auto'}}
                                color="error"
                                type="button"
                                onClick={() => {
                                    setConfirmDelete(() => () => {
                                        DepartmentsService.destroy(department?.id);
                                        navigate('/departments');
                                    })
                                }}
                            >
                                Löschen
                            </Button>
                        }
                    </Box>
                </form>
            }

            <ConfirmDialog
                title="Fachbereich wirklich löschen"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Sind Sie sicher, dass Sie den Fachbereich <strong>{department?.name}</strong> wirklich löschen wollen?
                Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.<br/>
                Es werden <u>alle Anträge</u>, die von diesem Fachbereich entwickelt wurden gelöscht.
            </ConfirmDialog>
        </>
    );
}