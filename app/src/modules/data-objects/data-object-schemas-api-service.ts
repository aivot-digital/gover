import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {DataObjectSchema} from './models/data-object-schema';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../../models/element-data';
import {type ElementDerivationOptions} from '../elements/elements-api-service';

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

    /**
     * Derives values against the unsaved schema of a new data object schema. Requires the permission to create schemas.
     */
    public async deriveNew(schema: GroupLayout,
                           authoredElementValues: AuthoredElementValues,
                           derivationOptions: ElementDerivationOptions): Promise<DerivedRuntimeElementData> {
        return await this.api.post<DerivedRuntimeElementData>(`${this.path}derive/`, {
            element: schema,
            authoredElementValues,
            derivationOptions,
        });
    }

    /**
     * Derives values against a data object schema. An unsaved schema requires the permission to update schemas;
     * without it, pass null to derive the stored schema.
     */
    public async derive(key: string,
                        unsavedSchema: GroupLayout | null,
                        authoredElementValues: AuthoredElementValues,
                        derivationOptions: ElementDerivationOptions): Promise<DerivedRuntimeElementData> {
        return await this.api.post<DerivedRuntimeElementData>(`${this.buildPath(key)}derive/`, {
            element: unsavedSchema,
            authoredElementValues,
            derivationOptions,
        });
    }
}