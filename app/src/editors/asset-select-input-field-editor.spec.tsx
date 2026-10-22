import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AssetSelectInputFieldEditor} from './asset-select-input-field-editor';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';
import {type BaseEditorProps} from './base-editor';
import {type AssetSelectInputElement} from '../models/elements/form/input/asset-select-input-element';

const {listProviders} = vi.hoisted(() => ({listProviders: vi.fn()}));
vi.mock('../modules/assets/assets-api-service', () => ({
    AssetsApiService: class {listStorageProviders = listProviders;},
}));
vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

describe('AssetSelectInputFieldEditor', () => {
    beforeEach(() => {
        listProviders.mockReset().mockResolvedValue([{id: 7, name: 'Vorlagen', readOnlyStorage: true}]);
    });

    it('offers asset providers including read-only storage and an unrestricted option', async () => {
        const onPatch = vi.fn();
        const element = generateElementWithDefaultValues(ElementType.AssetSelectInput);
        const props: BaseEditorProps<AssetSelectInputElement> = {
            element, editable: true, onPatch, hasSummaryLayoutParent: false, scope: 'application',
        };
        const {rerender} = render(<AssetSelectInputFieldEditor {...props}/>);
        const select = screen.getByRole('combobox', {name: /^Speicheranbieter/});
        await waitFor(() => expect(select).not.toHaveAttribute('aria-readonly', 'true'));
        fireEvent.mouseDown(select);
        fireEvent.click(await screen.findByRole('option', {name: 'Vorlagen'}));
        expect(onPatch).toHaveBeenLastCalledWith({storageProviderId: 7});
        rerender(<AssetSelectInputFieldEditor {...props} element={{...element, storageProviderId: 7}}/>);
        fireEvent.mouseDown(select);
        fireEvent.click(await screen.findByRole('option', {name: 'Alle verfügbaren Speicheranbieter'}));
        expect(onPatch).toHaveBeenLastCalledWith({storageProviderId: null});
    });

    it('keeps an unavailable provider configured', async () => {
        const onPatch = vi.fn();
        render(<AssetSelectInputFieldEditor {...{
            element: {...generateElementWithDefaultValues(ElementType.AssetSelectInput), storageProviderId: 99},
            editable: true, onPatch, hasSummaryLayoutParent: false, scope: 'application',
        }}/>);
        await waitFor(() => expect(listProviders).toHaveBeenCalledOnce());
        expect(screen.getByRole('combobox', {name: /^Speicheranbieter/})).toHaveTextContent('Auswahl derzeit nicht verfügbar');
        expect(onPatch).not.toHaveBeenCalled();
    });

    it('reports failed loading without changing the configuration', async () => {
        listProviders.mockRejectedValue(new Error('Forbidden'));
        const onPatch = vi.fn();
        render(<AssetSelectInputFieldEditor {...{
            element: {...generateElementWithDefaultValues(ElementType.AssetSelectInput), storageProviderId: 7},
            editable: true, onPatch, hasSummaryLayoutParent: false, scope: 'application',
        }}/>);
        expect(await screen.findByRole('alert')).toHaveTextContent('Die Speicheranbieter konnten nicht geladen werden.');
        expect(onPatch).not.toHaveBeenCalled();
    });
});
