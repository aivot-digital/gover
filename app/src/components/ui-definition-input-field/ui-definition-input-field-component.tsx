import React, {useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {flattenElements} from '../../utils/flatten-elements';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {getElementNameForType} from '../../data/element-type/element-names';
import {ElementType} from '../../data/element-type/element-type';
import {UiDefinitionInputFieldElementItem} from '../../models/elements/form/input/ui-definition-input-field-element';

interface UiDefinitionInputFieldComponentProps {
    label: string;
    hint?: string | null;
    error?: string | null;
    required?: boolean | null;
    disabled?: boolean;
    value?: UiDefinitionInputFieldElementItem | null;
    expectedRootType?: ElementType | null;
    onChange: (value: UiDefinitionInputFieldElementItem | undefined) => void;
}

function buildSummary(value?: UiDefinitionInputFieldElementItem | null): string {
    if (value == null) {
        return 'Keine UI-Definition konfiguriert';
    }

    const elementCount = flattenElements(value).length;
    const countLabel = `${elementCount} Element${elementCount === 1 ? '' : 'e'} enthalten`;
    const rootTypeLabel = getElementNameForType(value.type);
    const rootTitle = generateComponentTitle(value);

    if (rootTitle === rootTypeLabel) {
        return `${countLabel} - Wurzel: ${rootTypeLabel}`;
    }

    return `${countLabel} - Start: ${rootTitle}`;
}

export function UiDefinitionInputFieldComponent(props: UiDefinitionInputFieldComponentProps) {
    const {
        label,
        hint,
        error,
        required,
        disabled,
        value,
        expectedRootType,
        onChange,
    } = props;

    const displayLabel = `${label}${required ? ' *' : ''}`;
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [draftValue, setDraftValue] = useState<UiDefinitionInputFieldElementItem | null | undefined>(value ?? undefined);

    const summary = useMemo(() => {
        return buildSummary(value);
    }, [value]);

    const expectedRootTypeLabel = useMemo(() => {
        if (expectedRootType == null) {
            return null;
        }

        return getElementNameForType(expectedRootType);
    }, [expectedRootType]);

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    mb: 0.75,
                }}
            >
                <Typography variant="subtitle2">
                    {displayLabel}
                </Typography>

                <Button
                    variant="outlined"
                    size="small"
                    startIcon={<Edit />}
                    sx={{ml: 'auto'}}
                    disabled={disabled}
                    onClick={() => {
                        setDraftValue(value ?? undefined);
                        setIsDialogOpen(true);
                    }}
                >
                    Bearbeiten
                </Button>
            </Box>

            <Box
                sx={{
                    border: '1px solid',
                    borderColor: error != null ? 'error.main' : 'divider',
                    borderRadius: 1,
                    px: 1.5,
                    py: 1.25,
                    minHeight: 52,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    gap: 0.5,
                }}
            >
                <Typography
                    variant="body2"
                    title={summary}
                    sx={{
                        color: value == null ? 'text.secondary' : 'text.primary',
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                        lineHeight: 1.4,
                    }}
                >
                    {summary}
                </Typography>

                {
                    expectedRootTypeLabel != null &&
                    <Typography variant="caption" color="text.secondary">
                        Erwarteter Wurzeltyp: {expectedRootTypeLabel}
                    </Typography>
                }
            </Box>

            {
                error == null &&
                hint != null &&
                <Typography
                    sx={{
                        mt: 1,
                        color: 'text.secondary',
                    }}
                    variant="caption"
                >
                    {hint}
                </Typography>
            }

            {
                error != null &&
                <Typography
                    sx={{
                        mt: 1,
                        color: 'error.main',
                    }}
                    variant="caption"
                >
                    {error}
                </Typography>
            }

            <Dialog
                open={isDialogOpen}
                onClose={() => setIsDialogOpen(false)}
                fullWidth
                maxWidth="lg"
            >
                <DialogTitleWithClose onClose={() => setIsDialogOpen(false)}>
                    {displayLabel}
                </DialogTitleWithClose>

                <DialogContent>
                    <Box
                        sx={{
                            pt: 2,
                            pb: 1,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 2,
                        }}
                    >
                        <Box
                            sx={{
                                border: '1px dashed',
                                borderColor: 'divider',
                                borderRadius: 1,
                                px: 2,
                                py: 3,
                            }}
                        >
                            <Typography variant="subtitle2">
                                Editor in Vorbereitung
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{mt: 1, maxWidth: 720}}>
                                Der eigentliche UI-Definition-Editor wird in einem späteren Schritt an dieser Stelle
                                eingebunden. Dieser Dialog ist bereits als Integrationspunkt vorbereitet.
                            </Typography>
                        </Box>
                    </Box>
                </DialogContent>

                <DialogActions>
                    <Button
                        variant="contained"
                        onClick={() => {
                            onChange(draftValue ?? undefined);
                            setIsDialogOpen(false);
                        }}
                    >
                        Übernehmen
                    </Button>
                    <Button onClick={() => setIsDialogOpen(false)}>
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
