import {GroupLayout} from '../../../models/elements/form/layout/group-layout';

export const ID_GEN_UUID = '__UUID__';
export const ID_GEN_SERIAL = '__SERIAL__';
export const ID_GEN_CUSTOM = '__CUSTOM__';

export interface DataObjectSchema {
    key: string;
    name: string;
    description: string;
    idGen: '__UUID__' | '__SERIAL__' | '__CUSTOM__' | string;
    schema: GroupLayout;
    created: string;
    updated: string;
}
