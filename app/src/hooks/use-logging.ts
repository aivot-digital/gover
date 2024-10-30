import {useSelector} from 'react-redux';
import {useCallback} from 'react';
import {selectShowDebugOutput} from '../slices/admin-settings-slice';

export interface Logger {
    start: (scope: string) => void;
    end: () => void;
    log: (...data: any[]) => void;
}

/**
 * @deprecated irrelevant
 */
export function useLogging(): Logger[] {
    const showDebugOutput = useSelector(selectShowDebugOutput);

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
