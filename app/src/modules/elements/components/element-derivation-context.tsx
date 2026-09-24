import {InputMode} from '../../../models/input-mode';
import {
    applyComputedErrors,
    AuthoredElementValues,
    clearDerivedErrorsRecursively,
    ComputedElementErrors,
    ComputedElementStates,
    ComputedElementValueSource,
    createComputedElementSubState,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    EffectiveElementValues,
    type EffectiveReplicatingContainerElementValue,
    hasAnyErrorRecursively,
    type ReplicatingContainerElementValue,
    resolveComputedElementSubState,
    resolveComputedElementSubStateStates,
} from '../../../models/element-data';
import {AnyElement} from '../../../models/elements/any-element';
import React, {
    createContext,
    forwardRef,
    RefObject,
    useContext,
    useEffect,
    useImperativeHandle,
    useMemo,
    useRef,
    useState,
} from 'react';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../../../utils/flatten-elements';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {isReplicatingContainerLayout} from '../../../models/elements/form/layout/replicating-container-layout';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementsApiService} from '../elements-api-service';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {isApiError} from '../../../models/api-error';
import {
    applyElementErrorSuppressions,
    collectChangedElementErrorSuppressionTargets,
    type ElementErrorSuppressionTarget,
    mergeElementErrorSuppressionTargets,
    normalizeReplicatingContainerValues,
    preserveDerivedErrors,
} from '../../../utils/element-data-utils';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher/view-dispatcher.component';
import {
    type TaskViewMode,
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../../../components/view-dispatcher/view-dispatcher.context';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';
import {deepEquals} from '../../../utils/equality-utils';
import {type InputVariableSuggestion} from '../../../models/input-mode';

interface ElementDerivationContextProps {
    element: AnyElement;
    scrollContainerRef?: RefObject<HTMLDivElement | null>;
    authoredElementValues: AuthoredElementValues;
    onAuthoredElementValuesChange: (newData: AuthoredElementValues) => void;
    derivedData?: DerivedRuntimeElementData;
    computedErrors?: ComputedElementErrors | null;
    onDerivedDataChange?: (newData: DerivedRuntimeElementData) => void;
    disabled?: boolean;
    readOnly?: boolean;
    onDerivationStarted?: (triggeringElementData: AuthoredElementValues) => void;
    onDerivationFinished?: (derivedElementData: DerivedRuntimeElementData) => void;
    suppressErrors?: boolean;
    onDeriveOverride?: (aev: AuthoredElementValues, skipErrorsForElements: string[]) => Promise<DerivedRuntimeElementData>;
    onEvent?: (values: AuthoredElementValues, event: string) => Promise<boolean | void>;
    mode?: ViewDispatcherMode;
    disableValidation?: boolean;
    disableVisibilities?: boolean;
    highlightedElementId?: string | null;
    taskViewMode?: TaskViewMode | null;
    inputModesEnabled?: boolean;
    inputModeVariables?: InputVariableSuggestion[];
    deriveOnMount?: boolean;
}

export interface ElementDerivationContextHandle {
    replaceAuthoredElementValues: (newData: AuthoredElementValues) => Promise<DerivedRuntimeElementData>;
}

interface ElementDerivationContextType {
    renderMode: ViewDispatcherMode;
    isEditable: boolean;
    showInvisible: boolean;
    showTechnical: boolean;
    scrollContainerRef: RefObject<HTMLDivElement | null> | null;

    rootElement: AnyElement;
    allElements: ElementWithParents[];

    authoredElementValues: AuthoredElementValues;
    derivedRuntimeElementData: DerivedRuntimeElementData | null;
    additionalComputedErrors: ComputedElementErrors | null;

    suppressErrors?: boolean;
}

const ElementDerivationContextObject = createContext<ElementDerivationContextType | null>(null);

const ElementDerivationContextProvider = ElementDerivationContextObject.Provider;

export function useElementDerivationContext(): ElementDerivationContextType {
    const context = useContext(ElementDerivationContextObject);
    if (context == null) {
        // throw new Error('useElementDerivationContext must be used within an ElementDerivationContext');
        return {
            additionalComputedErrors: null,
            allElements: [],
            authoredElementValues: {},
            derivedRuntimeElementData: null,
            isEditable: false,
            renderMode: ViewDispatcherMode.Viewer,
            rootElement: {} as AnyElement,
            scrollContainerRef: null,
            showInvisible: false,
            showTechnical: false,
            suppressErrors: false,
        };
    }
    return context;
}


export const ElementDerivationContext = forwardRef<
    ElementDerivationContextHandle,
    ElementDerivationContextProps
>(function ElementDerivationContext(props, ref) {
    const {
        element,
        scrollContainerRef,
        authoredElementValues,
        onAuthoredElementValuesChange,
        derivedData: controlledDerivedData,
        computedErrors,
        onDerivedDataChange,
        disabled,
        readOnly,
        onDerivationStarted,
        onDerivationFinished,
        suppressErrors,
        onDeriveOverride,
        onEvent,
        mode: renderMode = ViewDispatcherMode.Viewer,
        disableValidation = false,
        disableVisibilities = false,
        highlightedElementId,
        taskViewMode = null,
        inputModesEnabled = false,
        inputModeVariables = [],
        deriveOnMount = true,
    } = props;

    const dispatch = useAppDispatch();

    const [mode, setMode] = useState<'deriving' | 'busy' | 'idle'>('idle');

    const [derivationTriggerIdQueue, setDerivationTriggerIdQueue] = useState<string[]>([]);

    const [internalDerivedData, setInternalDerivedData] = useState<DerivedRuntimeElementData>(
        controlledDerivedData ?? createDerivedRuntimeElementData(),
    );
    const [errorSuppressionTargets, setErrorSuppressionTargets] = useState<ElementErrorSuppressionTarget[]>([]);
    const deriveRequestIdRef = useRef(0);

    const allElements = useMemo(() => {
        return flattenElements(element, false);
    }, [element]);

    const baseDerivedData = useMemo(() => {
        return controlledDerivedData ?? internalDerivedData;
    }, [controlledDerivedData, internalDerivedData]);

    const derivedData = useMemo(() => {
        const derivedDataWithComputedErrors = computedErrors == null || Object.keys(computedErrors).length === 0 || suppressErrors ?
            baseDerivedData :
            {
                ...baseDerivedData,
                elementStates: applyComputedErrors(computedErrors, baseDerivedData.elementStates),
            };

        if (errorSuppressionTargets.length === 0) {
            return derivedDataWithComputedErrors;
        }

        return applyElementErrorSuppressions(derivedDataWithComputedErrors, errorSuppressionTargets);
    }, [computedErrors, baseDerivedData, suppressErrors, errorSuppressionTargets]);

    const contextValue = useMemo<ElementDerivationContextType>(() => {
        const allElements = flattenElementsWithParents(element, [], false);

        return {
            renderMode: renderMode,
            isEditable: !disabled && !readOnly,
            showInvisible: false,
            showTechnical: true,
            scrollContainerRef: scrollContainerRef ?? null,

            rootElement: element,
            allElements: allElements,

            authoredElementValues: authoredElementValues,
            derivedRuntimeElementData: derivedData,
            additionalComputedErrors: computedErrors ?? null,

            suppressErrors: suppressErrors,
        };
    }, [
        disabled,
        readOnly,
        element,
        authoredElementValues,
        derivedData,
        computedErrors,
        suppressErrors,
        renderMode,
        scrollContainerRef,
    ]);

    useEffect(() => {
        if (controlledDerivedData != null) {
            setInternalDerivedData(controlledDerivedData);
        }
    }, [controlledDerivedData]);

    useEffect(() => {
        // New external validation errors should be shown even if the field suppressed older edit-time errors.
        setErrorSuppressionTargets([]);
    }, [computedErrors]);

    useEffect(() => {
        if (!deriveOnMount) {
            return;
        }

        const controller = new AbortController();
        let isActive = true;

        setMode('busy');
        setErrorSuppressionTargets([]);
        derive(authoredElementValues, undefined, controller.signal)
            .finally(() => {
                if (isActive) {
                    setMode('idle');
                }
            });

        return () => {
            isActive = false;
            controller.abort();
        };
    }, [element, disableValidation, disableVisibilities, renderMode, deriveOnMount]);

    const handleAuthoredElementValuesChange = async (newData: AuthoredElementValues, triggeringElementIds: string[]) => {
        const normalizedNewData = normalizeReplicatingContainerValues(element, newData);
        const changedDynamicElementIds = new Set<string>();
        const patchedDerivedData = patchDerivedDataWithAuthoredValues(
            element,
            normalizedNewData,
            authoredElementValues,
            baseDerivedData,
            changedDynamicElementIds,
        );
        setInternalDerivedData(patchedDerivedData);
        onDerivedDataChange?.(patchedDerivedData);
        onAuthoredElementValuesChange(normalizedNewData);

        const changedErrorSuppressionTargets = collectChangedElementErrorSuppressionTargets(
            element,
            authoredElementValues,
            normalizedNewData,
        );

        if (changedErrorSuppressionTargets.length > 0) {
            setErrorSuppressionTargets((current) => mergeElementErrorSuppressionTargets(
                current,
                changedErrorSuppressionTargets,
            ));
        }

        // Input-mode expressions have no referencedIds contract. Their own edits still require backend derivation,
        // which remains responsible for deciding whether to evaluate them or defer them in authoring contexts.
        const relevantIds: string[] = [...changedDynamicElementIds];
        for (const id of triggeringElementIds) {
            for (const element of allElements) {
                if (checkElementReferencesId(element, id)) {
                    if (!relevantIds.includes(element.id)) {
                        relevantIds.push(element.id);
                    }
                }
            }
        }

        if (relevantIds.length === 0) {
            return;
        }

        setDerivationTriggerIdQueue((current) => [
            ...current,
            ...relevantIds,
        ]);

        // Change-driven derivation updates dependent visibility/values without surfacing validation errors.
        await deriveWithMinimumVisibleDuration(normalizedNewData, ['ALL'], patchedDerivedData);
        setDerivationTriggerIdQueue((current) => {
            const updated = [...current];
            for (const id of relevantIds) {
                const index = updated.indexOf(id);
                if (index !== -1) {
                    updated.splice(index, 1);
                }
            }
            return updated;
        });
    };

    const derive = async (
        authoredElementValues: AuthoredElementValues,
        skipErrorsForElements: string[] = ['ALL'],
        abort?: AbortSignal,
        preserveErrorsFrom?: DerivedRuntimeElementData,
    ) => {
        const normalizedAuthoredElementValues = normalizeReplicatingContainerValues(element, authoredElementValues);

        try {
            if (onDerivationStarted != null) {
                onDerivationStarted(normalizedAuthoredElementValues);
            }

            const requestId = ++deriveRequestIdRef.current;
            let derivedRuntimeElementData = await (onDeriveOverride != null ? onDeriveOverride(normalizedAuthoredElementValues, skipErrorsForElements) : new ElementsApiService()
                .derive({
                    element: element,
                    authoredElementValues: normalizedAuthoredElementValues,
                    derivationOptions: {
                        skipErrorsForElementIds: disableValidation && renderMode === ViewDispatcherMode.Editor ? ['ALL'] : skipErrorsForElements,
                        skipVisibilitiesForElementIds: disableVisibilities && renderMode === ViewDispatcherMode.Editor ? ['ALL'] : [],
                        skipOverridesForElementIds: [],
                        skipValuesForElementIds: [],
                    },
                    processExecutionData: {
                        $: {},
                        $$: {},
                        _: {},
                    },
                }, {
                    abort: abort,
                }));

            if (preserveErrorsFrom != null) {
                derivedRuntimeElementData = preserveDerivedErrors(preserveErrorsFrom, derivedRuntimeElementData);
            }

            if (requestId === deriveRequestIdRef.current) {
                setInternalDerivedData(derivedRuntimeElementData);
                onDerivedDataChange?.(derivedRuntimeElementData);

                if (onDerivationFinished != null) {
                    onDerivationFinished(derivedRuntimeElementData);
                }
            }

            return derivedRuntimeElementData;
        } catch (error) {
            if (!abort?.aborted) {
                if (isApiError(error) && error.displayableToUser) {
                    dispatch(showErrorSnackbar(error.message));
                } else {
                    console.error(error);
                    dispatch(showErrorSnackbar('Beim Verarbeiten der Eingaben ist ein unbekannter Fehler aufgetreten'));
                }
            }
        }

        return {
            effectiveValues: {},
            elementStates: {},
        };
    };

    const deriveWithMinimumVisibleDuration = (
        authoredElementValues: AuthoredElementValues,
        skipErrorsForElements: string[] = ['ALL'],
        preserveErrorsFrom?: DerivedRuntimeElementData,
    ): Promise<DerivedRuntimeElementData> => {
        return withAsyncWrapper<undefined, DerivedRuntimeElementData>({
            desiredMinRuntime: 600,
            runtimeCallback: (isRunning) => {
                setMode(isRunning ? 'deriving' : 'idle');
            },
            main: () => derive(authoredElementValues, skipErrorsForElements, undefined, preserveErrorsFrom),
        });
    };

    const replaceAuthoredElementValues = async (newData: AuthoredElementValues) => {
        const normalizedNewData = normalizeReplicatingContainerValues(element, newData);
        const patchedDerivedData = clearDerivedErrorsRecursively(
            patchDerivedDataWithAuthoredValues(element, normalizedNewData, authoredElementValues, baseDerivedData, new Set()),
        );

        setErrorSuppressionTargets([]);
        setInternalDerivedData(patchedDerivedData);
        onDerivedDataChange?.(patchedDerivedData);
        onAuthoredElementValuesChange(normalizedNewData);

        return await deriveWithMinimumVisibleDuration(normalizedNewData, ['ALL']);
    };

    useImperativeHandle(ref, () => ({
        replaceAuthoredElementValues,
    }));

    return (
        <ElementDerivationContextProvider
            value={contextValue}
        >
            <ViewDispatcherContextProvider
                value={{
                    scrollContainerRef,
                    rootElement: element,
                    allElements: allElements,
                    mode: renderMode,
                    rootAuthoredElementValues: authoredElementValues,
                    rootDerivedData: derivedData,
                    showInvisibleElements: disableVisibilities && renderMode === ViewDispatcherMode.Editor,
                    highlightedElementId: highlightedElementId,
                    taskViewMode,
                    inputModesEnabled,
                    inputModeVariables,
                    readOnly,
                }}
            >
                <ViewDispatcherComponent
                    element={element}
                    isBusy={mode === 'busy' || (disabled ?? false)}
                    isDeriving={mode === 'deriving'}
                    authoredElementValues={authoredElementValues}
                    derivedData={derivedData}
                    onAuthoredElementValuesChange={handleAuthoredElementValuesChange}
                    derivationTriggerIdQueue={derivationTriggerIdQueue}
                    onDerive={(authoredValues, _, skipErrorsForElements) => {
                        setInternalDerivedData((current) => {
                            return clearDerivedErrorsRecursively(current);
                        });
                        setErrorSuppressionTargets([]);
                        return deriveWithMinimumVisibleDuration(authoredValues, skipErrorsForElements);
                    }}
                    onEvent={(data, event) => {
                        const normalizedData = normalizeReplicatingContainerValues(element, data);

                        return deriveWithMinimumVisibleDuration(normalizedData)
                            .then((derived) => {
                                setInternalDerivedData(derived);
                                if (!hasAnyErrorRecursively(derived.elementStates) && onEvent != null) {
                                    return onEvent(normalizedData, event);
                                }
                                return false;
                            });
                    }}
                    onResetErrors={() => {
                        setErrorSuppressionTargets([]);
                        setInternalDerivedData((current) => {
                            return clearDerivedErrorsRecursively(current);
                        });
                    }}
                    suppressErrors={suppressErrors ?? false}
                />
            </ViewDispatcherContextProvider>
        </ElementDerivationContextProvider>
    );
});

function checkElementReferencesId(element: AnyElement, id: string): boolean {
    if (element.visibility?.referencedIds?.includes(id)) {
        return true;
    }
    if (element.override?.referencedIds?.includes(id)) {
        return true;
    }
    if (isAnyInputElement(element)) {
        if (element.value?.referencedIds?.includes(id)) {
            return true;
        }
    }
    return false;
}

function patchDerivedDataWithAuthoredValues(
    rootElement: AnyElement,
    authoredElementValues: AuthoredElementValues,
    previousAuthoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
    changedDynamicElementIds: Set<string>,
): DerivedRuntimeElementData {
    const effectiveValues = {
        ...derivedData.effectiveValues,
    };

    const elementStates = patchComputedElementStatesWithAuthoredValues(
        rootElement,
        authoredElementValues,
        previousAuthoredElementValues,
        derivedData.elementStates,
        effectiveValues,
        changedDynamicElementIds,
    );

    return createDerivedRuntimeElementData({
        ...derivedData,
        effectiveValues,
        elementStates,
    });
}

function patchComputedElementStatesWithAuthoredValues(
    currentElement: AnyElement,
    authoredElementValues: AuthoredElementValues,
    previousAuthoredElementValues: AuthoredElementValues,
    currentElementStates: ComputedElementStates,
    effectiveValues: EffectiveElementValues,
    changedDynamicElementIds: Set<string>,
): ComputedElementStates {
    const hasAuthoredValue = Object.prototype.hasOwnProperty.call(authoredElementValues, currentElement.id);
    const authoredValue = authoredElementValues[currentElement.id];
    const previousAuthoredValue = previousAuthoredElementValues[currentElement.id];
    const currentElementState = currentElementStates[currentElement.id];
    let nextElementStates = currentElementStates;

    if (
        isAnyInputElement(currentElement) &&
        hasAuthoredValue &&
        !currentElement.disabled &&
        !currentElement.technical &&
        currentElementState?.valueSource !== ComputedElementValueSource.Identity &&
        currentElementState?.disabled !== true
    ) {
        if (authoredValue != null && authoredValue.type !== InputMode.Literal) {
            // Editing another field must not erase an existing result or the states of a dynamic container.
            if (deepEquals(authoredValue, previousAuthoredValue)) {
                return nextElementStates;
            }
            changedDynamicElementIds.add(currentElement.id);
        } else if (previousAuthoredValue != null && previousAuthoredValue.type !== InputMode.Literal) {
            // Re-derive mode switches too, so an in-flight expression result cannot replace the new literal.
            changedDynamicElementIds.add(currentElement.id);
        }
        nextElementStates = {
            ...nextElementStates,
            [currentElement.id]: {
                ...(currentElementState ?? {}),
                valueSource: ComputedElementValueSource.Authored,
            },
        };
        const optimisticEffectiveValue = authoredValue?.type === InputMode.Literal ? authoredValue.value : null;
        const previousEffectiveValue = effectiveValues[currentElement.id];
        effectiveValues[currentElement.id] = optimisticEffectiveValue;

        if (isReplicatingContainerLayout(currentElement)) {
            const previousRows: EffectiveReplicatingContainerElementValue[] = Array.isArray(previousEffectiveValue)
                ? previousEffectiveValue : [];
            const previousAuthoredRows: ReplicatingContainerElementValue[] =
                previousAuthoredValue?.type === InputMode.Literal && Array.isArray(previousAuthoredValue.value)
                    ? previousAuthoredValue.value : [];
            const rows: ReplicatingContainerElementValue[] | null = Array.isArray(optimisticEffectiveValue)
                ? optimisticEffectiveValue : null;
            const projectedRows = rows?.map((row, index) => {
                const rowId = row.id;
                const previousRow = rowId != null
                    ? previousRows.find((candidate) => candidate.id === rowId)
                    : previousRows[index];
                const previousAuthoredRow = rowId != null
                    ? previousAuthoredRows.find((candidate) => candidate.id === rowId)
                    : previousAuthoredRows[index];
                const previousSubState = resolveComputedElementSubState(currentElementState?.subStates, rowId, index);
                const rowEffectiveValues = {...previousRow?.values};
                let rowStates = resolveComputedElementSubStateStates(previousSubState);

                // Only schema-defined child fields contain authored envelopes. Ordinary object payloads remain opaque.
                // Match prior values by row ID so reordering cannot move derived or identity values to another row.
                for (const child of currentElement.children ?? []) {
                    rowStates = patchComputedElementStatesWithAuthoredValues(
                        child,
                        row.values ?? {},
                        previousAuthoredRow?.values ?? {},
                        rowStates,
                        rowEffectiveValues,
                        changedDynamicElementIds,
                    );
                }

                return {
                    row: {id: rowId, values: rowEffectiveValues},
                    state: createComputedElementSubState(rowId, rowStates),
                };
            });
            effectiveValues[currentElement.id] = projectedRows?.map(({row}) => row) ?? optimisticEffectiveValue;
            nextElementStates[currentElement.id] = {
                ...nextElementStates[currentElement.id],
                subStates: projectedRows?.map(({state}) => state) ?? null,
            };
        }
    }

    if (isReplicatingContainerLayout(currentElement)) {
        return nextElementStates;
    }

    if (isAnyElementWithChildren(currentElement)) {
        for (const child of currentElement.children ?? []) {
            nextElementStates = patchComputedElementStatesWithAuthoredValues(
                child,
                authoredElementValues,
                previousAuthoredElementValues,
                nextElementStates,
                effectiveValues,
                changedDynamicElementIds,
            );
        }
    }

    return nextElementStates;
}
