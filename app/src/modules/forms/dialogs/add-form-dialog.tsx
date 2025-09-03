import {Alert, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {slugify} from '../../../utils/slugify';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {useApi} from '../../../hooks/use-api';
import {DepartmentsApiService} from '../../departments/departments-api-service';
import {Department} from '../../departments/models/department';
import {useFormManager} from '../../../hooks/use-form-manager';
import {FormDetailsResponseDTO} from '../dtos/form-details-response-dto';
import * as yup from 'yup';
import type {DialogProps} from '@mui/material/esm/Dialog';
import {FormsApiService} from '../forms-api-service';
import {AlertComponent} from '../../../components/alert/alert-component';
import {FormRequestDTO} from '../dtos/form-request-dto';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';

const FormSchema = yup.object({
    developingDepartmentId: yup
        .number()
        .min(0, 'Bitte wählen Sie einen Fachbereich aus')
        .required('Bitte wählen Sie einen Fachbereich aus'),
    internalTitle: yup
        .string()
        .trim()
        .min(3, 'Der interne Titel muss mindestens ${min} Zeichen lang sein')
        .max(96, 'Der interne Titel darf maximal ${max} Zeichen lang sein')
        .required('Bitte geben Sie einen internen Titel an'),
    publicTitle: yup
        .string()
        .trim()
        .min(3, 'Der öffentliche Titel muss mindestens ${min} Zeichen lang sein')
        .max(120, 'Der öffentliche Titel darf maximal ${max} Zeichen lang sein')
        .required(),
    slug: yup
        .string()
        .trim()
        .min(3, 'Die URL muss mindestens ${min} Zeichen lang sein')
        .max(96, 'Die URL darf maximal ${max} Zeichen lang sein')
        .matches(/^[a-z0-9]+[a-z0-9-]*$/, 'Die URL darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen und muss mit einem Buchstaben oder einer Zahl beginnen'),
});

export interface AddFormDialogProps extends DialogProps {
    basis: FormDetailsResponseDTO;
    onClose: () => void;
    onSave: (form: FormDetailsResponseDTO) => void;
}

export function AddFormDialog(props: AddFormDialogProps) {
    const {
        basis,
        onClose,
        onSave,
        ...passTroughProps
    } = props;

    const api = useApi();
    const dispatch = useAppDispatch();

    const user = useAppSelector(selectUser);

    const [availableDepartments, setAvailableDepartments] = useState<Department[]>([]);

    const {
        errors,
        currentItem,
        handleInputChange,
        handleInputBlur,
        validate: validateForm,
        reset: resetForm,
    } = useFormManager<FormDetailsResponseDTO>(basis, FormSchema as any);

    const {
        slug: currentItemSlug,
    } = currentItem as FormDetailsResponseDTO;

    const hasErrors = useMemo(() => {
        return Object.keys(errors).length > 0 &&
            Object.values(errors).some((v) => v != null && v.length > 0);
    }, [errors]);

    const [isCreating, setIsCreating] = useState(false);

    const [slugStatus, setSlugStatus] = useState<'available' | 'blocked' | 'unknown'>('unknown');

    useEffect(() => {
        if (user != null) {
            new DepartmentsApiService(api)
                .list(0, 999, undefined, undefined, {userId: user.id})
                .then(departments => setAvailableDepartments(departments.content));
        }
    }, [user]);

    useEffect(() => {
        if (currentItemSlug == null || currentItemSlug.length === 0) {
            setSlugStatus('unknown');
            return;
        }

        new FormsApiService(api)
            .checkSlugExists(currentItemSlug)
            .then((exists) => {
                setSlugStatus(exists ? 'blocked' : 'available');
            })
            .catch(err => {
                console.error(err);
                setSlugStatus('blocked');
            });
    }, [currentItemSlug]);

    const handleSave = async (): Promise<void> => {
        if (currentItem == null) {
            return;
        }

        if (!validateForm()) {
            return;
        }

        const formsApi = new FormsApiService(api);

        const slugExists = await formsApi
            .checkSlugExists(currentItemSlug);

        if (slugExists) {
            setSlugStatus('blocked');
            return;
        }

        setIsCreating(true);

        const newForm: FormRequestDTO = {
            slug: currentItem.slug,
            internalTitle: currentItem.internalTitle,
            publicTitle: currentItem.publicTitle,
            developingDepartmentId: currentItem.developingDepartmentId,
            managingDepartmentId: currentItem.managingDepartmentId,
            responsibleDepartmentId: currentItem.responsibleDepartmentId,
            type: currentItem.type,
            legalSupportDepartmentId: currentItem.legalSupportDepartmentId,
            technicalSupportDepartmentId: currentItem.technicalSupportDepartmentId,
            imprintDepartmentId: currentItem.imprintDepartmentId,
            privacyDepartmentId: currentItem.privacyDepartmentId,
            accessibilityDepartmentId: currentItem.accessibilityDepartmentId,
            destinationId: currentItem.destinationId,
            themeId: currentItem.themeId,
            pdfTemplateKey: currentItem.pdfTemplateKey,
            paymentProviderKey: currentItem.paymentProviderKey,
            paymentPurpose: currentItem.paymentPurpose,
            paymentDescription: currentItem.paymentDescription,
            paymentProducts: currentItem.paymentProducts,
            identityVerificationRequired: currentItem.identityVerificationRequired,
            identityProviders: currentItem.identityProviders,
            customerAccessHours: currentItem.customerAccessHours,
            submissionRetentionWeeks: currentItem.submissionRetentionWeeks,
            rootElement: currentItem.rootElement,
        };

        dispatch(showLoadingOverlay('Neues Formular wird erstellt…'));

        try {
            const createdForm = await formsApi
                .create(newForm);
            onSave(createdForm);
            handleClose(null, 'saveSuccess' as any);
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Das Formular konnte nicht erstellt werden. Bitte versuchen Sie es erneut.'));
        } finally {
            dispatch(hideLoadingOverlay());
            setIsCreating(false);
        }
    };

    const handleClose = (_: any, reason: string): void => {
        if (reason === 'backdropClick' || reason === 'escapeKeyDown') {
            return;
        }
        resetForm();
        onClose();
    };

    return (
        <Dialog
            {...passTroughProps}
            fullWidth
            onClose={handleClose}
            maxWidth="lg"
            disableEscapeKeyDown={true}
        >
            <DialogTitleWithClose
                onClose={props.onClose}
                closeTooltip="Schließen"
            >
                Neues Formular anlegen
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                {
                    hasErrors &&
                    <Alert
                        severity="error"
                        sx={{mb: 2}}
                    >
                        Bitte beheben Sie die markierten Fehler um fortzufahren. (Ggf. bitte nach unten scrollen.)
                    </Alert>
                }
                <Typography>
                    Wählen Sie den Fachbereich aus, für den Sie das Formular anlegen möchten.
                    Achten Sie darauf, dass <u>ausschließlich</u> Mitarbeiter:innen dieses Fachbereichs das Formular
                    einsehen und ändern können.
                </Typography>
                <Typography
                    sx={{my: 2}}
                >
                    Sie haben im Nachgang die Möglichkeit einen bewirtschaftenden oder zuständigen Fachbereich
                    auszuwählen.
                    Mitarbeiter:innen dieser Fachbereiche können die eingegangenen Anträge einsehen, aber das Formular
                    nicht abändern.
                </Typography>

                <SelectFieldComponent
                    label="Entwickelnder Fachbereich"
                    value={currentItem?.developingDepartmentId != null ? currentItem.developingDepartmentId.toString() : undefined}
                    onChange={(val) => {
                        handleInputChange('developingDepartmentId')(val != null ? parseInt(val) : 0);
                    }}
                    options={availableDepartments.map((department) => ({
                        value: department.id.toString(),
                        label: department.name,
                    }))}
                    disabled={isCreating}
                    error={errors.developingDepartmentId}
                    required
                />

                <Typography
                    variant="body2"
                    sx={{
                        mt: 4,
                        mb: 2,
                    }}
                >
                    Vergeben Sie einen Titel für das Formular um es besser identifizieren zu können.
                    Diesen Titel können nur Sie und ihre Kolleg:innen einsehen.
                </Typography>

                <TextFieldComponent
                    label="Interner Titel des Formulars"
                    placeholder="Hundesteueranmeldung"
                    value={currentItem?.internalTitle ?? undefined}
                    onChange={handleInputChange('internalTitle')}
                    onBlur={(val) => {
                        const title = val != null ? val.trim() : '';
                        if (currentItem?.slug.length === 0) {
                            handleInputChange('slug')(slugify(title, 96));
                        }
                        handleInputBlur('internalTitle')();
                    }}
                    disabled={isCreating}
                    required
                    error={errors.internalTitle}
                    maxCharacters={96}
                    minCharacters={3}
                />

                <Typography
                    variant="body2"
                    sx={{
                        mt: 4,
                        mb: 2,
                    }}
                >
                    Vergeben Sie einen Titel für das Formular um es besser identifizieren zu können.
                    Diesen Titel können nur Sie und ihre Kolleg:innen einsehen.
                </Typography>

                <TextFieldComponent
                    label="Öffentlicher Titel des Formulars"
                    placeholder="Hundesteueranmeldung"
                    value={currentItem?.publicTitle ?? undefined}
                    onChange={handleInputChange('publicTitle')}
                    onBlur={handleInputBlur('publicTitle')}
                    required
                    multiline
                    disabled={isCreating}
                    error={errors.publicTitle}
                    maxCharacters={120}
                    minCharacters={3}
                />

                <Typography
                    variant="body2"
                    sx={{
                        mt: 4,
                        mb: 2,
                    }}
                >
                    Vergeben Sie die URL des Formulars. Unter dieser wird das Formular für antragstellende Personen verfügbar
                    sein.
                    Technisch bedingt darf die URL nur aus Kleinbuchstaben (ohne Umlaute), Zahlen und Bindestrichen
                    bestehen.
                </Typography>

                <TextFieldComponent
                    label="URL-Element (Titel des Formulars innerhalb der URL)"
                    placeholder="antrag-hundesteueranmeldung"
                    value={currentItem?.slug ?? undefined}
                    onChange={handleInputChange('slug')}
                    onBlur={handleInputBlur('slug')}
                    required
                    disabled={isCreating}
                    error={errors.slug}
                    maxCharacters={96}
                    minCharacters={3}
                    debounce={600}
                />

                {
                    slugStatus == 'blocked' &&
                    <AlertComponent
                        color="error"
                    >
                        URL nicht verfügbar
                    </AlertComponent>
                }

                {
                    hasErrors &&
                    <Alert
                        severity="error"
                        sx={{mt: 2}}
                    >
                        Bitte beheben Sie die markierten Fehler um fortzufahren.
                    </Alert>
                }
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    onClick={handleSave}
                    disabled={hasErrors || isCreating}
                >
                    Anlegen und Bearbeiten
                </Button>
                <div style={{flexGrow: 2}} />
                <Button
                    onClick={() => handleClose(null, 'closeButtonClick')}
                    disabled={isCreating}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
