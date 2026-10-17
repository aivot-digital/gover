import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {InputVariableSource} from '../../models/input-mode';
import {createDerivedRuntimeElementData} from '../../models/element-data';
import {ElementType} from '../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../view-dispatcher/view-dispatcher.context';
import {HtmlTemplateInputComponentDialog} from './html-template-input-component-dialog';
import type {VStorageIndexItemWithAssetEntity} from '../../modules/storage/entities/storage-index-item-entity';

describe('HtmlTemplateInputComponentDialog', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('inserts a selected variable into the edited rich-text slot', async () => {
        const template = `
            <div
                data-slot="message"
                data-slot-type="richtext"
                data-slot-label="Nachricht"
            ></div>
        `;
        vi.spyOn(AssetsApiService.prototype, 'downloadContentInStorageProvider')
            .mockResolvedValue({text: async () => template} as Blob);
        const onChangeSlots = vi.fn();
        const user = userEvent.setup();
        const rootElement = generateElementWithDefaultValues(ElementType.GroupLayout);

        render(
            <ViewDispatcherContextProvider value={{
                mode: ViewDispatcherMode.Viewer,
                rootElement,
                allElements: [rootElement],
                rootAuthoredElementValues: {},
                rootDerivedData: createDerivedRuntimeElementData(),
                inputModeVariables: [{
                    source: InputVariableSource.ProcessData,
                    path: 'person.name',
                    label: 'Name der Person',
                }],
            }}>
                <HtmlTemplateInputComponentDialog
                    open
                    label="Brief"
                    asset={{pathFromRoot: '/template.html', storageProviderId: 1} as VStorageIndexItemWithAssetEntity}
                    slots={{message: 'Hallo '}}
                    onChangeSlots={onChangeSlots}
                    onClose={vi.fn()}
                />
            </ViewDispatcherContextProvider>,
        );

        const iframe = await screen.findByTitle('Brief Vorschau') as HTMLIFrameElement;
        iframe.contentWindow!.document.body.innerHTML = template;
        fireEvent.load(iframe);
        fireEvent.click(iframe.contentWindow!.document.querySelector('[data-slot]')!);

        await user.click(await screen.findByRole('button', {name: 'Variable referenzieren'}));
        await user.click(await screen.findByText('Name der Person'));
        await user.click(screen.getByTestId('use-variable-reference'));

        await waitFor(() => {
            expect(screen.queryByRole('heading', {name: 'Variable referenzieren'})).not.toBeInTheDocument();
        });
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));

        await waitFor(() => {
            expect(onChangeSlots).toHaveBeenCalledWith({
                message: expect.stringContaining('{{ $.person.name }}'),
            });
        });
    });
});
