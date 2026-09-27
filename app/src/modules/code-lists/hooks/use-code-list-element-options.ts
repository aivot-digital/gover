import {useEffect, useMemo, useState} from 'react';
import {ElementType} from '../../../data/element-type/element-type';
import {type NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {type AnyElement} from '../../../models/elements/any-element';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {BaseApiService} from '../../../services/base-api-service';

interface CodeListOptionsRequest {
    elementIds: string[];
    key: string;
    path: string;
    responseType: 'options' | 'suggestions';
}

interface LoadedCodeListOptionsState {
    requestKey: string;
    optionsByElementId: ReadonlyMap<string, NoCodeParameterOption[]>;
}

const pendingRequests = new Map<string, Promise<NoCodeParameterOption[]>>();
const EMPTY_OPTIONS_BY_ELEMENT_ID: ReadonlyMap<string, NoCodeParameterOption[]> = new Map();

export function useCodeListElementOptions(
    elements: readonly AnyElement[],
): ReadonlyMap<string, NoCodeParameterOption[]> {
    const requests = useMemo(() => collectRequests(elements), [elements]);
    const requestKey = useMemo(() => JSON.stringify(requests.map((request) => ([
        request.key,
        request.elementIds,
    ]))), [requests]);
    const [loadedState, setLoadedState] = useState<LoadedCodeListOptionsState>({
        requestKey: '',
        optionsByElementId: new Map(),
    });

    useEffect(() => {
        let active = true;

        if (requests.length === 0) {
            setLoadedState({
                requestKey,
                optionsByElementId: new Map(),
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

            const optionsByElementId = new Map<string, NoCodeParameterOption[]>();
            for (const result of results) {
                if (result.status !== 'fulfilled') {
                    continue;
                }

                for (const elementId of result.value.request.elementIds) {
                    optionsByElementId.set(elementId, result.value.options);
                }
            }

            setLoadedState({
                requestKey,
                optionsByElementId,
            });
        });

        return () => {
            active = false;
        };
    }, [requestKey]);

    if (loadedState.requestKey !== requestKey) {
        return EMPTY_OPTIONS_BY_ELEMENT_ID;
    }

    return loadedState.optionsByElementId;
}

function collectRequests(elements: readonly AnyElement[]): CodeListOptionsRequest[] {
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

        const requestKey = `${endpoint}:${codeListKey}`;
        const existingRequest = requestsByKey.get(requestKey);
        if (existingRequest != null) {
            existingRequest.elementIds.push(element.id);
            continue;
        }

        requestsByKey.set(requestKey, {
            elementIds: [element.id],
            key: requestKey,
            path: `/api/public/code-lists/${encodeURIComponent(codeListKey)}/${endpoint}/`,
            responseType: element.type === ElementType.ChipInput ? 'suggestions' : 'options',
        });
    }

    return Array.from(requestsByKey.values());
}

function isCodeListBackedElement(element: AnyElement): element is AnyElement & {
    codeListKey: string | null | undefined;
    optionsSource: OptionsSourceType | null | undefined;
} {
    return (
        (
            element.type === ElementType.Select ||
            element.type === ElementType.Radio ||
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
        case ElementType.Radio:
            return 'radio';
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
