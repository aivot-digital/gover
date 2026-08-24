import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {
    ProcessNodeExecutionType,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../services/process-node-provider-api-service';
import {ProcessNodeProviderDetailsContent} from './process-node-provider-details';

describe('ProcessNodeProviderDetailsContent', () => {
    it('renders the detailed description as Markdown', () => {
        render(<ProcessNodeProviderDetailsContent provider={createProvider()} showDescription/>);

        expect(screen.getByText('Markdowninhalt').tagName).toBe('STRONG');
        expect(screen.queryByText('Kurze Zusammenfassung.')).not.toBeInTheDocument();
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
        description: 'Ausführlicher **Markdowninhalt** für Details.',
        parentPluginKey: 'de.aivot.test',
        ports: [],
        outputs: [],
    };
}
