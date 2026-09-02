import {
    Alert,
    AlertTitle,
    Box,
    Button,
    Divider,
    FormControlLabel,
    Grid,
    IconButton,
    Stack,
    Switch,
    Tab,
    Tabs,
    TextField,
    ThemeProvider,
    Tooltip,
    Typography,
} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import * as yup from 'yup';
import {AlertComponent} from '../../../../components/alert/alert-component';
import AccessibilityNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/AccessibilityNew';
import DashboardOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Dashboard';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import SettingsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Settings';
import EditOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import type {Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ImageSelector} from '../../../assets/components/image-selector';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {
    useHasAnyDepartmentPermission,
    useHasSystemPermission,
} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {resolveAppearanceColors} from '../../../../theming/resolve-appearance-colors';
import {createAppTheme, resolveAppBackgroundColors} from '../../../../theming/themes';
import {BaseTheme} from '../../../../theming/base-theme';
import {type Theme as MuiTheme} from '@mui/material/styles';
import {ThemeColorPicker} from '../../components/theme-color-picker';
import StarOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Star';
import {useSetDefaultTheme} from '../../hooks/use-set-default-theme';
import {DepartmentApiService} from '../../../departments/services/department-api-service';
import {AssetsApiService} from '../../../assets/assets-api-service';
import {resolveThemeLogoKey} from '../../../../theming/resolve-theme-logo';

