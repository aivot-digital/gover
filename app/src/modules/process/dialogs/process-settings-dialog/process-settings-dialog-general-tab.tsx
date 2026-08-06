import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useState} from 'react';
import {Box, Button, CircularProgress, InputAdornment, Stack} from '@mui/material';
import History from '@aivot/mui-material-symbols-400-n25-outlined/History';
import MoveGroup from '@aivot/mui-material-symbols-400-n25-outlined/MoveGroup';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {ProcessEntity} from '../../entities/process-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {deepEquals} from '../../../../utils/equality-utils';
import {ProcessSettingsDialogSlugHistoryDialog} from './process-settings-dialog-slug-history-dialog';
import {normalizeProcessSlugInput, PROCESS_SLUG_MAX_LENGTH, validateProcessSlug} from '../../utils/process-slug-utils';
import {ElementEditorSectionHeader} from '../../../../components/element-editor-section-header/element-editor-section-header';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {MoveProcessToDepartmentDialog} from '../move-process-to-department-dialog';
import {DepartmentSelectField} from '../../../departments/components/department-select-field';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

interface ProcessSettingsDialogGeneralTabProps {
    open: boolean;
    process: ProcessEntity;
    departments: VDepartmentShadowedEntity[];
    onProcessChange: (process: ProcessEntity) => void;
    onDeleteProcess: () => void;
    onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
    onSavingChange?: (isSaving: boolean) => void;
    onValidationErrorChange?: (hasValidationError: boolean) => void;
}

export interface ProcessSettingsDialogGeneralTabHandle {
    save: () => void;
    reset: () => void;
}

