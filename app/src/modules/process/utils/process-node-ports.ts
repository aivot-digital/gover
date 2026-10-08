import {type ProcessDefinitionEdgeEntity} from '../entities/process-definition-edge-entity';
import {PROCESS_NODE_DEADLINE_PORT_KEY, type ProcessNodePort} from '../services/process-node-provider-api-service';

export function getDefaultProcessNodePort(ports: ProcessNodePort[]): ProcessNodePort | undefined {
    return ports.find((port) => port.key !== PROCESS_NODE_DEADLINE_PORT_KEY);
}

export function mapReplacementPorts(
    outgoingEdges: ProcessDefinitionEdgeEntity[],
    replacementPorts: ProcessNodePort[],
): Map<number, string | null> {
    const availableKeys = new Set(replacementPorts.map((port) => port.key));
    const result = new Map<number, string | null>();

    // Reserve same-key matches before considering fallbacks, including the reserved deadline path.
    for (const edge of outgoingEdges) {
        if (availableKeys.delete(edge.viaPort)) {
            result.set(edge.id, edge.viaPort);
        }
    }
    for (const edge of outgoingEdges) {
        if (result.has(edge.id)) {
            continue;
        }
        const replacementKey = edge.viaPort === PROCESS_NODE_DEADLINE_PORT_KEY ? undefined
            : Array.from(availableKeys).find((key) => key !== PROCESS_NODE_DEADLINE_PORT_KEY);
        result.set(edge.id, replacementKey ?? null);
        if (replacementKey != null) {
            availableKeys.delete(replacementKey);
        }
    }
    return result;
}
