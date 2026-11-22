import {useEffect, useMemo, useState} from 'react';
import {ElementType} from '../../../data/element-type/element-type';
import {type NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {type AnyElement} from '../../../models/elements/any-element';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {CodeListsApiService} from '../code-lists-api-service';

interface CodeListOptionsRequest {
    ids: string[];
    key: string;
    codeListKey: string;
    valueColumnKey: string;
    labelColumnKey: string;
}

export interface CodeListOptionsSource {
    // Key of the loaded options in the returned map.
    id: string;
    codeListKey: string | null | undefined;
    valueColumnKey: string | null | undefined;
    // Inputs that store the text itself, such as chip inputs, use the same column for value and label.
    labelColumnKey: string | null | undefined;
}

interface LoadedCodeListOptionsState {
    requestKey: string;
    optionsById: ReadonlyMap<string, NoCodeParameterOption[]>;
}

const pendingRequests = new Map<string, Promise<NoCodeParameterOption[]>>();
const EMPTY_OPTIONS_BY_ID: ReadonlyMap<string, NoCodeParameterOption[]> = new Map();

export function useCodeListElementOptions(
    elements: readonly AnyElement[],
): ReadonlyMap<string, NoCodeParameterOption[]> {
    const requests = useMemo(() => collectElementRequests(elements), [elements]);
    return useCodeListOptionRequests(requests);
}

export function useCodeListOptions(
    sources: readonly CodeListOptionsSource[],
): ReadonlyMap<string, NoCodeParameterOption[]> {
    const requests = useMemo(() => collectSourceRequests(sources), [sources]);
    return useCodeListOptionRequests(requests);
}

function useCodeListOptionRequests(
    requests: CodeListOptionsRequest[],
): ReadonlyMap<string, NoCodeParameterOption[]> {
    const requestKey = useMemo(() => JSON.stringify(requests.map((request) => ([
        request.key,
        request.ids,
    ]))), [requests]);
    const [loadedState, setLoadedState] = useState<LoadedCodeListOptionsState>({
        requestKey: '',
        optionsById: new Map(),
    });

    useEffect(() => {
        let active = true;

        if (requests.length === 0) {
            setLoadedState({
                requestKey,
                optionsById: new Map(),
            });
            return () => {
                active = false;
            };
        }

        Promise.allSettled(requests.map(async (request) => ({
            request,
            options: await loadOptions(request),
        }))).then((results) => {
            if (!active) {
                return;
            }

            const optionsById = new Map<string, NoCodeParameterOption[]>();
            for (const result of results) {
                if (result.status !== 'fulfilled') {
                    continue;
                }

                for (const id of result.value.request.ids) {
                    optionsById.set(id, result.value.options);
                }
            }

            setLoadedState({
                requestKey,
                optionsById,
            });
        });

        return () => {
            active = false;
        };
    }, [requestKey]);

    if (loadedState.requestKey !== requestKey) {
        return EMPTY_OPTIONS_BY_ID;
    }

    return loadedState.optionsById;
}

function collectSourceRequests(sources: readonly CodeListOptionsSource[]): CodeListOptionsRequest[] {
    const requestsByKey = new Map<string, CodeListOptionsRequest>();

    for (const source of sources) {
        addRequest(requestsByKey, source);
    }

    return Array.from(requestsByKey.values());
}

function collectElementRequests(elements: readonly AnyElement[]): CodeListOptionsRequest[] {
    const requestsByKey = new Map<string, CodeListOptionsRequest>();

    for (const element of elements) {
        if (element.type === ElementType.Select || element.type === ElementType.MultiCheckbox) {
            if (element.optionsSource === OptionsSourceType.CodeList) {
                addRequest(requestsByKey, {
                    id: element.id,
                    codeListKey: element.codeListKey,
                    valueColumnKey: element.codeListValueColumnKey,
                    labelColumnKey: element.codeListLabelColumnKey,
                });
            }
        } else if (element.type === ElementType.ChipInput && element.optionsSource === OptionsSourceType.CodeList) {
            // Chip inputs store the text of their suggestion column.
            addRequest(requestsByKey, {
                id: element.id,
                codeListKey: element.codeListKey,
                valueColumnKey: element.codeListLabelColumnKey,
                labelColumnKey: element.codeListLabelColumnKey,
            });
        }
    }

    return Array.from(requestsByKey.values());
}

function addRequest(
    requestsByKey: Map<string, CodeListOptionsRequest>,
    source: CodeListOptionsSource,
): void {
    const codeListKey = source.codeListKey?.trim();
    const valueColumnKey = source.valueColumnKey?.trim();
    const labelColumnKey = source.labelColumnKey?.trim();
    if (!codeListKey || !valueColumnKey || !labelColumnKey) {
        return;
    }

    const requestKey = JSON.stringify([codeListKey, valueColumnKey, labelColumnKey]);
    const existingRequest = requestsByKey.get(requestKey);
    if (existingRequest != null) {
        existingRequest.ids.push(source.id);
        return;
    }

    requestsByKey.set(requestKey, {
        ids: [source.id],
        key: requestKey,
        codeListKey,
        valueColumnKey,
        labelColumnKey,
    });
}

function loadOptions(request: CodeListOptionsRequest): Promise<NoCodeParameterOption[]> {
    const pendingRequest = pendingRequests.get(request.key);
    if (pendingRequest != null) {
        return pendingRequest;
    }

    const trackedRequest = new CodeListsApiService()
        .listOptions(request.codeListKey, request.valueColumnKey, request.labelColumnKey)
        .then((options) => options.map((option) => ({
            label: option.label,
            value: option.value,
        })))
        .finally(() => {
            pendingRequests.delete(request.key);
        });
    pendingRequests.set(request.key, trackedRequest);
    return trackedRequest;
}
