import {Box, Breadcrumbs, Button, FormControlLabel, Grid, Switch, Tooltip, Typography} from '@mui/material';
import React, {ComponentType, useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate, useSearchParams} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {RichTextInputComponent, RichTextInputComponentProps} from '../../../../components/rich-text-input-component/rich-text-input-component';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ThemeResponseDTO} from '../../../themes/models/theme';
import {ThemesApiService} from '../../../themes/themes-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {DepartmentsDetailsPageAdditionalData, NewParentIdQueryParam} from './departments-details-page';
import {TextFieldComponentProps} from '../../../../components/text-field/text-field-component-props';
import {SelectFieldComponentProps} from '../../../../components/select-field/select-field-component-props';
import {DepartmentEntity} from '../../entities/department-entity';
import {DepartmentApiService} from '../../services/department-api-service';
import {getDepartmentTypeLabel, getMaxDepartmentDepth} from '../../utils/department-utils';
import {ProcessDefinitionApiService} from '../../../process/services/process-definition-api-service';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import MoveGroup from '@aivot/mui-material-symbols-400-outlined/dist/move-group/MoveGroup';
import {MoveDepartmentDialog} from '../../dialogs/move-department-dialog';
import {VDepartmentShadowedApiService} from '../../services/v-department-shadowed-api-service';
import {VDepartmentShadowedEntity} from '../../entities/v-department-shadowed-entity';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useCheckDepartmentPermission} from '../../../permissions/hooks/use-permissions';
import {isApiError} from '../../../../models/api-error';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {alpha} from '@mui/material/styles';

const canInheritRequiredSetting = (context: yup.TestContext, isCreatedAsChild: boolean) => {
    return isCreatedAsChild || context.parent?.parentDepartmentId != null;
};

const requiredShadowedString = (
    emptyOverrideMessage: string,
    missingValueMessage: string,
    isCreatedAsChild: boolean,
) => yup.string()
    .nullable()
    .test('required-or-inherited', missingValueMessage, function (value) {
        return value != null || canInheritRequiredSetting(this, isCreatedAsChild);
    })
    .test('not-empty-override', emptyOverrideMessage, (value) => {
        if (value == null) {
            return true;
        }

        return value.trim().length > 0;
    });

const optionalShadowedString = () => yup.string()
    .nullable();

const optionalShadowedTrimmedString = () => yup.string()
    .transform((value, originalValue) => {
        if (typeof originalValue === 'string' && originalValue.trim().length === 0) {
            return '';
        }

        return value;
    })
    .trim()
    .nullable();

const requiredShadowedTrimmedString = (
    emptyOverrideMessage: string,
    missingValueMessage: string,
    isCreatedAsChild: boolean,
) => requiredShadowedString(emptyOverrideMessage, missingValueMessage, isCreatedAsChild)
    .trim();

const requiredShadowedEmail = (
    emptyOverrideMessage: string,
    missingValueMessage: string,
    isCreatedAsChild: boolean,
) => requiredShadowedTrimmedString(emptyOverrideMessage, missingValueMessage, isCreatedAsChild)
    .email('Bitte eine gültige E-Mail-Adresse eingeben.')
    .max(255, 'Die E-Mail-Adresse darf maximal 255 Zeichen lang sein.');

const requiredShadowedRichText = (
    emptyOverrideMessage: string,
    missingValueMessage: string,
    isCreatedAsChild: boolean,
) => requiredShadowedString(emptyOverrideMessage, missingValueMessage, isCreatedAsChild)
    .min(10, 'Der Text muss mindestens 10 Zeichen lang sein.');

const optionalShadowedRichText = () => optionalShadowedString();

const optionalShadowedTextAllowEmpty = () => optionalShadowedString();

const optionalShadowedTrimmedTextAllowEmpty = () => optionalShadowedTrimmedString();

const optionalShadowedPhone = () => optionalShadowedTrimmedTextAllowEmpty()
    .max(96, 'Die Telefonnummer darf maximal 96 Zeichen lang sein.');

const optionalShadowedInfo = () => optionalShadowedTextAllowEmpty();

