import {useCallback, useMemo, useState} from 'react';

export function useLoading<T extends string>(): {
    isLoading: boolean;
    setLoadingFlag: (flag: T) => void;
    clearLoadingFlag: (flag: T) => void;
} {
    const [loadingFlags, setLoadingFlags] = useState<T[]>([]);

    const isLoading = useMemo(() => {
        return loadingFlags.length > 0;
    }, [loadingFlags]);

    const setLoadingFlag = useCallback((flag: T) => {
        setLoadingFlags((prevState) => {
            if (prevState.includes(flag)) {
                return prevState;
            }
            return [...prevState, flag];
        });
    }, [loadingFlags, setLoadingFlags]);

    const clearLoadingFlag = useCallback((flag: T) => {
        setLoadingFlags((prevState) => prevState.filter(f => f !== flag));
    }, [setLoadingFlags]);

    return {
        isLoading,
        setLoadingFlag,
        clearLoadingFlag,
    };
}
