import {
    applyComputedErrors,
    AuthoredElementValues,
    ComputedElementErrors,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
} from '../../../models/element-data';
import {AnyElement} from '../../../models/elements/any-element';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import React, {createContext, RefObject, useContext, useEffect, useMemo, useState} from 'react';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../../../utils/flatten-elements';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementsApiService} from '../elements-api-service';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {isApiError} from '../../../models/api-error';
import {synchronizeAuthoredElementValuesByDestinationPath} from '../../../utils/element-data-utils';

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
}

export enum ElementDerivationContextRenderMode {
    Editor,
    Viewer,
}

interface ElementDerivationContextType {
    renderMode: ElementDerivationContextRenderMode;
    isEditable: boolean;
    showInvisible: boolean;
    showTechnical: boolean;
    scrollContainerRef: RefObject<HTMLDivElement | null> | null;

    rootElement: AnyElement;
    allElements: ElementWithParents[];

    authoredElementValues: AuthoredElementValues;
    derivedRuntimeElementData: DerivedRuntimeElementData | null;
    additionalComputedErrors: ComputedElementErrors | null;

    supressErrors?: boolean;
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
            renderMode: ElementDerivationContextRenderMode.Viewer,
            rootElement: {} as AnyElement,
            scrollContainerRef: null,
            showInvisible: false,
            showTechnical: false,
            supressErrors: false,
        }
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
    } = props;

    const dispatch = useAppDispatch();

    const [mode, setMode] = useState<'deriving' | 'busy' | 'idle'>('idle');

    const [derivationTriggerIdQueue, setDerivationTriggerIdQueue] = useState<string[]>([]);

    const [internalDerivedData, setInternalDerivedData] = useState<DerivedRuntimeElementData>(
        controlledDerivedData ?? createDerivedRuntimeElementData(),
    );

    const allElements = useMemo(() => {
        return flattenElements(element, false);
    }, [element]);


    const derivedData = useMemo(() => {
        const baseDerivedData = controlledDerivedData ?? internalDerivedData;

        if (computedErrors == null || Object.keys(computedErrors).length === 0 || suppressErrors) {
            return baseDerivedData;
        }

        return {
            ...baseDerivedData,
            elementStates: applyComputedErrors(computedErrors, baseDerivedData.elementStates),
        };
    }, [computedErrors, controlledDerivedData, internalDerivedData]);

    const contextValue = useMemo<ElementDerivationContextType>(() => {
        const allElements = flattenElementsWithParents(element, [], false);

        return {
            renderMode: ElementDerivationContextRenderMode.Editor,
            isEditable: !disabled,
            showInvisible: false,
            showTechnical: true,
            scrollContainerRef: null,

            rootElement: element,
            allElements: allElements,

            authoredElementValues: authoredElementValues,
            derivedRuntimeElementData: derivedData,
            additionalComputedErrors: computedErrors ?? null,

            supressErrors: suppressErrors,
        };
    }, [
        disabled,
        element,
        authoredElementValues,
        derivedData,
        computedErrors,
        suppressErrors,
    ]);

    useEffect(() => {
        if (controlledDerivedData != null) {
            setInternalDerivedData(controlledDerivedData);
        }
    }, [controlledDerivedData]);

    useEffect(() => {
        setMode('busy');
        derive(authoredElementValues)
            .finally(() => {
                setMode('idle');
            });
    }, [element]);

    const handleAuthoredElementValuesChange = async (newData: AuthoredElementValues, triggeringElementIds: string[]) => {
        // Synchronizing before reference analysis keeps mirrored destination-path aliases and their
        // dependents inside the same authored-data update, regardless of which subtree hosts them.
        const synchronizedUpdate = synchronizeAuthoredElementValuesByDestinationPath(
            element,
            authoredElementValues,
            newData,
            derivedData,
            triggeringElementIds,
        );
        const effectiveNewData = synchronizedUpdate.authoredElementValues;
        const effectiveTriggeringElementIds = synchronizedUpdate.triggeringElementIds;

        const relevantIds: string[] = [];
        for (const id of effectiveTriggeringElementIds) {
            for (const element of allElements) {
                if (checkElementReferencesId(element, id)) {
                    if (!relevantIds.includes(element.id)) {
                        relevantIds.push(element.id);
                    }
                }
            }
        }

        if (relevantIds.length === 0) {
            onAuthoredElementValuesChange(effectiveNewData);
            return;
        }

        setDerivationTriggerIdQueue([
            ...derivationTriggerIdQueue,
            ...relevantIds,
        ]);
        setMode('deriving');
        await derive(effectiveNewData);
        setMode('idle');
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

    const derive = async (authoredElementValues: AuthoredElementValues) => {
        try {
            if (onDerivationStarted != null) {
                onDerivationStarted(authoredElementValues);
            }

            const derivedRuntimeElementData = await new ElementsApiService()
                .derive({
                    element: element,
                    authoredElementValues: authoredElementValues,
                    derivationOptions: {
                        skipErrorsForElementIds: ['ALL'],
                        skipVisibilitiesForElementIds: [],
                        skipOverridesForElementIds: [],
                        skipValuesForElementIds: [],
                    },
                });

            setInternalDerivedData(derivedRuntimeElementData);
            onDerivedDataChange?.(derivedRuntimeElementData);
            onAuthoredElementValuesChange(authoredElementValues);

            if (onDerivationFinished != null) {
                onDerivationFinished(derivedRuntimeElementData);
            }
        } catch (error) {
            if (isApiError(error) && error.displayableToUser) {
                dispatch(showErrorSnackbar(error.message));
            } else {
                console.error(error);
                dispatch(showErrorSnackbar('Beim Verarbeiten der Eingaben ist ein unbekannter Fehler aufgetreten'));
            }
        }
    };

    return (
        <ElementDerivationContextProvider
            value={contextValue}
        >
            <ViewDispatcherComponent
                rootElement={element}
                allElements={allElements}
                element={element}
                isBusy={mode === 'busy' || (disabled ?? false)}
                isDeriving={mode === 'deriving'}
                mode="editor"
                authoredElementValues={authoredElementValues}
                derivedData={derivedData}
                onAuthoredElementValuesChange={handleAuthoredElementValuesChange}
                derivationTriggerIdQueue={derivationTriggerIdQueue}
            />
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
