import {useState, type ComponentProps} from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {getLiteralElementValue, literalAuthoredValue, type ReplicatingContainerElementValues} from '../../../../models/element-data';
import {ElementType} from '../../../../data/element-type/element-type';
import type {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import type {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import type {DataObjectItem} from '../../models/data-object-item';
import type {DataObjectSchema} from '../../models/data-object-schema';
import {DataObjectItemDetailsPageIndex} from './data-object-item-details-page-index';

vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../../hooks/use-api', () => ({useApi: () => ({})}));
vi.mock('../../../../hooks/use-change-blocker-2', () => ({useChangeBlocker: () => ({dialog: null})}));
vi.mock('../../../permissions/hooks/use-permissions', () => ({useHasSystemPermission: () => true}));
vi.mock('../../../elements/components/element-derivation-context', () => ({
    ElementDerivationContext: (props: ComponentProps<typeof ElementDerivationContext>) => {
        const rows = getLiteralElementValue<ReplicatingContainerElementValues>(props.authoredElementValues, 'rows')!;
        const name = getLiteralElementValue<string>(rows[0].values!, 'name')!;
        return <>
            <input aria-label="Name im Listeneintrag" value={name} onChange={event => {
                props.onAuthoredElementValuesChange({
                    ...props.authoredElementValues,
                    rows: literalAuthoredValue([{
                        ...rows[0], values: {...rows[0].values, name: literalAuthoredValue(event.target.value)},
                    }]),
                });
            }}/>
            <button onClick={() => props.onDerivedDataChange?.({effectiveValues: {rows: []}, elementStates: {}})}>
                Veraltetes Ableitungsergebnis
            </button>
        </>;
    },
}));

const schema: DataObjectSchema = {
    key: 'contacts', name: 'Kontakte', description: '', idGen: '__CUSTOM__', created: '', updated: '', displayFields: [],
    schema: {
        id: 'root', type: ElementType.GroupLayout,
        children: [{id: '$id', type: ElementType.Text}, {
            id: 'rows', type: ElementType.ReplicatingContainer,
            children: [{id: 'name', type: ElementType.Text}],
        }],
    } as GroupLayout,
};

function object(name = 'Ada'): DataObjectItem {
    return {
        id: '1', schemaKey: 'contacts', created: '', updated: '',
        data: {$id: '1', rows: [{id: 'row-1', values: {name}}]},
    };
}

function renderPage(isNewItem: boolean) {
    vi.spyOn(DataObjectSchemasApiService.prototype, 'retrieve').mockResolvedValue(schema);
    const create = vi.spyOn(DataObjectItemsApiService.prototype, 'create').mockResolvedValue(object('Saved'));
    const update = vi.spyOn(DataObjectItemsApiService.prototype, 'update').mockResolvedValue(object('Saved'));
    function Page() {
        const [item, setItem] = useState(object());
        const [isBusy, setIsBusy] = useState(false);
        return <GenericDetailsPageContext.Provider value={{
            item, setItem, isNewItem, isBusy, setIsBusy, isEditable: true,
            setAdditionalData: vi.fn(), refresh: vi.fn(),
        }}>
            <DataObjectItemDetailsPageIndex/>
        </GenericDetailsPageContext.Provider>;
    }
    render(<MemoryRouter initialEntries={['/data-objects/contacts/1']}>
        <Routes><Route path="/data-objects/:schemaKey/:id" element={<Page/>}/></Routes>
    </MemoryRouter>);
    return {create, update};
}

describe('Data-object editor API integration', () => {
    it.each([true, false])('saves current plain inputs and resets the draft from the response (new=%s)', async isNew => {
        const {create, update} = renderPage(isNew);
        const input = await screen.findByRole('textbox', {name: 'Name im Listeneintrag'});
        expect(input).toHaveValue('Ada');
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeDisabled();

        fireEvent.change(input, {target: {value: 'Changed'}});
        fireEvent.click(screen.getByRole('button', {name: 'Veraltetes Ableitungsergebnis'}));
        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expect(isNew ? create : update).toHaveBeenCalledTimes(1));
        if (isNew) {
            expect(create).toHaveBeenCalledWith(object('Changed'));
        } else {
            expect(update).toHaveBeenCalledWith('1', object('Changed'));
        }
        await waitFor(() => expect(input).toHaveValue('Saved'));
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeDisabled();
    });

    it('recognizes when the user restores the original input', async () => {
        renderPage(false);
        const input = await screen.findByRole('textbox', {name: 'Name im Listeneintrag'});
        fireEvent.change(input, {target: {value: 'Changed'}});
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeEnabled();
        fireEvent.change(input, {target: {value: 'Ada'}});
        expect(screen.getByRole('button', {name: 'Speichern'})).toBeDisabled();
    });
});
