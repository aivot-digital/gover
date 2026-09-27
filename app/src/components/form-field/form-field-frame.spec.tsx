import {afterEach, describe, expect, it, vi} from 'vitest';
import {act, render, screen, waitFor} from '@testing-library/react';
import {FormFieldGroup} from './form-field-group';

afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
});

describe('FormFieldFrame', () => {
    it.each([0.5, 1, 1.5])('keeps layout coordinates independent of a %s viewport scale', (scale) => {
        let onResize = () => {};
        const disconnect = vi.fn();
        vi.stubGlobal('ResizeObserver', class {
            constructor(callback: () => void) { onResize = callback; }
            observe = vi.fn();
            disconnect = disconnect;
        });
        let controlTop = 50;
        vi.spyOn(HTMLElement.prototype, 'offsetTop', 'get').mockImplementation(function (this: HTMLElement) {
            return this.hasAttribute('data-form-field-control') ? controlTop : 0;
        });
        vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get').mockImplementation(function (this: HTMLElement) {
            return this.hasAttribute('data-form-field-control') ? 44 : 40;
        });
        vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
            if (this.hasAttribute('data-form-field-control')) return new DOMRect(0, (100 + controlTop) * scale, 300 * scale, 44 * scale);
            if (this.hasAttribute('data-form-field-external-action')) return new DOMRect(0, 0, 160 * scale, 40 * scale);
            return new DOMRect(0, 100 * scale, 480 * scale, 120 * scale);
        });
        const {unmount} = render(
            <FormFieldGroup label="Auswahl" externalAction={<button>Freigeben</button>}>
                <input aria-label="Eintrag" />
            </FormFieldGroup>,
        );
        const grid = screen.getByRole('button', {name: 'Freigeben'}).parentElement!.parentElement!;
        expect(grid.style.getPropertyValue('--field-action-offset')).toBe('52px');

        controlTop = 90;
        onResize();
        expect(grid.style.getPropertyValue('--field-action-offset')).toBe('92px');

        disconnect.mockClear();
        unmount();
        expect(disconnect).toHaveBeenCalledOnce();
    });

    it('observes newly mounted helpers without resubscribing for edits within the control', async () => {
        const observe = vi.fn();
        const disconnect = vi.fn();
        vi.stubGlobal('ResizeObserver', class {
            observe = observe;
            disconnect = disconnect;
        });
        const {rerender} = render(
            <FormFieldGroup label="Auswahl" externalAction={<button>Freigeben</button>}>
                <span>Text</span>
            </FormFieldGroup>,
        );
        disconnect.mockClear();
        await act(async () => {
            rerender(
                <FormFieldGroup label="Auswahl" externalAction={<button>Freigeben</button>}>
                    <strong>Bearbeiteter Text</strong>
                </FormFieldGroup>,
            );
        });
        expect(disconnect).not.toHaveBeenCalled();

        rerender(
            <FormFieldGroup label="Auswahl" hint="Neuer Hinweis" externalAction={<button>Freigeben</button>}>
                <strong>Bearbeiteter Text</strong>
            </FormFieldGroup>,
        );
        await waitFor(() => expect(observe).toHaveBeenCalledWith(screen.getByText('Neuer Hinweis')));
    });
});
