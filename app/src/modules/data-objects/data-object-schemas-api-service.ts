import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {DataObjectSchema} from './models/data-object-schema';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {GroupLayout} from '../../models/elements/form/layout/group-layout';

interface DataObjectFilter {
    name: string;
}

export class DataObjectSchemasApiService extends CrudApiService<DataObjectSchema, DataObjectSchema, DataObjectSchema, DataObjectSchema, DataObjectSchema, string, DataObjectFilter> {
    public constructor(api: Api) {
        super(api, 'data-objects/');
    }

    public initialize(): DataObjectSchema {
        return {
            key: '',
            name: '',
            idGen: '__UUID__',
            description: '',
            schema: generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
            displayFields: [],
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }
}