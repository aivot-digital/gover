import {AuthoredElementValues} from '../../../models/element-data';

export interface DataObjectItem {
    id: string;
    schemaKey: string;
    data: AuthoredElementValues;
    created: string;
    updated: string;
}
