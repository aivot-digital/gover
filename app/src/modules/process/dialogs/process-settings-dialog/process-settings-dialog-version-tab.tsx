import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useState} from 'react';
import {Alert, Box, Grid, Skeleton, Stack} from '@mui/material';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {ElementEditorSectionHeader} from '../../../../components/element-editor-section-header/element-editor-section-header';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {RadioFieldComponent} from '../../../../components/radio-field/radio-field-component';
import {
    CASE_NUMBER_TEMPLATE_MAX_LENGTH,
    CASE_NUMBER_TYPE_TEMPLATE,
    CASE_NUMBER_TYPE_UUID,
    getCaseNumberType,
    validateCaseNumberTemplate,
} from '../../utils/process-case-number-utils';
import {ProcessStatus} from '../../enums/process-status';
import {deepEquals} from '../../../../utils/equality-utils';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {RichTextInputComponent} from '../../../../components/rich-text-input-component/rich-text-input-component';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {DepartmentSelectField} from '../../../departments/components/department-select-field';
import {type ThemeResponseDTO} from '../../../themes/models/theme';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {type SelectFieldComponentOption} from '../../../../components/select-field/select-field-component-option';
import {Hint} from '../../../../components/hint/hint';

interface ProcessSettingsDialogVersionTabProps {
    open: boolean;
    version: ProcessVersionEntity;
    departments: VDepartmentShadowedEntity[];
    themes: ThemeResponseDTO[] | null;
    onVersionChange: (version: ProcessVersionEntity) => void;
    onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
    onSavingChange?: (isSaving: boolean) => void;
    onValidationErrorChange?: (hasValidationError: boolean) => void;
}

export interface ProcessSettingsDialogVersionTabHandle {
    save: () => void;
    reset: () => void;
}

const caseNumberTypeOptions = [
    {
        label: 'UUID',
        subLabel: 'Erzeugt einen technischen UUID-Vorgangsschlüssel, z. B. 550e8400-e29b-41d4-a716-446655440000.',
        value: CASE_NUMBER_TYPE_UUID,
    },
    {
        label: 'Formatvorlage',
        subLabel: 'Erzeugt fortlaufende Vorgangsschlüssel nach einem Muster, z. B. VG-%YYY-%I(6).',
        value: CASE_NUMBER_TYPE_TEMPLATE,
    },
];

const PROCESS_VERSION_NOTES_MAX_LENGTH = 2048;

