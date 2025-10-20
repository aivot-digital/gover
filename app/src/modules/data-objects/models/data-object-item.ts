import {ElementData} from '../../../models/element-data';

export interface DataObjectItem {
    id: string;
    schemaKey: string;
    data: ElementData;
    created: string;
    updated: string;
}
