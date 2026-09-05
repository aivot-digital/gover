import {type BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {type StorageProviderType} from '../../../../modules/storage/enums/storage-provider-type';
import {type AnyElement} from '../../any-element';

export enum StoragePathSelectorMode {
    Folder = 'folder',
    File = 'file',
}

export interface StoragePathSelectorInputElement extends BaseInputElement<ElementType.StoragePathSelector> {
    mode: StoragePathSelectorMode | null | undefined;
    placeholder: string | null | undefined;
    storageProviderSelectHint: string | null | undefined;
    allowedStorageProviderTypes: StorageProviderType[] | null | undefined;
    allowReadOnlyStorageProviders: boolean | null | undefined;
}

export interface StoragePathSelectorInputElementValue {
    storageProviderId: number | null;
    path: string | null;
}

export function isStoragePathSelectorInputElement(
    element: AnyElement,
): element is StoragePathSelectorInputElement {
    return element.type === ElementType.StoragePathSelector;
}
