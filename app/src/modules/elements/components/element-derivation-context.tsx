import {
    applyComputedErrors,
    AuthoredElementValues,
    clearDerivedErrorsRecursively,
    ComputedElementErrors,
    ComputedElementStates,
    ComputedElementValueSource,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    hasAnyErrorRecursively,
} from '../../../models/element-data';
import {AnyElement} from '../../../models/elements/any-element';
import React, {createContext, RefObject, useContext, useEffect, useMemo, useState} from 'react';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../../../utils/flatten-elements';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {isReplicatingContainerLayout} from '../../../models/elements/form/layout/replicating-container-layout';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementsApiService} from '../elements-api-service';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {isApiError} from '../../../models/api-error';
import {walkAuthoredElementValues} from '../../../utils/element-data-utils';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher/view-dispatcher.component';
import {
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../../../components/view-dispatcher/view-dispatcher.context';
import {deepEquals} from '../../../utils/equality-utils';
import {withAsyncWrapper} from '../../../utils/with-async-wrapper';

interface ElementDerivationContextProps {
    element: AnyElement;
    authoredElementValues: AuthoredElementValues;
    onAuthoredElementValuesChange: (newData: AuthoredElementValues) => void;
    derivedData?: DerivedRuntimeElementData;
    computedErrors?: ComputedElementErrors | null;
    onDerivedDataChange?: (newData: DerivedRuntimeElementData) => void;
    disabled?: boolean;
    onDerivationStarted?: (triggeringElementData: AuthoredElementValues) => void;
    onDerivationFinished?: (derivedElementData: DerivedRuntimeElementData) => void;
    suppressErrors?: boolean;
    onDeriveOverride?: (aev: AuthoredElementValues, skipErrorsForElements: string[]) => Promise<DerivedRuntimeElementData>;
    onEvent?: (values: AuthoredElementValues, event: string) => Promise<boolean | void>;
    mode?: ViewDispatcherMode;
    disableValidation?: boolean;
    disableVisibilities?: boolean;
    highlightedElementId?: string | null;
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


export function ElementDerivationContext(props: ElementDerivationContextProps) {
    const {
        element,
        authoredElementValues,
        onAuthoredElementValuesChange,
        derivedData: controlledDerivedData,
        computedErrors,
        onDerivedDataChange,
        disabled,
        onDerivationStarted,
        onDerivationFinished,
        suppressErrors,
        onDeriveOverride,
        onEvent,
        mode: renderMode = ViewDispatcherMode.Viewer,
        disableValidation = false,
        disableVisibilities = false,
        highlightedElementId,
    } = props;

    const dispatch = useAppDispatch();

    const [mode, setMode] = useState<'deriving' | 'busy' | 'idle'>('idle');

    const [derivationTriggerIdQueue, setDerivationTriggerIdQueue] = useState<string[]>([]);

    const [internalDerivedData, setInternalDerivedData] = useState<DerivedRuntimeElementData>(
        controlledDerivedData ?? createDerivedRuntimeElementData(),
    );
    const [suppressedErrorElementIds, setSuppressedErrorElementIds] = useState<string[]>([]);

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

        if (suppressedErrorElementIds.length === 0) {
            return derivedDataWithComputedErrors;
        }

        return clearDerivedErrorsForElementIds(derivedDataWithComputedErrors, suppressedErrorElementIds);
    }, [computedErrors, baseDerivedData, suppressErrors, suppressedErrorElementIds]);

    const contextValue = useMemo<ElementDerivationContextType>(() => {
        const allElements = flattenElementsWithParents(element, [], false);

        return {
            renderMode: renderMode,
            isEditable: !disabled,
            showInvisible: false,
            showTechnical: true,
            scrollContainerRef: null,

            rootElement: element,
            allElements: allElements,

            authoredElementValues: authoredElementValues,
            derivedRuntimeElementData: derivedData,
            additionalComputedErrors: computedErrors ?? null,

            suppressErrors: suppressErrors,
        };
    }, [
        disabled,
        element,
        authoredElementValues,
        derivedData,
        computedErrors,
        suppressErrors,
        renderMode,
    ]);

    useEffect(() => {
        if (controlledDerivedData != null) {
            setInternalDerivedData(controlledDerivedData);
        }
    }, [controlledDerivedData]);

    useEffect(() => {
        // New external validation errors should be shown even if the field suppressed older edit-time errors.
        setSuppressedErrorElementIds([]);
    }, [computedErrors]);

    useEffect(() => {
        const controller = new AbortController();

        setMode('busy');
        setSuppressedErrorElementIds([]);
        derive(authoredElementValues, undefined, controller.signal)
            .finally(() => {
                setMode('idle');
            });

        return () => {
            controller.abort();
        };
    }, [element]);

    const handleAuthoredElementValuesChange = async (newData: AuthoredElementValues, triggeringElementIds: string[]) => {
        const patchedDerivedData = patchDerivedDataWithAuthoredValues(element, newData, derivedData);
        setInternalDerivedData(patchedDerivedData);
        onDerivedDataChange?.(patchedDerivedData);
        onAuthoredElementValuesChange(newData);

        const changedElementIds = getChangedAuthoredElementIds(
            element,
            authoredElementValues,
            newData,
        );

        if (changedElementIds.length > 0) {
            setSuppressedErrorElementIds((current) => {
                const updated = new Set(current);
                for (const id of changedElementIds) {
                    updated.add(id);
                }
                return Array.from(updated);
            });
        }

        const relevantIds: string[] = [];
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
        await deriveWithMinimumVisibleDuration(newData);
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

    const derive = async (authoredElementValues: AuthoredElementValues, skipErrorsForElements: string[] = ['ALL'], abort?: AbortSignal) => {
        try {
            if (onDerivationStarted != null) {
                onDerivationStarted(authoredElementValues);
            }

            let derivedRuntimeElementData = await (onDeriveOverride != null ? onDeriveOverride(authoredElementValues, skipErrorsForElements) : new ElementsApiService()
                .derive({
                    element: element,
                    authoredElementValues: authoredElementValues,
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

            setInternalDerivedData(derivedRuntimeElementData);
            onDerivedDataChange?.(derivedRuntimeElementData);

            if (onDerivationFinished != null) {
                onDerivationFinished(derivedRuntimeElementData);
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
    ): Promise<DerivedRuntimeElementData> => {
        return withAsyncWrapper<undefined, DerivedRuntimeElementData>({
            desiredMinRuntime: 600,
            runtimeCallback: (isRunning) => {
                setMode(isRunning ? 'deriving' : 'idle');
            },
            main: () => derive(authoredElementValues, skipErrorsForElements, undefined),
        });
    };

    // Derive all data if the disable visibilities flag is reset
    const [previousVisFlag, setPreviousVisFlag] = useState<boolean>(false);
    useEffect(() => {
        if (previousVisFlag !== disableVisibilities) {
            const controller = new AbortController();
            derive(authoredElementValues, undefined, controller.signal);
            setPreviousVisFlag(disableVisibilities);
            return () => {
                controller.abort();
            }
        }
    }, [disableVisibilities]);

    return (
        <ElementDerivationContextProvider
            value={contextValue}
        >
            <ViewDispatcherContextProvider
                value={{
                    rootElement: element,
                    allElements: allElements,
                    mode: renderMode,
                    rootAuthoredElementValues: authoredElementValues,
                    rootDerivedData: derivedData,
                    showInvisibleElements: disableVisibilities && renderMode === ViewDispatcherMode.Editor,
                    highlightedElementId: highlightedElementId,
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
                        setSuppressedErrorElementIds([]);
                        return derive(authoredValues, skipErrorsForElements);
                    }}
                    onEvent={(data, event) => {
                        return derive(data)
                            .then((derived) => {
                                setInternalDerivedData(derived);
                                if (!hasAnyErrorRecursively(derived.elementStates)) {
                                    if (onEvent != null) {
                                        onEvent(data, event);
                                    }
                                }
                            });
                    }}
                    onResetErrors={() => {
                        setSuppressedErrorElementIds([]);
                        setInternalDerivedData((current) => {
                            return clearDerivedErrorsRecursively(current);
                        });
                    }}
                    suppressErrors={suppressErrors ?? false}
                />
            </ViewDispatcherContextProvider>
        </ElementDerivationContextProvider>
    );
}

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

function clearDerivedErrorsForElementIds(derivedData: DerivedRuntimeElementData, elementIds: string[]): DerivedRuntimeElementData {
    const elementIdSet = new Set(elementIds);

    return {
        ...derivedData,
        elementStates: clearComputedElementStateErrorsByElementId(derivedData.elementStates, elementIdSet),
    };
}

function clearComputedElementStateErrorsByElementId(
    elementStates: ComputedElementStates,
    elementIdSet: Set<string>,
): ComputedElementStates {
    return Object.fromEntries(
        Object.entries(elementStates).map(([elementId, state]) => [
            elementId,
            {
                ...state,
                error: elementIdSet.has(elementId) ? null : state?.error,
                subStates: state?.subStates?.map((subState) => {
                    return clearComputedElementStateErrorsByElementId(subState ?? {}, elementIdSet);
                }) ?? null,
            },
        ]),
    );
}

function getChangedAuthoredElementIds(
    rootElement: AnyElement,
    previousValues: AuthoredElementValues,
    nextValues: AuthoredElementValues,
): string[] {
    const previousValuesByElementId = collectAuthoredValuesByElementId(rootElement, previousValues);
    const nextValuesByElementId = collectAuthoredValuesByElementId(rootElement, nextValues);
    const elementIds = new Set([
        ...Object.keys(previousValuesByElementId),
        ...Object.keys(nextValuesByElementId),
    ]);

    return Array.from(elementIds)
        .filter((elementId) => !deepEquals(
            previousValuesByElementId[elementId] ?? [],
            nextValuesByElementId[elementId] ?? [],
        ));
}

function collectAuthoredValuesByElementId(
    rootElement: AnyElement,
    authoredValues: AuthoredElementValues,
): Record<string, any[]> {
    const valuesByElementId: Record<string, any[]> = {};

    walkAuthoredElementValues(rootElement, authoredValues, (element, value) => {
        if (valuesByElementId[element.id] == null) {
            valuesByElementId[element.id] = [];
        }

        valuesByElementId[element.id].push(value);
    });

    return valuesByElementId;
}

function patchDerivedDataWithAuthoredValues(
    rootElement: AnyElement,
    authoredElementValues: AuthoredElementValues,
    derivedData: DerivedRuntimeElementData,
): DerivedRuntimeElementData {
    const effectiveValues = {
        ...derivedData.effectiveValues,
    };

    const elementStates = patchComputedElementStatesWithAuthoredValues(
        rootElement,
        authoredElementValues,
        derivedData.elementStates,
        effectiveValues,
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
    currentElementStates: ComputedElementStates,
    effectiveValues: AuthoredElementValues,
): ComputedElementStates {
    const hasAuthoredValue = Object.prototype.hasOwnProperty.call(authoredElementValues, currentElement.id);
    const authoredValue = authoredElementValues[currentElement.id];
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
        nextElementStates = {
            ...nextElementStates,
            [currentElement.id]: {
                ...(currentElementState ?? {}),
                valueSource: ComputedElementValueSource.Authored,
            },
        };
        effectiveValues[currentElement.id] = authoredValue;

        if (isReplicatingContainerLayout(currentElement)) {
            nextElementStates[currentElement.id] = {
                ...nextElementStates[currentElement.id],
                subStates: Array.isArray(authoredValue) ?
                    authoredValue.map((_, index) => currentElementState?.subStates?.[index] ?? {}) :
                    null,
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
                nextElementStates,
                effectiveValues,
            );
        }
    }

    return nextElementStates;
}