export const ProcessSettingsDialogVersionTab = forwardRef<ProcessSettingsDialogVersionTabHandle, ProcessSettingsDialogVersionTabProps>(function ProcessSettingsDialogVersionTab(props, ref) {
    const dispatch = useAppDispatch();

    const {
        open,
        version,
        departments,
        themes,
        onVersionChange,
        onUnsavedChangesChange,
        onSavingChange,
        onValidationErrorChange,
    } = props;

    const [draft, setDraft] = useState<ProcessVersionEntity>(version);
    const [isSaving, setIsSaving] = useState(false);

    const isEditable = version.status === ProcessStatus.Drafted;
    const caseNumberType = getCaseNumberType(draft.caseNumberTemplate);
    const themeOptions = useMemo<SelectFieldComponentOption[]>(() => {
        return (themes ?? []).map((theme) => ({
            value: theme.id.toString(),
            label: theme.name,
        }));
    }, [themes]);
    const departmentsById = useMemo(() => {
        return new Map(departments.map((department) => [
            department.id,
            department,
        ]));
    }, [departments]);
    const getDepartmentById = (departmentId: number | null | undefined) => {
        return departmentId != null ? departmentsById.get(departmentId) : undefined;
    };
    const legalSupportDepartment = getDepartmentById(draft.legalSupportDepartmentId);
    const technicalSupportDepartment = getDepartmentById(draft.technicalSupportDepartmentId);
    const imprintDepartment = getDepartmentById(draft.imprintDepartmentId);
    const privacyDepartment = getDepartmentById(draft.privacyDepartmentId);
    const accessibilityDepartment = getDepartmentById(draft.accessibilityDepartmentId);

    useEffect(() => {
        if (open) {
            setDraft(version);
        }
    }, [open, version]);

    const publicTitleError = useMemo(() => {
        const title = draft.publicTitle.trim();

        if (title.length === 0) {
            return 'Bitte geben Sie eine öffentliche Bezeichnung an.';
        }

        if (title.length < 3) {
            return 'Die öffentliche Bezeichnung muss mindestens 3 Zeichen lang sein.';
        }

        if (title.length > 96) {
            return 'Die öffentliche Bezeichnung darf maximal 96 Zeichen lang sein.';
        }

        return undefined;
    }, [draft.publicTitle]);

    const caseNumberTemplateError = useMemo(() => {
        if (caseNumberType !== CASE_NUMBER_TYPE_TEMPLATE) {
            return undefined;
        }

        return validateCaseNumberTemplate(draft.caseNumberTemplate);
    }, [caseNumberType, draft.caseNumberTemplate]);

    const notesError = useMemo(() => {
        if ((draft.notes?.length ?? 0) > PROCESS_VERSION_NOTES_MAX_LENGTH) {
            return `Die Notizen dürfen maximal ${PROCESS_VERSION_NOTES_MAX_LENGTH} Zeichen lang sein.`;
        }

        return undefined;
    }, [draft.notes]);

    const hasValidationError =
        publicTitleError != null ||
        caseNumberTemplateError != null ||
        notesError != null;

    const hasUnsavedChanges = useMemo(() => {
        return !deepEquals(
            {
                publicTitle: version.publicTitle,
                caseNumberTemplate: version.caseNumberTemplate,
                notes: version.notes,
                themeId: version.themeId,
                legalSupportDepartmentId: version.legalSupportDepartmentId,
                technicalSupportDepartmentId: version.technicalSupportDepartmentId,
                imprintDepartmentId: version.imprintDepartmentId,
                privacyDepartmentId: version.privacyDepartmentId,
                accessibilityDepartmentId: version.accessibilityDepartmentId,
                processSpecificPrivacyStatement: version.processSpecificPrivacyStatement,
                processSpecificAccessibilityStatement: version.processSpecificAccessibilityStatement,
            },
            {
                publicTitle: draft.publicTitle,
                caseNumberTemplate: draft.caseNumberTemplate,
                notes: draft.notes,
                themeId: draft.themeId,
                legalSupportDepartmentId: draft.legalSupportDepartmentId,
                technicalSupportDepartmentId: draft.technicalSupportDepartmentId,
                imprintDepartmentId: draft.imprintDepartmentId,
                privacyDepartmentId: draft.privacyDepartmentId,
                accessibilityDepartmentId: draft.accessibilityDepartmentId,
                processSpecificPrivacyStatement: draft.processSpecificPrivacyStatement,
                processSpecificAccessibilityStatement: draft.processSpecificAccessibilityStatement,
            },
        );
    }, [
        draft.accessibilityDepartmentId,
        draft.caseNumberTemplate,
        draft.imprintDepartmentId,
        draft.legalSupportDepartmentId,
        draft.notes,
        draft.privacyDepartmentId,
        draft.processSpecificAccessibilityStatement,
        draft.processSpecificPrivacyStatement,
        draft.publicTitle,
        draft.technicalSupportDepartmentId,
        draft.themeId,
        version.accessibilityDepartmentId,
        version.caseNumberTemplate,
        version.imprintDepartmentId,
        version.legalSupportDepartmentId,
        version.notes,
        version.privacyDepartmentId,
        version.processSpecificAccessibilityStatement,
        version.processSpecificPrivacyStatement,
        version.publicTitle,
        version.technicalSupportDepartmentId,
        version.themeId,
    ]);

    useEffect(() => {
        onUnsavedChangesChange?.(hasUnsavedChanges);
    }, [hasUnsavedChanges, onUnsavedChangesChange]);

    useEffect(() => {
        return () => {
            onUnsavedChangesChange?.(false);
        };
    }, [onUnsavedChangesChange]);

    useEffect(() => {
        onSavingChange?.(isSaving);
    }, [isSaving, onSavingChange]);

    useEffect(() => {
        return () => {
            onSavingChange?.(false);
        };
    }, [onSavingChange]);

    useEffect(() => {
        onValidationErrorChange?.(hasValidationError);
    }, [hasValidationError, onValidationErrorChange]);

    useEffect(() => {
        return () => {
            onValidationErrorChange?.(false);
        };
    }, [onValidationErrorChange]);

    const handleSave = useCallback(() => {
        if (!isEditable || !hasUnsavedChanges || isSaving || hasValidationError) {
            return;
        }

        const nextVersion: ProcessVersionEntity = {
            ...version,
            publicTitle: draft.publicTitle.trim(),
            caseNumberTemplate: caseNumberType === CASE_NUMBER_TYPE_TEMPLATE ? draft.caseNumberTemplate?.trim() ?? '' : null,
            notes: draft.notes?.trim() === '' ? null : draft.notes?.trim() ?? null,
            themeId: draft.themeId,
            legalSupportDepartmentId: draft.legalSupportDepartmentId,
            technicalSupportDepartmentId: draft.technicalSupportDepartmentId,
            imprintDepartmentId: draft.imprintDepartmentId,
            privacyDepartmentId: draft.privacyDepartmentId,
            accessibilityDepartmentId: draft.accessibilityDepartmentId,
            processSpecificPrivacyStatement: draft.processSpecificPrivacyStatement?.trim() === '' ? null : draft.processSpecificPrivacyStatement?.trim() ?? null,
            processSpecificAccessibilityStatement: draft.processSpecificAccessibilityStatement?.trim() === '' ? null : draft.processSpecificAccessibilityStatement?.trim() ?? null,
        };

        setIsSaving(true);

        new ProcessDefinitionVersionApiService()
            .update({
                processDefinitionId: version.processId,
                processDefinitionVersion: version.processVersion,
            }, nextVersion)
            .then((updatedVersion) => {
                onVersionChange(updatedVersion);
                setDraft(updatedVersion);
                dispatch(showSuccessSnackbar('Die versionsspezifischen Einstellungen wurden gespeichert.'));
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die versionsspezifischen Einstellungen konnten nicht gespeichert werden.'));
            })
            .finally(() => {
                setIsSaving(false);
            });
    }, [caseNumberType, dispatch, draft, hasUnsavedChanges, hasValidationError, isEditable, isSaving, onVersionChange, version]);

    const handleReset = useCallback(() => {
        setDraft(version);
    }, [version]);

    useImperativeHandle(ref, () => ({
        save: handleSave,
        reset: handleReset,
    }), [handleReset, handleSave]);

    return (
        <Stack spacing={3}>
            <ElementEditorSectionHeader
                title="Versionsspezifische Einstellungen"
                variant="h5"
                disableMarginTop
                maxWidth={680}
            >
                Diese Angaben gelten nur für die aktuell geöffnete Prozessversion und werden erst wirksam, wenn diese Version veröffentlicht wird.
            </ElementEditorSectionHeader>

            {
                !isEditable &&
                <Alert severity="info">
                    Versionsspezifische Einstellungen können nur in Entwurfsversionen geändert werden.
                </Alert>
            }

            <TextFieldComponent
                label="Öffentliche Bezeichnung"
                value={draft.publicTitle}
                onChange={(val) => {
                    setDraft({
                        ...draft,
                        publicTitle: val ?? '',
                    });
                }}
                required
                disabled={!isEditable || isSaving}
                error={publicTitleError}
                minCharacters={3}
                maxCharacters={96}
                hint="Diese Bezeichnung wird öffentlich im Kontext der Prozessversion verwendet. Formulare können z. B. darauf zurückfallen, wenn das Formular-Prozesselement keine eigene öffentliche Bezeichnung hat."
            />

            <RichTextInputComponent
                label="Notizen zu dieser Prozessversion"
                value={draft.notes}
                onChange={(val) => {
                    setDraft({
                        ...draft,
                        notes: val,
                    });
                }}
                disabled={!isEditable || isSaving}
                error={notesError}
                hint="Halten Sie übergreifende Hinweise zur aktuell geöffneten Prozessversion fest, z. B. offene Punkte, Annahmen oder spätere Ergänzungen der Prozesskonfiguration."
            />

            <ElementEditorSectionHeader
                title="Erscheinungsbild"
                variant="h6"
                disableMarginTop
                disableMarginBottom
                maxWidth={680}
            >
                Das ausgewählte Erscheinungsbild gilt für alle Formulare dieser Prozessversion.
            </ElementEditorSectionHeader>

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    maxWidth: 680,
                }}
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
                        value={draft.themeId?.toString() ?? null}
                        onChange={(value) => {
                            setDraft({
                                ...draft,
                                themeId: value != null ? parseInt(value) : null,
                            });
                        }}
                        options={themeOptions}
                        disabled={!isEditable || isSaving}
                    />
                }
                <Hint
                    summary="Sie können ein abweichendes Erscheinungsbild für alle Formulare dieser Prozessversion auswählen."
                    detailsTitle="Erscheinungsbild"
                    details={
                        <>
                            <p>
                                Erscheinungsbilder werden nach folgendem Prioritätsprinzip angewendet. Der erste passende
                                Eintrag in der folgenden Liste wird verwendet:
                            </p>
                            <ol>
                                <li>Das Erscheinungsbild der Prozessversion</li>
                                <li>Das Erscheinungsbild der zuständigen Organisationseinheit (Nur in Formularen)</li>
                                <li>Das Erscheinungsbild der bewirtschaftenden Organisationseinheit (Nur in Formularen)</li>
                                <li>Das Erscheinungsbild der entwickelnden Organisationseinheit</li>
                                <li>Das globale Erscheinungsbild der Prosuna-Instanz</li>
                            </ol>
                            <p>
                                Das Erscheinungsbild legt Farben, Logo und Favicon aller Formulare der Prozessversion fest.
                            </p>
                        </>
                    }
                    sx={{ml: 2}}
                />
            </Box>

            <ElementEditorSectionHeader
                title="Rechtliche Angaben"
                variant="h6"
                disableMarginTop
                disableMarginBottom
                maxWidth={680}
            >
                Rechtstexte werden auf Ebene der Organisationseinheiten hinterlegt und verwaltet. Diese Angaben gelten für alle Formulare dieser Prozessversion.
            </ElementEditorSectionHeader>

            <Grid
                container
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <DepartmentSelectField
                        label="Text für das Impressum"
                        value={imprintDepartment ?? null}
                        onChange={(department) => {
                            setDraft({
                                ...draft,
                                imprintDepartmentId: department?.id ?? null,
                            });
                        }}
                        required
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <DepartmentSelectField
                        label="Allgemeiner Teil der Datenschutzerklärung"
                        value={privacyDepartment ?? null}
                        onChange={(department) => {
                            setDraft({
                                ...draft,
                                privacyDepartmentId: department?.id ?? null,
                            });
                        }}
                        required
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <DepartmentSelectField
                        label="Allgemeiner Teil der Barrierefreiheitserklärung"
                        value={accessibilityDepartment ?? null}
                        onChange={(department) => {
                            setDraft({
                                ...draft,
                                accessibilityDepartmentId: department?.id ?? null,
                            });
                        }}
                        required
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <RichTextInputComponent
                        label="Prozessspezifischer Teil der Datenschutzerklärung"
                        value={draft.processSpecificPrivacyStatement}
                        hint="Beschreiben Sie hier die prozessspezifischen Datenschutzinformationen nach Art. 13 DSGVO, insbesondere konkret verarbeitete Daten, Zwecke, Rechtsgrundlagen, Empfänger, Speicherdauer und zuständige Stellen."
                        onChange={(value) => {
                            setDraft({
                                ...draft,
                                processSpecificPrivacyStatement: value,
                            });
                        }}
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <RichTextInputComponent
                        label="Prozessspezifischer Teil der Barrierefreiheitserklärung"
                        value={draft.processSpecificAccessibilityStatement}
                        onChange={(value) => {
                            setDraft({
                                ...draft,
                                processSpecificAccessibilityStatement: value,
                            });
                        }}
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
            </Grid>

            <ElementEditorSectionHeader
                title="Kontakte"
                variant="h6"
                disableMarginTop
                disableMarginBottom
                maxWidth={680}
            >
                Kontaktinformationen werden auf Ebene der Organisationseinheit hinterlegt und verwaltet. Diese Angaben gelten für alle Formulare dieser Prozessversion.
            </ElementEditorSectionHeader>

            <Grid
                container
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <DepartmentSelectField
                        label="Fachlicher Support"
                        value={legalSupportDepartment ?? null}
                        onChange={(department) => {
                            setDraft({
                                ...draft,
                                legalSupportDepartmentId: department?.id ?? null,
                            });
                        }}
                        required
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <DepartmentSelectField
                        label="Technischer Support"
                        value={technicalSupportDepartment ?? null}
                        onChange={(department) => {
                            setDraft({
                                ...draft,
                                technicalSupportDepartmentId: department?.id ?? null,
                            });
                        }}
                        required
                        disabled={!isEditable || isSaving}
                    />
                </Grid>
            </Grid>

            <ElementEditorSectionHeader
                title="Vorgangsschlüssel"
                variant="h6"
                disableMarginTop
                disableMarginBottom
                maxWidth={680}
            >
                Legen Sie fest, wie neue Vorgänge dieser Version einen Vorgangsschlüssel erhalten.
            </ElementEditorSectionHeader>

            <RadioFieldComponent
                label="Vorgangsschlüssel-Typ"
                value={caseNumberType}
                onChange={(val) => {
                    setDraft({
                        ...draft,
                        caseNumberTemplate: val === CASE_NUMBER_TYPE_TEMPLATE ? draft.caseNumberTemplate ?? '' : null,
                    });
                }}
                options={caseNumberTypeOptions}
                required
                disabled={!isEditable || isSaving}
            />

            {
                caseNumberType === CASE_NUMBER_TYPE_TEMPLATE &&
                <TextFieldComponent
                    label="Vorgangsschlüssel-Formatvorlage"
                    value={draft.caseNumberTemplate}
                    onChange={(val) => {
                        setDraft({
                            ...draft,
                            caseNumberTemplate: val ?? '',
                        });
                    }}
                    required
                    disabled={!isEditable || isSaving}
                    error={caseNumberTemplateError}
                    minCharacters={3}
                    maxCharacters={CASE_NUMBER_TEMPLATE_MAX_LENGTH}
                    hint="Unterstützte Platzhalter: %YYY, %Y, %M, %D, %h, %m und genau einmal %I(n) mit 4 bis 12 Stellen, z. B. VG-%YYY-%I(6)."
                />
            }
        </Stack>
    );
});
