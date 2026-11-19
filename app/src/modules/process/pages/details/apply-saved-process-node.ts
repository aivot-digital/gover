import {type ProcessNodeEntity} from '../../entities/process-node-entity';
import {type ProcessFlow} from './process-details-page';

/** A confirmed save can arrive after edits to the graph or a switch to another process version. */
export function applySavedProcessNode(flow: ProcessFlow | null, saved: ProcessNodeEntity): ProcessFlow | null {
    if (flow == null || flow.definition.id !== saved.processId || flow.version.processVersion !== saved.processVersion) {
        return flow;
    }

    const current = flow.nodes.find((node) => node.id === saved.id);
    // Persisted versions start at zero. An unversioned snapshot cannot replace a known version.
    if (current == null || (current.lockVersion ?? -1) > (saved.lockVersion ?? -1)) {
        return flow;
    }

    return {...flow, nodes: flow.nodes.map((node) => node.id === saved.id ? saved : node)};
}
