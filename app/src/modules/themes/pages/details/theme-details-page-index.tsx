import {Alert, AlertTitle, Box, Button, Divider, Grid, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {isAdmin} from '../../../../utils/is-admin';
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import {useChangeBlocker} from "../../../../hooks/use-change-blocker";
import {useFormManager} from '../../../../hooks/use-form-manager';
import {FormsApiService} from "../../../forms/forms-api-service";
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";
import {ConstraintDialog} from "../../../../dialogs/constraint-dialog/constraint-dialog";
import {ConstraintLinkProps} from "../../../../dialogs/constraint-dialog/constraint-link-props";
import * as yup from "yup";
import {AlertComponent} from "../../../../components/alert/alert-component";
import AccessibilityNewIcon from "@mui/icons-material/AccessibilityNew";
import {PresetColor} from "react-color/lib/components/sketch/Sketch";
import {SketchPicker} from "react-color";
import ContrastOutlinedIcon from "@mui/icons-material/ContrastOutlined";
import {calculateContrastRatio} from "../../../../utils/calculate-contrast-ratio";
import type {Theme} from "../../models/theme";
import {ThemesApiService} from "../../themes-api-service";
import {useAppSelector} from "../../../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../../../slices/system-config-slice";
import {SystemConfigKeys} from "../../../../data/system-config-keys";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";

export const ThemeSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, "Der Name des Farbschemas muss mindestens 3 Zeichen lang sein.")
        .max(96, "Der Name des Farbschemas darf maximal 96 Zeichen lang sein.")
        .required("Der Name des Farbschemas ist ein Pflichtfeld."),
});

