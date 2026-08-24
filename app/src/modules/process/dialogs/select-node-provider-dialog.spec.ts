import {describe, expect, it} from 'vitest';
import {
    ProcessNodeExecutionType,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../services/process-node-provider-api-service';
import {getSearchedNodeProviders} from './select-node-provider-dialog';

describe('getSearchedNodeProviders', () => {
    it('searches both the abstract and detailed description', () => {
        const provider = createProvider();

        expect(getSearchedNodeProviders([provider], 'Kurzfassung')).toEqual([provider]);
        expect(getSearchedNodeProviders([provider], 'Markdowninhalt')).toEqual([provider]);
    });
});

function createProvider(): ProcessNodeProvider {
    return {
        key: 'de.aivot.test.node',
        componentKey: 'node',
        componentType: 'ProcessNodeDefinition',
        componentVersion: '1.0.0',
        deprecationNotice: null,
        majorVersion: 1,
        type: ProcessNodeType.Action,
        executionTypes: [ProcessNodeExecutionType.Automatic],
        name: 'Test node',
        abstractDescription: 'Kurze Kurzfassung für Listen.',
        description: 'Ausführlicher **Markdowninhalt** für Details.',
        parentPluginKey: 'de.aivot.test',
        ports: [],
        outputs: [],
    };
}
