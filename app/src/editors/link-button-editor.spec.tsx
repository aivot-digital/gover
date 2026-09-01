import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ElementType} from '../data/element-type/element-type';
import type {LinkButtonElement} from '../models/elements/form/content/link-button-element';
import {LinkButtonEditor} from './link-button-editor';

vi.mock('../components/text-field/text-field-component', () => ({
    TextFieldComponent: (props: {
        label: string;
        value?: string | null;
        onChange: (value: string | null) => void;
        disabled?: boolean;
    }) => (
        <input
            aria-label={props.label}
            value={props.value ?? ''}
            disabled={props.disabled}
            onChange={(event) => props.onChange(event.target.value.length > 0 ? event.target.value : null)}
        />
    ),
}));

vi.mock('../components/select-field/select-field-component', () => ({
    SelectFieldComponent: (props: {
        label: string;
        value?: string | null;
        options: { label: string; value: string }[];
        onChange: (value: string | null) => void;
        disabled?: boolean;
    }) => (
        <select
            aria-label={props.label}
            value={props.value ?? ''}
            disabled={props.disabled}
            onChange={(event) => props.onChange(event.target.value.length > 0 ? event.target.value : null)}
        >
            {props.options.map((option) => (
                <option
                    key={option.value}
                    value={option.value}
                >
                    {option.label}
                </option>
            ))}
        </select>
    ),
}));

vi.mock('../components/checkbox-field/checkbox-field-component', () => ({
    CheckboxFieldComponent: (props: {
        label: string;
        value?: boolean | null;
        onChange: (value: boolean) => void;
        disabled?: boolean;
    }) => (
        <input
            type="checkbox"
            aria-label={props.label}
            checked={props.value ?? false}
            disabled={props.disabled}
            onChange={(event) => props.onChange(event.target.checked)}
        />
    ),
}));

describe('LinkButtonEditor', () => {
    it('should patch link, presentation, and target-tab settings', () => {
        const onPatch = vi.fn();

        renderEditor(createElement(), onPatch);

        fireEvent.change(screen.getByLabelText('Beschriftung'), {
            target: {
                value: 'Zur Website',
            },
        });
        fireEvent.change(screen.getByLabelText('Link'), {
            target: {
                value: 'https://example.org',
            },
        });
        fireEvent.click(screen.getByLabelText('In neuem Tab öffnen'));
        fireEvent.change(screen.getByLabelText('Variante'), {
            target: {
                value: 'outlined',
            },
        });
        fireEvent.change(screen.getByLabelText('Farbe'), {
            target: {
                value: 'secondary',
            },
        });

        expect(onPatch).toHaveBeenCalledWith({label: 'Zur Website'});
        expect(onPatch).toHaveBeenCalledWith({href: 'https://example.org'});
        expect(onPatch).toHaveBeenCalledWith({openInNewTab: false});
        expect(onPatch).toHaveBeenCalledWith({variant: 'outlined'});
        expect(onPatch).toHaveBeenCalledWith({color: 'secondary'});
    });

    it('should clear incompatible targets when switching to staff task events', () => {
        const onPatch = vi.fn();

        renderEditor(createElement({
            href: 'https://example.org',
            customerTaskEvent: 'submit',
        }), onPatch);

        fireEvent.change(screen.getByLabelText('Ziel'), {
            target: {
                value: 'staffTaskEvent',
            },
        });

        expect(onPatch).toHaveBeenCalledWith({
            href: null,
            customerTaskEvent: null,
        });
    });

    it('should patch staff task event values', () => {
        const onPatch = vi.fn();

        renderEditor(createElement({
            href: null,
            staffTaskEvent: 'approve',
        }), onPatch);

        fireEvent.change(screen.getByLabelText('Mitarbeitenden-Ereignis'), {
            target: {
                value: 'reject',
            },
        });

        expect(onPatch).toHaveBeenCalledWith({staffTaskEvent: 'reject'});
    });
});

function renderEditor(element: LinkButtonElement, onPatch = vi.fn()) {
    return render(
        <LinkButtonEditor
            element={element}
            onPatch={onPatch}
            editable
            hasSummaryLayoutParent={false}
            scope="application"
        />,
    );
}

function createElement(overrides?: Partial<LinkButtonElement>): LinkButtonElement {
    return {
        id: 'lb_test',
        type: ElementType.LinkButton,
        weight: 12,
        label: 'Link öffnen',
        href: undefined,
        openInNewTab: true,
        staffTaskEvent: undefined,
        customerTaskEvent: undefined,
        variant: 'contained',
        color: 'primary',
        metadata: undefined,
        name: undefined,
        override: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
        ...overrides,
    };
}
