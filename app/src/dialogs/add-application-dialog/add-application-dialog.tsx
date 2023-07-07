import { Alert, Button, Dialog, DialogActions, DialogContent, Typography } from '@mui/material';
import React, { useEffect, useState } from 'react';
import {
    DialogTitleWithClose
} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import { Application } from '../../models/entities/application';
import { ElementType } from '../../data/element-type/element-type';
import { AddApplicationDialogProps } from './add-application-dialog-props';
import { ApplicationStatus } from "../../data/application-status/application-status";
import { generateElementWithDefaultValues } from "../../utils/generate-element-with-default-values";
import { RootElement } from "../../models/elements/root-element";
import { TextFieldComponent } from "../../components/text-field/text-field-component";
import { slugify } from "../../utils/slugify";
import { checkTitle } from "../../utils/check-title";
import { checkVersion } from "../../utils/version-utils";
import { checkSlugAndVersion } from "../../utils/check-slug-and-version";
import { Department } from "../../models/entities/department";
import { DepartmentsService } from "../../services/departments-service";
import { SelectFieldComponent } from "../../components/select-field/select-field-component";
import { useAppSelector } from "../../hooks/use-app-selector";
import { selectMemberships, selectUser } from "../../slices/user-slice";


type ErrorsType = {
    [key in keyof Application]?: string;
};

function createEmptyApplication(): Application {
    return {
        id: 0,
        slug: '',
        version: '',
        title: '',
        status: ApplicationStatus.Drafted,
        root: generateElementWithDefaultValues(ElementType.Root) as RootElement,
        developingDepartment: 0,
        created: '',
        updated: '',
    };
}

