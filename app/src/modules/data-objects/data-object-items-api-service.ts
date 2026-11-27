import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {DataObjectItem} from './models/data-object-item';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../../models/element-data';
import {type ElementDerivationOptions} from '../elements/elements-api-service';

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
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }

    public async deriveNew(authoredElementValues: AuthoredElementValues,
                           derivationOptions: ElementDerivationOptions): Promise<DerivedRuntimeElementData> {
        return await this.api.post<DerivedRuntimeElementData>(`${this.path}derive/`, {
            authoredElementValues,
            derivationOptions,
        });
    }

    public async derive(itemId: string,
                        authoredElementValues: AuthoredElementValues,
                        derivationOptions: ElementDerivationOptions): Promise<DerivedRuntimeElementData> {
        return await this.api.post<DerivedRuntimeElementData>(`${this.buildPath(itemId)}derive/`, {
            authoredElementValues,
            derivationOptions,
        });
    }
}