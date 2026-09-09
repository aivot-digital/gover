import {renderHook, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {SearchItemService} from '../search-item-service';
import {useRecordRecentSearchItem} from './use-record-recent-search-item';

describe('useRecordRecentSearchItem', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('records an opened item once across edits and reloads of the same identity', () => {
        const record = vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem').mockResolvedValue();
        const {rerender} = renderHook(({item}) => {
            useRecordRecentSearchItem(ServerEntityType.ProcessNodes, item.id.toString());
        }, {initialProps: {item: {id: 17, name: 'Original'}}});

        rerender({item: {id: 17, name: 'Edited'}});
        rerender({item: {id: 17, name: 'Reloaded'}});

        expect(record).toHaveBeenCalledExactlyOnceWith({id: '17', originTable: ServerEntityType.ProcessNodes});

        rerender({item: {id: 18, name: 'Another item'}});
        rerender({item: {id: 17, name: 'Revisited'}});

        expect(record).toHaveBeenCalledTimes(3);
        expect(record).toHaveBeenNthCalledWith(2, {id: '18', originTable: ServerEntityType.ProcessNodes});
        expect(record).toHaveBeenNthCalledWith(3, {id: '17', originTable: ServerEntityType.ProcessNodes});
    });

    it('waits for a valid identity and distinguishes tables with the same item id', () => {
        const record = vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem').mockResolvedValue();
        const initialProps: {originTable?: ServerEntityType; id?: string} = {};
        const {rerender} = renderHook(({originTable, id}) => {
            useRecordRecentSearchItem(originTable, id);
        }, {initialProps});

        rerender({originTable: undefined, id: '17'});
        rerender({originTable: ServerEntityType.ProcessNodes, id: ''});
        expect(record).not.toHaveBeenCalled();

        rerender({originTable: ServerEntityType.ProcessNodes, id: '17'});
        rerender({originTable: ServerEntityType.ProcessInstances, id: '17'});

        expect(record).toHaveBeenCalledTimes(2);
        expect(record).toHaveBeenLastCalledWith({id: '17', originTable: ServerEntityType.ProcessInstances});
    });

    it('handles a failed history request and still records subsequent navigation', async () => {
        const record = vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem')
            .mockRejectedValueOnce(new Error('History unavailable'))
            .mockResolvedValue();
        const {rerender} = renderHook(({id}) => {
            useRecordRecentSearchItem(ServerEntityType.ProcessNodes, id);
        }, {initialProps: {id: '17'}});

        await waitFor(() => expect(record).toHaveBeenCalledTimes(1));
        rerender({id: '18'});
        await waitFor(() => expect(record).toHaveBeenCalledTimes(2));
    });
});
