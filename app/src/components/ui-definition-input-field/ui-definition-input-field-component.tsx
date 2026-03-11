import React, {useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography, useTheme} from '@mui/material';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {flattenElements} from '../../utils/flatten-elements';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {getElementNameForType} from '../../data/element-type/element-names';
import {ElementType} from '../../data/element-type/element-type';
import {UiDefinitionInputFieldElementItem} from '../../models/elements/form/input/ui-definition-input-field-element';
import {ElementTree} from '../element-tree-2/element-tree';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementDerivationContext} from '../../modules/elements/components/element-derivation-context';
import {Allotment} from 'allotment';
import {ElementData} from '../../models/element-data';

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
    const theme = useTheme();

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
    const [showDraftDialog, setShowDraftDialog] = useState<boolean>(false);
    const [draftValue, setDraftValue] = useState<UiDefinitionInputFieldElementItem | null>(null);
    const [inputData, setInputData] = useState<ElementData>({});

    const summary = useMemo(() => {
        return buildSummary(value);
    }, [value]);

    const expectedRootTypeLabel = useMemo(() => {
        if (expectedRootType == null) {
            return null;
        }

        return getElementNameForType(expectedRootType);
    }, [expectedRootType]);

    const defaultValue = useMemo(() => {
        return generateElementWithDefaultValues(expectedRootType ?? ElementType.GroupLayout) as UiDefinitionInputFieldElementItem;
    }, [expectedRootType]);

    const handleClose = () => {
        setShowDraftDialog(false);
        setTimeout(() => {
            setDraftValue(generateElementWithDefaultValues(expectedRootType ?? ElementType.GroupLayout) as UiDefinitionInputFieldElementItem);
            setInputData({});
        }, 300);
    };

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
                    startIcon={<Edit/>}
                    sx={{
                        ml: 'auto',
                    }}
                    disabled={disabled}
                    onClick={() => {
                        setDraftValue(value ?? defaultValue);
                        setShowDraftDialog(true);
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
                    <Typography
                        variant="caption"
                        color="text.secondary"
                    >
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
                open={showDraftDialog}
                onClose={handleClose}
                fullWidth
                maxWidth="xl"
            >
                <DialogTitleWithClose
                    sx={{
                        boxShadow: '0px 1px 3px rgba(0, 0, 0, 0.2)',
                    }}
                    onClose={handleClose}
                >
                    {displayLabel}
                </DialogTitleWithClose>

                <DialogContent
                    sx={{
                        height: 'calc(100vh - 200px)',
                    }}
                >
                    <Allotment vertical>
                        <Allotment>
                            <Allotment.Pane minSize={732}>
                                <Box
                                    sx={{
                                        p: 2,
                                    }}
                                >
                                    <ElementDerivationContext
                                        element={draftValue ?? value ?? defaultValue}
                                        elementData={inputData}
                                        onElementDataChange={setInputData}
                                    />
                                </Box>
                            </Allotment.Pane>
                            <Allotment.Pane
                                minSize={480}
                                preferredSize={480}
                            >
                                <ElementTree
                                    value={draftValue ?? value ?? defaultValue}
                                    onChange={setDraftValue}
                                    editable={!disabled}
                                    drawerZIndexOverride={theme.zIndex.modal + 10}
                                />
                            </Allotment.Pane>
                        </Allotment>
                    </Allotment>
                </DialogContent>

                <DialogActions
                    sx={{
                        borderTop: '1px solid',
                        borderColor: 'divider',
                        display: 'flex',
                        width: '100%',
                        justifyContent: 'flex-start',
                        pt: 2,
                    }}
                >
                    <Button
                        variant="contained"
                        onClick={() => {
                            onChange(draftValue ?? undefined);
                            handleClose();
                        }}
                    >
                        Übernehmen
                    </Button>

                    <Button
                        onClick={() => {
                            // TODO
                        }}
                    >
                        Validierung testen
                    </Button>

                    <Button
                        sx={{
                            ml: 'auto',
                        }}
                        onClick={handleClose}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
