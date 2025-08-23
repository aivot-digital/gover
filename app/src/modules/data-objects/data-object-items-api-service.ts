import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {DataObjectItem} from './models/data-object-item';

interface DataObjectItemFilter {
    id: string;
    schemaKey: string;
}

export class DataObjectItemsApiService extends CrudApiService<DataObjectItem, DataObjectItem, DataObjectItem, DataObjectItem, DataObjectItem, string, DataObjectItemFilter> {
    public constructor(api: Api, schemaKey: string) {
        super(api, `data-objects/${schemaKey}/items/`);
    }

    public initialize(): DataObjectItem {
        return {
            id: '',
            schemaKey: '',
            data: {},
        };
    }
}