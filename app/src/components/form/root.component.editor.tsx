import React, {useEffect, useMemo, useState} from 'react';
import {Box, Grid, Skeleton} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {TextFieldComponent} from '../text-field/text-field-component';
import {Link} from 'react-router-dom';
import {Hint} from '../hint/hint';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {withDelay} from '../../utils/with-delay';
import {AssetSelector} from '../../modules/assets/components/asset-selector';
import {type VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../../modules/departments/services/v-department-shadowed-api-service';
import {DepartmentSelectField} from '../../modules/departments/components/department-select-field';

export function RootComponentEditor(props: BaseEditorProps<FormLayoutElement>) {
    const dispatch = useAppDispatch();

    const {
        element: form,
        onPatch,
    } = props;

    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[] | null>(null);

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

    }, [dispatch]);

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
                        hint="Dieser Titel wird öffentlich für das Formular verwendet und ausfüllenden Personen angezeigt."
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
                title="Organisationseinheiten im Formular"
                variant="h5"
            >
                Wählen Sie bei Bedarf die verantwortliche und die betreuende Organisationseinheit aus.
                Ihre Anschriften werden in den optionalen Abschnitten „Allgemeine Informationen“ und
                „Abschließende Informationen und Captcha“ angezeigt.
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
                            label="Verantwortliche Organisationseinheit"
                            hint="Die hinterlegte Anschrift wird als „Verantwortliche Stelle“ angezeigt."
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
                            label="Betreuende Organisationseinheit"
                            hint="Die hinterlegte Anschrift wird als „Betreuende Stelle“ angezeigt."
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
                title="PDF-Generierung"
                variant="h5"
            >
                Wählen Sie bei Bedarf eine PDF-Vorlage für die Kopie der übermittelten Angaben und den Formularvordruck.
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
                        externalAction={
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
                                            Die Vorlage wird für die Kopie der Angaben verwendet, die Beteiligte und
                                            Mitarbeiter:innen als PDF erhalten.
                                            Auch der Vordruck des Formulars verwendet die ausgewählte Vorlage.
                                        </p>
                                        <p>
                                            Vorlagen können im Bereich <Link to="/assets">Dateien und Medien</Link> hochgeladen
                                            werden.
                                        </p>
                                    </>
                                }
                            />
                        }
                    />
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Hinweise zur Abgabe des Formularvordrucks"
                variant="h5"
            >
                Diese Angaben werden für den PDF-Vordruck des Formulars genutzt. Sie sind nicht relevant, wenn
                ausschließlich eine Onlineübermittlung zugelassen wird.
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
                        hint="Wenn Sie dieses Formular als Vordruck bereitstellen, geben Sie hier an, wie und an welche Post- oder E-Mail-Adresse das ausgefüllte Formular übermittelt werden kann."
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
