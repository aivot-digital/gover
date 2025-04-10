import {useSelector} from 'react-redux';
import {useCallback, useMemo} from 'react';
import {useAppDispatch} from './use-app-dispatch';
import {logDebug, logError, logInfo, logWarning} from '../slices/logging-slice';

export interface Logger {
    start: (scope: string) => void;
    end: () => void;
    log: (...data: any[]) => void;
}

/**
 * @deprecated irrelevant
 */
export function useLogging(): Logger[] {
    const showDebugOutput = false;

    const startDebug = useCallback((scope: string) => {
        if (showDebugOutput) {
            console.group(scope);
        }
    }, [showDebugOutput]);

    const debug = useCallback((...data: any[]) => {
        if (showDebugOutput) {
            console.log(...data);
        }
    }, [showDebugOutput]);

    const endDebug = useCallback(() => {
        if (showDebugOutput) {
            console.groupEnd();
        }
    }, [showDebugOutput]);

    return [{
        start: startDebug,
        end: endDebug,
        log: debug,
    }];
}

export function useLogger(scope: string) {
    const dispatch = useAppDispatch();

    return useMemo(() => ({
        debug: (data: any) => dispatch(logDebug({
            source: scope,
            message: data,
        })),
        info: (data: any) => dispatch(logInfo({
            source: scope,
            message: data,
        })),
        warn: (data: any) => dispatch(logWarning({
            source: scope,
            message: data,
        })),
        error: (data: any) => dispatch(logError({
            source: scope,
            message: data,
        })),
    }), [dispatch]);
}