const validatePostalAddress = (isCreatedAsChild: boolean) => requiredShadowedTrimmedString(
    'Die Postadresse darf nicht leer überschrieben werden.',
    'Die Postadresse ist ein Pflichtfeld.',
    isCreatedAsChild,
)
    .min(3, 'Die Postadresse muss mindestens 3 Zeichen lang sein.');

const validateRichTextRequired = (fieldName: string, isCreatedAsChild: boolean) => requiredShadowedRichText(
    `${fieldName} darf nicht leer überschrieben werden.`,
    `${fieldName} ist ein Pflichtfeld.`,
    isCreatedAsChild,
);

const validateRequiredEmail = (fieldName: string, isCreatedAsChild: boolean) => requiredShadowedEmail(
    `${fieldName} darf nicht leer überschrieben werden.`,
    `${fieldName} ist ein Pflichtfeld.`,
    isCreatedAsChild,
);

const validateOptionalMailSignature = () => optionalShadowedRichText();

const validateOptionalContactInfo = () => optionalShadowedInfo();

const validateOptionalContactPhone = () => optionalShadowedPhone();

const validateThemeId = () => yup.number()
    .optional()
    .nullable()
    .test('valid-theme', 'Bitte wählen Sie ein Standard-Theme aus oder übernehmen Sie den geerbten Wert.', (value) => {
        return value == null || value > 0;
    });

function getProcessDetailsPath(processId: number, version: number | null): string {
    if (version == null) {
        return '/processes';
    }

    return `/processes/${processId}/versions/${version}`;
}

export const createDepartmentSchema = (isCreatedAsChild: boolean) => yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name der Organisationseinheit muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name der Organisationseinheit darf maximal 96 Zeichen lang sein.')
        .required('Der Name der Organisationseinheit ist ein Pflichtfeld.'),
    postalAddress: validatePostalAddress(isCreatedAsChild),
    specialSupportEmail: validateRequiredEmail('Die E-Mail-Adresse für fachliche Unterstützung', isCreatedAsChild),
    specialSupportPhone: validateOptionalContactPhone(),
    specialSupportInfo: validateOptionalContactInfo(),
    technicalSupportEmail: validateRequiredEmail('Die E-Mail-Adresse für technische Unterstützung', isCreatedAsChild),
    technicalSupportPhone: validateOptionalContactPhone(),
    technicalSupportInfo: validateOptionalContactInfo(),
    defaultMailSignature: validateOptionalMailSignature(),
    imprint: validateRichTextRequired('Das Impressum', isCreatedAsChild),
    commonPrivacy: validateRichTextRequired('Die Datenschutzerklärung', isCreatedAsChild),
    commonAccessibility: validateRichTextRequired('Die Barrierefreiheitserklärung', isCreatedAsChild),
    themeId: validateThemeId(),
});

export const DepartmentSchema = createDepartmentSchema(false);

