import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData, type AuthoredElementValues} from '../models/element-data';
import type {BaseViewProps} from './base-view';
import type {LinkButtonElement} from '../models/elements/form/content/link-button-element';
import {LinkButtonView} from './link-button-view';
import {
    type TaskViewMode,
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../components/view-dispatcher/view-dispatcher.context';

describe('LinkButtonView', () => {
    it('should render href buttons as links with a trailing link icon', () => {
        renderLinkButton({
            label: 'Mehr erfahren',
            href: 'https://example.org',
            openInNewTab: true,
            variant: 'outlined',
            color: 'secondary',
        });

        const link = screen.getByRole('link', {name: /Mehr erfahren/});
        expect(link).toHaveAttribute('href', 'https://example.org');
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        expect(link.querySelector('svg')).not.toBeNull();
    });

    it('should trigger staff task events in staff task views', () => {
        const onEvent = vi.fn().mockResolvedValue(undefined);
        const authoredElementValues = {
            requiredField: 'value',
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
});

function renderLinkButton(
    overrides: Partial<LinkButtonElement>,
    taskViewMode: TaskViewMode | null = null,
    propOverrides?: Partial<BaseViewProps<LinkButtonElement, void>>,
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