export const ProcessSettingsDialogGeneralTab = forwardRef<ProcessSettingsDialogGeneralTabHandle, ProcessSettingsDialogGeneralTabProps>(function ProcessSettingsDialogGeneralTab(props, ref) {
    const dispatch = useAppDispatch();

    const {
        open,
        process,
        departments,
        onProcessChange,
        onDeleteProcess,
        onUnsavedChangesChange,
        onSavingChange,
        onValidationErrorChange,
    } = props;

    const [draft, setDraft] = useState<ProcessEntity>(process);
    const [isSaving, setIsSaving] = useState(false);
    const [slugAvailabilityError, setSlugAvailabilityError] = useState<string | undefined>();
    const [isCheckingSlugAvailability, setIsCheckingSlugAvailability] = useState(false);
    const [showSlugHistoryDialog, setShowSlugHistoryDialog] = useState(false);
    const [showMoveDialog, setShowMoveDialog] = useState(false);

    useEffect(() => {
        if (open) {
            setDraft(process);
        }
    }, [open, process]);

    const hasUnsavedChanges = useMemo(() => {
        return !deepEquals(
            {
                internalTitle: process.internalTitle,
                slug: process.slug,
            },
            {
                internalTitle: draft.internalTitle,
                slug: draft.slug,
            },
        );
    }, [draft.internalTitle, draft.slug, process.internalTitle, process.slug]);

    const internalTitleError = useMemo(() => {
        const title = draft.internalTitle.trim();

        if (title.length === 0) {
            return 'Bitte geben Sie einen internen Titel an.';
        }

        if (title.length < 3) {
            return 'Der interne Titel muss mindestens 3 Zeichen lang sein.';
        }

        if (title.length > 96) {
            return 'Der interne Titel darf maximal 96 Zeichen lang sein.';
        }

        return undefined;
    }, [draft.internalTitle]);

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

    const slugFormatError = useMemo(() => {
        return validateProcessSlug(draft.slug);
    }, [draft.slug]);
    const slugError = slugFormatError ?? slugAvailabilityError;

    const managingDepartment = useMemo(() => {
        return departments.find((department) => department.id === process.departmentId);
    }, [departments, process.departmentId]);

    useEffect(() => {
        onValidationErrorChange?.(internalTitleError != null || slugError != null || isCheckingSlugAvailability);
    }, [internalTitleError, isCheckingSlugAvailability, onValidationErrorChange, slugError]);

    useEffect(() => {
        return () => {
            onValidationErrorChange?.(false);
        };
    }, [onValidationErrorChange]);

    useEffect(() => {
        setSlugAvailabilityError(undefined);

        if (draft.slug === process.slug || slugFormatError != null) {
            setIsCheckingSlugAvailability(false);
            return;
        }

        let cancelled = false;
        setIsCheckingSlugAvailability(true);

        const timeoutId = window.setTimeout(() => {
            new ProcessDefinitionApiService()
                .checkSlugAvailability(draft.slug, process.id)
                .then((isAvailable) => {
                    if (!cancelled) {
                        setSlugAvailabilityError(isAvailable ? undefined : 'Dieser URL-Namespace ist bereits vergeben.');
                    }
                })
                .catch((error) => {
                    if (!cancelled) {
                        setSlugAvailabilityError('Die Verfügbarkeit des URL-Namespace konnte nicht geprüft werden.');
                        dispatch(showApiErrorSnackbar(error, 'Die Verfügbarkeit des URL-Namespace konnte nicht geprüft werden.'));
                    }
                })
                .finally(() => {
                    if (!cancelled) {
                        setIsCheckingSlugAvailability(false);
                    }
                });
        }, 500);

        return () => {
            cancelled = true;
            window.clearTimeout(timeoutId);
        };
    }, [dispatch, draft.slug, process.id, process.slug, slugFormatError]);

    const handleSave = useCallback(() => {
        if (!hasUnsavedChanges || isSaving || internalTitleError != null || slugError != null || isCheckingSlugAvailability) {
            return;
        }

        setIsSaving(true);

        new ProcessDefinitionApiService()
            .update(process.id, {
                ...process,
                internalTitle: draft.internalTitle.trim(),
                slug: draft.slug,
            })
            .then((updatedProcess) => {
                onProcessChange(updatedProcess);
                setDraft(updatedProcess);
                dispatch(showSuccessSnackbar('Die Prozesseinstellungen wurden gespeichert.'));
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Die Prozesseinstellungen konnten nicht gespeichert werden.'));
            })
            .finally(() => {
                setIsSaving(false);
            });
    }, [dispatch, draft.internalTitle, draft.slug, hasUnsavedChanges, internalTitleError, isCheckingSlugAvailability, isSaving, onProcessChange, process, slugError]);

    const handleReset = useCallback(() => {
        setDraft(process);
        setSlugAvailabilityError(undefined);
        setIsCheckingSlugAvailability(false);
    }, [process]);

    useImperativeHandle(ref, () => ({
        save: handleSave,
        reset: handleReset,
    }), [handleReset, handleSave]);

    return (
        <>
            <Stack spacing={3}>
                <ElementEditorSectionHeader
                    title="Allgemeine Einstellungen"
                    variant="h5"
                    disableMarginTop
                >
                    Diese Einstellungen gelten versionsunabhängig für den gesamten Prozess. Legen Sie fest, wie der Prozess intern bezeichnet wird und über welchen URL-Namespace seine öffentlichen Einstiegspunkte erreichbar sind.
                </ElementEditorSectionHeader>

                <TextFieldComponent
                    label="Interner Titel"
                    value={draft.internalTitle}
                    onChange={(val) => {
                        setDraft({
                            ...draft,
                            internalTitle: val ?? '',
                        });
                    }}
                    required
                    error={internalTitleError}
                    minCharacters={3}
                    maxCharacters={96}
                    hint="Nur intern sichtbar; dient zur Wiedererkennung des Prozesses in der Verwaltung."
                />

                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: {
                            xs: 'column',
                            sm: 'row',
                        },
                        alignItems: {
                            xs: 'stretch',
                            sm: 'flex-start',
                        },
                        gap: 2,
                    }}
                >
                    <Box
                        sx={{
                            flex: 1,
                            minWidth: 0,
                        }}
                    >
                        <TextFieldComponent
                            label="URL-Namespace des Prozesses"
                            value={draft.slug}
                            onChange={(val) => {
                                const nextSlug = normalizeProcessSlugInput(val) ?? '';
                                setDraft({
                                    ...draft,
                                    slug: nextSlug,
                                });
                                setSlugAvailabilityError(undefined);
                            }}
                            required
                            error={slugError}
                            minCharacters={3}
                            maxCharacters={PROCESS_SLUG_MAX_LENGTH}
                            hint="Wenn Sie den Namespace ändern, wird der bisherige Namespace automatisch umgeleitet, bis Sie die Historie leeren."
                            pattern={{
                                regex: '^[a-z0-9-]+$',
                                message: 'Der URL-Namespace darf nur aus Kleinbuchstaben, Zahlen und Bindestrichen bestehen.',
                            }}
                            muiPassTroughProps={{
                                InputProps: {
                                    startAdornment: (
                                        <InputAdornment position="start" sx={{whiteSpace: 'nowrap', flexShrink: 0}}>
                                            <Box component="span" sx={{whiteSpace: 'nowrap'}}>
                                                /element-typ/
                                            </Box>
                                        </InputAdornment>
                                    ),
                                    endAdornment: (
                                        <InputAdornment position="end" sx={{whiteSpace: 'nowrap', flexShrink: 0}}>
                                            <Box
                                                component="span"
                                                sx={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    gap: 0.75,
                                                    whiteSpace: 'nowrap',
                                                }}
                                            >
                                                {
                                                    isCheckingSlugAvailability &&
                                                    <CircularProgress size={16} color="inherit"/>
                                                }
                                                /element-slug
                                            </Box>
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />
                    </Box>

                    <Button
                        variant="outlined"
                        startIcon={<History/>}
                        onClick={() => {
                            setShowSlugHistoryDialog(true);
                        }}
                        sx={{
                            mt: 3,
                            alignSelf: {
                                xs: 'stretch',
                                sm: 'flex-start',
                            },
                            whiteSpace: 'nowrap',
                        }}
                    >
                        URL-Namespace-Historie anzeigen
                    </Button>
                </Box>

                <ElementEditorSectionHeader
                    title="Verwaltende Organisationseinheit"
                    variant="h6"
                    disableMarginBottom
                >
                    Die verwaltende Organisationseinheit ist für diesen Prozess zuständig und bildet die Grundlage für prozessbezogene Berechtigungen.
                </ElementEditorSectionHeader>

                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: {
                            xs: 'column',
                            sm: 'row',
                        },
                        alignItems: {
                            xs: 'stretch',
                            sm: 'flex-start',
                        },
                        gap: 2,
                    }}
                >
                    <Box
                        sx={{
                            flex: 1,
                            minWidth: 0,
                        }}
                    >
                        <DepartmentSelectField
                            label="Aktuell verwaltende Organisationseinheit"
                            value={managingDepartment ?? null}
                            onChange={() => undefined}
                            disabled
                            placeholder={
                                departments.length === 0
                                    ? 'Organisationseinheit wird geladen...'
                                    : `Unbekannte Organisationseinheit (${process.departmentId})`
                            }
                            hint={
                                'Versionsunabhängig. Eine Übertragung kann die Sichtbarkeit des Prozesses und Ihre eigenen Berechtigungen verändern.' +
                                (hasUnsavedChanges ? ' Speichern Sie zuerst die allgemeinen Einstellungen.' : '')
                            }
                        />
                    </Box>

                    <Button
                        variant="outlined"
                        startIcon={<MoveGroup/>}
                        disabled={hasUnsavedChanges || isSaving}
                        onClick={() => {
                            // Moving the process changes its permission boundary, so keep it separate from unsaved metadata edits.
                            setShowMoveDialog(true);
                        }}
                        sx={{
                            mt: 3,
                            alignSelf: {
                                xs: 'stretch',
                                sm: 'flex-start',
                            },
                            whiteSpace: 'nowrap',
                        }}
                    >
                        Prozess übertragen
                    </Button>
                </Box>

                <ElementEditorSectionHeader
                    title="Prozess löschen"
                    variant="h6"
                    disableMarginBottom
                >
                    Das Löschen entfernt den gesamten Prozess inklusive aller Versionen, Modellierungen und Vorgänge. Diese Aktion kann nicht rückgängig gemacht werden.
                </ElementEditorSectionHeader>

                <DisabledTooltip
                    disabled={hasUnsavedChanges || isSaving}
                    title={hasUnsavedChanges ? 'Speichern Sie zuerst die allgemeinen Einstellungen.' : undefined}
                    wrapperSx={{
                        alignSelf: 'flex-start',
                    }}
                >
                    <Button
                        variant="outlined"
                        color="error"
                        startIcon={<Delete/>}
                        disabled={hasUnsavedChanges || isSaving}
                        onClick={onDeleteProcess}
                    >
                        Prozess löschen
                    </Button>
                </DisabledTooltip>

                <Box sx={{my: 3}}/>
            </Stack>

            <ProcessSettingsDialogSlugHistoryDialog
                open={showSlugHistoryDialog}
                process={process}
                onClose={() => {
                    setShowSlugHistoryDialog(false);
                }}
            />

            {
                showMoveDialog &&
                <MoveProcessToDepartmentDialog
                    processId={process.id}
                    onClose={() => {
                        setShowMoveDialog(false);
                    }}
                    onMoved={(updatedProcess) => {
                        setShowMoveDialog(false);
                        setDraft(updatedProcess);
                        onProcessChange(updatedProcess);
                    }}
                />
            }
        </>
    );
});