export function DepartmentsDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const [searchParams, _] = useSearchParams();

    const parentOrgUnitId = useMemo(() => {
        const parentId = searchParams.get(NewParentIdQueryParam);
        if (parentId != null && !isNaN(Number(parentId))) {
            return parseInt(parentId);
        }
        return undefined;
    }, [searchParams]);

    const api = useApi();
    const {
        item,
        setItem,
        setAdditionalData,
        isBusy,
        setIsBusy,
        isEditable,
        additionalData,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<DepartmentEntity, DepartmentsDetailsPageAdditionalData>;

    const departmentSchema = useMemo(() => createDepartmentSchema(parentOrgUnitId != null), [parentOrgUnitId]);

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<DepartmentEntity>(item, departmentSchema as any);

    const apiService = useMemo(() => new DepartmentApiService(), []);
    const department = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);
    const editPermission = department?.id === 0 ? Permission.DEPARTMENT_CREATE : Permission.DEPARTMENT_UPDATE;
    const canDeleteDepartment = useCheckDepartmentPermission(
        department?.id === 0 ? undefined : department?.id,
        Permission.DEPARTMENT_DELETE,
    );
    const effectiveDepartmentDepth = department?.id === 0 && parentOrgUnitId != null && additionalData?.shadowedDepartment != null
        ? additionalData.shadowedDepartment.depth + 1
        : department?.depth ?? 0;

    type ShadowedStringField =
        | 'postalAddress'
        | 'specialSupportEmail'
        | 'specialSupportPhone'
        | 'specialSupportInfo'
        | 'technicalSupportEmail'
        | 'technicalSupportPhone'
        | 'technicalSupportInfo'
        | 'defaultMailSignature'
        | 'imprint'
        | 'commonPrivacy'
        | 'commonAccessibility';

    const normalizeShadowedStringValue = (value: string | null | undefined) => value ?? '';
    const handleShadowedStringOverride = (field: ShadowedStringField) => (override: boolean) => {
        handleInputChange(field)((override ? '' : null) as DepartmentEntity[typeof field]);
    };
    const handleShadowedStringChange = (field: ShadowedStringField) => (value: string | null | undefined) => {
        handleInputChange(field)(normalizeShadowedStringValue(value) as DepartmentEntity[typeof field]);
    };
    const handleShadowedStringBlur = (field: ShadowedStringField) => (value?: string | null) => {
        handleInputBlur(field)(normalizeShadowedStringValue(value) as DepartmentEntity[typeof field]);
    };

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedApplications, setRelatedApplications] = useState<ConstraintLinkProps[] | undefined>(undefined);
    const [availableThemes, setAvailableThemes] = useState<ThemeResponseDTO[]>();
    const [showMoveDialog, setShowMoveDialog] = useState(false);
    const [inheritedDepartment, setInheritedDepartment] = useState<VDepartmentShadowedEntity | null>(null);

    useEffect(() => {
        new ThemesApiService(api)
            .listAll()
            .then((result) => {
                setAvailableThemes(result.content);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar(
                    isApiError(err) && err.status === 403
                        ? `Die verfügbaren Farbschemata konnten nicht geladen werden. Für die Auswahl ist die Berechtigung ${Permission.THEME_READ} erforderlich.`
                        : 'Fehler beim Laden der verfügbaren Farbschemata.',
                ));
                setAvailableThemes([]);
            });
    }, []);

    useEffect(() => {
        if (department == null) {
            return;
        }

        if (department.id === 0) {
            setInheritedDepartment(parentOrgUnitId != null ? additionalData?.shadowedDepartment ?? null : null);
            return;
        }

        if (department.parentDepartmentId == null) {
            setInheritedDepartment(null);
            return;
        }

        let isActive = true;

        new VDepartmentShadowedApiService()
            .retrieve(department.parentDepartmentId)
            .then((parentDepartment) => {
                if (!isActive) {
                    return;
                }

                setInheritedDepartment(parentDepartment);
            })
            .catch((error) => {
                if (!isActive) {
                    return;
                }

                console.error(error);
                setInheritedDepartment(null);
            });

        return () => {
            isActive = false;
        };
    }, [department?.id, department?.parentDepartmentId, parentOrgUnitId, additionalData?.shadowedDepartment]);

    if (department == null || availableThemes == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const saveDisabledByPermission = !isEditable;
    const saveDisabledTooltip = saveDisabledByPermission
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledByPermission = !canDeleteDepartment;
    const deleteDisabledTooltip = deleteDisabledByPermission
        ? formatMissingPermissionTooltip(Permission.DEPARTMENT_DELETE)
        : undefined;

    const handleSave = () => {
        // Do not save if department is null
        if (department == null) {
            return;
        }

        if (department.id === 0 && effectiveDepartmentDepth > getMaxDepartmentDepth()) {
            dispatch(showErrorSnackbar(`Organisationseinheiten sind auf ${getMaxDepartmentDepth() + 1} Ebenen beschränkt.`));
            return;
        }

        // Validate form
        const validationResult = validate();

        // If validation fails, show error snackbar and do not proceed
        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        if (department.id === 0) {
            const parentId = parseInt(searchParams.get(NewParentIdQueryParam) ?? '');

            apiService
                .create({
                    ...department,
                    parentDepartmentId: isNaN(parentId) ? undefined : parentId,
                })
                .then((newDepartment) => {
                    setItem(newDepartment);
                    reset();

                    dispatch(showSuccessSnackbar('Neue Organisationseinheit erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/departments/${newDepartment.id}`, {
                            replace: true,
                        });
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
                .update(department.id, department)
                .then((updatedDepartment) => {
                    setItem(updatedDepartment);
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen an der Organisationseinheit erfolgreich gespeichert.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const checkAndHandleDelete = async () => {
        if (department.id === 0) {
            return;
        }

        setIsBusy(true);
        try {
            const processesApi = new ProcessDefinitionApiService();
            const managedProcesses = await processesApi.listAll({
                departmentId: department.id,
            });

            const uniqueProcesses = managedProcesses.content;

            if (uniqueProcesses.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = uniqueProcesses.slice(0, maxVisibleLinks).map(process => ({
                    label: process.internalTitle,
                    to: getProcessDetailsPath(process.id, process.draftedVersion ?? process.publishedVersion),
                }));

                if (uniqueProcesses.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: 'Weitere Prozesse anzeigen…',
                        to: `/departments/${department.id}/processes`,
                    });
                }

                setRelatedApplications(processedLinks);
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
        if (department.id === 0) return;

        setIsBusy(true);
        apiService.destroy(department.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/departments', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Organisationseinheit wurde erfolgreich gelöscht.'));
            })
            .catch(() => dispatch(showErrorSnackbar('Beim Löschen ist ein Fehler aufgetreten.')))
            .finally(() => setIsBusy(false));
    };

    const doNotShadow = department.depth === 0 && parentOrgUnitId == null;
    const canMoveDepartment = department.id !== 0 && isEditable && hasNotChanged;
    const moveDisabledReason = isBusy
        ? 'Bitte warten, bis die aktuelle Aktion abgeschlossen ist.'
            : department.id === 0
                ? 'Die Organisationseinheit kann erst nach dem Anlegen verschoben werden.'
                : !isEditable
                    ? formatMissingPermissionTooltip(Permission.DEPARTMENT_UPDATE)
                    : !hasNotChanged
                        ? 'Bitte speichern oder verwerfen Sie zuerst Ihre Änderungen.'
                        : null;

    const orgUnitPathParts = (() => {
        const safeName = department.name?.trim() || 'Unbenannt';

        if (department.id === 0 && parentOrgUnitId != null && additionalData?.shadowedDepartment != null) {
            const parentPath = [
                ...(additionalData.shadowedDepartment.parentNames ?? []),
                additionalData.shadowedDepartment.name,
            ].filter(Boolean);

            return [...parentPath, safeName];
        }

        const parentPath = additionalData?.shadowedDepartment?.parentNames ?? [];
        return [...parentPath, safeName];
    })();
    const shouldShowOrgUnitHierarchy = orgUnitPathParts.length > 1;
    const inheritedDepartmentValues = inheritedDepartment;

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{
                    mt: 1.5,
                    mb: 1,
                }}
            >
                Angaben zu {getDepartmentTypeLabel(effectiveDepartmentDepth)}
            </Typography>
            <Typography
                sx={{
                    mb: 2,
                    maxWidth: 900,
                }}
            >
                Hinterlegen Sie interne Angaben, Postadresse, Kontaktinformationen, rechtliche Texte und Standardwerte dieser Organisationseinheit.
                Ein Teil dieser Angaben wird in Formularen und Dialogen für Nutzer:innen angezeigt.
            </Typography>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Name der Organisationseinheit"
                        value={department.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        hint="Diese Bezeichnung wird nur intern in Gover verwendet, zum Beispiel in Listen, Zuweisungen und Auswahlen."
                        error={errors.name}
                        disabled={!isEditable}
                    />
                    {
                        shouldShowOrgUnitHierarchy &&
                        <>
                            <Typography
                                variant="caption"
                                color="text.secondary"
                                sx={{
                                    mt: 0.25,
                                    display: 'block',
                                }}
                            >
                                Einordnung in der Organisationsstruktur:
                            </Typography>
                            <Breadcrumbs
                                separator="›"
                                maxItems={5}
                                itemsBeforeCollapse={2}
                                itemsAfterCollapse={2}
                                sx={{
                                    mt: 0,
                                    mb: 2,
                                    color: 'text.secondary',
                                    '& .MuiBreadcrumbs-ol': {
                                        flexWrap: 'nowrap',
                                        overflow: 'hidden',
                                    },
                                }}
                            >
                                {
                                    orgUnitPathParts.map((segment, index) => (
                                        <Typography
                                            key={`${department.id}-${index}`}
                                            variant="caption"
                                            color="text.secondary"
                                            sx={{
                                                maxWidth: 220,
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                whiteSpace: 'nowrap',
                                            }}
                                            title={segment}
                                        >
                                            {segment}
                                        </Typography>
                                    ))
                                }
                            </Breadcrumbs>
                        </>
                    }
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                />
                {
                    !doNotShadow &&
                    <Grid
                        size={{
                            xs: 12,
                        }}
                    >
                        <AlertComponent
                            color="info"
                            sx={{
                                maxWidth: 900,
                            }}
                        >
                            <Typography variant="body2">
                                Vererbbare Felder ohne eigene Angabe übernehmen die Werte der übergeordneten Organisationseinheit.
                                Sie können bei jedem Feld steuern, ob diese Organisationseinheit einen eigenen Wert verwenden soll.
                            </Typography>
                        </AlertComponent>
                    </Grid>
                }
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.postalAddress != null}
                        onSetOverride={handleShadowedStringOverride('postalAddress')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.postalAddress ?? '',
                            disabled: true,
                        }}
                        label="Postadresse"
                        value={department.postalAddress ?? ''}
                        onChange={handleShadowedStringChange('postalAddress')}
                        onBlur={handleShadowedStringBlur('postalAddress')}
                        required
                        multiline
                        rows={5}
                        hint="Vollständiger Postempfänger inklusive Name der Organisation und ggf. Organisationseinheit, Straße, Hausnummer, Postleitzahl und Ort."
                        error={errors.postalAddress}
                        disabled={!isEditable}
                    />
                </Grid>
            </Grid>
            <Typography
                variant="h6"
                sx={{
                    mt: 2,
                    mb: 1,
                }}
            >
                Erscheinungsbild der Organisationseinheit
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Hinterlegen Sie das Standard-Erscheinungsbild, das für Formulare dieser Organisationseinheit verwendet werden soll.
                Dieses überschreibt das Erscheinungsbild der Gover-Instanz.
                Bearbeiter:innen können für Formulare weiterhin ein individuelles Erscheinungsbild auswählen.
                Wenn Sie kein Erscheinungsbild auswählen, wird das Erscheinungsbild der Gover-Instanz verwendet.
            </Typography>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <ShadowedInput<SelectFieldComponentProps, typeof SelectFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={SelectFieldComponent}
                        override={department.themeId != null}
                        onSetOverride={(override) => {
                            if (override) {
                                handleInputChange('themeId')(0);
                            } else {
                                handleInputChange('themeId')(null);
                            }
                        }}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.themeId?.toString() ?? undefined,
                            disabled: true,
                        }}
                        label="Erscheinungsbild der Organisationseinheit"
                        value={department.themeId?.toString()}
                        onChange={(val) => {
                            if (val == null) {
                                handleInputChange('themeId')(null);
                            } else {
                                const intVal = parseInt(val);

                                if (isNaN(intVal)) {
                                    handleInputChange('themeId')(null);
                                } else {
                                    handleInputChange('themeId')(intVal);
                                }
                            }
                        }}
                        required={false}
                        error={errors.themeId}
                        options={availableThemes.map(theme => ({
                            label: theme.name,
                            value: theme.id.toString(),
                        }))}
                        disabled={!isEditable}
                    />
                </Grid>
            </Grid>


            <Typography
                variant="h6"
                sx={{
                    mt: 2,
                    mb: 1,
                }}
            >
                Kontaktinformationen für antragstellende Personen
            </Typography>
            <Typography sx={{mb: 2}}>
                Die hier hinterlegten Kontaktinformationen werden Nutzer:innen zum Beispiel im Hilfe-Dialog zur Verfügung gestellt.
            </Typography>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2,
                    }}
                >
                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.specialSupportEmail != null}
                        onSetOverride={handleShadowedStringOverride('specialSupportEmail')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.specialSupportEmail ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-E-Mail-Adresse für fachliche Unterstützung"
                        type="email"
                        value={department.specialSupportEmail ?? ''}
                        onChange={handleShadowedStringChange('specialSupportEmail')}
                        onBlur={handleShadowedStringBlur('specialSupportEmail')}
                        required
                        maxCharacters={255}
                        error={errors.specialSupportEmail}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.specialSupportPhone != null}
                        onSetOverride={handleShadowedStringOverride('specialSupportPhone')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.specialSupportPhone ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-Telefonnummer für fachliche Unterstützung"
                        type="tel"
                        value={department.specialSupportPhone ?? ''}
                        onChange={handleShadowedStringChange('specialSupportPhone')}
                        onBlur={handleShadowedStringBlur('specialSupportPhone')}
                        maxCharacters={96}
                        error={errors.specialSupportPhone}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<RichTextInputComponentProps, typeof RichTextInputComponent>
                        doNotShadow={doNotShadow}
                        Component={RichTextInputComponent}
                        override={department.specialSupportInfo != null}
                        onSetOverride={handleShadowedStringOverride('specialSupportInfo')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.specialSupportInfo ?? '',
                            disabled: true,
                        }}
                        label="Informationen zur fachlichen Unterstützung"
                        value={department.specialSupportInfo ?? ''}
                        onChange={handleShadowedStringChange('specialSupportInfo')}
                        error={errors.specialSupportInfo}
                        disabled={!isEditable}
                        hint="Zusätzliche Informationen, z. B. zu Kontaktwegen, Supportzeiten oder Links. Diese Angaben werden im Hilfe-Dialog angezeigt."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2,
                    }}
                >
                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.technicalSupportEmail != null}
                        onSetOverride={handleShadowedStringOverride('technicalSupportEmail')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.technicalSupportEmail ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-E-Mail-Adresse für technische Unterstützung"
                        type="email"
                        value={department.technicalSupportEmail ?? ''}
                        onChange={handleShadowedStringChange('technicalSupportEmail')}
                        onBlur={handleShadowedStringBlur('technicalSupportEmail')}
                        required
                        maxCharacters={255}
                        error={errors.technicalSupportEmail}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                        doNotShadow={doNotShadow}
                        Component={TextFieldComponent}
                        override={department.technicalSupportPhone != null}
                        onSetOverride={handleShadowedStringOverride('technicalSupportPhone')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.technicalSupportPhone ?? '',
                            disabled: true,
                        }}
                        label="Kontakt-Telefonnummer für technische Unterstützung"
                        type="tel"
                        value={department.technicalSupportPhone ?? ''}
                        onChange={handleShadowedStringChange('technicalSupportPhone')}
                        onBlur={handleShadowedStringBlur('technicalSupportPhone')}
                        maxCharacters={96}
                        error={errors.technicalSupportPhone}
                        disabled={!isEditable}
                    />

                    <ShadowedInput<RichTextInputComponentProps, typeof RichTextInputComponent>
                        doNotShadow={doNotShadow}
                        Component={RichTextInputComponent}
                        override={department.technicalSupportInfo != null}
                        onSetOverride={handleShadowedStringOverride('technicalSupportInfo')}
                        shadowedProps={{
                            value: inheritedDepartmentValues?.technicalSupportInfo ?? '',
                            disabled: true,
                        }}
                        label="Informationen zur technischen Unterstützung"
                        value={department.technicalSupportInfo ?? ''}
                        onChange={handleShadowedStringChange('technicalSupportInfo')}
                        error={errors.technicalSupportInfo}
                        disabled={!isEditable}
                        hint="Zusätzliche Informationen, z. B. zu Kontaktwegen, Supportzeiten oder Links. Diese Angaben werden im Hilfe-Dialog angezeigt."
                    />
                </Grid>
            </Grid>
            <Typography
                variant="h6"
                sx={{mt: 2, mb: 1}}
            >
                Rechtliche Informationen
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Die folgenden rechtlichen Angaben und Texte können in Formularen referenziert werden.
            </Typography>
            <Box sx={{mb: 3}}>
                <ShadowedInput<RichTextInputComponentProps, typeof RichTextInputComponent>
                    doNotShadow={doNotShadow}
                    Component={RichTextInputComponent}
                    override={department.imprint != null}
                    onSetOverride={handleShadowedStringOverride('imprint')}
                    shadowedProps={{
                        value: inheritedDepartmentValues?.imprint ?? '',
                        disabled: true,
                    }}
                    label="Impressum"
                    value={department.imprint ?? ''}
                    onChange={handleShadowedStringChange('imprint')}
                    required
                    error={errors.imprint}
                    disabled={!isEditable}
                />
            </Box>
            <Box sx={{mb: 3}}>
                <ShadowedInput<RichTextInputComponentProps, typeof RichTextInputComponent>
                    doNotShadow={doNotShadow}
                    Component={RichTextInputComponent}
                    override={department.commonPrivacy != null}
                    onSetOverride={handleShadowedStringOverride('commonPrivacy')}
                    shadowedProps={{
                        value: inheritedDepartmentValues?.commonPrivacy ?? '',
                        disabled: true,
                    }}
                    label="Datenschutzerklärung - allgemeiner Teil"
                    value={department.commonPrivacy ?? ''}
                    onChange={handleShadowedStringChange('commonPrivacy')}
                    hint="Allgemeiner Teil der Datenschutzerklärung für diese Organisationseinheit. Formulare können zusätzlich einen spezifischen Teil enthalten, in dem die konkret verarbeiteten Daten, Zwecke und Rechtsgrundlagen beschrieben werden. Zusammen können beide Teile die Informationspflichten nach Art. 13 DSGVO erfüllen."
                    required
                    error={errors.commonPrivacy}
                    disabled={!isEditable}
                />
            </Box>
            <Box sx={{mb: 3}}>
                <ShadowedInput<RichTextInputComponentProps, typeof RichTextInputComponent>
                    doNotShadow={doNotShadow}
                    Component={RichTextInputComponent}
                    override={department.commonAccessibility != null}
                    onSetOverride={handleShadowedStringOverride('commonAccessibility')}
                    shadowedProps={{
                        value: inheritedDepartmentValues?.commonAccessibility ?? '',
                        disabled: true,
                    }}
                    label="Barrierefreiheitserklärung - allgemeiner Teil"
                    value={department.commonAccessibility ?? ''}
                    onChange={handleShadowedStringChange('commonAccessibility')}
                    hint="Allgemeiner Teil der Barrierefreiheitserklärung für diese Organisationseinheit. Formulare können zusätzlich einen spezifischen Teil enthalten, etwa für formularspezifische Gestaltung, eingebundene Dokumente oder besondere Barrieren."
                    required
                    error={errors.commonAccessibility}
                    disabled={!isEditable}
                />
            </Box>

            <Typography
                variant="h5"
                sx={{
                    mt: 6,
                    mb: 1,
                }}
            >
                Standard-E-Mail-Signatur
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Hinterlegen Sie eine allgemeine E-Mail-Signatur der Organisationseinheit ohne personenbezogene Angaben.
            </Typography>

            <ShadowedInput<TextFieldComponentProps, typeof TextFieldComponent>
                doNotShadow={doNotShadow}
                Component={TextFieldComponent}
                override={department.defaultMailSignature != null}
                onSetOverride={handleShadowedStringOverride('defaultMailSignature')}
                shadowedProps={{
                    value: inheritedDepartmentValues?.defaultMailSignature ?? '',
                    disabled: true,
                }}
                label="Standard-E-Mail-Signatur"
                value={department.defaultMailSignature ?? ''}
                onChange={handleShadowedStringChange('defaultMailSignature')}
                onBlur={handleShadowedStringBlur('defaultMailSignature')}
                multiline
                rows={6}
                error={errors.defaultMailSignature}
                disabled={!isEditable}
            />

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
                    department.id !== 0 &&
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
                    department.id !== 0 &&
                    <Box
                        sx={{
                            display: 'flex',
                            gap: 2,
                            marginLeft: 'auto',
                        }}
                    >
                        <DisabledTooltip
                            title={moveDisabledReason}
                            disabled={isBusy || !canMoveDepartment}
                        >
                            <Button
                                variant="outlined"
                                onClick={() => {
                                    setShowMoveDialog(true);
                                }}
                                disabled={isBusy || !canMoveDepartment}
                                startIcon={<MoveGroup />}
                            >
                                Verschieben
                            </Button>
                        </DisabledTooltip>
                        <DisabledTooltip
                            title={deleteDisabledTooltip}
                            disabled={isBusy || deleteDisabledByPermission}
                        >
                            <Button
                                variant="outlined"
                                onClick={checkAndHandleDelete}
                                disabled={isBusy || deleteDisabledByPermission}
                                color="error"
                                startIcon={<Delete />}
                            >
                                Löschen
                            </Button>
                        </DisabledTooltip>
                    </Box>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Organisationseinheit löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={department.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Organisationseinheit wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Diese Organisationseinheit kann (noch) nicht gelöscht werden, da sie noch Prozesse verwaltet."
                solutionText="Bitte übertragen Sie die Prozesse an eine andere Organisationseinheit und versuchen Sie es erneut:"
                links={relatedApplications}
            />

            {/* TODO: The move does currently not correctly refresh this page. */}
            {
                showMoveDialog &&
                <MoveDepartmentDialog
                    department={department}
                    onClose={() => {
                        setShowMoveDialog(false);
                    }}
                    onMoved={(updatedDepartment) => {
                        setItem(updatedDepartment);
                        reset();
                        setShowMoveDialog(false);

                        new VDepartmentShadowedApiService()
                            .retrieve(updatedDepartment.id)
                            .then((shadowedDepartment) => {
                                setAdditionalData({
                                    shadowedDepartment,
                                });
                            })
                            .catch((error) => {
                                console.error(error);
                            });
                    }}
                />
            }
        </Box>
    );
}

