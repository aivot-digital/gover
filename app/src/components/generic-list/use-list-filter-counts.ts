import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {type FetchListFilterCounts, type ListFilterCounts} from './generic-list-props';

interface CountState {
    context: object;
    counts?: ListFilterCounts;
    busy: boolean;
    failed: boolean;
}

export function useListFilterCounts(fetchCounts: FetchListFilterCounts | undefined, activeTab: string | void, refreshKey: unknown) {
    // Keep old values while refreshing a tab, but never carry them across access or list-scope changes.
    const context = useMemo(() => ({}), [fetchCounts, refreshKey]);
    const [state, setState] = useState<CountState>({context, busy: fetchCounts != null, failed: false});
    const controllerRef = useRef<AbortController | null>(null);

    const refresh = useCallback(() => {
        controllerRef.current?.abort();
        if (fetchCounts == null) {
            setState({context, busy: false, failed: false});
            return;
        }

        const controller = new AbortController();
        controllerRef.current = controller;
        setState(previous => ({
            context,
            counts: previous.context === context ? previous.counts : undefined,
            busy: true,
            failed: false,
        }));
        void Promise.resolve()
            .then(() => {
                if (!controller.signal.aborted) {
                    return fetchCounts({signal: controller.signal});
                }
            })
            .then((result) => {
                if (!controller.signal.aborted) {
                    setState({context, counts: result, busy: false, failed: false});
                }
            })
            .catch(() => {
                if (!controller.signal.aborted) {
                    setState({context, busy: false, failed: true});
                }
            });
    }, [fetchCounts, context]);

    useEffect(() => {
        refresh();
        return () => controllerRef.current?.abort();
    }, [refresh, activeTab]);

    return {
        counts: state.context === context ? state.counts : undefined,
        failed: state.context === context && state.failed,
        busy: state.context === context ? state.busy : fetchCounts != null,
        refresh,
    };
}
