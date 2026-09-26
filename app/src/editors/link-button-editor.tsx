import React from 'react';
import {Grid} from '@mui/material';
import type {BaseEditorProps} from './base-editor';
import type {
    LinkButtonElement,
    LinkButtonElementColor,
    LinkButtonElementVariant,
} from '../models/elements/form/content/link-button-element';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';

type LinkButtonTarget = 'href' | 'staffTaskEvent' | 'customerTaskEvent';

const variantOptions = [
    {label: 'Gefüllt', value: 'contained'},
    {label: 'Text', value: 'text'},
    {label: 'Umrandet', value: 'outlined'},
];

const colorOptions = [
    {label: 'Primär', value: 'primary'},
    {label: 'Sekundär', value: 'secondary'},
];

const targetOptions = [
    {label: 'Link', value: 'href'},
    {label: 'Mitarbeitenden-Ereignis', value: 'staffTaskEvent'},
    {label: 'Kunden-Ereignis', value: 'customerTaskEvent'},
];

export function LinkButtonEditor(props: BaseEditorProps<LinkButtonElement>): React.JSX.Element {
    const {
        element,
        onPatch,
        editable,
    } = props;
    const target = resolveTarget(element);

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
                    label="Ziel"
                    value={target}
                    onChange={(value) => {
                        onPatch(resolveTargetPatch(value as LinkButtonTarget | null));
                    }}
                    options={targetOptions}
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
                        />
                    </Grid>
                </>
            }
            {
                target === 'staffTaskEvent' &&
                <Grid size={{xs: 12, lg: 6}}>
                    <TextFieldComponent
                        value={element.staffTaskEvent}
                        label="Mitarbeitenden-Ereignis"
                        onChange={(staffTaskEvent) => {
                            onPatch({staffTaskEvent});
                        }}
                        disabled={!editable}
                    />
                </Grid>
            }
            {
                target === 'customerTaskEvent' &&
                <Grid size={{xs: 12, lg: 6}}>
                    <TextFieldComponent
                        value={element.customerTaskEvent}
                        label="Kunden-Ereignis"
                        onChange={(customerTaskEvent) => {
                            onPatch({customerTaskEvent});
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
                    includeEmptyOption={false}
                    disabled={!editable}
                />
            </Grid>
        </Grid>
    );
}

function resolveTarget(element: LinkButtonElement): LinkButtonTarget {
    if (element.staffTaskEvent != null && element.staffTaskEvent.trim().length > 0) {
        return 'staffTaskEvent';
    }

    if (element.customerTaskEvent != null && element.customerTaskEvent.trim().length > 0) {
        return 'customerTaskEvent';
    }

    return 'href';
}

function resolveTargetPatch(target: LinkButtonTarget | null): Partial<LinkButtonElement> {
    switch (target ?? 'href') {
        case 'staffTaskEvent':
            return {
                href: null,
                customerTaskEvent: null,
            };
        case 'customerTaskEvent':
            return {
                href: null,
                staffTaskEvent: null,
            };
        case 'href':
        default:
            return {
                staffTaskEvent: null,
                customerTaskEvent: null,
            };
    }
}
