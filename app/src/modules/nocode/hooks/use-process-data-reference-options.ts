import {useMemo} from 'react';
import {type NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {isNoCodeProcessDataReference, type NoCodeOperand} from '../../../models/functions/no-code-expression';
import {parseProcessDataPath} from '../../../utils/process-data-path';
import {CodeListValueMode} from '../../code-lists/enums/code-list-value-mode';
import {type CodeListOptionsSource, useCodeListOptions} from '../../code-lists/hooks/use-code-list-element-options';
import {type ProcessNodeDefinitionMetadataForwardedProcessDataKey} from '../../process/entities/process-node-definition-metadata';
import {useOptionalProcessNodeEditorContext} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';

const EMPTY_FORWARDED_KEYS: ProcessNodeDefinitionMetadataForwardedProcessDataKey[] = [];

/**
 * Collects the value options of the process data key that each operand references, by operand position. Operands that
 * reference no process data have no options. Options are only available inside a process node editor, which provides
 * the metadata of the incoming process data keys.
 */
export function useProcessDataReferenceOptions(
    operands: ReadonlyArray<NoCodeOperand | null>,
): NoCodeParameterOption[][] {
    const forwardedKeys = useOptionalProcessNodeEditorContext()?.incomingMetadata?.forwardedProcessDataKeys ?? EMPTY_FORWARDED_KEYS;

    const referencedKeysByOperand = useMemo(() => operands
        .map((operand) => isNoCodeProcessDataReference(operand)
            ? findForwardedProcessDataKeys(forwardedKeys, operand.path)
            : []), [operands, forwardedKeys]);

    // Several upstream nodes can forward the same key with different code lists, so the positions identify the source.
    const codeListSources: CodeListOptionsSource[] = useMemo(() => referencedKeysByOperand
        .flatMap((keys, operandIndex) => keys.flatMap((key, keyIndex) => key.codeListKey == null ? [] : [{
            id: getCodeListSourceId(operandIndex, keyIndex),
            codeListKey: key.codeListKey,
            valueMode: key.codeListValueMode ?? CodeListValueMode.ValueAndLabel,
        }])), [referencedKeysByOperand]);
    const codeListOptions = useCodeListOptions(codeListSources);

    return useMemo(() => referencedKeysByOperand.map((keys, operandIndex) => {
        const optionsByValue = new Map<string, NoCodeParameterOption>();

        keys.forEach((key, keyIndex) => {
            const options = key.codeListKey != null
                ? codeListOptions.get(getCodeListSourceId(operandIndex, keyIndex)) ?? []
                : key.valueOptions ?? [];

            for (const option of options) {
                if (!optionsByValue.has(option.value)) {
                    optionsByValue.set(option.value, option);
                }
            }
        });

        return Array.from(optionsByValue.values());
    }), [referencedKeysByOperand, codeListOptions]);
}

function getCodeListSourceId(operandIndex: number, keyIndex: number): string {
    return `${operandIndex}:${keyIndex}`;
}

/**
 * Finds the forwarded process data keys for a referenced path. Exact matches take precedence. Otherwise, a wildcard
 * row of a forwarded key matches any concrete index, because every row of a replicating container has the same options.
 */
export function findForwardedProcessDataKeys(
    forwardedKeys: readonly ProcessNodeDefinitionMetadataForwardedProcessDataKey[],
    path: string | null | undefined,
): ProcessNodeDefinitionMetadataForwardedProcessDataKey[] {
    const pathSegments = path == null ? null : parseProcessDataPath(path);
    if (pathSegments == null || pathSegments.length === 0) {
        return [];
    }

    const candidates = forwardedKeys.map((key) => ({
        key,
        segments: parseProcessDataPath(key.processDataKey),
    }));

    const exactMatches = candidates
        .filter(({segments}) => segments != null && segmentsMatch(segments, pathSegments, false))
        .map(({key}) => key);
    if (exactMatches.length > 0) {
        return exactMatches;
    }

    return candidates
        .filter(({segments}) => segments != null && segmentsMatch(segments, pathSegments, true))
        .map(({key}) => key);
}

function segmentsMatch(keySegments: string[], pathSegments: string[], matchWildcards: boolean): boolean {
    return keySegments.length === pathSegments.length && keySegments.every((segment, index) => (
        segment === pathSegments[index] ||
        (matchWildcards && segment === '*' && /^[0-9]+$/.test(pathSegments[index]))
    ));
}
