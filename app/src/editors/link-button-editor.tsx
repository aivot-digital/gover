import React, {useState} from 'react';
import {Box, Button, Grid, Typography} from '@mui/material';
import type {BaseEditorProps} from './base-editor';
import type {
    LinkButtonElement,
    LinkButtonElementColor,
    LinkButtonElementVariant,
} from '../models/elements/form/content/link-button-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {FormFieldGroup} from '../components/form-field';
import {StepIconsMap} from '../data/step-icons';
import {IconPickerDialog} from '../dialogs/icon-picker-dialog/icon-picker-dialog';
import {resolveLinkButtonHref} from '../utils/resolve-link-button-href';

type LinkButtonTarget = 'href' | 'staffTaskEvent' | 'customerTaskEvent';

const variantOptions = [
    {label: 'Gefüllt', value: 'contained'},
    {label: 'Text', value: 'text'},
    {label: 'Umrandet', value: 'outlined'},
];

const colorOptions = [
    {label: 'Primär', value: 'primary'},
    {label: 'Sekundär', value: 'secondary'},
    {label: 'Erfolg', value: 'success'},
    {label: 'Information', value: 'info'},
    {label: 'Warnung', value: 'warning'},
    {label: 'Fehler', value: 'error'},
];

const targetOptions = [
    {
        label: 'Link öffnen', value: 'href',
        subLabel: 'Öffnet den angegebenen Link, zum Beispiel eine Website oder einen Download.',
    },
    {
        label: 'Aufgabenaktion für Mitarbeitende', value: 'staffTaskEvent',
        subLabel: 'Löst eine Aktion in der Aufgabenansicht für Mitarbeitende aus. Außerhalb dieser Ansicht ist die Schaltfläche deaktiviert. Vor dem Ausführen werden die Formulareingaben geprüft.',
    },
    {
        label: 'Aufgabenaktion für externe Beteiligte', value: 'customerTaskEvent',
        subLabel: 'Löst eine Aktion in der Aufgabenansicht für externe Beteiligte aus. Außerhalb dieser Ansicht ist die Schaltfläche deaktiviert. Vor dem Ausführen werden die Formulareingaben geprüft.',
    },
];

