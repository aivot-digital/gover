import {Box, Button, Grid, Typography} from '@mui/material';
import React, {ComponentType, useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate, useSearchParams} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {RichTextEditorComponentView, RichTextEditorComponentViewProps} from '../../../../components/richt-text-editor/rich-text-editor.component.view';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ThemeResponseDTO} from '../../../themes/models/theme';
import {ThemesApiService} from '../../../themes/themes-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../slices/shell-slice';
import {DepartmentsDetailsPageAdditionalData, NewParentIdQueryParam} from './departments-details-page';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {TextFieldComponentProps} from '../../../../components/text-field/text-field-component-props';
import {SelectFieldComponentProps} from '../../../../components/select-field/select-field-component-props';
import {DepartmentEntity} from '../../entities/department-entity';
import {DepartmentApiService} from '../../services/department-api-service';
import {getDepartmentTypeLabelGenitiv} from '../../utils/department-utils';
import {FormApiService} from '../../../forms/services/form-api-service';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';


export const DepartmentSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Fachbereichs muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Fachbereichs darf maximal 96 Zeichen lang sein.')
        .required('Der Name des Fachbereichs ist ein Pflichtfeld.'),
    address: yup.string()
        .trim()
        .min(3, 'Die Adresse muss mindestens 3 Zeichen lang sein.')
        .max(255, 'Die Adresse darf maximal 255 Zeichen lang sein.')
        .optional()
        .nullable(),
    //.required('Die Adresse ist ein Pflichtfeld.'),
    specialSupportAddress: yup.string()
        .trim()
        .email('Bitte eine gültige E-Mail-Adresse eingeben.')
        .max(255, 'Die E-Mail-Adresse darf maximal 255 Zeichen lang sein.')
        .optional()
        .nullable(),
    //.required('Die E-Mail-Adresse für fachliche Unterstützung ist ein Pflichtfeld.'),
    technicalSupportAddress: yup.string()
        .trim()
        .email('Bitte eine gültige E-Mail-Adresse eingeben.')
        .max(255, 'Die E-Mail-Adresse darf maximal 255 Zeichen lang sein.')
        .optional()
        .nullable(),
    //.required('Die E-Mail-Adresse für technische Unterstützung ist ein Pflichtfeld.'),
    imprint: yup.string()
        .trim()
        .min(10, 'Das Impressum muss mindestens 10 Zeichen lang sein.')
        .optional()
        .nullable(),
    //.required('Das Impressum ist ein Pflichtfeld.'),
    commonPrivacy: yup.string()
        .trim()
        .min(10, 'Die Datenschutzerklärung muss mindestens 10 Zeichen lang sein.')
        .optional()
        .nullable(),
    //.required('Die Datenschutzerklärung ist ein Pflichtfeld.'),
    commonAccessibility: yup.string()
        .trim()
        .min(10, 'Die Barrierefreiheitserklärung muss mindestens 10 Zeichen lang sein.')
        .optional()
        .nullable(),
    //.required('Die Barrierefreiheitserklärung ist ein Pflichtfeld.'),
    departmentMail: yup.string()
        .optional()
        .nullable()
        .test('valid-email-list', 'Bitte eine oder mehrere gültige E-Mail-Adressen, durch Komma getrennt, eingeben.', (val) => {
            if (!val) return true;
            return val.split(',').every(email => /\S+@\S+\.\S+/.test(email.trim()));
        }),
});

