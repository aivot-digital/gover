import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useState} from 'react';
import {Alert, Stack} from '@mui/material';
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

interface ProcessSettingsDialogVersionTabProps {
    open: boolean;
    version: ProcessVersionEntity;
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
        onVersionChange,
        onUnsavedChangesChange,
        onSavingChange,
        onValidationErrorChange,
    } = props;

    const [draft, setDraft] = useState<ProcessVersionEntity>(version);
    const [isSaving, setIsSaving] = useState(false);

    const isEditable = version.status === ProcessStatus.Drafted;
    const caseNumberType = getCaseNumberType(draft.caseNumberTemplate);

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

    const hasValidationError = publicTitleError != null || caseNumberTemplateError != null || notesError != null;

    const hasUnsavedChanges = useMemo(() => {
        return !deepEquals(
            {
                publicTitle: version.publicTitle,
                caseNumberTemplate: version.caseNumberTemplate,
                notes: version.notes,
            },
            {
                publicTitle: draft.publicTitle,
                caseNumberTemplate: draft.caseNumberTemplate,
                notes: draft.notes,
            },
        );
    }, [draft.caseNumberTemplate, draft.notes, draft.publicTitle, version.caseNumberTemplate, version.notes, version.publicTitle]);

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
    }, [caseNumberType, dispatch, draft.caseNumberTemplate, draft.notes, draft.publicTitle, hasUnsavedChanges, hasValidationError, isEditable, isSaving, onVersionChange, version]);

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
