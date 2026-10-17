import {useEffect} from 'react';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {SearchItemService} from '../search-item-service';

export function useRecordRecentSearchItem(originTable: ServerEntityType | undefined, id: string | undefined): void {
    useEffect(() => {
        if (originTable == null || id == null || id.length === 0) {
            return;
        }

        new SearchItemService()
            .recordRecentSearchItem({id, originTable})
            .catch(() => {
                // Recording navigation history must not interrupt work on the opened item.
            });
    }, [originTable, id]);
}
