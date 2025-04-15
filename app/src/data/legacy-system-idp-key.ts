import {ElementMetadata} from '../models/elements/element-metadata';

export enum LegacySystemIdpKey {
    BayernId = 'bayern_id',
    BundId = 'bund_id',
    Muk = 'muk',
    ShId = 'sh_id',
}

export const LegacySystemIdpMappingSource: Record<LegacySystemIdpKey, keyof ElementMetadata> = {
    [LegacySystemIdpKey.BayernId]: 'bayernIdMapping',
    [LegacySystemIdpKey.BundId]: 'bundIdMapping',
    [LegacySystemIdpKey.Muk]: 'mukMapping',
    [LegacySystemIdpKey.ShId]: 'shIdMapping',
}