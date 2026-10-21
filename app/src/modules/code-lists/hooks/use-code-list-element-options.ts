import {useEffect, useMemo, useState} from 'react';
import {ElementType} from '../../../data/element-type/element-type';
import {type NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {type AnyElement} from '../../../models/elements/any-element';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {BaseApiService} from '../../../services/base-api-service';
import {CodeListValueMode} from '../enums/code-list-value-mode';

type CodeListOptionsResponseType = 'options' | 'suggestions';

interface CodeListOptionsRequest {
    ids: string[];
    key: string;
    path: string;
    responseType: CodeListOptionsResponseType;
}

export interface CodeListOptionsSource {
    // Key of the loaded options in the returned map.
    id: string;
    codeListKey: string | null | undefined;
    valueMode: CodeListValueMode;
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
        const codeListKey = source.codeListKey?.trim();
        if (codeListKey == null || codeListKey.length === 0) {
            continue;
        }

        // Select, radio, and multi-checkbox endpoints map items identically, so select serves every value-based input.
        if (source.valueMode === CodeListValueMode.LabelOnly) {
            addRequest(requestsByKey, source.id, codeListKey, 'chip-input', 'suggestions');
        } else {
            addRequest(requestsByKey, source.id, codeListKey, 'select', 'options');
        }
    }

    return Array.from(requestsByKey.values());
}

function collectElementRequests(elements: readonly AnyElement[]): CodeListOptionsRequest[] {
    const requestsByKey = new Map<string, CodeListOptionsRequest>();

    for (const element of elements) {
        if (!isCodeListBackedElement(element)) {
            continue;
        }

        const codeListKey = element.codeListKey?.trim();
        if (codeListKey == null || codeListKey.length === 0) {
            continue;
        }

        const endpoint = getEndpoint(element.type);
        if (endpoint == null) {
            continue;
        }

        addRequest(
            requestsByKey,
            element.id,
            codeListKey,
            endpoint,
            element.type === ElementType.ChipInput ? 'suggestions' : 'options',
        );
    }

    return Array.from(requestsByKey.values());
}

function addRequest(
    requestsByKey: Map<string, CodeListOptionsRequest>,
    id: string,
    codeListKey: string,
    endpoint: string,
    responseType: CodeListOptionsResponseType,
): void {
    const requestKey = `${endpoint}:${codeListKey}`;
    const existingRequest = requestsByKey.get(requestKey);
    if (existingRequest != null) {
        existingRequest.ids.push(id);
        return;
    }

    requestsByKey.set(requestKey, {
        ids: [id],
        key: requestKey,
        path: `/api/public/code-lists/${encodeURIComponent(codeListKey)}/${endpoint}/`,
        responseType,
    });
}

function isCodeListBackedElement(element: AnyElement): element is AnyElement & {
    codeListKey: string | null | undefined;
    optionsSource: OptionsSourceType | null | undefined;
} {
    return (
        (
            element.type === ElementType.Select ||
            element.type === ElementType.MultiCheckbox ||
            element.type === ElementType.ChipInput
        ) &&
        element.optionsSource === OptionsSourceType.CodeList
    );
}

function getEndpoint(type: ElementType): string | undefined {
    switch (type) {
        case ElementType.Select:
            return 'select';
        case ElementType.MultiCheckbox:
            return 'multi-checkbox';
        case ElementType.ChipInput:
            return 'chip-input';
        default:
            return undefined;
    }
}

function loadOptions(request: CodeListOptionsRequest): Promise<NoCodeParameterOption[]> {
    const pendingRequest = pendingRequests.get(request.key);
    if (pendingRequest != null) {
        return pendingRequest;
    }

    const api = new BaseApiService();
    const requestPromise = request.responseType === 'suggestions'
        ? api
            .get<string[]>(request.path, {skipAuthCheck: true})
            .then((suggestions) => suggestions.map((suggestion) => ({
                label: suggestion,
                value: suggestion,
            })))
        : api.get<NoCodeParameterOption[]>(request.path, {skipAuthCheck: true});

    const trackedRequest = requestPromise.finally(() => {
        pendingRequests.delete(request.key);
    });
    pendingRequests.set(request.key, trackedRequest);
    return trackedRequest;
}
