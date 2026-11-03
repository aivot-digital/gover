import {useEffect, useState} from 'react';
import {CodeListsApiService} from '../code-lists-api-service';
import {type CodeList, getAvailableColumnMappings} from '../models/code-list';

export interface CodeListColumnOption {
    label: string;
    subLabel: string;
    value: string;
}

interface CodeListColumnOptionsState {
    codeListKey: string | null;
    options: CodeListColumnOption[];
    loaded: boolean;
}

// Several column fields of one editor load the same list at once; they share the request while it is pending.
const pendingRequests = new Map<string, Promise<CodeList>>();

/** Lists the columns of a code list for column selections: the name as label, the stable key as value. */
export function useCodeListColumnOptions(codeListKey: string | null | undefined): CodeListColumnOptionsState {
    const key = codeListKey?.trim() || null;
    const [state, setState] = useState<CodeListColumnOptionsState>({
        codeListKey: null,
        options: [],
        loaded: false,
    });

    useEffect(() => {
        if (key == null) {
            setState({codeListKey: null, options: [], loaded: false});
            return;
        }

        let active = true;
        loadCodeList(key)
            .then((codeList) => {
                if (active) {
                    setState({
                        codeListKey: key,
                        options: getAvailableColumnMappings(codeList).map((mapping) => ({
                            label: mapping.name,
                            subLabel: mapping.key,
                            value: mapping.key,
                        })),
                        loaded: true,
                    });
                }
            })
            .catch(() => {
                if (active) {
                    setState({codeListKey: key, options: [], loaded: false});
                }
            });

        return () => {
            active = false;
        };
    }, [key]);

    return state.codeListKey === key ? state : {codeListKey: key, options: [], loaded: false};
}

function loadCodeList(codeListKey: string): Promise<CodeList> {
    const pendingRequest = pendingRequests.get(codeListKey);
    if (pendingRequest != null) {
        return pendingRequest;
    }

    const request = new CodeListsApiService()
        .retrieve(codeListKey)
        .finally(() => {
            pendingRequests.delete(codeListKey);
        });
    pendingRequests.set(codeListKey, request);
    return request;
}