export function DepartmentsDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const [searchParams, _] = useSearchParams();

    const parentOrgUnitId = useMemo(() => {
        const parentId = searchParams.get(NewParentIdQueryParam);
        if (parentId != null && !isNaN(Number(parentId))) {
            return parseInt(parentId);
        }
        return undefined;
    }, [searchParams]);

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
        additionalData,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<DepartmentEntity, DepartmentsDetailsPageAdditionalData>;

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
            message: 'Dieser Fachbereich kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            key: 'no-edit-permission-department',
        }));

        return () => {
            dispatch(removeSnackbarMessage('no-edit-permission-department'));
        };
    }, [isEditable]);

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<DepartmentEntity>(item, DepartmentSchema as any);

    const apiService = useMemo(() => new DepartmentApiService(), []);
    const department = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedApplications, setRelatedApplications] = useState<ConstraintLinkProps[] | undefined>(undefined);
    const [availableThemes, setAvailableThemes] = useState<ThemeResponseDTO[]>();

    useEffect(() => {
        new ThemesApiService(api)
            .listAll()
            .then((result) => {
                setAvailableThemes(result.content);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der verfügbaren Fabschemata.'));
            });
    }, []);

    if (department == null || availableThemes == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        // Do not save if department is null
        if (department == null) {
            return;
        }

        // Validate form
        const validationResult = validate();

        // If validation fails, show error snackbar and do not proceed
        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        if (department.id === 0) {
            const parentId = parseInt(searchParams.get(NewParentIdQueryParam) ?? '');

            apiService
                .create({
                    ...department,
                    parentDepartmentId: isNaN(parentId) ? undefined : parentId,
                })
                .then((newDepartment) => {
                    setItem(newDepartment);
                    reset();

                    dispatch(showSuccessSnackbar('Neuer Fachbereich erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/departments/${newDepartment.id}`, {
                            replace: true,
                        });
                    }, 0);
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            apiService
                .update(department.id, department)
                .then((updatedDepartment) => {
                    setItem(updatedDepartment);
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen an Fachbereich erfolgreich gespeichert.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const checkAndHandleDelete = async () => {
        if (department.id === 0) {
            return;
        }

        setIsBusy(true);
        try {
            const formsApi = new FormApiService();
            const developingForms = await formsApi.listAll({
                developingDepartmentId: department.id,
            });

            const uniqueForms = developingForms.content;

            if (uniqueForms.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = uniqueForms.slice(0, maxVisibleLinks).map(f => ({
                    label: f.internalTitle,
                    to: `/forms/${f.id}`,
                }));

                if (uniqueForms.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: 'Weitere Formulare anzeigen…',
                        to: `/departments/${department.id}/forms`,
                    });
                }

                setRelatedApplications(processedLinks);
                setShowConstraintDialog(true);
            } else {
                setConfirmDeleteAction(() => confirmDelete);
            }
        } catch (error) {
            console.error(error);
            dispatch(showErrorSnackbar('Fehler beim Prüfen der Löschbarkeit.'));
        } finally {
            setIsBusy(false);
        }
    };

    const confirmDelete = () => {
        if (department.id === 0) return;

        setIsBusy(true);
        apiService.destroy(department.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/departments', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Der Fachbereich wurde erfolgreich gelöscht.'));
            })
            .catch(() => dispatch(showErrorSnackbar('Beim Löschen ist ein Fehler aufgetreten.')))
            .finally(() => setIsBusy(false));
    };

    const doNotShadow = department.depth === 0 && parentOrgUnitId == null;

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{
                    mt: 1.5,
                    mb: 1,
                }}
            >
                Öffentliche Informationen {getDepartmentTypeLabelGenitiv(department.depth)}
            </Typography>
            <Typography
                sx={{
                    mb: 2,
                    maxWidth: 900,
                }}
            >
                Hinterlegen Sie grundsätzliche Informationen über diesen Fachbereich. Diese Informationen werden in der Anwendung angezeigt und sind für die Nutzer:innen sichtbar.
            </Typography>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Name des Fachbereichs"
                        value={department.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        error={errors.name}
                        disabled={!isEditable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                />
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.address != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('address')('');
                            } else {
                                handleInputChange('address')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.address ?? '',
                            disabled: true,
                        }}
                        label="Adresse des Fachbereichs"
                        value={department.address}
                        onChange={handleInputChange('address')}
                        onBlur={handleInputBlur('address')}
                        required
                        maxCharacters={255}
                        multiline
                        rows={3}
                        error={errors.address}
                        disabled={!isEditable}
                    />
                </Grid>
            </Grid>
            <Typography
                variant="h6"
                sx={{
                    mt: 2,
                    mb: 1,
                }}
            >
                Farbschema des Fachbereichs
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Hinterlegen Sie das Standard-Farbschema, das für Formulare dieses Fachbereichs verwendet werden soll.
                Dieses überschreibt das System-Farbschema.
                Bearbeiter:innen können für Formulare weiterhin ein individuelles Farbschema auswählen.
                Wenn Sie kein Farbschema auswählen, wird das System-Farbschema verwendet.
            </Typography>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ShadowedInput<SelectFieldComponentProps, typeof SelectFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={SelectFieldComponent}
                        override={department.themeId != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('themeId')(0);
                            } else {
                                handleInputChange('themeId')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.themeId?.toString() ?? undefined,
                            disabled: true,
                        }}
                        label="Farbschema des Fachbereichs"
                        value={department.themeId?.toString()}
                        onChange={(val) => {
                            if (val == null) {
                                handleInputChange('themeId')(null);
                            } else {
                                const intVal = parseInt(val);

                                if (isNaN(intVal)) {
                                    handleInputChange('themeId')(null);
                                } else {
                                    handleInputChange('themeId')(intVal);
                                }
                            }
                        }}
                        required={false}
                        error={errors.themeId}
                        options={availableThemes.map(theme => ({
                            label: theme.name,
                            value: theme.id.toString(),
                        }))}
                        disabled={!isEditable}
                    />
                </Grid>
            </Grid>


            <Typography
                variant="h6"
                sx={{
                    mt: 2,
                    mb: 1,
                }}
            >
                Kontakt-E-Mail-Adressen für antragstellende Personen
            </Typography>
            <Typography sx={{mb: 2}}>
                Die hier hinterlegten Kontaktinformationen werden Nutzer:innen zum Beispiel im Hilfe-Dialog zur Verfügung gestellt.
            </Typography>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.specialSupportAddress != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('specialSupportAddress')('');
                            } else {
                                handleInputChange('specialSupportAddress')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.specialSupportAddress ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-E-Mail-Adresse fachliche Unterstützung"
                        type="email"
                        value={department.specialSupportAddress}
                        onChange={handleInputChange('specialSupportAddress')}
                        onBlur={handleInputBlur('specialSupportAddress')}
                        required
                        maxCharacters={255}
                        error={errors.specialSupportAddress}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.specialSupportPhone != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('specialSupportPhone')('');
                            } else {
                                handleInputChange('specialSupportPhone')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.specialSupportPhone ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-Telefonnummer fachliche Unterstützung"
                        type="tel"
                        value={department.specialSupportPhone}
                        onChange={handleInputChange('specialSupportPhone')}
                        onBlur={handleInputBlur('specialSupportPhone')}
                        required
                        maxCharacters={255}
                        error={errors.specialSupportPhone}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.specialSupportInfo != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('specialSupportInfo')('');
                            } else {
                                handleInputChange('specialSupportInfo')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.specialSupportInfo ?? '',
                            disabled: true,
                        }}
                        label="Informationen zur fachliche Unterstützung"
                        value={department.specialSupportInfo}
                        onChange={handleInputChange('specialSupportInfo')}
                        onBlur={handleInputBlur('specialSupportInfo')}
                        required
                        multiline={true}
                        rows={5}
                        maxCharacters={1024}
                        error={errors.specialSupportInfo}
                        disabled={!isEditable}
                        hint="Zusätzliche Informationen, z.B. zu Erreichbarkeiten oder Supportzeiten."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.technicalSupportAddress != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('technicalSupportAddress')('');
                            } else {
                                handleInputChange('technicalSupportAddress')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.technicalSupportAddress ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-E-Mail-Adresse technische Unterstützung"
                        type="email"
                        value={department.technicalSupportAddress}
                        onChange={handleInputChange('technicalSupportAddress')}
                        onBlur={handleInputBlur('technicalSupportAddress')}
                        required
                        maxCharacters={255}
                        error={errors.technicalSupportAddress}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.technicalSupportPhone != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('technicalSupportPhone')('');
                            } else {
                                handleInputChange('technicalSupportPhone')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.technicalSupportPhone ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-Telefonnummer technische Unterstützung"
                        type="tel"
                        value={department.technicalSupportPhone}
                        onChange={handleInputChange('technicalSupportPhone')}
                        onBlur={handleInputBlur('technicalSupportPhone')}
                        required
                        maxCharacters={255}
                        error={errors.technicalSupportPhone}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.technicalSupportInfo != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('technicalSupportInfo')('');
                            } else {
                                handleInputChange('technicalSupportInfo')(null);
                            }
                        }}
                        shadowedProps={{
                            value: additionalData?.shadowedDepartment.technicalSupportInfo ?? '',
                            disabled: true,
                        }}
                        label="Informationen zur technischen Unterstützung"
                        value={department.technicalSupportInfo}
                        onChange={handleInputChange('technicalSupportInfo')}
                        onBlur={handleInputBlur('technicalSupportInfo')}
                        required
                        multiline={true}
                        rows={5}
                        maxCharacters={1024}
                        error={errors.technicalSupportInfo}
                        disabled={!isEditable}
                        hint="Zusätzliche Informationen, z.B. zu Erreichbarkeiten oder Supportzeiten."
                    />
                </Grid>
            </Grid>
            <Typography
                variant="h6"
                sx={{mt: 2, mb: 1}}
            >
                Rechtliche Informationen
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Die folgenden rechtlichen Angaben und Texte können in Formularen referenziert werden.
            </Typography>
            <Box sx={{mb: 3}}>
                <ShadowedInput<RichTextEditorComponentViewProps, typeof RichTextEditorComponentView>
                    doNotShadow={doNotShadow}
                    Component={RichTextEditorComponentView}
                    override={department.imprint != null}
                    onSetOverride={(override) => {
                        if (override) {
                            handleInputChange('imprint')('');
                        } else {
                            handleInputChange('imprint')(null);
                        }
                    }}
                    shadowedProps={{
                        value: additionalData?.shadowedDepartment.imprint ?? '',
                        disabled: true,
                    }}
                    label="Impressum"
                    value={department.imprint}
                    onChange={handleInputChange('imprint')}
                    required
                    error={errors.imprint}
                    disabled={!isEditable}
                />
            </Box>
            <Box sx={{mb: 3}}>
                <ShadowedInput<RichTextEditorComponentViewProps, typeof RichTextEditorComponentView>
                    doNotShadow={doNotShadow}
                    Component={RichTextEditorComponentView}
                    override={department.commonPrivacy != null}
                    onSetOverride={(override) => {
                        if (override) {
                            handleInputChange('commonPrivacy')('');
                        } else {
                            handleInputChange('commonPrivacy')(null);
                        }
                    }}
                    shadowedProps={{
                        value: additionalData?.shadowedDepartment.commonPrivacy ?? '',
                        disabled: true,
                    }}
                    label="Datenschutzerklärung"
                    value={department.commonPrivacy}
                    onChange={handleInputChange('commonPrivacy')}
                    required
                    error={errors.commonPrivacy}
                    disabled={!isEditable}
                />
            </Box>
            <Box sx={{mb: 3}}>
                <ShadowedInput<RichTextEditorComponentViewProps, typeof RichTextEditorComponentView>
                    doNotShadow={doNotShadow}
                    Component={RichTextEditorComponentView}
                    override={department.commonAccessibility != null}
                    onSetOverride={(override) => {
                        if (override) {
                            handleInputChange('commonAccessibility')('');
                        } else {
                            handleInputChange('commonAccessibility')(null);
                        }
                    }}
                    shadowedProps={{
                        value: additionalData?.shadowedDepartment.commonAccessibility ?? '',
                        disabled: true,
                    }}
                    label="Barrierefreiheitserklärung"
                    value={department.commonAccessibility}
                    onChange={handleInputChange('commonAccessibility')}
                    required
                    error={errors.commonAccessibility}
                    disabled={!isEditable}
                />
            </Box>

            <Typography
                variant="h5"
                sx={{
                    mt: 6,
                    mb: 1,
                }}
            >
                Zentrale E-Mail-Adressen für Systembenachrichtigungen
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Systembenachrichtigungen (wie z.B. Eingang eines neuen Antrags) werden grundsätzlich an jede Mitarbeiter:in im Fachbereich gesendet.
                Wenn Sie hier eine oder mehrere zentrale E-Mail-Adressen hinterlegen, erhalten nur noch diese die Systembenachrichtigungen.
            </Typography>

            <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                doNotShadow={doNotShadow}
                Component={TextFieldComponent}
                override={department.departmentMail != null}
                onSetOverride={(override) => {
                    if (override) {
                        handleInputChange('departmentMail')('');
                    } else {
                        handleInputChange('departmentMail')(null);
                    }
                }}
                shadowedProps={{
                    value: additionalData?.shadowedDepartment.departmentMail ?? '',
                    disabled: true,
                }}
                label="Zentrale E-Mail-Adressen für Systembenachrichtigungen"
                value={department.departmentMail ?? undefined}
                onChange={handleInputChange('departmentMail')}
                onBlur={handleInputBlur('departmentMail')}
                maxCharacters={255}
                error={errors.departmentMail}
                hint="Sie können mehrere E-Mail-Adressen durch ein Komma getrennt eingeben."
                disabled={!isEditable}
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon />}
                >
                    Speichern
                </Button>

                {
                    department.id !== 0 &&
                    <Button
                        onClick={() => {
                            reset();
                        }}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                }

                {
                    department.id !== 0 &&
                    <Button
                        variant="outlined"
                        onClick={checkAndHandleDelete}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<Delete />}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Fachbereich löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={department.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Fachbereich wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieser Fachbereich kann (noch) nicht gelöscht werden, da er noch für Formulare als entwickelnder, zuständiger oder bewirtschaftender Fachbereich zugewiesen ist."
                solutionText="Bitte übertragen Sie die Formulare an einen anderen Fachbereich und versuchen Sie es erneut:"
                links={relatedApplications}
            />
        </Box>
    );
}

interface ShadowedInputProps<P, C extends ComponentType<P>> {
    doNotShadow: boolean;
    override: boolean;
    onSetOverride: (override: boolean) => void;
    Component: C;
    shadowedProps: Partial<P>;
}

function ShadowedInput<P, C extends ComponentType<P>>(props: ShadowedInputProps<P, C> & P) {
    const {
        override,
        onSetOverride,
        doNotShadow,

        Component,
        shadowedProps,

        ...rest
    } = props;

    const propsToPass: any = useMemo(() => {
        if (override) {
            return {
                ...rest as P,
            };
        }

        return {
            ...rest as P,
            ...shadowedProps,
        };
    }, [override, rest, shadowedProps]);

    if (doNotShadow) {
        return <Component {...(rest as any)} />;
    }

    return (
        <Box
            sx={{
                display: 'flex',
                gap: 1,
                flexDirection: 'column',
            }}
        >
            <Box>
                <CheckboxFieldComponent
                    label="Überschreiben"
                    onChange={onSetOverride}
                    value={override}
                />
            </Box>
            <Box
                sx={{
                    flex: 1,
                }}
            >
                <Component {...propsToPass} />
            </Box>
        </Box>
    );
}