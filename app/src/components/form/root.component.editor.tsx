import React, {useEffect, useMemo, useState} from 'react';
import {Box, Grid, Skeleton} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {useApi} from '../../hooks/use-api';
import {Link} from 'react-router-dom';
import {Hint} from '../hint/hint';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {ThemesApiService} from '../../modules/themes/themes-api-service';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {withDelay} from '../../utils/with-delay';
import {AssetSelector} from '../../modules/assets/components/asset-selector';
import {type VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../../modules/departments/services/v-department-shadowed-api-service';
import {DepartmentSelectField} from '../../modules/departments/components/department-select-field';

export function RootComponentEditor(props: BaseEditorProps<FormLayoutElement>) {
    const dispatch = useAppDispatch();
    const api = useApi();

    const {
        element: form,
        onPatch,
    } = props;

    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[] | null>(null);
    const [themes, setThemes] = useState<SelectFieldComponentOption[] | null>(null);

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
                    <CheckboxFieldComponent
                        label="Im öffentlichen Formularverzeichnis anzeigen"
                        hint="Wenn diese Option aktiviert ist, wird das veröffentlichte Formular im öffentlichen Formularverzeichnis aufgeführt. Wenn sie deaktiviert ist, bleibt das Formular weiterhin über direkte Links erreichbar."
                        value={form.showOnFormIndexPage !== false}
                        onChange={(val) => {
                            onPatch({
                                showOnFormIndexPage: val,
                            });
                        }}
                        disabled={!props.editable}
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
                            onChange={(department) => {
                                onPatch({
                                    responsibleDepartmentId: department?.id ?? null,
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
                            onChange={(department) => {
                                onPatch({
                                    managingDepartmentId: department?.id ?? null,
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
                Organisationseinheiten auswählen und formularspezifische Ergänzungen pflegen.
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
                            onChange={(department) => {
                                onPatch({
                                    imprintDepartmentId: department?.id ?? null,
                                });
                            }}
                            required
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
                            label="Allgemeiner Teil der Datenschutzerklärung"
                            value={privacyDepartment}
                            onChange={(department) => {
                                onPatch({
                                    privacyDepartmentId: department?.id ?? null,
                                });
                            }}
                            required
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
                            label="Allgemeiner Teil der Barrierefreiheitserklärung"
                            value={accessibilityDepartment}
                            onChange={(department) => {
                                onPatch({
                                    accessibilityDepartmentId: department?.id ?? null,
                                });
                            }}
                            required
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
                    <RichTextInputComponent
                        label="Formularspezifischer Teil der Datenschutzerklärung"
                        value={form.formSpecificPrivacyStatement}
                        hint="Beschreiben Sie hier die formularspezifischen Datenschutzinformationen nach Art. 13 DSGVO, insbesondere konkret verarbeitete Daten, Zwecke, Rechtsgrundlagen, Empfänger, Speicherdauer und zuständige Stellen. Zusammen mit dem allgemeinen Teil der ausgewählten Organisationseinheit ergibt sich die vollständige Datenschutzerklärung für dieses Formular."
                        onChange={(value) => {
                            onPatch({
                                formSpecificPrivacyStatement: value,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <RichTextInputComponent
                        label="Formularspezifischer Teil der Barrierefreiheitserklärung"
                        value={form.formSpecificAccessibilityStatement}
                        onChange={(value) => {
                            onPatch({
                                formSpecificAccessibilityStatement: value,
                            });
                        }}
                        disabled={!props.editable}
                    />
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
                        sx={{
                            display: "flex",
                            alignItems: "center"
                        }}>
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
                                            <li>Das globale Erscheinungsbild der Prosuna-Instanz</li>
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
                        sx={{
                            display: "flex",
                            alignItems: "center"
                        }}>
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
                            disabled={!props.editable}
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
                            onChange={(department) => {
                                onPatch({
                                    legalSupportDepartmentId: department?.id ?? null,
                                });
                            }}
                            required
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
                            onChange={(department) => {
                                onPatch({
                                    technicalSupportDepartmentId: department?.id ?? null,
                                });
                            }}
                            required
                            disabled={!props.editable}
                        />
                    }
                </Grid>
            </Grid>
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
