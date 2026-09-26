import {describe, expect, it} from 'vitest';
import {type ProcessNodeEntity} from '../../../../../entities/process-node-entity';
import {
    ProcessNodeExecutionType,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../../../../../services/process-node-provider-api-service';
import {getNodeDescription} from './node-utils';

describe('getNodeDescription', () => {
    it('uses the component abstract when the node has no custom description', () => {
        const node = {description: null} as ProcessNodeEntity;

        expect(getNodeDescription(node, createProvider())).toBe('Kurze Zusammenfassung.');
    });

    it('keeps a custom node description', () => {
        const node = {description: 'Eigene Beschreibung'} as ProcessNodeEntity;

        expect(getNodeDescription(node, createProvider())).toBe('Eigene Beschreibung');
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
        abstractDescription: 'Kurze Zusammenfassung.',
        description: 'Ausführliche Beschreibung.',
        documentationUrl: null,
        parentPluginKey: 'de.aivot.test',
        ports: [],
        outputs: [],
    };
}
