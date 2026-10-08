import {expect, it} from 'vitest';
import {getDefaultProcessNodePort, mapReplacementPorts} from './process-node-ports';

const port = (key: string, deadlineKey: string | null = null) => ({key, deadlineKey, label: key, description: 'Fortsetzen'});
const edge = (id: number, viaPort: string) => ({id, viaPort, fromNodeId: 1, toNodeId: 2, processId: 1, processVersion: 1});

it('never uses any deadline path as the automatic continuation when inserting or importing a node', () => {
    expect(getDefaultProcessNodePort([])).toBeUndefined();
    expect(getDefaultProcessNodePort([port('message_overdue', 'message'), port('form_overdue', 'form')])).toBeUndefined();
    expect(getDefaultProcessNodePort([port('form_overdue', 'form'), port('approved')])?.key).toBe('approved');
});

it('preserves stable keys on replacement before assigning remaining compatible ports', () => {
    const source = [port('removed'), port('approved'), port('form_overdue', 'form')];
    const mapping = mapReplacementPorts([edge(1, 'removed'), edge(2, 'approved'), edge(3, 'form_overdue')], source,
        [port('approved'), port('new'), port('form_overdue', 'form')]);
    expect(mapping.get(1)).toBe('new');
    expect(mapping.get(2)).toBe('approved');
    expect(mapping.get(3)).toBe('form_overdue');
});

it('keeps deadline phases separate from regular ports and other phases even when port keys match', () => {
    for (const [from, to] of [[port('same'), port('same', 'form')], [port('same', 'form'), port('same')],
        [port('same', 'form'), port('same', 'message')], [port('form_overdue', 'form'), port('message_overdue', 'message')]]) {
        expect(mapReplacementPorts([edge(1, from.key)], [from], [to]).get(1)).toBeNull();
    }
});

it('does not reconnect an unavailable source port by guessing its type', () => {
    expect(mapReplacementPorts([edge(1, 'removed')], [], [port('next')]).get(1)).toBeNull();
});
