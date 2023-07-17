import React, {type FormEvent, useState} from 'react';
import {Box, Button, Divider} from '@mui/material';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {validateEmail} from '../../../../utils/validate-email';
import {type Department} from '../../../../models/entities/department';
import {RichTextEditorComponentView} from '../../../../components/richt-text-editor/rich-text-editor.component.view';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';

type Errors = {
    [key in keyof Department]?: string;
};

interface EditDepartmentPageCommonTabProps {
    department: Department;
    hasChanged: boolean;
    onChange: (dep: Partial<Department>) => void;
    onSave: () => void;
    onReset: () => void;
    onDelete: () => void;
}

export function EditDepartmentPageCommonTab(props: EditDepartmentPageCommonTabProps): JSX.Element {
    const [errors, setErrors] = useState<Errors>({});

    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (props.department != null) {
            const errors: Errors = {};

            if (props.department.name.length < 3) {
                errors.name = 'Bitte geben Sie mindestens 3 Zeichen ein';
            }

            if (props.department.address.length === 0) {
                errors.address = 'Bitte geben Sie eine Adresse ein';
            }

            if (!validateEmail(props.department.specialSupportAddress)) {
                errors.specialSupportAddress = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
            }

            if (!validateEmail(props.department.technicalSupportAddress)) {
                errors.technicalSupportAddress = 'Bitte geben Sie eine gültige E-Mail-Adresse ein';
            }

            if (props.department.imprint.length === 0) {
                errors.imprint = 'Bitte geben Sie ein Impressum an';
            }

            if (props.department.privacy.length === 0) {
                errors.privacy = 'Bitte geben Sie eine Datenschutzerklärung an';
            }

            if (props.department.accessibility.length === 0) {
                errors.accessibility = 'Bitte geben Sie eine Barrierefreiheitserklärung an';
            }

            if (Object.keys(errors).length > 0) {
                setErrors(errors);
            } else {
                setErrors({});
                props.onSave();
            }
        }
    };

    return (
        <>
            <form onSubmit={handleSubmit}>
                <TextFieldComponent
                    label="Name des Fachbereichs"
                    value={props.department.name}
                    onChange={val => {
                        props.onChange({
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
                    value={props.department.address}
                    onChange={val => {
                        props.onChange({
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
                    value={props.department.specialSupportAddress}
                    onChange={val => {
                        props.onChange({
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
                    value={props.department.technicalSupportAddress}
                    onChange={val => {
                        props.onChange({
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
                        value={props.department.imprint}
                        onChange={val => {
                            props.onChange({
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
                        value={props.department.privacy}
                        onChange={val => {
                            props.onChange({
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
                        value={props.department.accessibility}
                        onChange={val => {
                            props.onChange({
                                accessibility: val,
                            });
                        }}
                        required
                        error={errors.accessibility}
                    />
                </Box>

                <Box sx={{
                    mt: 4,
                    display: 'flex',
                }}>
                    <Button
                        type="submit"
                        disabled={!props.hasChanged}
                    >
                        Speichern
                    </Button>

                    <Button
                        sx={{ml: 2}}
                        type="reset"
                        color="error"
                        onClick={props.onReset}
                        disabled={!props.hasChanged}
                    >
                        Zurücksetzen
                    </Button>

                    {
                        props.department.id !== 0 &&
                        <Button
                            sx={{ml: 'auto'}}
                            color="error"
                            type="button"
                            onClick={() => {
                                setConfirmDelete(() => props.onDelete);
                            }}
                        >
                            Löschen
                        </Button>
                    }
                </Box>
            </form>

            <ConfirmDialog
                title="Fachbereich wirklich löschen"
                onConfirm={confirmDelete}
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
            >
                Sind Sie sicher, dass Sie den Fachbereich <strong>{props.department.name}</strong> wirklich löschen
                wollen? Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.<br/>
                Es werden alle Formulare und eingegangenen Anträge, in denen der
                Fachbereich <strong>{props.department.name}</strong> als entwickelnder Fachbereich geführt wird,
                gelöscht.
            </ConfirmDialog>
        </>
    );
}
