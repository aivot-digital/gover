import React, {useEffect, useState} from 'react';
import {Box, Grid, Skeleton} from '@mui/material';
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
import {ThemesApiService} from '../../modules/themes/themes-api-service';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import {downloadQrCode} from '../../utils/download-qrcode';
import {FormType, FormTypeDescriptions, FormTypeLabels, FormTypes} from '../../modules/forms/enums/form-type';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {createCustomerPath} from '../../utils/url-path-utils';
import {withDelay} from '../../utils/with-delay';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';

export function RootComponentEditor(props: BaseEditorProps<RootElement, Application>) {
    const dispatch = useAppDispatch();
    const api = useApi();

    const [departments, setDepartments] = useState<SelectFieldComponentOption[] | null>(null);
    const [themes, setThemes] = useState<SelectFieldComponentOption[] | null>(null);
    const [templateOptions, setTemplateOptions] = useState<SelectFieldComponentOption[] | null>(null);

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
            new DepartmentApiService().listAll(),
            600,
        )
            .then((deps) => deps.content.map((department) => ({
                value: department.id.toString(),
                label: department.name,
            })))
            .then(setDepartments)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Fachbereiche!'));
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
                dispatch(showErrorSnackbar('Fehler beim Laden der Farbschemata!'));
            });

        withDelay(new AssetsApiService(api)
            .listAll({contentType: 'text/html'}), 600)
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

    const generalLink = createCustomerPath(`${props.entity?.slug ?? ''}`);
    const versionedLink = createCustomerPath(`${props.entity?.slug ?? ''}/${props.entity?.version ?? ''}`);

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
                        value={props.entity?.internalTitle}
                        label="Interner Titel des Formulars"
                        hint="Dieser Titel wird intern in Gover verwendet und ist nicht öffentlich sichtbar."
                        onChange={(val) => {
                            props.onPatchEntity({
                                internalTitle: val,
                            });
                        }}
                        minCharacters={1}
                        maxCharacters={96}
                        disabled={!props.editable}
                        required={true}
                        error={
                            !props.entity?.internalTitle || props.entity.internalTitle.length < 1
                                ? 'Der Titel muss mindestens 1 Zeichen lang sein.'
                                : props.entity.internalTitle.length > 96
                                    ? 'Der Titel darf maximal 96 Zeichen lang sein.'
                                    : undefined
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
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        value={props.entity.publicTitle}
                        label="Öffentlicher Titel & Überschrift des Formulars"
                        multiline
                        hint="Dieser Titel wird öffentlich für das Formular verwendet und ggü. Anstragstellenden angezeigt."
                        onChange={(val) => {
                            props.onPatchEntity({
                                publicTitle: val,
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
                                        await handleDownloadQrCode(versionedLink, `qr-code-${props.entity?.slug ?? ''}-${(props.entity?.version ?? '')}.png`);
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
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Für dieses Formular zuständige Fachbereiche"
                variant="h5"
            >
                Hinterlegen Sie die für dieses Formular zuständigen Fachbereiche. Der Zuständige Fachbereich hat die inhaltliche Hoheit über das Formular, während der Bewirtschaftende Fachbereich die eingegangenen Anträge bearbeitet (falls
                abweichend).
            </ElementEditorSectionHeader>
            <Grid
                container
                columnSpacing={4}
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
                    }
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Erscheinungsbild"
                variant="h5"
            >
                Hinterlegen Sie bei Bedarf ein abweichendes Farbschema und wählen Sie ggf. eine PDF-Vorlage, welche zur Generierung des Formulars zur Offline-Einreichung verwendet wird.
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
                        }
                        <Hint
                            summary="Sie können ein abweichendes Farbschema für dieses Formular auswählen."
                            detailsTitle="Farbschema"
                            details={
                                <>
                                    <p>
                                        Sie können hier ein abweichendes Farbschema für dieses Formular auswählen.
                                    </p>
                                    <p>
                                        Farbschemata werden immer nach absteigendem Prioritätsprinzip angewendet.
                                        Das bedeutet, dass das Farbschema mit der niedrigsten Nummer in der folgenden Liste angewendet wird:

                                        <ol>
                                            <li>Das Farbschema des Formulars</li>
                                            <li>Das Farbschema des zuständigen Fachbereichs</li>
                                            <li>Das Farbschema des bewirtschaftenden Fachbereichs</li>
                                            <li>Das Farbschema des entwickelnden Fachbereichs</li>
                                            <li>Das globale Farbschema der Gover-Instanz</li>
                                        </ol>
                                    </p>
                                    <p>
                                        Das Farbschema setzt die Farben sowie die Logos des Formulars.
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
                        {
                            templateOptions == null &&
                            <Skeleton
                                width="100%"
                                height={80}
                            />
                        }
                        {
                            templateOptions != null &&
                            <SelectFieldComponent
                                label="PDF-Vorlage"
                                value={props.entity?.pdfTemplateKey ?? undefined}
                                onChange={(val) => {
                                    props.onPatchEntity({
                                        pdfTemplateKey: val,
                                    });
                                }}
                                options={templateOptions}
                                disabled={!props.editable}
                            />
                        }

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
            <ElementEditorSectionHeader
                title="Fristen"
                variant="h5"
            >
                Geben Sie die für diesen Antrag gültigen Fristen ein, welche den Antragstellenden im Formular angezeigt werden.
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
                Kontaktinformationen werden auf Fachbereichs-Ebene hinterlegt und verwaltet. Sie können hier die Fachbereiche auswählen, deren Kontakt Sie für dieses Formular verwenden und anzeigen möchten.
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
                    }
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Hinweise zur Offline-Einreichung"
                variant="h5"
            >
                Diese Angaben werden für den PDF-Vordruck des Formulars genutzt. Sie sind nicht relevant, wenn ausschließlich eine Online-Einreichung zugelassen wird.
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