interface ShadowedInputProps<P, C extends ComponentType<P>> {
    doNotShadow: boolean;
    override: boolean;
    onSetOverride: (override: boolean) => void;
    Component: C;
    shadowedProps: Partial<P>;
}

function ShadowedInput<P, C extends ComponentType<P>>(props: ShadowedInputProps<P, C> & P) {
    const {
        override,
        onSetOverride,
        doNotShadow,

        Component,
        shadowedProps,

        ...rest
    } = props;

    const restProps = rest as Record<string, unknown>;
    const isSwitchDisabled = Boolean(restProps.disabled);
    const inheritanceStatusText = override
        ? 'Für diese Einheit wird eine eigene Angabe verwendet.'
        : 'Wert wird von übergeordneter Einheit geerbt.';

    const propsToPass: any = override
        ? {...rest as P}
        : {
            ...rest as P,
            ...shadowedProps,
        };

    if (doNotShadow) {
        return <Component {...(rest as any)} />;
    }

    return (
        <Box
            sx={(theme) => ({
                border: '1px solid',
                borderColor: override ? alpha(theme.palette.primary.main, 0.22) : alpha(theme.palette.text.primary, 0.12),
                borderRadius: 1,
                backgroundColor: theme.palette.background.paper,
                p: 1.5,
                transition: theme.transitions.create(['background-color', 'border-color'], {
                    duration: theme.transitions.duration.shorter,
                }),
                '&:focus-within': {
                    borderColor: override ? alpha(theme.palette.primary.main, 0.42) : alpha(theme.palette.text.primary, 0.24),
                },
            })}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: {
                        xs: 'flex-start',
                        sm: 'center',
                    },
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    columnGap: 1.5,
                    rowGap: 0.25,
                    mb: 0.75,
                }}
            >
                <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{
                        display: 'block',
                        fontWeight: 400,
                        lineHeight: 1.8,
                        minWidth: 0,
                        flex: '1 1 260px',
                    }}
                >
                    {inheritanceStatusText}
                </Typography>

                <FormControlLabel
                    control={
                        <Switch
                            checked={override}
                            disabled={isSwitchDisabled}
                            size="small"
                            onChange={(event) => {
                                onSetOverride(event.target.checked);
                            }}
                        />
                    }
                    label="Eigene Angabe verwenden"
                    sx={{
                        m: 0,
                        flexShrink: 0,
                        '& .MuiFormControlLabel-label': {
                            color: isSwitchDisabled ? 'text.disabled' : 'text.secondary',
                            fontSize: '0.8125rem',
                            lineHeight: 1.4,
                        },
                    }}
                />
            </Box>
            <Component {...propsToPass} />
        </Box>
    );
}