export function LinkButtonEditor(props: BaseEditorProps<LinkButtonElement>): React.JSX.Element {
    const {
        element,
        onPatch,
        editable,
    } = props;
    const target = resolveTarget(element);
    const [pickerOpen, setPickerOpen] = useState(false);
    const selectedIcon = element.icon ? StepIconsMap[element.icon] : undefined;
    const IconComponent = selectedIcon?.def;

    return (
        <Grid
            container
            columnSpacing={4}
        >
            <Grid size={{xs: 12, lg: 6}}>
                <TextFieldComponent
                    value={element.label}
                    label="Beschriftung"
                    onChange={(label) => {
                        onPatch({label});
                    }}
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Aktion"
                    value={target}
                    onChange={(value) => {
                        onPatch(resolveTargetPatch(value as LinkButtonTarget | null, element));
                    }}
                    options={targetOptions}
                    muiPassTroughProps={{
                        slotProps: {
                            select: {
                                MenuProps: {
                                    sx: {
                                        '& .MuiListItemText-primary, & .MuiListItemText-secondary': {
                                            whiteSpace: 'normal',
                                        },
                                    },
                                },
                            },
                        },
                    }}
                    includeEmptyOption={false}
                    disabled={!editable}
                />
            </Grid>
            {
                target === 'href' &&
                <>
                    <Grid size={{xs: 12, lg: 6}}>
                        <TextFieldComponent
                            value={element.href}
                            label="Link"
                            hint="Geben Sie Webadressen mit Protokoll ein, zum Beispiel https://www.beispiel.de."
                            validate={(value) => value?.trim() && resolveLinkButtonHref(value) == null
                                ? 'Geben Sie einen gültigen Link an, zum Beispiel https://www.beispiel.de. Möglich sind auch mailto:, tel:, /pfad oder #anker.'
                                : undefined}
                            onChange={(href) => {
                                onPatch({href});
                            }}
                            disabled={!editable}
                        />
                    </Grid>
                    <Grid size={{xs: 12, lg: 6}}>
                        <CheckboxFieldComponent
                            label="In neuem Tab öffnen"
                            value={element.openInNewTab ?? true}
                            onChange={(openInNewTab) => {
                                onPatch({openInNewTab});
                            }}
                            disabled={!editable}
                            sx={{mt: 4}}
                        />
                    </Grid>
                </>
            }
            {
                target !== 'href' &&
                <Grid size={{xs: 12, lg: 6}}>
                    <TextFieldComponent
                        value={element[target]}
                        label="Ereigniskennung"
                        hint={'Geben Sie die technische Kennung einer Aktion an, die der jeweilige Prozessknoten unterstützt. Eine frei gewählte Kennung legt keine neue Aktion an.' + (target === 'staffTaskEvent'
                            ? ' Beispiel: „complete“ schließt beim Knoten „Manuelle Aktion ausführen“ die Aufgabe ab.'
                            : ' Die gültigen Kennungen erhalten Sie von den Entwickler:innen des Prozessknotens.')}
                        onChange={(event) => {
                            onPatch({[target]: event ?? ''});
                        }}
                        disabled={!editable}
                    />
                </Grid>
            }
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Variante"
                    value={element.variant ?? 'contained'}
                    onChange={(variant) => {
                        onPatch({variant: variant as LinkButtonElementVariant | null});
                    }}
                    options={variantOptions}
                    includeEmptyOption={false}
                    disabled={!editable}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Farbe"
                    value={element.color ?? 'primary'}
                    onChange={(color) => {
                        onPatch({color: color as LinkButtonElementColor | null});
                    }}
                    options={colorOptions}
                    hint="Primär und Sekundär verwenden die entsprechenden Farben des Erscheinungsbilds. Die weiteren Farben sind Signalfarben."
                    includeEmptyOption={false}
                    disabled={!editable}
                />
            </Grid>
            <Grid size={12}>
                <FormFieldGroup
                    label="Symbol (Icon)"
                    hint="Wird vor der Beschriftung angezeigt."
                    disabled={!editable}
                >
                    <Box sx={{display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2}}>
                        {IconComponent && (
                            <Box sx={{display: 'flex', p: 1, border: '1px solid', borderColor: 'divider', borderRadius: 1, bgcolor: 'action.selected', color: 'text.secondary'}}>
                                <IconComponent/>
                            </Box>
                        )}
                        <Typography variant="body2" color={selectedIcon ? 'text.primary' : 'text.secondary'}>
                            {selectedIcon?.label ?? (element.icon ? 'Symbol nicht verfügbar' : 'Kein Symbol ausgewählt')}
                        </Typography>
                        <Button variant="outlined" disabled={!editable} onClick={() => setPickerOpen(true)}>
                            {element.icon ? 'Symbol ändern' : 'Symbol auswählen'}
                        </Button>
                        {element.icon && (
                            <Button disabled={!editable} onClick={() => onPatch({icon: null})}>
                                Symbol entfernen
                            </Button>
                        )}
                    </Box>
                </FormFieldGroup>
                <IconPickerDialog
                    open={pickerOpen && editable}
                    onClose={() => setPickerOpen(false)}
                    onSelect={(icon) => onPatch({icon})}
                    selectedIconId={element.icon ?? undefined}
                    title="Symbol (Icon) für Schaltfläche auswählen"
                    showLabels
                    autoSelect
                />
            </Grid>
        </Grid>
    );
}

function resolveTarget(element: LinkButtonElement): LinkButtonTarget {
    // Existing definitions may contain multiple targets. A link takes precedence at runtime.
    if (element.href != null && element.href.trim().length > 0) {
        return 'href';
    }

    if (element.staffTaskEvent != null && element.staffTaskEvent.trim().length > 0) {
        return 'staffTaskEvent';
    }

    if (element.customerTaskEvent != null && element.customerTaskEvent.trim().length > 0) {
        return 'customerTaskEvent';
    }

    // An empty string preserves a selected, unfinished action across edits and save/reload.
    // Null/undefined means unselected; execution still requires a non-blank event name.
    if (element.staffTaskEvent != null) {
        return 'staffTaskEvent';
    }
    if (element.customerTaskEvent != null) {
        return 'customerTaskEvent';
    }

    return 'href';
}

function resolveTargetPatch(target: LinkButtonTarget | null, element: LinkButtonElement): Partial<LinkButtonElement> {
    switch (target ?? 'href') {
        case 'staffTaskEvent':
            return {
                href: null,
                staffTaskEvent: element.staffTaskEvent ?? '',
                customerTaskEvent: null,
            };
        case 'customerTaskEvent':
            return {
                href: null,
                staffTaskEvent: null,
                customerTaskEvent: element.customerTaskEvent ?? '',
            };
        case 'href':
        default:
            return {
                staffTaskEvent: null,
                customerTaskEvent: null,
            };
    }
}
