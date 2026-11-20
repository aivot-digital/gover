import {getNoCodeParameterSuggestions, getReferencedElementOptions} from '../utils/no-code-options';
import {useMemo} from 'react';
import {NoCodeParameter, NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {isNoCodeReference, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {useCodeListElementOptions} from '../../code-lists/hooks/use-code-list-element-options';
import {useProcessDataReferenceOptions} from './use-process-data-reference-options';

/** Suggestions belong to direct sibling references of compatible parameters, never to unrelated subtrees. */
export function useNoCodeParameterSuggestions(
    operands: Array<NoCodeOperand | null>,
    parameters: NoCodeParameter[],
    allElements: ElementWithParents[],
): NoCodeParameterOption[][] {
    const referencedElements = useMemo(() => {
        return operands
            .filter(isNoCodeReference)
            .map((op) => allElements.find((entry) => entry.element.id === op.elementId)?.element)
            .filter((element): element is ElementWithParents['element'] => element != null);
    }, [operands, allElements]);
    const codeListOptions = useCodeListElementOptions(referencedElements);
    const processDataOptions = useProcessDataReferenceOptions(operands);

    // The options of the form element or process data key that each operand references, by operand position.
    const referenceOptions: NoCodeParameterOption[][] = useMemo(() => {
        return operands.map((op, index) => {
            if (!isNoCodeReference(op)) {
                return processDataOptions[index] ?? [];
            }

            const element = allElements
                .find(el => el.element.id === op.elementId)?.element;

            return element == null ? [] : getReferencedElementOptions(element, codeListOptions);
        });
    }, [operands, allElements, codeListOptions, processDataOptions]);

    return useMemo(() => getNoCodeParameterSuggestions(parameters, referenceOptions), [parameters, referenceOptions]);
}
