import {Alert, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {slugify} from '../../../utils/slugify';
import {useFormManager} from '../../../hooks/use-form-manager';
import * as yup from 'yup';
import {type ObjectSchema} from 'yup';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {useConfirm} from '../../../providers/confirm-provider';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../departments/utils/department-utils';
import {FormEntity} from '../entities/form-entity';
import {FormVersionEntity} from '../entities/form-version-entity';
import {FormApiService} from '../services/form-api-service';
import {SelectFieldComponent, SelectFieldComponentOption} from '../../../components/select-field-2/select-field-component';
import {FormVersionApiService} from '../services/form-version-api-service';
import {setLoadingMessage} from '../../../slices/shell-slice';
import {VDepartmentMembershipWithDetailsService} from '../../departments/services/v-department-membership-with-details-service';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';

type FormSchema = Partial<
    Pick<FormEntity, 'slug' | 'developingDepartmentId' | 'internalTitle'> &
    Pick<FormVersionEntity, 'publicTitle'>
>;

const formSchema: ObjectSchema<FormSchema> = yup.object({
    developingDepartmentId: yup
        .number()
        .integer()
        .moreThan(0, 'Bitte wählen Sie einen Fachbereich aus')
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
        .required('Bitte geben Sie einen öffentlichen Titel an'),
    slug: yup
        .string()
        .trim()
        .min(3, 'Die URL muss mindestens ${min} Zeichen lang sein')
        .max(96, 'Die URL darf maximal ${max} Zeichen lang sein')
        .matches(/^[a-z0-9]+[a-z0-9-]*$/, 'Die URL darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen und muss mit einem Buchstaben oder einer Zahl beginnen')
        .required('Bitte geben Sie ein URL-Element an'),
});

export interface AddFormDialogProps {
    basis: {
        form: FormEntity,
        version: FormVersionEntity,
    } | undefined;
    onClose: () => void;
    onSave: (form: FormEntity, version: FormVersionEntity) => void;
    open: boolean;
}

export function AddFormDialog(props: AddFormDialogProps) {
    const {
        basis,
        onClose,
        onSave,
        open,
    } = props;

    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const user = useAppSelector(selectUser);
    const [availableDepartments, setAvailableDepartments] = useState<SelectFieldComponentOption<number>[]>([]);

    const {
        errors,
        currentItem,
        handleInputChange,
        handleInputChangeWithValidation,
        handleInputBlur,
        validate: validateForm,
        reset: resetForm,
        hasNotChanged,
    } = useFormManager<FormSchema>({
        developingDepartmentId: basis?.form.developingDepartmentId,
        internalTitle: basis?.form.internalTitle,
        publicTitle: basis?.version.publicTitle,
        slug: basis?.form.slug,
    }, formSchema);

    const {
        slug: currentItemSlug,
    } = currentItem as FormEntity;

    const hasChangedSinceOpen = useMemo(() => {
        return !hasNotChanged;
    }, [hasNotChanged]);

    const hasErrors = useMemo(() => {
        return Object.keys(errors).length > 0 &&
            Object.values(errors).some((v) => v != null && v.length > 0);
    }, [errors]);

    const [isCreating, setIsCreating] = useState(false);

    const [slugStatus, setSlugStatus] = useState<'available' | 'blocked' | 'unknown'>('unknown');

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
                const options: SelectFieldComponentOption<number>[] = content
                    .map((membership) => ({
                        value: membership.departmentId,
                        label: membership.name,
                        icon: getDepartmentTypeIcons(membership.depth),
                        subLabel: getDepartmentTypeLabel(membership.depth),
                    }));
                setAvailableDepartments(options);
            })
            .catch(err => {
                dispatch(showApiErrorSnackbar(err, 'Die Fachbereiche konnten nicht geladen werden. Bitte versuchen Sie es erneut.'));
            });
    }, [user]);

    useEffect(() => {
        if (currentItemSlug == null || currentItemSlug.length === 0) {
            setSlugStatus('unknown');
            return;
        }

        new FormApiService()
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

        const formApiService = new FormApiService();
        const versionApiService = new FormVersionApiService();

        const slugExists = await formApiService
            .checkSlugExists(currentItemSlug);

        if (slugExists) {
            setSlugStatus('blocked');
            return;
        }

        dispatch(setLoadingMessage({
            message: 'Neues Formular wird erstellt',
            blocking: true,
            estimatedTime: 5000,
        }));

        setIsCreating(true);

        const newForm: FormEntity = {
            ...formApiService.initialize(),
            ...basis?.form,
            slug: currentItem.slug!,
            internalTitle: currentItem.internalTitle!,
            developingDepartmentId: currentItem.developingDepartmentId!,
        };

        const newVersion: FormVersionEntity = {
            ...versionApiService.initialize(),
            ...basis?.version,
            publicTitle: currentItem.publicTitle!,
        };

        let createdForm: FormEntity | null = null;
        let createdVersion: FormVersionEntity | null = null;
        try {
            createdForm = await formApiService
                .create(newForm);

            createdVersion = await versionApiService
                .create({
                    ...newVersion,
                    formId: createdForm.id,
                });
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Das Formular konnte nicht erstellt werden. Bitte versuchen Sie es erneut.'));
        } finally {
            dispatch(setLoadingMessage(undefined));
            setIsCreating(false);
        }

        if (createdForm != null && createdVersion != null) {
            onSave(createdForm, createdVersion);
            resetForm();
        }
    };

    const handleClose = async (_: any, reason: string): Promise<void> => {
        if (hasChangedSinceOpen && reason !== 'saveSuccess') {
            const confirmed = await showConfirm({
                title: 'Möchten Sie die eingegebenen Daten wirklich verwerfen?',
                children: (
                    <Typography>
                        Dieser Vorgang kann nicht rückgängig gemacht werden.
                        Wenn Sie die Daten verwerfen, müssen Sie diese bei Bedarf erneut eingeben.
                    </Typography>
                ),
                confirmButtonText: 'Ja, Eingaben verwerfen',
                isDestructive: false,
            });

            if (!confirmed) {
                return;
            }
        }

        resetForm();
        onClose();
    };

    return (
        <Dialog
            open={open}
            fullWidth
            onClose={handleClose}
            maxWidth="lg"
            disableEscapeKeyDown={true}
        >
            <DialogTitleWithClose
                onClose={() => handleClose(null, 'closeButtonClick')}
                closeTooltip="Schließen"
            >
                Neues Formular anlegen
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                {
                    (hasErrors || slugStatus == 'blocked') &&
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
                    value={currentItem?.developingDepartmentId}
                    onChange={(val) => {
                        handleInputChangeWithValidation('developingDepartmentId')(val != null ? val : undefined);
                    }}
                    options={availableDepartments}
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
                    Vergeben Sie einen internen Titel für das Formular um dieses identifizieren zu können.
                    Diesen Titel können nur Sie und ihre Kolleg:innen einsehen.
                </Typography>

                <TextFieldComponent
                    label="Interner Titel des Formulars"
                    placeholder="Hundesteueranmeldung"
                    value={currentItem?.internalTitle ?? undefined}
                    onChange={handleInputChange('internalTitle')}
                    onBlur={(val) => {
                        const title = val != null ? val.trim() : '';
                        if (currentItem?.slug != null && currentItem.slug.length === 0) {
                            handleInputChangeWithValidation('slug')(slugify(title, 96));
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
                    Vergeben Sie einen öffentlichen Titel für das Formular.
                    Dieser Titel ist öffentlich sichtbar und wird ggü. Anstragstellenden angezeigt.
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
                    error={slugStatus == 'blocked' ? 'Das gewählte URL-Element ist nicht verfügbar, weil es bereits von einem anderen Formular verwendet wird. Bitte wählen Sie ein anderes URL-Element.' : errors.slug}
                    maxCharacters={96}
                    minCharacters={3}
                    debounce={600}
                />

                {
                    (hasErrors || slugStatus == 'blocked') &&
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
