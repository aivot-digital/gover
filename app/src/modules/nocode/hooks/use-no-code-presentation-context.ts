import {useContext, useMemo} from 'react';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {ViewDispatcherContext} from '../../../components/view-dispatcher/view-dispatcher.context';
import {
    useOptionalProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {useCodeListElementOptions} from '../../code-lists/hooks/use-code-list-element-options';
import {useProcessDataReferenceOptions} from './use-process-data-reference-options';
import {NoCodePresentationContext} from '../utils/humanize-no-code';

export function useNoCodePresentationContext(operand: NoCodeOperand | null, allElements: ElementWithParents[]): NoCodePresentationContext {
    const view = useContext(ViewDispatcherContext);
    const process = useOptionalProcessNodeEditorContext();
    const references = useMemo(() => {
        const result: NoCodeOperand[] = [];
        const visit = (node: NoCodeOperand | null) => {
            if (node?.type === 'NoCodeExpression') node.operands?.forEach(visit);
            else if (node != null) result.push(node);
        };
        visit(operand);
        return result;
    }, [operand]);
    const fields = useMemo(() => allElements.filter(({element}) => references.some((ref) => ref.type === 'NoCodeReference' && ref.elementId === element.id)).map(({element}) => element), [references, allElements]);
    const codeListOptions = useCodeListElementOptions(fields);
    const options = useProcessDataReferenceOptions(references);
    const processOptions = useMemo(() => new Map(references.flatMap((ref, i) => ref.type === 'NoCodeProcessDataReference' ? [[ref.path ?? '', options[i]] as const] : [])), [references, options]);
    return useMemo(() => ({
        codeListOptions,
        processOptions,
        variables: view?.inputModeVariables ?? process?.incomingMetadata?.inputVariables,
    }), [codeListOptions, processOptions, view?.inputModeVariables, process?.incomingMetadata?.inputVariables]);
}
