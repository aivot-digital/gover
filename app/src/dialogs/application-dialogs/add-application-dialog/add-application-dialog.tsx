import {Alert, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {type Form as Application} from '../../../models/entities/form';
import {ElementType} from '../../../data/element-type/element-type';
import {type AddApplicationDialogProps} from './add-application-dialog-props';
import {ApplicationStatus} from '../../../data/application-status';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {type RootElement} from '../../../models/elements/root-element';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {slugify} from '../../../utils/slugify';
import {checkTitle} from '../../../utils/check-title';
import {checkVersion} from '../../../utils/version-utils';
import {checkSlugAndVersion} from '../../../utils/check-slug-and-version';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {useApi} from '../../../hooks/use-api';
import {useMembershipsApi} from '../../../hooks/use-memberships-api';
import {DepartmentMembershipWithDepartment} from '../../../models/entities/department-membership';


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
        developingDepartmentId: 0,
        created: new Date().toISOString(),
        updated: new Date().toISOString(),
        openSubmissions: 0,
        inProgressSubmissions: 0,
        totalSubmissions: 0,
        destinationId: null,
        imprintDepartmentId: null,
        privacyDepartmentId: null,
        accessibilityDepartmentId: null,
        legalSupportDepartmentId: null,
        technicalSupportDepartmentId: null,
        customerAccessHours: 4,
        managingDepartmentId: null,
        responsibleDepartmentId: null,
        themeId: null,
        submissionDeletionWeeks: 2,
        bundIdEnabled: false,
        bundIdLevel: undefined,
        bayernIdEnabled: false,
        bayernIdLevel: undefined,
        shIdEnabled: false,
        shIdLevel: undefined,
        mukEnabled: false,
    };
}

