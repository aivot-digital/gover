import {act, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {FormField} from '../../../components/form-field';
import {NoCodeTreeRow} from './no-code-tree-row';

let notifyResize: () => void;
const disconnect = vi.fn();
const observe = vi.fn();

function Field({top = 60, hint = 'Hinweis'}: {top?: number; hint?: string}) {
    return <FormField label="Summand" hint={hint}>
        {({controlId}) => <div data-testid="control" data-rect-top={top} data-rect-height="44">
            <input id={controlId}/>
        </div>}
    </FormField>;
}

function row() {
    return document.querySelector<HTMLElement>('[data-no-code-tree-row]')!;
}

describe('NoCodeTreeRow', () => {
    beforeEach(() => {
        disconnect.mockClear();
        observe.mockClear();
        vi.stubGlobal('ResizeObserver', class {
            constructor(callback: () => void) {notifyResize = callback;}
            observe = observe;
            disconnect = disconnect;
        });
        vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
            return new DOMRect(0, Number(this.dataset.rectTop ?? 0), 400, Number(this.dataset.rectHeight ?? 200));
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
    });

    it('connects to the visual control instead of the full field including its label and hint', () => {
        render(<NoCodeTreeRow up={false} down><Field/></NoCodeTreeRow>);

        expect(row().style.getPropertyValue('--no-code-connector-y')).toBe('82px');
        expect(observe).toHaveBeenCalledWith(screen.getByTestId('control'));
        expect(row().firstElementChild).toHaveAttribute('aria-hidden', 'true');
    });

    it('tracks label resizing but does not center the connection on additional helper text', () => {
        const {rerender} = render(<NoCodeTreeRow up down><Field/></NoCodeTreeRow>);
        rerender(<NoCodeTreeRow up down><Field top={100} hint="Ein mehrzeiliger Hinweis unter dem Control"/></NoCodeTreeRow>);
        act(() => notifyResize());

        expect(row().style.getPropertyValue('--no-code-connector-y')).toBe('122px');
    });

    it('connects nested expressions to their own operator rather than their first descendant', () => {
        render(<NoCodeTreeRow up down>
            <div data-no-code-expression>
                <div data-no-code-tree-row>
                    <div data-no-code-expression>
                        <div data-no-code-operator data-rect-top="20" data-rect-height="40"/>
                    </div>
                    <Field/>
                </div>
                <div data-no-code-operator data-rect-top="160" data-rect-height="40"/>
            </div>
        </NoCodeTreeRow>);

        expect(row().style.getPropertyValue('--no-code-connector-y')).toBe('180px');
    });

    it('rebinds the anchor when the operand is replaced by a selector', async () => {
        const {rerender} = render(<NoCodeTreeRow up down={false}><Field/></NoCodeTreeRow>);
        rerender(<NoCodeTreeRow up down={false}>
            <div data-no-code-selector data-rect-top="30" data-rect-height="32">Wert oder Ausdruck wählen</div>
        </NoCodeTreeRow>);

        await waitFor(() => expect(row().style.getPropertyValue('--no-code-connector-y')).toBe('46px'));
    });

    it('disconnects its observers on unmount', () => {
        const {unmount} = render(<NoCodeTreeRow up down><Field/></NoCodeTreeRow>);
        disconnect.mockClear();
        unmount();

        expect(disconnect).toHaveBeenCalledOnce();
    });
});
