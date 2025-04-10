import React, {useEffect, useState} from 'react';
import {Box, Grid, Typography} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {TextFieldComponent} from '../text-field/text-field-component';
import {type SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {Form as Application} from '../../models/entities/form';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {useApi} from '../../hooks/use-api';
import {Link} from 'react-router-dom';
import {Hint} from '../hint/hint';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';
import {ThemesApiService} from '../../modules/themes/themes-api-service';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import {downloadQrCode} from '../../utils/download-qrcode';
import {FormType, FormTypeDescriptions, FormTypeLabels, FormTypes} from '../../modules/forms/enums/form-type';

export function RootComponentEditor(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();

    const [departments, setDepartments] = useState<SelectFieldComponentOption[]>([]);
    const [themes, setThemes] = useState<SelectFieldComponentOption[]>([]);
    const [templateOptions, setTemplateOptions] = useState<SelectFieldComponentOption[]>([]);

    const handleDownloadQrCode = async (link: string, filename: string) => {
        try {
            await downloadQrCode(link, filename);
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
    };

    useEffect(() => {
        new DepartmentsApiService(api)
            .list(0, 999, undefined, undefined, {
                ignoreMemberships: true,
            })
            .then((deps) => deps.content.map((department) => ({
                value: department.id.toString(),
                label: department.name,
            })))
            .then(setDepartments)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Fachbereiche!'));
            });

        new ThemesApiService(api)
            .list(0, 999, undefined, undefined, {})
            .then((themes) => themes.content.map((theme) => ({
                value: theme.id.toString(),
                label: theme.name,
            })))
            .then(setThemes)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Farbschemata!'));
            });

        new AssetsApiService(api)
            .list(0, 999, undefined, undefined, {contentType: 'text/html'})
            .then((assets) => assets.content.map((asset) => ({
                value: asset.key,
                label: asset.filename,
            })))
            .then(setTemplateOptions)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der PDF-Vorlagen!'));
            });
    }, []);

    const generalLink = `${window.location.protocol}//${window.location.host}/${props.entity?.slug ?? ''}`;
    const versionedLink = `${window.location.protocol}//${window.location.host}/${props.entity?.slug ?? ''}/${props.entity?.version ?? ''}`;

    return (
        <>
            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Über dieses Formular
            </Typography>

            <TextFieldComponent
                value={props.entity?.title}
                label="Interner Titel des Formulars"
                hint="Dieser Titel wird intern in Gover verwendet und ist nicht öffentlich sichtbar."
                onChange={(val) => {
                    props.onPatchEntity({
                        title: val,
                    });
                }}
                minCharacters={1}
                maxCharacters={96}
                disabled={!props.editable}
                required={true}
                error={
                    !props.entity?.title || props.entity.title.length < 1
                        ? "Der Titel muss mindestens 1 Zeichen lang sein."
                        : props.entity.title.length > 96
                            ? "Der Titel darf maximal 96 Zeichen lang sein."
                            : undefined
                }
            />

            <TextFieldComponent
                value={props.element.headline}
                label="Öffentlicher Titel & Überschrift des Formulars"
                multiline
                hint="Dieser Titel wird öffentlicht für das Formular verwendet und ggü. Anstragstellenden angezeigt."
                onChange={(val) => {
                    props.onPatch({
                        headline: val,
                    });
                }}
                rows={3}
                maxCharacters={120}
                required
                disabled={!props.editable}
            />

            <TextFieldComponent
                value={props.element.tabTitle}
                label="Titel des Formulars im Browser-Tab"
                hint="Dieser Titel (oftmals eine Kurzform) erscheint als Bezeichnung des Formulars im Tab des Webbrowsers. Wenn Sie keinen spezifischen Titel angeben, wird die Überschrift des Formulars verwendet."
                onChange={(val) => {
                    props.onPatch({
                        tabTitle: val,
                    });
                }}
                maxCharacters={60}
                disabled={!props.editable}
            />

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={6}
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
                                    icon: <ContentPasteOutlinedIcon />,
                                    tooltip: 'Link in Zwischenablage kopieren',
                                    onClick: () => {
                                        navigator
                                            .clipboard
                                            .writeText(generalLink)
                                            .then(() => {
                                                dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                            })
                                            .catch((err) => {
                                                console.error(err);
                                                dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                            });
                                    },
                                },
                                {
                                    icon: <QrCode2OutlinedIcon />,
                                    tooltip: 'QR-Code herunterladen',
                                    onClick: async () => {
                                        await handleDownloadQrCode(generalLink, `qr-code-${props.entity?.slug ?? ''}.png`);
                                    },
                                },
                            ]
                        }
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={6}
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
                                    icon: <ContentPasteOutlinedIcon />,
                                    tooltip: 'Link in Zwischenablage kopieren',
                                    onClick: () => {
                                        navigator
                                            .clipboard
                                            .writeText(versionedLink)
                                            .then(() => {
                                                dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                            })
                                            .catch((err) => {
                                                console.error(err);
                                                dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                            });
                                    },
                                },
                                {
                                    icon: <QrCode2OutlinedIcon />,
                                    tooltip: 'QR-Code herunterladen',
                                    onClick: async () => {
                                        await handleDownloadQrCode(versionedLink, `qr-code-${props.entity?.slug ?? ''}-${(props.entity?.version ?? '').replace(/\./g, '-')}.png`);
                                    },
                                },
                            ]
                        }
                    />
                </Grid>
            </Grid>

            <SelectFieldComponent
                label="Art des Formulars"
                hint="Öffentliche Formulare werden auf der Übersichtsseite angezeigt und können von Bürger:innen ausgefüllt werden. Interne Formulare werden nicht auf der Übersichtsseite angezeigt, können aber über den Link geteilt werden."
                value={(props.entity.type ?? FormType.Public).toString()}
                required={true}
                onChange={(val) => {
                    props.onPatchEntity({
                        type: val != null ? parseInt(val) : FormType.Public,
                    });
                }}
                options={FormTypes.map((type) => ({
                    value: type.toString(),
                    label: FormTypeLabels[type],
                    subLabel: FormTypeDescriptions[type],
                }))}
                disabled={!props.editable}
            />

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Zuständige Fachbereiche
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Entwickelnder Fachbereich"
                        value={props.entity?.developingDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                developingDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departments}
                        required
                        /*disabled={!props.editable}*/
                        disabled
                        hint="Dieser Fachbereich wurde bei der Erstellung des Formulars festgelegt."
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Zuständiger Fachbereich"
                        value={props.entity?.responsibleDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                responsibleDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departments}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Bewirtschaftender Fachbereich"
                        value={props.entity?.managingDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                managingDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departments}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Erscheinungsbild
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <SelectFieldComponent
                        label="Farbschema (Visuelles Erscheinungsbild)"
                        value={props.entity?.themeId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                themeId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={themes}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                    >
                        <SelectFieldComponent
                            label="PDF-Vorlage"
                            value={props.entity?.pdfBodyTemplateKey ?? undefined}
                            onChange={(val) => {
                                props.onPatchEntity({
                                    pdfBodyTemplateKey: val,
                                });
                            }}
                            options={templateOptions}
                            disabled={!props.editable}
                        />

                        <Hint
                            summary="Sie können eine individuelle Vorlage für die Generierung von PDF-Dokumenten auswählen."
                            detailsTitle="PDF-Vorlage"
                            details={
                                <>
                                    <p>
                                        Sie können eine individuelle Vorlage für die Generierung von PDF-Dokumenten auswählen.
                                    </p>
                                    <p>
                                        Die Vorlage wird für das PDF des eingereichten Antrags (welches antragstellende Personen und
                                        Mitarbeiter:innen der Verwaltung erhalten) verwendet.
                                        Auch der Vordruck des Formulars verwendet die ausgewählte Vorlage.
                                    </p>
                                    <p>
                                        Vorlagen können im Bereich <Link to="/assets">Dokumente &
                                        Medieninhalte</Link> hochgeladen werden.
                                    </p>
                                </>
                            }
                            sx={{ml: 2}}
                        />
                    </Box>
                </Grid>
            </Grid>

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Fristen
            </Typography>

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

            <Typography
                variant="h5"
                sx={{
                    mt: 4,
                }}
            >
                Kontakte
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <SelectFieldComponent
                        label="Fachlicher Support"
                        value={props.entity?.legalSupportDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                legalSupportDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departments}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <SelectFieldComponent
                        label="Technischer Support"
                        value={props.entity?.technicalSupportDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                technicalSupportDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departments}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>

            <Typography
                variant="caption"
                color={'text.secondary'}
            >
                Kontakte werden auf Fachbereichs-Ebene hinterlegt und verwaltet. Sie können hier die Fachbereiche auswählen, deren Kontakt Sie verwenden und anzeigen möchten.
            </Typography>

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Hinweise zur Offline-Einreichung
            </Typography>

            <RichTextEditorComponentView
                hint="Wenn Sie dieses Formular als Vordruck z.B. zum Ausfüllen auf Papier, bereitstellen möchten, sollten Sie hier die Adresse und/oder E-Mail etc. nennen, an welche das Formular einzureichen ist."
                value={props.element.offlineSubmissionText ?? ''}
                onChange={val => {
                    props.onPatch({
                        offlineSubmissionText: val,
                    });
                }}
                disabled={!props.editable}
            />

            <CheckboxFieldComponent
                label="Das Formular erfordert eine Unterschrift"
                hint="Wenn diese Option aktiviert ist, wird im PDF-Vordruck ein Unterschriftenfeld eingefügt. Dies erfolgt nur, sofern die PDF-Vorlage dies unterstützt."
                value={props.element.offlineSignatureNeeded}
                onChange={val => {
                    props.onPatch({
                        offlineSignatureNeeded: val,
                    });
                }}
                disabled={!props.editable}
            />
        </>
    );
}
