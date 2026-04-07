import {
    applyComputedErrors,
    AuthoredElementValues,
    ComputedElementErrors,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
} from '../../../models/element-data';
import {AnyElement} from '../../../models/elements/any-element';
import {ViewDispatcherComponent} from '../../../components/view-dispatcher.component';
import {useEffect, useMemo, useState} from 'react';
import {flattenElements} from '../../../utils/flatten-elements';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {useApi} from '../../../hooks/use-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementsApiService} from '../elements-api-service';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {isApiError} from '../../../models/api-error';

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
    } = props;

    const api = useApi();
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

        if (computedErrors == null || Object.keys(computedErrors).length === 0) {
            return baseDerivedData;
        }

        return {
            ...baseDerivedData,
            elementStates: applyComputedErrors(computedErrors, baseDerivedData.elementStates),
        };
    }, [computedErrors, controlledDerivedData, internalDerivedData]);

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
            onAuthoredElementValuesChange(newData);
            return;
        }

        setDerivationTriggerIdQueue([
            ...derivationTriggerIdQueue,
            ...relevantIds,
        ]);
        setMode('deriving');
        await derive(newData);
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

            const derivedRuntimeElementData = await new ElementsApiService(api)
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
