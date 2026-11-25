import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {NoCodeExpressionSummary} from './no-code-expression-summary';

describe('NoCodeExpressionSummary', () => {
    let resize: () => void;
    let fullHeight: number;
    let previewHeight: number;
    let disconnect: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        fullHeight = 40;
        previewHeight = 60;
        disconnect = vi.fn();
        vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(function (this: HTMLElement) {
            return this.id === 'summary' ? fullHeight : 0;
        });
        vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockImplementation(function (this: HTMLElement) {
            return this.id === 'summary' ? previewHeight : 0;
        });
        vi.stubGlobal('ResizeObserver', class {
            constructor(callback: () => void) {
                resize = callback;
            }

            observe = vi.fn();
            disconnect = disconnect;
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows a short sentence without unnecessary display controls', () => {
        render(<NoCodeExpressionSummary id="summary"
                                        summary={{text: 'Geburtsdatum ist gleich heute', details: []}}/>);

        expect(screen.getByText('Geburtsdatum ist gleich heute')).toBeVisible();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('expands and limits only the sentence with keyboard-accessible controls', async () => {
        fullHeight = 120;
        const user = userEvent.setup();
        const parentKeyDown = vi.fn();
        render(<div onKeyDown={parentKeyDown}>
            <NoCodeExpressionSummary id="summary"
                                     summary={{text: 'nicht (erste Bedingung und zweite Bedingung)', details: []}}/>
        </div>);

        const expand = screen.getByRole('button', {name: 'Vollständig anzeigen'});
        expect(expand).toHaveAttribute('aria-controls', 'summary');
        expect(expand).toHaveAttribute('aria-expanded', 'false');
        await user.tab();
        expect(expand).toHaveFocus();
        await user.keyboard('{Enter}');

        const limit = screen.getByRole('button', {name: 'Weniger anzeigen'});
        expect(limit).toHaveAttribute('aria-expanded', 'true');
        expect(limit).toHaveFocus();
        await user.keyboard(' ');
        expect(screen.getByRole('button', {name: 'Vollständig anzeigen'})).toHaveAttribute('aria-expanded', 'false');
        expect(parentKeyDown).not.toHaveBeenCalled();
    });

    it('reveals exact technical values separately from the sentence', async () => {
        const user = userEvent.setup();
        render(<NoCodeExpressionSummary id="summary"
                                        summary={{
                                            text: 'Bundesland ist gleich „Berlin“',
                                            details: [{label: 'Bundesland', value: '$.person.bundesland'}, {label: 'Berlin', value: 'BE'}],
                                        }}/>);

        const details = screen.getByRole('button', {name: 'Technische Details'});
        const overview = document.getElementById(details.getAttribute('aria-controls')!);
        expect(details).toHaveAttribute('aria-expanded', 'false');
        expect(overview).not.toBeVisible();
        expect(screen.getByText('Bundesland ist gleich „Berlin“')).toBeVisible();

        await user.tab();
        await user.keyboard('{Enter}');
        expect(details).toHaveAttribute('aria-expanded', 'true');
        expect(overview).toBeVisible();
        expect(screen.getByText('$.person.bundesland')).toBeVisible();
        expect(screen.getByText('BE')).toBeVisible();

        await user.keyboard(' ');
        expect(overview).not.toBeVisible();
        expect(details).toHaveFocus();
    });

    it('lets Escape reach the containing dialog while a reading control has focus', async () => {
        const user = userEvent.setup();
        const parentKeyDown = vi.fn();
        render(<div onKeyDown={parentKeyDown}>
            <NoCodeExpressionSummary id="summary"
                                     summary={{text: 'Name ist gesetzt', details: [{label: 'Name', value: '$.name'}]}}/>
        </div>);

        await user.tab();
        expect(screen.getByRole('button', {name: 'Technische Details'})).toHaveFocus();
        await user.keyboard('{Escape}');

        expect(parentKeyDown).toHaveBeenCalledOnce();
        expect(parentKeyDown.mock.calls[0][0].key).toBe('Escape');
    });

    it('detects overflow after resizing, including while the sentence is expanded', async () => {
        const user = userEvent.setup();
        const {unmount} = render(<NoCodeExpressionSummary id="summary"
                                                        summary={{text: 'Eine Bedingung mit langem Feldnamen', details: []}}/>);
        expect(screen.queryByRole('button')).not.toBeInTheDocument();

        fullHeight = 100;
        act(() => resize());
        await user.click(screen.getByRole('button', {name: 'Vollständig anzeigen'}));
        expect(screen.getByRole('button', {name: 'Weniger anzeigen'})).toBeInTheDocument();

        fullHeight = 40;
        act(() => resize());
        expect(screen.queryByRole('button')).not.toBeInTheDocument();

        fullHeight = 100;
        act(() => resize());
        expect(screen.getByRole('button', {name: 'Vollständig anzeigen'})).toHaveAttribute('aria-expanded', 'false');

        unmount();
        expect(disconnect).toHaveBeenCalledOnce();
    });

    it('remeasures changed sentences and resets their expanded state', async () => {
        fullHeight = 120;
        const user = userEvent.setup();
        const {rerender} = render(<NoCodeExpressionSummary id="summary"
                                                         summary={{text: 'Eine lange Bedingung', details: []}}/>);
        await user.click(screen.getByRole('button', {name: 'Vollständig anzeigen'}));

        rerender(<NoCodeExpressionSummary id="summary"
                                         summary={{text: 'Eine andere lange Bedingung', details: []}}/>);
        expect(screen.getByRole('button', {name: 'Vollständig anzeigen'})).toHaveAttribute('aria-expanded', 'false');

        fullHeight = 20;
        rerender(<NoCodeExpressionSummary id="summary"
                                         summary={{text: 'Kurze Bedingung', details: []}}/>);
        expect(screen.getByText('Kurze Bedingung')).toBeVisible();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('still measures window resizes when ResizeObserver is unavailable', () => {
        vi.stubGlobal('ResizeObserver', undefined);
        render(<NoCodeExpressionSummary id="summary"
                                        summary={{text: 'Eine Bedingung', details: []}}/>);
        fullHeight = 100;
        act(() => window.dispatchEvent(new Event('resize')));
        expect(screen.getByRole('button', {name: 'Vollständig anzeigen'})).toBeInTheDocument();
    });
});