export function ThemeDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Theme, undefined>;

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<Theme>(item, ThemeSchema as any);

    const apiService = useMemo(() => new ThemesApiService(api), [api]);
    const theme = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);

    const themeId = useParams().id;
    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [showConstraintDefaultThemeDialog, setConstraintDefaultThemeDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedApplications, setRelatedApplications] = useState<ConstraintLinkProps[] | undefined>(undefined);

    if (theme == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (theme != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar("Bitte überprüfen Sie Ihre Eingaben."));
                return;
            }

            setIsBusy(true);

            if (theme.id === 0) {
                apiService
                    .create(theme)
                    .then((newTheme) => {
                        setItem(newTheme);
                        reset();

                        dispatch(showSuccessSnackbar('Neues Farbschema erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/themes/${newTheme.id}`, { replace: true });
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
                    .update(theme.id, theme)
                    .then((updatedTheme) => {
                        setItem(updatedTheme);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an Farbschema erfolgreich gespeichert.'));
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            }
        }
    };

    const checkAndHandleDelete = async () => {
        if (theme.id === 0) return;

        setIsBusy(true);
        try {
            const uniqueForms = await new FormsApiService(api)
                .list(0, 999, undefined, undefined, {themeId: parseInt(themeId ?? "")})

            if (uniqueForms.content.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = uniqueForms.content.slice(0, maxVisibleLinks).map(f => ({
                    label: f.title,
                    to: `/forms/${f.id}`
                }));

                if (uniqueForms.content.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: "Weitere Formulare anzeigen…",
                        to: `/themes/${theme.id}/forms`
                    });
                }

                setRelatedApplications(processedLinks);
                setShowConstraintDialog(true);
            } else if(themeId === appThemeId) {
                setConstraintDefaultThemeDialog(true);
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
        if (theme.id === 0) return;

        setIsBusy(true);
        apiService.destroy(theme.id)
            .then(() => {
                navigate('/themes', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Das Farbschema wurde erfolgreich gelöscht.'));
            })
            .catch(() => dispatch(showErrorSnackbar('Beim Löschen ist ein Fehler aufgetreten.')))
            .finally(() => setIsBusy(false));
    };

    return (
        <Box>
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
                        label="Name des Farbschemas"
                        value={theme.name}
                        onChange={handleInputChange("name")}
                        onBlur={handleInputBlur("name")}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        error={errors.name}
                        hint={"Eine interne Bezeichnung für Mitarbeiter:innen."}
                    />
                </Grid>
            </Grid>

            {
                themeId === appThemeId &&
                <AlertComponent
                    color="info"
                    title="Dies ist das aktive Farbschema der Gover-Instanz"
                >
                    <Box sx={{maxWidth: 860}}>
                        Bitte beachten Sie, dass sich Änderungen an diesem Farbschema auf die ganze Gover-Instanz
                        auswirken.
                        Sie können die Zuweisung als Farbschema für die Gover-Instanz in den <Link
                        to={'/settings'}
                        style={{color: 'inherit'}}
                            >Systemeinstellungen</Link> ändern.
                    </Box>
                </AlertComponent>
            }

            <Typography
                variant="h5"
                sx={{
                    mt: 4,
                    mb: 1,
                }}
            >
                Auswahl der Farben
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Wählen Sie die Farben für das Farbschema aus. Die Farben werden in der Anwendung
                verwendet, um die Benutzeroberfläche zu gestalten. Bitte beachten Sie auch die Hinweise zur Barrierefreiheit.
            </Typography>

            <Grid
                container
                columnSpacing={4}
                rowSpacing={4}
                sx={{
                    mt: 2,
                }}
            >
                <ColorPicker
                    label="Primärfarbe"
                    value={theme?.main}
                    contrastColor={'#EEF2EE'}
                    contrastColorLabel={'hellgrau'}
                    onChange={handleInputChange("main")}
                />

                <ColorPicker
                    label="Primärfarbe (Dunkel)"
                    value={theme?.mainDark}
                    onChange={handleInputChange("mainDark")}
                />

                <ColorPicker
                    label="Akzentfarbe"
                    value={theme?.accent}
                    contrastColor={theme?.mainDark}
                    contrastColorLabel={'Primär/dunkel'}
                    onChange={handleInputChange("accent")}
                />
            </Grid>

            <Divider
                sx={{
                    my: 8,
                }}
            />

            <Grid
                container
                columnSpacing={4}
                rowSpacing={4}
            >
                <ColorPicker
                    label="Fehlerfarbe"
                    value={theme?.error}
                    onChange={handleInputChange("error")}
                />

                <ColorPicker
                    label="Warnungsfarbe"
                    value={theme?.warning}
                    onChange={handleInputChange("warning")}
                />

                <ColorPicker
                    label="Informationsfarbe"
                    value={theme?.info}
                    onChange={handleInputChange("info")}
                />

                <ColorPicker
                    label="Erfolgsfarbe"
                    value={theme?.success}
                    onChange={handleInputChange("success")}
                />
            </Grid>

            <Alert
                severity="info"
                sx={{mt: 4}}
                icon={<AccessibilityNewIcon />}
            >
                <AlertTitle>Hinweis zur Barrierefreiheit des Farbschemas</AlertTitle>
                <Typography sx={{maxWidth: 860}}>
                    Bitte beachten Sie, dass ausgewählte Farben ein Kontrastverhältnis von mindestens 4.5:1 aufweisen müssen,
                    um den Anforderungen der Barrierefreiheit gemäß der <abbr title={'Web Content Accessibility Guidelines'}>WCAG</abbr> 2.1 (AA) zu entsprechen.
                    Hierbei gilt der Kontrast von der gewählten Farbe zur Vorder- (i.d.R. Text) oder Hintergrundfarbe.
                </Typography>
            </Alert>

            {
                userIsAdmin &&
                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 4,
                        gap: 2,
                    }}
                >
                    <Button
                        onClick={handleSave}
                        disabled={isBusy || hasNotChanged}
                        variant="contained"
                        color="primary"
                        startIcon={<SaveOutlinedIcon />}
                    >
                        Speichern
                    </Button>

                    {
                        theme.id !== 0 &&
                        <Button
                            onClick={() => {
                                reset();
                            }}
                            disabled={isBusy || hasNotChanged}
                            color="error"
                        >
                            Zurücksetzen
                        </Button>
                    }

                    {
                        theme.id !== 0 &&
                        <Button
                            variant={'outlined'}
                            onClick={checkAndHandleDelete}
                            disabled={isBusy}
                            color="error"
                            sx={{
                                marginLeft: 'auto',
                            }}
                            startIcon={<DeleteOutlinedIcon />}
                        >
                            Löschen
                        </Button>
                    }
                </Box>
            }

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Fachbereich löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={theme.name}
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
                message="Dieses Farbschema kann (noch) nicht gelöscht werden, da es aktuell von einem oder mehreren Formularen verwendet wird."
                solutionText="Bitte konfigurieren Sie für diese Formulare ein anderes Farbschema und versuchen Sie es erneut:"
                links={relatedApplications}
            />

            <ConstraintDialog
                open={showConstraintDefaultThemeDialog}
                onClose={() => setConstraintDefaultThemeDialog(false)}
                message="Dieses Farbschema kann (noch) nicht gelöscht werden, da es das aktive Farbschema der Gover-Instanz ist."
                solutionText="Um dieses Farbschema löschen zu können, müssen Sie zuerst in den Systemeinstellungen ein anderes Farbschema als Standard festlegen."
                links={[{
                    label: "Systemeinstellungen aufrufen",
                    to: '/settings',
                }]}
            />
        </Box>
    );
}


const colors: PresetColor[] = [
    {
        title: 'Standard-Primärfarbe',
        color: '#253B5B',
    },
    {
        title: 'Standard-Primärfarbe (Dunkel)',
        color: '#102334',
    },
    {
        title: 'Standard-Akzent',
        color: '#F8D27C',
    },
    {
        title: 'Standard-Fehler',
        color: '#CD362D',
    },
    {
        title: 'Standard-Warnung',
        color: '#B55E06',
    },
    {
        title: 'Standard-Info',
        color: '#1F7894',
    },
    {
        title: 'Standard-Erfolg',
        color: '#378550',
    },


    {
        title: 'Rot',
        color: '#f44336',
    },
    {
        title: 'Rot (Dunkel)',
        color: '#aa2e25',
    },
    {
        title: 'Rot (Akzentuiert)',
        color: '#ff1744',
    },

    {
        title: 'Pink',
        color: '#e91e63',
    },
    {
        title: 'Pink (Dunkel)',
        color: '#a31545',
    },
    {
        title: 'Pink (Akzentuiert)',
        color: '#f50057',
    },

    {
        title: 'Flieder',
        color: '#9c27b0',
    },
    {
        title: 'Flieder (Dunkel)',
        color: '#6d1b7b',
    },
    {
        title: 'Flieder (Akzentuiert)',
        color: '#d500f9',
    },

    {
        title: 'Violett',
        color: '#673ab7',
    },
    {
        title: 'Violett (Dunkel)',
        color: '#482880',
    },
    {
        title: 'Violett (Akzentuiert)',
        color: '#651fff',
    },

    {
        title: 'Indigo',
        color: '#3f51b5',
    },
    {
        title: 'Indigo (Dunkel)',
        color: '#2c387e',
    },
    {
        title: 'Indigo (Akzentuiert)',
        color: '#3d5afe',
    },

    {
        title: 'Blau',
        color: '#2196f3',
    },
    {
        title: 'Blau (Dunkel)',
        color: '#1769aa',
    },
    {
        title: 'Blau (Akzentuiert)',
        color: '#2979ff',
    },

    {
        title: 'Cyan',
        color: '#00bcd4',
    },
    {
        title: 'Cyan (Dunkel)',
        color: '#008394',
    },
    {
        title: 'Cyan (Akzentuiert)',
        color: '#00e5ff',
    },

    {
        title: 'Blaugrün',
        color: '#009688',
    },
    {
        title: 'Blaugrün (Dunkel)',
        color: '#00695f',
    },
    {
        title: 'Blaugrün (Akzentuiert)',
        color: '#1de9b6',
    },

    {
        title: 'Grün',
        color: '#4caf50',
    },
    {
        title: 'Grün (Dunkel)',
        color: '#357a38',
    },
    {
        title: 'Grün (Akzentuiert)',
        color: '#00e676',
    },

    {
        title: 'Limette',
        color: '#cddc39',
    },
    {
        title: 'Limette (Dunkel)',
        color: '#8f9a27',
    },
    {
        title: 'Limette (Akzentuiert)',
        color: '#c6ff00',
    },

    {
        title: 'Gelb',
        color: '#ffeb3b',
    },
    {
        title: 'Gelb (Dunkel)',
        color: '#b2a429',
    },
    {
        title: 'Gelb (Akzentuiert)',
        color: '#ffea00',
    },

    {
        title: 'Bernstein',
        color: '#ffc107',
    },
    {
        title: 'Bernstein (Dunkel)',
        color: '#b28704',
    },
    {
        title: 'Bernstein (Akzentuiert)',
        color: '#ffc400',
    },

    {
        title: 'Orange',
        color: '#ff5722',
    },
    {
        title: 'Orange (Dunkel)',
        color: '#b23c17',
    },
    {
        title: 'Orange (Dunkel)',
        color: '#ff3d00',
    },
];

function ColorPicker({
                         label,
                         value,
                         onChange,
                         contrastColor,
                         contrastColorLabel,
                     }: {
    label: string;
    value?: string;
    onChange: (val: string) => void;
    contrastColor?: string;
    contrastColorLabel?: string;
}): JSX.Element {
    return (
        <Grid
            item
            xs={12}
            md={6}
            lg={4}
        >
            <Box
                sx={{
                    mb: 1,
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                <Box
                    sx={{
                        mr: 1,
                        width: '1em',
                        height: '1em',
                        borderRadius: '100%',
                        backgroundColor: value,
                    }}
                />

                <Typography
                    variant="subtitle1"
                    component="h2"
                >
                    {label}
                </Typography>
            </Box>

            <SketchPicker
                color={value}
                onChange={(color) => {
                    onChange(color.hex);
                }}
                disableAlpha={true}
                width="256px"
                styles={{
                    default: {
                        picker: {
                            boxShadow: 'none',
                            border: '1px solid #ccc',
                        },
                    },
                }}
                presetColors={colors}
            />
            <Typography
                variant="caption"
                color={'text.secondary'}
                sx={{mt: '8px', display: 'inline-block'}}
            >
                <ContrastOutlinedIcon sx={{fontSize: '0.875rem', transform: 'translateY(2px)', mr: '4px', ml: '10px'}} /> Kontrastverhältnis: {calculateContrastRatio(value ?? '#000000', contrastColor ?? '#ffffff')}:1
                (zu {contrastColorLabel ?? 'weiß'})
            </Typography>
        </Grid>
    );
}
