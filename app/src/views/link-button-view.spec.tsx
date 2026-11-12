import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData, type AuthoredElementValues, literalAuthoredValue} from '../models/element-data';
import type {BaseViewProps} from './base-view';
import type {LinkButtonElement} from '../models/elements/form/content/link-button-element';
import {LinkButtonView} from './link-button-view';
import {
    type TaskViewMode,
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../components/view-dispatcher/view-dispatcher.context';

describe('LinkButtonView', () => {
    it.each(['javascript:alert(1)', '//example.org', 'example.org', 'https://', 'mailto:', 'tel:'])('does not fall back to a task action for an invalid link: %s', href => {
        const onEvent = vi.fn().mockResolvedValue(undefined);
        renderLinkButton({label: 'Weiter', href, staffTaskEvent: 'complete'}, 'staff', {onEvent});
        const button = screen.getByRole('button', {name: 'Weiter'});
        expect(button).toBeDisabled();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
        fireEvent.click(button);
        expect(onEvent).not.toHaveBeenCalled();
    });

    it('keeps a valid link authoritative when a legacy event is also configured', () => {
        renderLinkButton({label: 'Weiter', href: 'https://example.org', staffTaskEvent: 'complete'}, 'staff');
        expect(screen.getByRole('link', {name: 'Weiter'})).toHaveAttribute('href', 'https://example.org');
    });

    it('allows a task action when the link contains only whitespace', () => {
        const onEvent = vi.fn().mockResolvedValue(undefined);
        renderLinkButton({label: 'Abschließen', href: '  ', staffTaskEvent: 'complete'}, 'staff', {onEvent});
        fireEvent.click(screen.getByRole('button', {name: 'Abschließen'}));
        expect(onEvent).toHaveBeenCalledWith({}, 'complete');
    });

    it.each([true, false])('renders a leading icon independently of the trailing link indicator (new tab=%s)', openInNewTab => {
        renderLinkButton({label: 'Informationen', href: 'https://example.org', icon: 'info', openInNewTab});
        const link = screen.getByRole('link', {name: 'Informationen'});
        expect(link.querySelector('.MuiButton-startIcon svg')).not.toBeNull();
        expect(link.querySelector('.MuiButton-endIcon svg')).toHaveStyle({opacity: '0.65'});
    });

    it('renders a leading icon on task actions without adding a link indicator', () => {
        renderLinkButton({label: 'Abschließen', staffTaskEvent: 'complete', icon: 'check'}, 'staff');
        const button = screen.getByRole('button', {name: 'Abschließen'});
        expect(button.querySelector('.MuiButton-startIcon svg')).not.toBeNull();
        expect(button.querySelector('.MuiButton-endIcon')).toBeNull();
    });

    it.each([undefined, null, 'unknown-icon'])('omits missing or unavailable leading icons (%s)', icon => {
        renderLinkButton({label: 'Informationen', href: 'https://example.org', icon});
        const link = screen.getByRole('link', {name: 'Informationen'});
        expect(link.querySelector('.MuiButton-startIcon')).toBeNull();
        expect(link.querySelector('.MuiButton-endIcon svg')).not.toBeNull();
    });

    describe.each(['success', 'info', 'warning', 'error'] as const)('signal color %s', color => {
        it.each(['link', 'event'])('applies the color to a %s button', mode => {
            renderLinkButton({
                label: 'Aktion', color,
                ...(mode === 'link' ? {href: 'https://example.org'} : {staffTaskEvent: 'complete'}),
            }, 'staff');
            expect(screen.getByRole(mode === 'link' ? 'link' : 'button', {name: 'Aktion'}))
                .toHaveClass(`MuiButton-color${color.charAt(0).toUpperCase()}${color.slice(1)}`);
        });
    });

    it.each(['staff', 'customer'] as const)('keeps an unfinished %s action disabled', taskViewMode => {
        const onEvent = vi.fn().mockResolvedValue(undefined);
        renderLinkButton({
            label: 'Aufgabe abschließen',
            staffTaskEvent: taskViewMode === 'staff' ? '' : null,
            customerTaskEvent: taskViewMode === 'customer' ? '' : null,
        }, taskViewMode, {onEvent});

        const button = screen.getByRole('button', {name: 'Aufgabe abschließen'});
        expect(button).toBeDisabled();
        fireEvent.click(button);
        expect(onEvent).not.toHaveBeenCalled();
    });

    it('should render href buttons as links with a trailing link icon', () => {
        renderLinkButton({
            label: 'Mehr erfahren',
            href: 'https://example.org',
            openInNewTab: true,
            variant: 'outlined',
            color: 'secondary',
        }, null, undefined, true);

        const link = screen.getByRole('link', {name: /Mehr erfahren/});
        expect(link).toHaveAttribute('href', 'https://example.org');
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        expect(link.querySelector('svg')).not.toBeNull();
    });

    it('should trigger staff task events in staff task views', () => {
        const onEvent = vi.fn().mockResolvedValue(undefined);
        const authoredElementValues = {
            requiredField: literalAuthoredValue('value'),
        };

        renderLinkButton(
            {
                label: 'Freigeben',
                staffTaskEvent: 'approve',
            },
            'staff',
            {
                onEvent,
                authoredElementValues,
            },
        );

        fireEvent.click(screen.getByRole('button', {name: 'Freigeben'}));

        expect(onEvent).toHaveBeenCalledWith(authoredElementValues, 'approve');
    });

    it('should not trigger customer task events outside customer task views', () => {
        const onEvent = vi.fn().mockResolvedValue(undefined);

        renderLinkButton(
            {
                label: 'Absenden',
                customerTaskEvent: 'submit',
            },
            'staff',
            {
                onEvent,
            },
        );

        const button = screen.getByRole('button', {name: 'Absenden'});
        expect(button).toBeDisabled();

        fireEvent.click(button);

        expect(onEvent).not.toHaveBeenCalled();
    });

    it('should disable customer task events in read-only task views', () => {
        const onEvent = vi.fn().mockResolvedValue(undefined);

        renderLinkButton(
            {
                label: 'Absenden',
                customerTaskEvent: 'submit',
            },
            'customer',
            {onEvent},
            true,
        );

        const button = screen.getByRole('button', {name: 'Absenden'});
        expect(button).toBeDisabled();
        fireEvent.click(button);
        expect(onEvent).not.toHaveBeenCalled();
    });
});

function renderLinkButton(
    overrides: Partial<LinkButtonElement>,
    taskViewMode: TaskViewMode | null = null,
    propOverrides?: Partial<BaseViewProps<LinkButtonElement, void>>,
    readOnly = false,
) {
    const element = createElement(overrides);
    const authoredElementValues = propOverrides?.authoredElementValues ?? {};
    const derivedData = createDerivedRuntimeElementData();

    return render(
        <ViewDispatcherContextProvider
            value={{
                rootElement: element,
                allElements: [element],
                mode: ViewDispatcherMode.Viewer,
                rootAuthoredElementValues: authoredElementValues,
                rootDerivedData: derivedData,
                taskViewMode,
                readOnly,
            }}
        >
            <LinkButtonView
                {...createBaseProps(element, authoredElementValues, derivedData)}
                {...propOverrides}
            />
        </ViewDispatcherContextProvider>,
    );
}

function createBaseProps(
    element: LinkButtonElement,
    authoredElementValues: AuthoredElementValues,
    derivedData = createDerivedRuntimeElementData(),
): BaseViewProps<LinkButtonElement, void> {
    return {
        element,
        isBusy: false,
        isDeriving: false,
        value: undefined,
        setValue: vi.fn(),
        onBlur: vi.fn(),
        errors: undefined,
        errorDetails: undefined,
        authoredElementValues,
        onAuthoredElementValuesChange: vi.fn(),
        onElementBlur: vi.fn(),
        derivedData,
        onDerive: vi.fn(),
        onEvent: vi.fn(),
        onResetErrors: vi.fn(),
        suppressErrors: false,
        derivationTriggerIdQueue: [],
    };
}

function createElement(overrides: Partial<LinkButtonElement>): LinkButtonElement {
    return {
        id: 'lb_test',
        type: ElementType.LinkButton,
        weight: 12,
        label: undefined,
        href: undefined,
        openInNewTab: undefined,
        staffTaskEvent: undefined,
        customerTaskEvent: undefined,
        variant: undefined,
        color: undefined,
        metadata: undefined,
        name: undefined,
        override: undefined,
        testProtocolSet: undefined,
        visibility: undefined,
        ...overrides,
    };
}
