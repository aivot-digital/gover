import {expect, it} from 'vitest';
import ELK, {type ElkNode} from 'elkjs/lib/elk.bundled.js';
import {createElkLoader} from './elk-loader';

it('preserves layered layout results with the real dynamically imported ELK runtime', async () => {
    const graph: ElkNode = {
        id: 'root',
        layoutOptions: {'elk.algorithm': 'layered', 'elk.direction': 'DOWN'},
        children: [{id: 'a', width: 100, height: 50}, {id: 'b', width: 100, height: 50}],
        edges: [{id: 'a-b', sources: ['a'], targets: ['b']}],
    };
    const eager = new ELK();
    const lazy = await createElkLoader()();
    const expected = await eager.layout(structuredClone(graph));
    const actual = await lazy.layout(structuredClone(graph));
    expect(actual.children).toMatchObject(expected.children!.map(({id, x, y, width, height}) => ({id, x, y, width, height})));
    const edgeGeometry = (result: ElkNode) => result.edges![0].sections!.map(({startPoint, endPoint, bendPoints}) => ({startPoint, endPoint, bendPoints}));
    expect(edgeGeometry(actual)).toEqual(edgeGeometry(expected));
    expect(actual.children![1].y).toBeGreaterThan(actual.children![0].y!);
    expect(actual.edges![0].sections).toHaveLength(1);
});
