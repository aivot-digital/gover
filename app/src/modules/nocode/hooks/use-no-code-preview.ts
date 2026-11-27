import {NoCodeDataType} from '../../../data/no-code-data-type';
import {useNoCodePresentationContext} from './use-no-code-presentation-context';
import {useEffect, useMemo, useState} from 'react';
import {useApi} from '../../../hooks/use-api';
import {NoCodeApiService} from '../../../services/no-code-api-service';
import {type NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {isNoCodeExpression, type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {type AnyElement} from '../../../models/elements/any-element';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {flattenElementsWithParents} from '../../../utils/flatten-elements';
import {summarizeNoCode} from '../utils/no-code-summary';

export function useNoCodePreview(operand: NoCodeOperand | null, rootElement?: AnyElement, type?: NoCodeDataType): string | undefined {
    const api = useApi();
    const needsOperators = isNoCodeExpression(operand);
    const [operators, setOperators] = useState<NoCodeOperatorDetailsDTO[] | null>(null);
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        if (!needsOperators) return;

        let active = true;
        setOperators(null);
        setFailed(false);
        new NoCodeApiService(api).getNoCodeOperators()
            .then((result) => {
                if (active) setOperators(result);
            })
            .catch(() => {
                // Preview metadata is optional: its failure must not block editing the stored expression.
                if (active) setFailed(true);
            });

        return () => {
            active = false;
        };
    }, [api, needsOperators]);

    const allElements = useMemo(() => rootElement == null ? [] : flattenElementsWithParents(rootElement, [], true)
        .filter((entry) => isAnyInputElement(entry.element)), [rootElement]);

    const presentation = useNoCodePresentationContext(operand, allElements);

    return useMemo(() => {
        if (operand == null) return undefined;
        if (needsOperators && failed) return 'Vorschau nicht verfügbar';
        if (needsOperators && operators == null) return 'Vorschau wird geladen …';

        try {
            return summarizeNoCode(operand, allElements, operators ?? [], {
                ...presentation,
                type,
            }).text;
        } catch {
            return 'Vorschau nicht verfügbar';
        }
    }, [operand, allElements, needsOperators, operators, failed, presentation, type]);
}
