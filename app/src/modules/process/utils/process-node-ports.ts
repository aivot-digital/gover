import {type ProcessDefinitionEdgeEntity} from '../entities/process-definition-edge-entity';
import {type ProcessNodePort} from '../services/process-node-provider-api-service';

export function getDefaultProcessNodePort(ports: ProcessNodePort[]): ProcessNodePort | undefined {
    return ports.find((port) => port.deadlineKey == null);
}

export function mapReplacementPorts(
    outgoingEdges: ProcessDefinitionEdgeEntity[],
    sourcePorts: ProcessNodePort[],
    replacementPorts: ProcessNodePort[],
): Map<number, string | null> {
    const availablePorts = new Map(replacementPorts.map((port) => [port.key, port]));
    const sourceByKey = new Map(sourcePorts.map((port) => [port.key, port]));
    const result = new Map<number, string | null>();

    // Preserve exact matches before assigning fallbacks; expiry paths retain their phase identity.
    for (const edge of outgoingEdges) {
        const source = sourceByKey.get(edge.viaPort);
        const replacement = availablePorts.get(edge.viaPort);
        if (source && replacement && source.deadlineKey === replacement.deadlineKey) {
            availablePorts.delete(edge.viaPort);
            result.set(edge.id, edge.viaPort);
        }
    }
    for (const edge of outgoingEdges) {
        if (result.has(edge.id)) {
            continue;
        }
        const source = sourceByKey.get(edge.viaPort);
        const replacement = source && Array.from(availablePorts.values())
            .find((port) => port.deadlineKey === source.deadlineKey);
        result.set(edge.id, replacement?.key ?? null);
        if (replacement) {
            availablePorts.delete(replacement.key);
        }
    }
    return result;
}
