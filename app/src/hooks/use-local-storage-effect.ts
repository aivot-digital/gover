import {useEffect, useState} from 'react';
import {StorageService} from '../services/storage-service';
import {StorageKey} from '../data/storage-key';

/**
 * Only pass useCallback Functions here
 * @param func only useCallback functions
 * @param key the key to listen to
 */
export function useLocalStorageEffect<T>(func: (value: T | null) => void, key: StorageKey) {
    const [value, setValue] = useState<T | null>(() => {
        return StorageService.loadObject<T>(key);
    });

    useEffect(() => {
        const handleStorageChange = (event: StorageEvent) => {
            if (event.key === StorageService.generateKey(key)) {
                const newValue = event.newValue;

                if (newValue == null) {
                    setValue(null);
                } else {
                    const parsedValue = JSON.parse(newValue) as T;
                    setValue(parsedValue);
                }
            }
        };

        window.addEventListener('storage', handleStorageChange);

        return () => {
            window.removeEventListener('storage', handleStorageChange);
        };
    }, [key]);

    useEffect(() => {
        func(value);
    }, [func, value]);
}