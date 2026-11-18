import {render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {HtmlTemplateInputComponent} from './html-template-input-component';

const dispatch = vi.hoisted(() => vi.fn());

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => dispatch}));

// Only the template load is under test; the selector and dialog have their own data flows.
vi.mock('../../modules/assets/components/asset-selector', () => ({
    AssetSelector: (props: { label: string; error?: string; disabled?: boolean }) => (
        <div>
            <span>{props.label}</span>
            {props.error != null && <span role="alert">{props.error}</span>}
            {props.disabled === true && <span>disabled</span>}
        </div>
    ),
}));
vi.mock('./html-template-input-component-dialog', () => ({
    HtmlTemplateInputComponentDialog: () => null,
}));

function mockPendingLoadThatRejectsOnAbort() {
    return vi
        .spyOn(AssetsApiService.prototype, 'retrieveByKey')
        .mockImplementation((_key, options) => new Promise((_resolve, reject) => {
            options?.abort?.addEventListener('abort', () => {
                reject(new DOMException('Aborted', 'AbortError'));
            });
        }));
}

async function flushPromises() {
    await new Promise((resolve) => setTimeout(resolve, 0));
}

describe('HtmlTemplateInputComponent', () => {
    beforeEach(() => {
        dispatch.mockClear();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('does not report an error when unmounted while the template is loading', async () => {
        const retrieveByKey = mockPendingLoadThatRejectsOnAbort();

        const {unmount} = render(
            <HtmlTemplateInputComponent
                label="Vorlage"
                value={{assetKey: 'template-a', slots: {}}}
                onChange={vi.fn()}
            />,
        );

        expect(retrieveByKey).toHaveBeenCalledTimes(1);
        unmount();
        await flushPromises();

        expect(dispatch).not.toHaveBeenCalled();
    });

    it('does not report an error when the template changes while loading', async () => {
        const retrieveByKey = mockPendingLoadThatRejectsOnAbort();

        const {rerender} = render(
            <HtmlTemplateInputComponent
                label="Vorlage"
                value={{assetKey: 'template-a', slots: {}}}
                onChange={vi.fn()}
            />,
        );
        rerender(
            <HtmlTemplateInputComponent
                label="Vorlage"
                value={{assetKey: 'template-b', slots: {}}}
                onChange={vi.fn()}
            />,
        );
        await flushPromises();

        expect(retrieveByKey).toHaveBeenCalledTimes(2);
        expect(dispatch).not.toHaveBeenCalled();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        // The superseded load must not end the loading state of the running one.
        expect(screen.getByText('disabled')).toBeInTheDocument();
    });

    it('ends the loading state when the template is removed while loading', async () => {
        mockPendingLoadThatRejectsOnAbort();

        const {rerender} = render(
            <HtmlTemplateInputComponent
                label="Vorlage"
                value={{assetKey: 'template-a', slots: {}}}
                onChange={vi.fn()}
            />,
        );
        rerender(
            <HtmlTemplateInputComponent
                label="Vorlage"
                value={null}
                onChange={vi.fn()}
            />,
        );
        await flushPromises();

        expect(screen.queryByText('disabled')).not.toBeInTheDocument();
        expect(dispatch).not.toHaveBeenCalled();
    });

    it('reports a failed template load', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.spyOn(AssetsApiService.prototype, 'retrieveByKey').mockRejectedValue(new Error('Network'));

        render(
            <HtmlTemplateInputComponent
                label="Vorlage"
                value={{assetKey: 'template-a', slots: {}}}
                onChange={vi.fn()}
            />,
        );

        expect(await screen.findByRole('alert')).toHaveTextContent('Die HTML-Vorlage konnte nicht geladen werden.');
        await waitFor(() => expect(dispatch).toHaveBeenCalledTimes(1));
        expect(screen.queryByText('disabled')).not.toBeInTheDocument();
    });
});
