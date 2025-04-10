import {useEffect, useState} from 'react';

export function useLocalStorageEffect(func: (value: string | null) => void, key: string) {
    const [value, setValue] = useState(() => {
        return localStorage.getItem(key);
    });

    useEffect(() => {
        const handleStorageChange = (event: StorageEvent) => {
            if (event.key === key) {
                setValue(event.newValue);
            }
        };

        window.addEventListener('storage', handleStorageChange);

        return () => {
            window.removeEventListener('storage', handleStorageChange);
        };
    }, [key]);

    useEffect(() => {
        func(value);
    }, [value, func]);
}