import {NoCodeNodeDataReference} from '../../../models/functions/no-code-expression';
import {isValidProcessDataPath} from '../../../utils/process-data-path';

type NodeDataPath = Partial<Pick<NoCodeNodeDataReference, 'nodeDataKey' | 'path'>>;

export function formatNoCodeNodeDataPath({
                                             nodeDataKey,
                                             path,
                                         }: NodeDataPath): string {
    if (!nodeDataKey && !path) {
        return '';
    }
    return `${nodeDataKey ?? ''}${path == null ? '' : path.startsWith('[') ? path : `.${path}`}`;
}

/** Split without normalizing: incomplete edits and existing numeric node keys must survive parent echoes. */
export function splitNoCodeNodeDataPath(value: string): NodeDataPath {
    const separator = value.search(/[.\[]/);
    if (separator < 0) return {
        nodeDataKey: value,
        path: undefined,
    };
    return {
        nodeDataKey: value.slice(0, separator),
        path: value.slice(separator + (value[separator] === '.' ? 1 : 0)),
    };
}

export function validateNoCodeNodeDataPath({
                                               nodeDataKey,
                                               path,
                                           }: NodeDataPath): string | undefined {
    // NoCodeNodeDataReference's persisted contract differs from current process node key authoring rules.
    if (!nodeDataKey || !/^[a-zA-Z0-9_]+$/.test(nodeDataKey)) {
        return 'Geben Sie einen Datenschlüssel aus Buchstaben (A–Z), Zahlen oder Unterstrichen an.';
    }
    if (!path || !isValidProcessDataPath(path, false, true)) {
        return 'Ergänzen Sie nach dem Element-Datenschlüssel einen Pfad wie person.name oder items[0].name.';
    }
}