export function AddApplicationDialog(props: AddApplicationDialogProps) {
    const {
        applicationToBaseOn,
        existingApplications,
        mode,
        onClose,
        onSave,
        ...passTroughProps
    } = props;

    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    const [departments, setDepartments] = useState<Department[]>([]);
    const [application, setApplication] = useState<Application>(createEmptyApplication);
    const [errors, setErrors] = useState<ErrorsType>({});

    useEffect(() => {
        if (user != null && user.admin) {
            DepartmentsService
                .list()
                .then(setDepartments);
        } else if (memberships != null) {
            Promise
                .all(memberships.map(mem => DepartmentsService.retrieve(mem.department)))
                .then(setDepartments);
        }
    }, [user, memberships]);

    useEffect(() => {
        if (applicationToBaseOn != null) {
            if (mode === 'clone') {
                setApplication({
                    ...JSON.parse(JSON.stringify(applicationToBaseOn)),
                    title: '',
                    slug: '',
                    version: '',
                    developingDepartment: 0,
                });
            } else {
                setApplication(JSON.parse(JSON.stringify(applicationToBaseOn)));
            }
        } else {
            setApplication(createEmptyApplication);
        }
    }, [applicationToBaseOn, mode]);

    const handlePatch = (patch: Partial<Application>) => {
        setApplication({
            ...application,
            ...patch,
        });
    }

    const handleSave = (navigateToEditAfterwards: boolean) => {
        setErrors({});

        const errors: ErrorsType = {};

        if (application.developingDepartment == null || application.developingDepartment === 0) {
            errors.developingDepartment = 'Bitte wählen Sie einen Fachbereich aus';
        }

        const titleErrors = checkTitle(application.title);
        if (titleErrors.length > 0) {
            errors.title = titleErrors[0];
        }

        const versionErrors = checkVersion(application.version);
        if (versionErrors.length > 0) {
            errors.version = versionErrors[0];
        }

        const {
            slugError,
            versionError
        } = checkSlugAndVersion(existingApplications, application.slug, application.version);
        if (slugError != null) {
            errors.slug = slugError;
        }
        if (versionError != null) {
            errors.version = versionError;
        }

        if (Object.keys(errors).length > 0) {
            setErrors(errors);
        } else {
            onSave(application, navigateToEditAfterwards);
            handleClose();
        }
    };

    const handleClose = () => {
        setApplication(createEmptyApplication);
        setErrors({});
        onClose();
    };

    return (
        <Dialog
            { ...passTroughProps }
            fullWidth
            onClose={ handleClose }
            maxWidth="lg"
        >
            <DialogTitleWithClose
                onClose={ props.onClose }
                closeTooltip="Schließen"
            >
                {
                    mode === 'new' &&
                    'Neues Formular anlegen'
                }

                {
                    mode === 'new-version' &&
                    'Neuen Version anlegen'
                }

                {
                    mode === 'clone' &&
                    'Formular duplizieren'
                }

                {
                    mode === 'import' &&
                    'Formular importieren'
                }
            </DialogTitleWithClose>
            <DialogContent>
                <Typography
                    variant="body2"
                    sx={ {mb: 2} }
                >
                    Wählen Sie den Fachbereich aus, für den Sie das Formular anlegen möchten.
                    Achten Sie darauf, dass <u>ausschließlich</u> Mitarbeiter:innen dieses Fachbereichs das Formular
                    einsehen und ändern können.<br/>
                    Sie haben im Nachgang die Möglichkeit einen bewirtschaftende oder zuständigen Fachbereich
                    auszuwählen.
                    Mietarbeiter dieser Fachbereiche können die eingegangenen Anträge einsehen, nicht aber as Formular
                    abändern.
                </Typography>

                <SelectFieldComponent
                    label="Entwickelnder Fachbereich"
                    value={ application.developingDepartment.toString() }
                    onChange={ val => {
                        handlePatch({
                            developingDepartment: val != null ? parseInt(val) : 0,
                        });
                    } }
                    options={ departments.map(dep => ({value: dep.id.toString(), label: dep.name})) }
                    error={ errors.developingDepartment }
                    required
                    disabled={ mode === 'new-version' }
                />

                <Typography
                    variant="body2"
                    sx={ {mt: 4, mb: 2} }
                >
                    Vergeben Sie einen Titel für den Antrag um ihn besser identifizieren zu können. Diesen Titel können
                    nur Sie und ihre Kolleg:innen einsehen.
                </Typography>

                <TextFieldComponent
                    label="Titel des Formulars"
                    placeholder="Hundesteueranmeldung"
                    value={ application.title ?? '' }
                    onChange={ val => {
                        handlePatch({
                            title: val,
                        });
                    } }
                    onBlur={ val => {
                        const title = val != null ? val.trim() : '';
                        if (application.slug.length === 0) {
                            handlePatch({
                                slug: slugify(title),
                            });
                        }
                    } }
                    required
                    error={ errors.title }
                    maxCharacters={ 96 }
                    disabled={ mode === 'new-version' }
                />

                <Typography
                    variant="body2"
                    sx={ {mt: 4, mb: 2} }
                >
                    Vergeben Sie die URL des Formulars. Unter dieser wird das Formular für Antragstellende verfügbar
                    sein.
                    Technisch bedingt darf die URL nur aus Kleinbuchstaben (ohne Umlaute), Zahlen und Bindestrichen
                    bestehen.
                </Typography>

                <TextFieldComponent
                    label="URL-Element (Titel des Antrages innerhalb der URL)"
                    placeholder="antrag-hundesteueranmeldung"
                    value={ application.slug }
                    onChange={ val => {
                        handlePatch({
                            slug: val,
                        });
                    } }
                    onBlur={ val => {
                        handlePatch({
                            slug: val != null ? val.trim().replace(/-\s*$/, '') : '',
                        });
                    } }
                    required
                    error={ errors.slug }
                    maxCharacters={ 60 }
                    disabled={ mode === 'new-version' }
                />

                <Typography
                    variant="body2"
                    sx={ {mt: 4, mb: 2} }
                >
                    Vergeben Sie die Version des Antrages. Unter dieser wird der Antrag für Antragstellende verfügbar
                    sein. Achten Sie darauf, dass Sie dem Schema der semantischen Versionierung folgen.
                    Die Version besteht aus drei Zahlen, die jeweils durch einen Punkt getrennt werden. Die erste Zahl
                    gibt die Hauptversion (Major) an und sollte nur bei tiefgreifenden Änderungen erhöht werden. Die
                    zweite Zahl gibt die Nebenversion (Minor) an und sollte bei kleineren Änderungen erhöht werden. Die
                    dritte Zahl gibt die Fehlerkorrekturen (Fix) an und sollte nur bei solchen erhöht werden.
                </Typography>

                <TextFieldComponent
                    label="Version des Antrags"
                    placeholder="1.0.0"
                    value={ application.version }
                    onChange={ val => {
                        handlePatch({
                            version: val,
                        });
                    } }
                    required
                    error={ errors.version }
                    maxCharacters={ 11 }
                />

                <Alert
                    severity="warning"
                    variant="outlined"
                    sx={ {mt: 1} }
                >
                    Bitte beachten Sie, dass diese Angaben später nicht mehr geändert werden können.
                </Alert>

                {
                    Object.keys(errors).length > 0 &&
                    <Alert
                        severity="error"
                        sx={ {mt: 2} }
                    >
                        Bitte beheben Sie die existierenden Fehler!
                    </Alert>
                }
            </DialogContent>

            <DialogActions
                sx={ {
                    pb: 3,
                    px: 3,
                    justifyContent: 'flex-start',
                } }
            >
                <Button
                    size="large"
                    variant="outlined"
                    onClick={ () => handleSave(true) }
                >
                    Anlegen und Bearbeiten
                </Button>
                <Button
                    size="large"
                    onClick={ () => handleSave(false) }
                >
                    Nur Anlegen
                </Button>
                <Button
                    onClick={ handleClose }
                    sx={ {
                        ml: 'auto!important',
                    } }
                    size="large"
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
