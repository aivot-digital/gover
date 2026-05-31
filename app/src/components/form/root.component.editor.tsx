import React, {useEffect, useMemo, useState} from 'react';
import {Box, Grid, Skeleton} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type SelectFieldComponentOption} from '../select-field/select-field-component-option';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {useApi} from '../../hooks/use-api';
import {Link} from 'react-router-dom';
import {Hint} from '../hint/hint';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {ThemesApiService} from '../../modules/themes/themes-api-service';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import {downloadQrCode} from '../../utils/download-qrcode';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {withDelay} from '../../utils/with-delay';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';
import {AssetSelector} from '../../modules/assets/components/asset-selector';
import {type VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../../modules/departments/services/v-department-shadowed-api-service';
import {DepartmentSelectField} from '../../modules/departments/components/department-select-field';
import {SelectDepartmentDialog} from '../../modules/departments/dialogs/select-department-dialog';

type DepartmentDialogTarget =
    'responsible' |
    'managing' |
    'imprint' |
    'privacy' |
    'accessibility' |
    'legalSupport' |
    'technicalSupport';

export function RootComponentEditor(props: BaseEditorProps<FormLayoutElement>) {
    const dispatch = useAppDispatch();
    const api = useApi();

    const {
        element: form,
        onPatch,
    } = props;

    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[] | null>(null);
    const [themes, setThemes] = useState<SelectFieldComponentOption[] | null>(null);
    const [activeDepartmentDialog, setActiveDepartmentDialog] = useState<DepartmentDialogTarget | null>(null);

    const handleDownloadQrCode = async (link: string, filename: string) => {
        try {
            await downloadQrCode(link, filename);
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
    };

    useEffect(() => {
        withDelay(
            new VDepartmentShadowedApiService().listAllOrdered([
                'parentNames',
                'name',
            ], 'ASC'),
            600,
        )
            .then((deps) => deps.content)
            .then(setDepartments)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Organisationseinheiten!'));
            });

        withDelay(new ThemesApiService(api)
            .listAll(), 600)
            .then((themes) => themes.content.map((theme) => ({
                value: theme.id.toString(),
                label: theme.name,
            })))
            .then(setThemes)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Erscheinungsbilder!'));
            });
    }, [api, dispatch]);

    const departmentsById = useMemo(() => {
        return new Map((departments ?? []).map((department) => [
            department.id,
            department,
        ]));
    }, [departments]);

    const getDepartmentById = (departmentId: number | null | undefined) => {
        return departmentId != null ? departmentsById.get(departmentId) : undefined;
    };

    const responsibleDepartment = getDepartmentById(form.responsibleDepartmentId);
    const managingDepartment = getDepartmentById(form.managingDepartmentId);
    const imprintDepartment = getDepartmentById(form.imprintDepartmentId);
    const privacyDepartment = getDepartmentById(form.privacyDepartmentId);
    const accessibilityDepartment = getDepartmentById(form.accessibilityDepartmentId);
    const legalSupportDepartment = getDepartmentById(form.legalSupportDepartmentId);
    const technicalSupportDepartment = getDepartmentById(form.technicalSupportDepartmentId);

    const handleSelectDepartment = (departmentId: number) => {
        switch (activeDepartmentDialog) {
            case 'responsible':
                onPatch({
                    responsibleDepartmentId: departmentId,
                });
                return;
            case 'managing':
                onPatch({
                    managingDepartmentId: departmentId,
                });
                return;
            case 'imprint':
                onPatch({
                    imprintDepartmentId: departmentId,
                });
                return;
            case 'privacy':
                onPatch({
                    privacyDepartmentId: departmentId,
                });
                return;
            case 'accessibility':
                onPatch({
                    accessibilityDepartmentId: departmentId,
                });
                return;
            case 'legalSupport':
                onPatch({
                    legalSupportDepartmentId: departmentId,
                });
                return;
            case 'technicalSupport':
                onPatch({
                    technicalSupportDepartmentId: departmentId,
                });
                return;
            default:
                return;
        }
    };

    const getActiveDepartmentId = (): number | null | undefined => {
        switch (activeDepartmentDialog) {
            case 'responsible':
                return form.responsibleDepartmentId;
            case 'managing':
                return form.managingDepartmentId;
            case 'imprint':
                return form.imprintDepartmentId;
            case 'privacy':
                return form.privacyDepartmentId;
            case 'accessibility':
                return form.accessibilityDepartmentId;
            case 'legalSupport':
                return form.legalSupportDepartmentId;
            case 'technicalSupport':
                return form.technicalSupportDepartmentId;
            default:
                return null;
        }
    };

    const generalLink = ''; //TODO: createCustomerPath(`${props.entity?.form.slug ?? ''}`);
    const versionedLink = ''; //TODO: createCustomerPath(`${props.entity?.form.slug ?? ''}/${form.version ?? ''}`);

    return (
        <>
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
                        value={form.tabTitle}
                        label="Titel des Formulars im Browser-Tab"
                        hint="Dieser Titel (oftmals eine Kurzform) erscheint als Bezeichnung des Formulars im Tab des Webbrowsers. Wenn Sie keinen spezifischen Titel angeben, wird die Überschrift des Formulars verwendet."
                        onChange={(val) => {
                            onPatch({
                                tabTitle: val,
                            });
                        }}
                        maxCharacters={60}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        value={form.publicTitle}
                        label="Öffentlicher Titel & Überschrift des Formulars"
                        multiline
                        hint="Dieser Titel wird öffentlich für das Formular verwendet und ggü. Anstragstellenden angezeigt."
                        onChange={(val) => {
                            onPatch({
                                publicTitle: val ?? '',
                            });
                        }}
                        rows={3}
                        maxCharacters={120}
                        required
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
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
                        label="Allgemeiner Link des Formulars"
                        disabled
                        onChange={() => {
                        }}
                        value={generalLink}
                        hint="Wenn Sie immer die aktuellste Version des Formulars verlinken möchten, dann wählen Sie den Link ohne Versionierung. Es wird immer das zuletzt veröffentlichte Formular unter diesem Link angezeigt."
                        endAction={
                            [
                                {
                                    icon: <ContentPasteOutlinedIcon/>,
                                    tooltip: 'Link in Zwischenablage kopieren',
                                    onClick: async () => {
                                        const success = await copyToClipboardText(generalLink);
                                        if (success) {
                                            dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                        } else {
                                            dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                        }
                                    },
                                },
                                {
                                    icon: <QrCode2OutlinedIcon/>,
                                    tooltip: 'QR-Code herunterladen',
                                    onClick: async () => {
                                        //TODO: await handleDownloadQrCode(generalLink, `qr-code-${props.entity?.form.slug ?? ''}.png`);
                                    },
                                },
                            ]
                        }
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >

                    <TextFieldComponent
                        label="Versionsspezifischer Link des Formulars"
                        disabled
                        onChange={() => {
                        }}
                        value={versionedLink}
                        hint="Wenn Sie die explizite Version eines Formulars verlinken möchten, dann wählen Sie den Link inkl. Versionierung. Sobald Sie eine andere Version des Formulars nutzen möchten, müssen Sie den Link z.B. auf Ihrer Webseite entsprechend austauschen."
                        endAction={
                            [
                                {
                                    icon: <ContentPasteOutlinedIcon/>,
                                    tooltip: 'Link in Zwischenablage kopieren',
                                    onClick: async () => {
                                        const success = await copyToClipboardText(versionedLink);
                                        if (success) {
                                            dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                        } else {
                                            dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                        }
                                    },
                                },
                                {
                                    icon: <QrCode2OutlinedIcon/>,
                                    tooltip: 'QR-Code herunterladen',
                                    onClick: async () => {
                                        // TODO: await handleDownloadQrCode(versionedLink, `qr-code-${props.entity?.form.slug ?? ''}-${(form.version ?? '')}.png`);
                                    },
                                },
                            ]
                        }
                    />
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Zuständige Organisationseinheiten"
                variant="h5"
            >
                Geben Sie hier an, welche Organisationseinheiten für die Bearbeitung der im Formular abgefragten Daten
                zuständig sind.
            </ElementEditorSectionHeader>
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
                    {
                        departments == null &&
                        <Skeleton
                            width="100%"
                            height={88}
                        />
                    }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Zuständige Organisationseinheit"
                            value={responsibleDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('responsible');
                            }}
                            onClear={() => {
                                onPatch({
                                    responsibleDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    {
                        departments == null &&
                        <Skeleton
                            width="100%"
                            height={88}
                        />
                    }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Bewirtschaftende Organisationseinheit"
                            value={managingDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('managing');
                            }}
                            onClear={() => {
                                onPatch({
                                    managingDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Rechtliche Angaben"
                variant="h5"
            >
                Rechtstexte werden auf Ebene der Organisationseinheiten hinterlegt und verwaltet. Sie können hier die
                Organisationseinheiten auswählen, deren Texte Sie verwenden und anzeigen möchten.
            </ElementEditorSectionHeader>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    {
                        departments == null &&
                        <Skeleton
                            width="100%"
                            height={80}
                        />
                    }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Text für das Impressum"
                            value={imprintDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('imprint');
                            }}
                            onClear={() => {
                                onPatch({
                                    imprintDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    {
                        departments == null &&
                        <Skeleton
                            width="100%"
                            height={80}
                        />
                    }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Text für die Datenschutzerklärung"
                            value={privacyDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('privacy');
                            }}
                            onClear={() => {
                                onPatch({
                                    privacyDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    {
                        departments == null &&
                        <Skeleton
                            width="100%"
                            height={80}
                        />
                    }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Text für die Erklärung der Barrierefreiheit"
                            value={accessibilityDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('accessibility');
                            }}
                            onClear={() => {
                                onPatch({
                                    accessibilityDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Erscheinungsbild"
                variant="h5"
            >
                Hinterlegen Sie bei Bedarf ein abweichendes Erscheinungsbild und wählen Sie ggf. eine PDF-Vorlage,
                welche zur Generierung des Formulars zur Offline-Einreichung verwendet wird.
            </ElementEditorSectionHeader>
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
                    <Box
                        display="flex"
                        alignItems="center"
                    >
                        {
                            themes == null &&
                            <Skeleton
                                width="100%"
                                height={80}
                            />
                        }
                        {
                            themes != null &&
                            <SelectFieldComponent
                                label="Erscheinungsbild"
                                value={form.themeId?.toString() ?? undefined}
                                onChange={(val) => {
                                    onPatch({
                                        themeId: val != null ? parseInt(val) : null,
                                    });
                                }}
                                options={themes}
                                disabled={!props.editable}
                                hint={'Bitte speichern Sie das Formular nach Änderungen, damit das Erscheinungsbild in der Vorschau angewendet wird.'}
                            />
                        }
                        <Hint
                            summary="Sie können ein abweichendes Erscheinungsbild für dieses Formular auswählen."
                            detailsTitle="Erscheinungsbild"
                            details={
                                <>
                                    <p>
                                        Sie können hier ein abweichendes Erscheinungsbild für dieses Formular auswählen.
                                    </p>
                                    <p>
                                        Erscheinungsbilder werden nach folgendem Prioritätsprinzip angewendet.
                                        Der erste passende Eintrag in der folgenden Liste wird verwendet:

                                        <ol>
                                            <li>Das Erscheinungsbild des Formulars</li>
                                            <li>Das Erscheinungsbild der zuständigen Organisationseinheit</li>
                                            <li>Das Erscheinungsbild der bewirtschaftenden Organisationseinheit</li>
                                            <li>Das Erscheinungsbild der entwickelnden Organisationseinheit</li>
                                            <li>Das globale Erscheinungsbild der Gover-Instanz</li>
                                        </ol>
                                    </p>
                                    <p>
                                        Das Erscheinungsbild legt Farben, Logo und Favicon des Formulars fest.
                                    </p>
                                </>
                            }
                            sx={{ml: 2}}
                        />
                    </Box>
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                    >
                        <AssetSelector
                            label="PDF-Vorlage"
                            selectLabel="PDF-Vorlage auswählen"
                            value={form.pdfTemplateKey ?? null}
                            onChange={(val) => {
                                onPatch({
                                    pdfTemplateKey: val ?? null,
                                });
                            }}
                            mimetype="text/html"

                        />

                        <Hint
                            summary="Sie können eine individuelle Vorlage für die Generierung von PDF-Dokumenten auswählen."
                            detailsTitle="PDF-Vorlage"
                            details={
                                <>
                                    <p>
                                        Sie können eine individuelle Vorlage für die Generierung von PDF-Dokumenten
                                        auswählen.
                                    </p>
                                    <p>
                                        Die Vorlage wird für das PDF des eingereichten Antrags (welches antragstellende
                                        Personen und
                                        Mitarbeiter:innen der Verwaltung erhalten) verwendet.
                                        Auch der Vordruck des Formulars verwendet die ausgewählte Vorlage.
                                    </p>
                                    <p>
                                        Vorlagen können im Bereich <Link to="/assets">Dokumente &
                                                                                      Medieninhalte</Link> hochgeladen
                                        werden.
                                    </p>
                                </>
                            }
                            sx={{ml: 2}}
                        />
                    </Box>
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Fristen"
                variant="h5"
            >
                Geben Sie die für diesen Antrag gültigen Fristen ein, welche den Antragstellenden im Formular angezeigt
                werden.
            </ElementEditorSectionHeader>
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
                        label="Antragsfristen"
                        multiline
                        value={props.element.expiring}
                        onChange={(val) => {
                            props.onPatch({
                                expiring: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Kontakte"
                variant="h5"
            >
                Kontaktinformationen werden auf Ebene der Organisationseinheit hinterlegt und verwaltet. Sie können hier
                die Organisationseinheiten auswählen, deren Kontakt Sie für dieses Formular verwenden und anzeigen
                möchten.
            </ElementEditorSectionHeader>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >{
                    departments == null &&
                    <Skeleton
                        width="100%"
                        height={80}
                    />
                }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Fachlicher Support"
                            value={legalSupportDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('legalSupport');
                            }}
                            onClear={() => {
                                onPatch({
                                    legalSupportDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    {
                        departments == null &&
                        <Skeleton
                            width="100%"
                            height={80}
                        />
                    }
                    {
                        departments != null &&
                        <DepartmentSelectField
                            label="Technischer Support"
                            value={technicalSupportDepartment}
                            onOpenDialog={() => {
                                setActiveDepartmentDialog('technicalSupport');
                            }}
                            onClear={() => {
                                onPatch({
                                    technicalSupportDepartmentId: null,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    }
                </Grid>
            </Grid>
            <SelectDepartmentDialog
                open={activeDepartmentDialog != null}
                onClose={() => {
                    setActiveDepartmentDialog(null);
                }}
                onSelect={(department) => {
                    handleSelectDepartment(department.id);
                }}
                selectedDepartmentId={getActiveDepartmentId()}
            />
            <ElementEditorSectionHeader
                title="Hinweise zur Offline-Einreichung"
                variant="h5"
            >
                Diese Angaben werden für den PDF-Vordruck des Formulars genutzt. Sie sind nicht relevant, wenn
                ausschließlich eine Online-Einreichung zugelassen wird.
            </ElementEditorSectionHeader>
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
                    <RichTextInputComponent
                        hint="Wenn Sie dieses Formular als Vordruck z.B. zum Ausfüllen auf Papier, bereitstellen möchten, sollten Sie hier die Adresse und/oder E-Mail etc. nennen, an welche das Formular einzureichen ist."
                        value={props.element.offlineSubmissionText ?? ''}
                        onChange={val => {
                            props.onPatch({
                                offlineSubmissionText: val ?? undefined,
                            });
                        }}
                        disabled={!props.editable}
                    />

                    <CheckboxFieldComponent
                        label="Das Formular erfordert eine Unterschrift"
                        hint="Wenn diese Option aktiviert ist, wird im PDF-Vordruck ein Unterschriftenfeld eingefügt. Dies erfolgt nur, sofern die PDF-Vorlage dies unterstützt."
                        value={props.element.offlineSignatureNeeded ?? false}
                        onChange={val => {
                            props.onPatch({
                                offlineSignatureNeeded: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
        </>
    );
}
