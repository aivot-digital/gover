import {afterEach, describe, expect, it, vi} from 'vitest';
import {ProcessNodeApiService} from './process-node-api-service';
import {generateProcessNodeDataKey} from '../utils/process-node-data-key';

afterEach(() => vi.restoreAllMocks());

describe('automatic process node data keys', () => {
    it('creates six lowercase characters beginning with a letter', () => {
        for (let i = 0; i < 100; i++) {
            expect(generateProcessNodeDataKey()).toMatch(/^[a-hjkmnp-tv-z][0-9a-hjkmnp-tv-z]{5}$/);
        }
    });

    it('retries a scoped key collision and returns the persisted key', async () => {
        const api = new ProcessNodeApiService();
        const create = vi
            .spyOn(api, 'create')
            .mockRejectedValueOnce({
                status: 409,
                message: 'Belegt',
                displayableToUser: true,
                details: {reason: 'process_node_data_key_conflict'},
            })
            .mockImplementationOnce(async (node) => ({
                ...node,
                ports: [],
                portResolutionProblems: [],
                id: 42,
            }));
        const input = {
            ...api.initialize(),
            dataKey: 'source_key',
        };
        const saved = await api.createWithGeneratedDataKey(input);
        expect(create).toHaveBeenCalledTimes(2);
        expect(saved.dataKey).toMatch(/^[a-hjkmnp-tv-z][0-9a-hjkmnp-tv-z]{5}$/);
        expect(saved.dataKey).not.toBe(create.mock.calls[0][0].dataKey);
        expect(input.dataKey).toBe('source_key');
    });

    it('limits collision retries and does not retry unrelated errors', async () => {
        const api = new ProcessNodeApiService();
        const conflict = {
            status: 409,
            message: 'Belegt',
            displayableToUser: true,
            details: {reason: 'process_node_data_key_conflict'},
        };
        const create = vi.spyOn(api, 'create').mockRejectedValue(conflict);
        await expect(api.createWithGeneratedDataKey(api.initialize())).rejects.toBe(conflict);
        expect(create).toHaveBeenCalledTimes(5);
        create.mockClear().mockRejectedValue({
            ...conflict,
            details: null,
        });
        await expect(api.createWithGeneratedDataKey(api.initialize())).rejects.toMatchObject({status: 409});
        expect(create).toHaveBeenCalledTimes(1);
    });
});

it('returns saved ports while sending only authoring data and supports prospective imports', async () => {
    const api = new ProcessNodeApiService();
    expect(api.initialize().timeLimitStrict).toBe(false);
    const node = {...api.initialize(), id: 12, timeLimitDays: 3, timeLimitStrict: true};
    const ports = [{key: '__deadline_exceeded', label: 'Fristüberschreitung', description: 'Fristablauf'}];
    const saved = {...node, ports};
    const fetch = vi.spyOn(api, 'fetch').mockResolvedValue(new Response(JSON.stringify(saved)));
    expect((await api.update(node.id, saved)).ports).toEqual(ports);
    const writeBody = JSON.parse(fetch.mock.calls[0][2] as string);
    expect(writeBody).not.toHaveProperty('ports');
    expect(writeBody).not.toHaveProperty('portResolutionProblems');
    expect(writeBody.timeLimitDays).toBe(3);
    expect(writeBody.timeLimitStrict).toBe(true);

    fetch.mockResolvedValue(new Response(JSON.stringify({ports, problems: []})));
    expect(await api.resolvePorts(saved, {forImport: true, useInitialConfiguration: true})).toEqual({ports, problems: []});
    expect(fetch.mock.calls[1][0]).toBe('POST');
    expect(fetch.mock.calls[1][1]).toBe('/api/process-nodes/resolve-ports/');
    const preview = JSON.parse(fetch.mock.calls[1][2] as string);
    expect(preview).toMatchObject({forImport: true, useInitialConfiguration: true, node: {timeLimitDays: 3, timeLimitStrict: true}});
    expect(preview.node).not.toHaveProperty('ports');
});
