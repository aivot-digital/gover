import {expect, it} from 'vitest';
import {PROCESS_NODE_DEADLINE_PORT_KEY} from '../services/process-node-provider-api-service';
import {getDefaultProcessNodePort, mapReplacementPorts} from './process-node-ports';

const port = (key: string) => ({key, label: key, description: 'Fortsetzen'});
const edge = (id: number, viaPort: string) => ({id, viaPort, fromNodeId: 1, toNodeId: 2, processId: 1, processVersion: 1});

it('never uses the deadline path as the automatic continuation when inserting or importing a node', () => {
    expect(getDefaultProcessNodePort([])).toBeUndefined();
    expect(getDefaultProcessNodePort([port(PROCESS_NODE_DEADLINE_PORT_KEY)])).toBeUndefined();
    expect(getDefaultProcessNodePort([port(PROCESS_NODE_DEADLINE_PORT_KEY), port('approved')])?.key).toBe('approved');
});

it('preserves stable keys on replacement before assigning remaining configured ports', () => {
    const mapping = mapReplacementPorts([edge(1, 'removed'), edge(2, 'approved'), edge(3, PROCESS_NODE_DEADLINE_PORT_KEY)],
        [port('approved'), port('new'), port(PROCESS_NODE_DEADLINE_PORT_KEY)]);
    expect(mapping.get(1)).toBe('new');
    expect(mapping.get(2)).toBe('approved');
    expect(mapping.get(3)).toBe(PROCESS_NODE_DEADLINE_PORT_KEY);
});

it('keeps deadline paths separate from ordinary fallback paths', () => {
    expect(mapReplacementPorts([edge(1, 'approved')], [port(PROCESS_NODE_DEADLINE_PORT_KEY)]).get(1)).toBeNull();
    expect(mapReplacementPorts([edge(1, PROCESS_NODE_DEADLINE_PORT_KEY)], [port('approved')]).get(1)).toBeNull();
});