export function AddApplicationDialog(props: AddApplicationDialogProps): JSX.Element {
    const {
        applicationToBaseOn,
        existingApplications,
        mode,
        onClose,
        onSave,
        ...passTroughProps
    } = props;

    const api = useApi();

    const user = useAppSelector(selectUser);

    const [departments, setDepartments] = useState<DepartmentMembershipWithDepartment[]>([]);
    const [application, setApplication] = useState<Application>(createEmptyApplication);
    const [errors, setErrors] = useState<ErrorsType>({});

    useEffect(() => {
        if (user != null) {
            useMembershipsApi(api)
                .listWithDepartments({user: user?.id})
                .then(setDepartments);
        }
    }, [user]);

    useEffect((): void => {
        if (applicationToBaseOn != null) {
            if (mode === 'clone') {
                setApplication({
                    ...JSON.parse(JSON.stringify(applicationToBaseOn)),
                    title: '',
                    slug: '',
                    version: '',
                    developingDepartment: 0,
                    id: 0,
                });
            } else {
                setApplication(JSON.parse(JSON.stringify(applicationToBaseOn)));
            }
        } else {
            setApplication(createEmptyApplication);
        }
    }, [applicationToBaseOn, mode]);

    const handlePatch = (patch: Partial<Application>): void => {
        setApplication({
            ...application,
            ...patch,
        });
    };

    const handleSave = (navigateToEditAfterwards: boolean): void => {
        setErrors({});

        const errors: ErrorsType = {};

        if (application.developingDepartmentId == null || application.developingDepartmentId === 0) {
            errors.developingDepartmentId = 'Bitte wählen Sie einen Fachbereich aus';
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
            versionError,
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

    const handleClose = (): void => {
        setApplication(createEmptyApplication);
        setErrors({});
        onClose();
    };

    return (
        <Dialog
            {...passTroughProps}
            fullWidth
            onClose={handleClose}
            maxWidth="lg"
        >
            <DialogTitleWithClose
                onClose={props.onClose}
                closeTooltip="Schließen"
            >
                {
                    mode === 'new' &&
                    'Neues Formular anlegen'
                }

                {
                    mode === 'new-version' &&
                    'Neue Version anlegen'
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
            <DialogContent tabIndex={0}>
                {
                    Object.keys(errors).length > 0 &&
                    <Alert
                        severity="error"
                        sx={{mb: 2}}
                    >
                        Bitte beheben Sie die markierten Fehler um fortzufahren. (Ggf. bitte nach unten scrollen.)
                    </Alert>
                }
                <Typography
                    variant="body2"
                    sx={{mb: 2}}
                >
                    Wählen Sie den Fachbereich aus, für den Sie das Formular anlegen möchten.
                    Achten Sie darauf, dass <u>ausschließlich</u> Mitarbeiter:innen dieses Fachbereichs das Formular
                    einsehen und ändern können.<br />
                    Sie haben im Nachgang die Möglichkeit einen bewirtschaftenden oder zuständigen Fachbereich
                    auszuwählen.
                    Mitarbeiter:innen dieser Fachbereiche können die eingegangenen Anträge einsehen, nicht aber das Formular
                    abändern.
                </Typography>

                <SelectFieldComponent
                    label="Entwickelnder Fachbereich"
                    value={application.developingDepartmentId.toString()}
                    onChange={(val) => {
                        handlePatch({
                            developingDepartmentId: val != null ? parseInt(val) : 0,
                        });
                    }}
                    options={departments.map((membership) => ({
                        value: membership.departmentId.toString(),
                        label: membership.department.name,
                    }))}
                    error={errors.developingDepartmentId}
                    required
                    disabled={mode === 'new-version'}
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
                    label="Titel des Formulars"
                    placeholder="Hundesteueranmeldung"
                    value={application.title ?? ''}
                    onChange={(val) => {
                        handlePatch({
                            title: val,
                        });
                    }}
                    onBlur={(val) => {
                        const title = val != null ? val.trim() : '';
                        if (application.slug.length === 0) {
                            handlePatch({
                                slug: slugify(title, 96),
                            });
                        }
                    }}
                    required
                    error={errors.title}
                    maxCharacters={96}
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
                    value={application.slug}
                    onChange={(val) => {
                        handlePatch({
                            slug: val,
                        });
                    }}
                    onBlur={(val) => {
                        handlePatch({
                            slug: val != null ? val.trim().replace(/-\s*$/, '') : '',
                        });
                    }}
                    required
                    error={errors.slug}
                    maxCharacters={96}
                    disabled={mode === 'new-version'}
                />

                <Typography
                    variant="body2"
                    sx={{
                        mt: 4,
                        mb: 2,
                    }}
                >
                    Vergeben Sie die Version des Formulars. Unter dieser wird das Formular für antragstellende Personen verfügbar
                    sein. Achten Sie darauf, dass Sie dem Schema der semantischen Versionierung folgen.
                    Die Version besteht aus drei Zahlen, die jeweils durch einen Punkt getrennt werden. Die erste Zahl
                    gibt die Hauptversion (Major) an und sollte nur bei tiefgreifenden Änderungen erhöht werden. Die
                    zweite Zahl gibt die Nebenversion (Minor) an und sollte bei kleineren Änderungen erhöht werden. Die
                    dritte Zahl gibt die Fehlerkorrekturen (Fix) an und sollte nur bei solchen erhöht werden.
                </Typography>

                <TextFieldComponent
                    label="Version des Formulars"
                    placeholder="1.0.0"
                    value={application.version}
                    onChange={(val) => {
                        handlePatch({
                            version: val,
                        });
                    }}
                    required
                    error={errors.version}
                    maxCharacters={11}
                />

                <Alert
                    severity="warning"
                    variant="outlined"
                    sx={{mt: 1}}
                >
                    Bitte beachten Sie, dass die Angaben (abgesehen vom Titel) nachträglich nicht mehr geändert werden können.
                </Alert>

                {
                    Object.keys(errors).length > 0 &&
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
                    onClick={() => {
                        handleSave(true);
                    }}
                >
                    Anlegen und Bearbeiten
                </Button>
                <Button
                    variant="outlined"
                    onClick={() => {
                        handleSave(false);
                    }}
                    sx={{
                        ml: 1,
                    }}
                >
                    Nur Anlegen
                </Button>
                <div style={{flexGrow: 2}}/>
                <Button
                    onClick={handleClose}
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
