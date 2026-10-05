import {useLocalStorageEffect} from './use-local-storage-effect';
import {StorageKey} from '../data/storage-key';
import {StorageService} from '../services/storage-service';

export function useShowExperimentalFeatures() {
    return useLocalStorageEffect<string>((value) => {
        return value === 'true';
    }, StorageKey.ShowExperimentalFeatures);
}

export function showExperimentalFeatures() {
    return StorageService.loadObject(StorageKey.ShowExperimentalFeatures) === 'true';
}
