import {ElementData} from '../../../models/element-data';
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
    elementData: ElementData;
    onElementDataChange: (newData: ElementData) => void;
}

export function ElementDerivationContext(props: ElementDerivationContextProps) {
    const {
        element,
        elementData,
        onElementDataChange,
    } = props;

    const api = useApi();
    const dispatch = useAppDispatch();

    const [mode, setMode] = useState<'deriving' | 'busy' | 'idle'>('idle');
    const [derivationTriggerIdQueue, setDerivationTriggerIdQueue] = useState<string[]>([]);

    const allElements = useMemo(() => {
        return flattenElements(element, false);
    }, [element]);

    useEffect(() => {
        setMode('busy');
        derive(elementData)
            .finally(() => {
                setMode('idle');
            });
    }, [element]);

    const handleElementDataChange = async (newData: ElementData, triggeringElementIds: string[]) => {
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
            onElementDataChange(newData);
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

    const derive = async (elementData: ElementData) => {
        try {
            const res = await new ElementsApiService(api)
                .derive({
                    element: element,
                    elementData: elementData,
                    options: {
                        skipErrorsForElementIds: ['ALL'],
                        skipVisibilitiesForElementIds: [],
                        skipOverridesForElementIds: [],
                        skipValuesForElementIds: [],
                    },
                });
            onElementDataChange(res.elementData);
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
            isBusy={mode === 'busy'}
            isDeriving={mode === 'deriving'}
            mode="viewer"
            elementData={elementData}
            onElementDataChange={handleElementDataChange}
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