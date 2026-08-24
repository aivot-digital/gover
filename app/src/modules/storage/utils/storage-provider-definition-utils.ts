import {type StorageProviderDefinition} from '../entities/storage-provider-definition';

export function createStorageProviderDefinitionOption(definition: StorageProviderDefinition) {
    return {
        value: definition.key,
        label: definition.name,
        subLabel: definition.abstractDescription,
    };
}