export const ThemeSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Erscheinungsbildes muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Erscheinungsbildes darf maximal 96 Zeichen lang sein.')
        .required('Der Name des Erscheinungsbildes ist ein Pflichtfeld.'),
    primaryColor: yup.string()
        .matches(/^#[0-9a-f]{6}$/i, 'Die Markenfarbe muss eine gültige HEX-Farbe sein.')
        .required('Die Markenfarbe ist ein Pflichtfeld.'),
    secondaryColor: yup.string()
        .matches(/^#[0-9a-f]{6}$/i, 'Die Sekundärfarbe muss eine gültige HEX-Farbe sein.')
        .required('Die Sekundärfarbe ist ein Pflichtfeld.'),
    primaryColorDark: yup.string()
        .nullable()
        .matches(/^#[0-9a-f]{6}$/i, 'Die Primärfarbe für das dunkle Farbschema muss eine gültige HEX-Farbe sein.'),
    secondaryColorDark: yup.string()
        .nullable()
        .matches(/^#[0-9a-f]{6}$/i, 'Die Sekundärfarbe für das dunkle Farbschema muss eine gültige HEX-Farbe sein.'),
});

export function ThemeDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
        isNewItem,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Theme, undefined>;
    const isNewTheme = isNewItem === true;
    const editPermission = isNewTheme ? Permission.THEME_CREATE : Permission.THEME_UPDATE;
    const canDeleteTheme = useHasSystemPermission(Permission.THEME_DELETE);
    const canReadDepartments = useHasAnyDepartmentPermission(Permission.DEPARTMENT_READ);
    const {
        canSetDefaultTheme,
        setDefaultThemeDisabledTooltip,
        isSettingDefaultTheme,
        setDefaultTheme,
    } = useSetDefaultTheme();

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
        handleInputPatch,
    } = useFormManager<Theme>(item, ThemeSchema as any);

    const apiService = useMemo(() => new ThemesApiService(api), [api]);
    const theme = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);
    const lightPreviewTheme = useMemo(
        () => theme == null ? null : createAppTheme(theme, BaseTheme, 'light'),
        [theme],
    );
    const darkPreviewTheme = useMemo(
        () => theme == null ? null : createAppTheme(theme, BaseTheme, 'dark'),
        [theme],
    );
    const lightPreviewLogoKey = theme == null ? null : resolveThemeLogoKey(theme, 'light');
    const darkPreviewLogoKey = theme == null ? null : resolveThemeLogoKey(theme, 'dark');
    const lightPreviewLogoUrl = lightPreviewLogoKey == null
        ? null
        : AssetsApiService.useAssetLink(lightPreviewLogoKey);
    const darkPreviewLogoUrl = darkPreviewLogoKey == null
        ? null
        : AssetsApiService.useAssetLink(darkPreviewLogoKey);

    const appThemeId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.system.theme));

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [showConstraintDefaultThemeDialog, setConstraintDefaultThemeDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedDepartments, setRelatedDepartments] = useState<ConstraintLinkProps[] | undefined>(undefined);

    if (theme == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const isDefaultTheme = theme.id.toString() === appThemeId;
    const saveDisabledByPermission = !isEditable;
    const saveDisabledTooltip = saveDisabledByPermission
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledByPermission = !canDeleteTheme;
    const deleteDisabledTooltip = deleteDisabledByPermission
        ? formatMissingPermissionTooltip(Permission.THEME_DELETE)
        : undefined;
    const setDefaultThemeActionDisabled = !canSetDefaultTheme || !hasNotChanged || isBusy || isSettingDefaultTheme;
    const setDefaultThemeActionDisabledTooltip = !canSetDefaultTheme
        ? setDefaultThemeDisabledTooltip
        : !hasNotChanged
            ? 'Speichern Sie Ihre Änderungen, bevor Sie dieses Erscheinungsbild als Standard festlegen.'
            : isBusy || isSettingDefaultTheme
                ? 'Bitte warten Sie, bis der aktuelle Vorgang abgeschlossen ist.'
                : undefined;
    const usesDarkModeColors = theme.primaryColorDark != null || theme.secondaryColorDark != null;

    const handleDarkModeColorsToggle = (enabled: boolean) => {
        if (!enabled) {
            handleInputPatch({
                primaryColorDark: null,
                secondaryColorDark: null,
            });
            return;
        }

        const darkColorInput = {
            primaryColor: theme.primaryColorDark ?? theme.primaryColor,
            secondaryColor: theme.secondaryColorDark ?? theme.secondaryColor,
        };
        const darkBackground = resolveAppBackgroundColors('dark', darkColorInput.primaryColor);
        const suggestedColors = resolveAppearanceColors(darkColorInput, darkBackground.paper);
        handleInputPatch({
            primaryColorDark: theme.primaryColorDark ?? suggestedColors.primaryForeground,
            secondaryColorDark: theme.secondaryColorDark ?? suggestedColors.secondaryForeground,
        });
    };

    const handleSave = () => {
        if (theme != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (isNewTheme) {
                apiService
                    .create(theme)
                    .then((newTheme) => {
                        setItem(newTheme);
                        reset();

                        dispatch(showSuccessSnackbar('Neues Erscheinungsbild erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/themes/${newTheme.id}`, {replace: true});
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

                        dispatch(showSuccessSnackbar('Änderungen am Erscheinungsbild erfolgreich gespeichert.'));
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
        if (isNewTheme) return;

        if (isDefaultTheme) {
            setConstraintDefaultThemeDialog(true);
            return;
        }

        if (!canReadDepartments) {
            setConfirmDeleteAction(() => confirmDelete);
            return;
        }

        setIsBusy(true);
        try {
            const assignedDepartments = await new DepartmentApiService()
                .list(0, 6, 'name', 'ASC', {themeId: theme.id});

            if (assignedDepartments.content.length > 0) {
                const maxVisibleLinks = 5;
                const processedLinks = assignedDepartments.content.slice(0, maxVisibleLinks).map(department => ({
                    label: department.name,
                    to: `/departments/${department.id}`,
                }));

                if (assignedDepartments.page.totalElements > maxVisibleLinks) {
                    processedLinks.push({
                        label: 'Weitere Organisationseinheiten anzeigen…',
                        to: `/themes/${theme.id}/departments`,
                    });
                }

                setRelatedDepartments(processedLinks);
                setShowConstraintDialog(true);
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
        if (isNewTheme) return;

        setIsBusy(true);
        apiService.destroy(theme.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/themes', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Das Erscheinungsbild wurde erfolgreich gelöscht.'));
            })
            .catch((error) => dispatch(showApiErrorSnackbar(error, 'Beim Löschen ist ein Fehler aufgetreten.')))
            .finally(() => setIsBusy(false));
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Erscheinungsbild konfigurieren
            </Typography>
            <Typography sx={{mb: 3, maxWidth: 900}}>
                Konfigurieren Sie das Erscheinungsbild, um Namen, Logos, Favicon und Farben für Prosuna und veröffentlichte Formulare festzulegen.
                Die Einstellungen können jederzeit angepasst werden, wirken sich aber unmittelbar auf alle Formulare aus, die dieses Erscheinungsbild verwenden.
            </Typography>

            <Grid
                container
                columnSpacing={4}
                rowSpacing={3}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Name des Erscheinungsbildes"
                        value={theme.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        error={errors.name}
                        hint="Eine interne Bezeichnung für Mitarbeiter:innen."
                        disabled={!isEditable}
                    />
                </Grid>

                <Grid
                    sx={{
                        display: {
                            xs: 'none',
                            lg: 'block',
                        },
                    }}
                    size={{
                        lg: 6,
                    }}
                />

                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ImageSelector
                        label="Logo für helle Hintergründe"
                        hint="Dieses Logo wird im hellen Farbschema sowie in Dokumenten und E-Mails verwendet."
                        selectLabel="Logo für helle Hintergründe auswählen"
                        value={theme.logoKey ?? null}
                        onChange={(key) => {
                            handleInputChange('logoKey')(key);
                        }}
                        size={{
                            aspectRatio: 2, // Default aspect ratio of a logo is 2:1. See logo.tsx
                        }}
                        previewBackgroundColor={lightPreviewTheme?.palette.background.paper}
                        previewForegroundColor={lightPreviewTheme?.palette.text.secondary}
                        previewBorderColor={lightPreviewTheme?.palette.divider}
                        disabled={!isEditable}
                    />
                </Grid>

                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ImageSelector
                        label="Logo für dunkle Hintergründe"
                        hint="Ohne Auswahl wird auch im dunklen Farbschema das Logo für helle Hintergründe verwendet."
                        selectLabel="Logo für dunkle Hintergründe auswählen"
                        value={theme.logoKeyDark ?? null}
                        onChange={(key) => {
                            handleInputChange('logoKeyDark')(key);
                        }}
                        size={{
                            aspectRatio: 2,
                        }}
                        previewBackgroundColor={darkPreviewTheme?.palette.background.paper}
                        previewForegroundColor={darkPreviewTheme?.palette.text.secondary}
                        previewBorderColor={darkPreviewTheme?.palette.divider}
                        disabled={!isEditable}
                    />
                </Grid>

                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ImageSelector
                        label="Favicon des Erscheinungsbildes"
                        hint="Dieses Favicon wird im Browser-Tab angezeigt."
                        selectLabel="Favicon für das Erscheinungsbild auswählen"
                        value={theme.faviconKey ?? null}
                        onChange={(key) => {
                            handleInputChange('faviconKey')(key);
                        }}
                        size={{
                            width: '4rem',
                            height: '4rem',
                        }}
                        disabled={!isEditable}
                    />
                </Grid>
            </Grid>
            {
                !isNewTheme && isDefaultTheme &&
                <AlertComponent
                    color="info"
                    title="Standard-Erscheinungsbild der Prosuna-Instanz"
                    sx={{my: 4}}
                >
                    <Box sx={{maxWidth: 700}}>
                        Dieses Erscheinungsbild wird für die Prosuna-Instanz und überall dort verwendet, wo kein
                        spezifischeres Erscheinungsbild einer Organisationseinheit greift.
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
                Mit diesen Farben übertragen Sie die visuelle Identität Ihrer Organisation auf Prosuna. Wählen Sie
                als Primärfarbe Ihre prägende Markenfarbe für wichtige Aktionen und aktive Bereiche. Die
                Sekundärfarbe sollte sich davon sichtbar unterscheiden und ergänzt weniger zentrale Aktionen. Gut
                unterscheidbare Farben erleichtern die Orientierung und machen die Bedienoberfläche verständlicher.
            </Typography>
            <Box sx={{mt: 4}}>
                <Typography variant="h6" component="h2" sx={{mb: 0.5}}>
                    Helles Farbschema
                </Typography>
                <Typography
                    sx={{
                        color: "text.secondary",
                        mb: 3,
                        maxWidth: 900
                    }}>
                    Diese Farben werden standardmäßig in der gesamten Anwendung verwendet.
                </Typography>
                <Grid container columnSpacing={3} rowSpacing={3} sx={{
                    alignItems: "stretch"
                }}>
                    <Grid size={{xs: 12, md: 6}} sx={{minWidth: 0}}>
                        <ThemeColorPicker
                            label="Primärfarbe"
                            value={theme.primaryColor}
                            contrastTextColor={lightPreviewTheme?.palette.primary.contrastText}
                            contrastBackgroundColor={lightPreviewTheme?.palette.background.paper}
                            onChange={handleInputChange('primaryColor')}
                            disabled={!isEditable}
                            margin="none"
                        />
                    </Grid>

                    <Grid size={{xs: 12, md: 6}} sx={{minWidth: 0}}>
                        <ThemeColorPicker
                            label="Sekundärfarbe"
                            value={theme.secondaryColor}
                            contrastTextColor={lightPreviewTheme?.palette.secondary.contrastText}
                            contrastBackgroundColor={lightPreviewTheme?.palette.background.paper}
                            onChange={handleInputChange('secondaryColor')}
                            disabled={!isEditable}
                            margin="none"
                        />
                    </Grid>

                    <Grid size={12} sx={{minWidth: 0, mt: 1}}>
                        {lightPreviewTheme != null && (
                            <ThemePreviewPanel
                                label="Vorschau des hellen Farbschemas"
                                theme={lightPreviewTheme}
                                logoUrl={lightPreviewLogoUrl}
                            />
                        )}
                    </Grid>
                </Grid>
            </Box>

            <Divider sx={{my: 6}}/>

            <Box>
                <Typography variant="h6" component="h2" sx={{mb: 1}}>
                    Dunkles Farbschema
                </Typography>
                <FormControlLabel
                    control={(
                        <Switch
                            checked={usesDarkModeColors}
                            onChange={(_, checked) => handleDarkModeColorsToggle(checked)}
                            disabled={!isEditable}
                            slotProps={{
                                input: {
                                    'aria-describedby': 'dark-color-scheme-description',
                                },
                            }}
                        />
                    )}
                    label="Farben für das dunkle Farbschema anpassen"
                />
                <Typography
                    id="dark-color-scheme-description"
                    sx={{
                        color: "text.secondary",
                        mt: 0.5,
                        mb: 3,
                        maxWidth: 900
                    }}>
                    Eine Anpassung ist sinnvoll, wenn die Farben auf dunklen Hintergründen zu wenig Kontrast bieten
                    oder zu dunkel wirken. Ohne Anpassung werden die Farben des hellen Farbschemas übernommen.
                </Typography>
                <Grid container columnSpacing={3} rowSpacing={3} sx={{
                    alignItems: "stretch"
                }}>
                    <Grid size={{xs: 12, md: 6}} sx={{minWidth: 0}}>
                        <ThemeColorPicker
                            label="Primärfarbe"
                            value={theme.primaryColorDark ?? theme.primaryColor}
                            contrastTextColor={darkPreviewTheme?.palette.primary.contrastText}
                            contrastBackgroundColor={darkPreviewTheme?.palette.background.paper}
                            onChange={handleInputChange('primaryColorDark')}
                            disabled={!isEditable || !usesDarkModeColors}
                            margin="none"
                        />
                    </Grid>

                    <Grid size={{xs: 12, md: 6}} sx={{minWidth: 0}}>
                        <ThemeColorPicker
                            label="Sekundärfarbe"
                            value={theme.secondaryColorDark ?? theme.secondaryColor}
                            contrastTextColor={darkPreviewTheme?.palette.secondary.contrastText}
                            contrastBackgroundColor={darkPreviewTheme?.palette.background.paper}
                            onChange={handleInputChange('secondaryColorDark')}
                            disabled={!isEditable || !usesDarkModeColors}
                            margin="none"
                        />
                    </Grid>

                    <Grid size={12} sx={{minWidth: 0, mt: 1}}>
                        {darkPreviewTheme != null && (
                            <ThemePreviewPanel
                                label="Vorschau des dunklen Farbschemas"
                                theme={darkPreviewTheme}
                                logoUrl={darkPreviewLogoUrl}
                            />
                        )}
                    </Grid>
                </Grid>
            </Box>
            <Alert
                severity="info"
                sx={{mt: 6}}
                icon={<AccessibilityNewIcon />}
            >
                <AlertTitle>Hinweis zur Barrierefreiheit des Erscheinungsbildes</AlertTitle>
                <Typography sx={{maxWidth: 860}}>
                    Die Primär- und Sekundärfarben werden im hellen und dunklen Farbschema unverändert für gefüllte
                    Flächen verwendet und erhalten automatisch schwarzen oder weißen Text mit mindestens 4,5:1
                    Kontrast. Bei farbigem Text, Textschaltflächen und umrandeten Schaltflächen verwendet Prosuna bei
                    Bedarf einen kontrastfähigen Ton derselben Farbe. Für Symbolschaltflächen zeigt die Kontrastangabe
                    das Verhältnis der gewählten Farbe zur Standardfläche der Anwendung.
                </Typography>
            </Alert>

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <DisabledTooltip
                    title={saveDisabledTooltip}
                    disabled={isBusy || hasNotChanged || !isEditable}
                >
                    <Button
                        onClick={handleSave}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        variant="contained"
                        color="primary"
                        startIcon={<SaveOutlinedIcon />}
                    >
                        Speichern
                    </Button>
                </DisabledTooltip>

                {
                    !isNewTheme &&
                    <DisabledTooltip
                        title={saveDisabledTooltip}
                        disabled={isBusy || hasNotChanged || !isEditable}
                    >
                        <Button
                            onClick={() => {
                                reset();
                            }}
                            disabled={isBusy || hasNotChanged || !isEditable}
                            color="error"
                        >
                            Zurücksetzen
                        </Button>
                    </DisabledTooltip>
                }

                {
                    !isNewTheme && !isDefaultTheme &&
                    <DisabledTooltip
                        disabled={setDefaultThemeActionDisabled}
                        title={setDefaultThemeActionDisabledTooltip}
                        wrapperSx={{marginLeft: 'auto'}}
                    >
                        <Button
                            variant="text"
                            startIcon={<StarOutlined />}
                            disabled={setDefaultThemeActionDisabled}
                            onClick={() => {
                                void setDefaultTheme(theme);
                            }}
                        >
                            Als Standard festlegen
                        </Button>
                    </DisabledTooltip>
                }

                {
                    !isNewTheme &&
                    <DisabledTooltip
                        title={deleteDisabledTooltip}
                        disabled={isBusy || deleteDisabledByPermission}
                        wrapperSx={{marginLeft: isDefaultTheme ? 'auto' : undefined}}
                    >
                        <Button
                            variant={'outlined'}
                            onClick={checkAndHandleDelete}
                            disabled={isBusy || deleteDisabledByPermission}
                            color="error"
                            startIcon={<Delete />}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Erscheinungsbild löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={theme.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie dieses Erscheinungsbild wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieses Erscheinungsbild kann nicht gelöscht werden, da es von einer oder mehreren Organisationseinheiten verwendet wird."
                solutionText="Bitte weisen Sie diesen Organisationseinheiten zunächst ein anderes Erscheinungsbild zu:"
                links={relatedDepartments}
            />
            <ConstraintDialog
                open={showConstraintDefaultThemeDialog}
                onClose={() => setConstraintDefaultThemeDialog(false)}
                message="Dieses Erscheinungsbild kann nicht gelöscht werden, da es das Standard-Erscheinungsbild der Prosuna-Instanz ist."
                solutionText="Legen Sie zunächst ein anderes Erscheinungsbild als Standard fest."
                links={[{
                    label: 'Erscheinungsbilder anzeigen',
                    to: '/themes',
                }]}
            />
        </Box>
    );
}


function ThemePreviewPanel({label, theme, logoUrl}: {label: string; theme: MuiTheme; logoUrl: string | null}) {
    return (
        <ThemeProvider theme={theme}>
            <Box
                sx={{
                    overflow: 'hidden',
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    backgroundColor: 'background.paper',
                    color: 'text.primary',
                }}
            >
                <Box sx={{px: 2, py: 1, borderBottom: '1px solid', borderColor: 'divider'}}>
                    <Typography variant="subtitle1">{label}</Typography>
                </Box>
                <Grid container sx={{
                    alignItems: "stretch"
                }}>
                    <Grid
                        size={{xs: 12, md: 6}}
                        sx={{
                            p: 3,
                            backgroundColor: 'background.default',
                            borderRight: {xs: 'none', md: `1px solid ${theme.palette.divider}`},
                            borderBottom: {xs: `1px solid ${theme.palette.divider}`, md: 'none'},
                        }}
                    >
                        {logoUrl != null && (
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    height: 48,
                                    maxWidth: 220,
                                    mb: 2,
                                }}
                            >
                                <Box
                                    component="img"
                                    src={logoUrl}
                                    alt="Beispielansicht des ausgewählten Logos"
                                    sx={{
                                        display: 'block',
                                        width: 'auto',
                                        maxWidth: '100%',
                                        maxHeight: '100%',
                                        objectFit: 'contain',
                                    }}
                                />
                            </Box>
                        )}
                        <Typography variant="h5">Prosuna</Typography>
                        <Typography
                            sx={{
                                color: "text.secondary",
                                mt: 0.5
                            }}>
                            Digitale Verwaltungsservices
                        </Typography>
                        <Stack spacing={1} sx={{mt: 3, maxWidth: 320}}>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    px: 1.5,
                                    py: 1,
                                    borderRadius: 1,
                                    color: theme.palette.primary.contrastText,
                                    backgroundColor: theme.palette.primary.main,
                                }}
                            >
                                <DashboardOutlinedIcon sx={{fontSize: 20, mr: 1.25}} />
                                Übersicht
                            </Box>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    px: 1.5,
                                    py: 1,
                                    color: 'text.secondary',
                                }}
                            >
                                <DescriptionOutlinedIcon sx={{fontSize: 20, mr: 1.25}} />
                                <Box component="span" sx={{flex: 1}}>Formulare</Box>
                                <Box
                                    component="span"
                                    sx={{
                                        display: 'inline-flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        width: 24,
                                        height: 24,
                                        borderRadius: '50%',
                                        color: theme.palette.secondary.contrastText,
                                        backgroundColor: theme.palette.secondary.main,
                                        fontSize: '0.75rem',
                                        fontWeight: 600,
                                    }}
                                >
                                    3
                                </Box>
                            </Box>
                            <Box
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    px: 1.5,
                                    py: 1,
                                    color: 'text.secondary',
                                }}
                            >
                                <SettingsOutlinedIcon sx={{fontSize: 20, mr: 1.25}} />
                                Einstellungen
                            </Box>
                        </Stack>
                        <Box sx={{mt: 3, pt: 2, borderTop: `1px solid ${theme.palette.divider}`}}>
                            <Typography variant="caption" sx={{
                                color: "text.secondary"
                            }}>
                                Organisationseinheit
                            </Typography>
                            <Typography variant="body2">Verwaltung Musterstadt</Typography>
                        </Box>
                    </Grid>
                    <Grid size={{xs: 12, md: 6}} sx={{p: 3, backgroundColor: 'background.paper'}}>
                        <Tabs
                            value={0}
                            sx={{mb: 2, borderBottom: '1px solid', borderColor: 'divider'}}
                        >
                            <Tab label="Allgemeine Angaben"/>
                            <Tab label="Zuordnung"/>
                        </Tabs>
                        <Stack
                            direction="row"
                            useFlexGap
                            spacing={1}
                            sx={{
                                flexWrap: "wrap",
                                mb: 2
                            }}>
                            <Button size="small" variant="contained">Primäre Aktion</Button>
                            <Button size="small" variant="outlined">Weitere Aktion</Button>
                            <Button size="small" color="secondary" variant="contained">Sekundäre Aktion</Button>
                            <Box sx={{display: 'inline-flex', alignItems: 'center', gap: 0.5}}>
                                <Tooltip title="Bearbeiten" arrow>
                                    <IconButton size="small" color="primary" aria-label="Bearbeiten">
                                        <EditOutlinedIcon/>
                                    </IconButton>
                                </Tooltip>
                                <Tooltip title="Einstellungen" arrow>
                                    <IconButton size="small" color="secondary" aria-label="Einstellungen">
                                        <SettingsOutlinedIcon/>
                                    </IconButton>
                                </Tooltip>
                            </Box>
                        </Stack>
                        <TextField
                            fullWidth
                            size="small"
                            label="Beispiel-Eingabefeld"
                            value="Beispielinhalt"
                            slotProps={{input: {readOnly: true}}}
                            sx={{mb: 2}}
                        />
                        <Stack spacing={1}>
                            <Alert severity="success">Erfolgreich gespeichert</Alert>
                            <Alert severity="warning">Bitte prüfen Sie Ihre Eingaben</Alert>
                        </Stack>
                    </Grid>
                </Grid>
            </Box>
        </ThemeProvider>
    );
}
