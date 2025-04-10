import {ElementMetadata} from '../models/elements/element-metadata';

export enum Idp {
    BayernId = 'bayern_id',
    BundId = 'bund_id',
    Muk = 'muk',
    ShId = 'sh_id',
}

export const IdpMappingSource: Record<Idp, keyof ElementMetadata> = {
    [Idp.BayernId]: 'bayernIdMapping',
    [Idp.BundId]: 'bundIdMapping',
    [Idp.Muk]: 'mukMapping',
    [Idp.ShId]: 'shIdMapping',
}