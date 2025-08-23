import {GroupLayout} from '../../../models/elements/form/layout/group-layout';

export const ID_GEN_UUID = '__UUID__';

export interface DataObjectSchema {
    key: string;
    name: string;
    description: string;
    idGen: '__UUID__' | string;
    schema: GroupLayout;
    created: string;
    updated: string;
}
