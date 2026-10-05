import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {type PluginDTO, PluginsApiService} from '../../../services/plugins-api-service';
import {
    ProcessNodeExecutionType,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../services/process-node-provider-api-service';
import {
    ProcessNodeProviderDetailsContent,
    ProcessNodeProviderDetailsHeader,
} from './process-node-provider-details';
import {ProcessNodeProviderBadges} from './process-node-provider-badges';

const mocks = vi.hoisted(() => ({
    useHasSystemPermission: vi.fn(),
}));

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: mocks.useHasSystemPermission,
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

describe('ProcessNodeProviderDetails', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        mocks.useHasSystemPermission.mockReset();
        mocks.useHasSystemPermission.mockReturnValue(false);
    });

    it('renders the detailed description, type and documentation link', () => {
        render(<>
            <ProcessNodeProviderDetailsHeader provider={createProvider()}/>
            <ProcessNodeProviderDetailsContent provider={createProvider()} showDescription/>
        </>);

        expect(screen.getByText('Markdowninhalt').tagName).toBe('STRONG');
        expect(screen.queryByText('Kurze Zusammenfassung.')).not.toBeInTheDocument();
        expect(screen.getByText('Aktion')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Dokumentation öffnen'})).toHaveAttribute(
            'href',
            'https://docs.example.com/process-nodes/test',
        );
    });

    it('keeps the full version in technical information without adding header badges for active elements', () => {
        render(<>
            <ProcessNodeProviderDetailsHeader provider={createProvider()}/>
            <ProcessNodeProviderDetailsContent provider={createProvider()}/>
        </>);

        expect(screen.getByText('Technische Informationen')).toBeInTheDocument();
        expect(screen.getByText('Version der Elementdefinition')).toBeInTheDocument();
        expect(screen.getByText('1.4.2')).toBeInTheDocument();
        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();
        expect(screen.queryByText('Typ')).not.toBeInTheDocument();
        expect(screen.queryByText('Veraltet')).not.toBeInTheDocument();
    });

    it.each(['', '   '])('does not mark an element with an empty deprecation notice as outdated', (deprecationNotice) => {
        render(<>
            <ProcessNodeProviderDetailsHeader provider={createProvider({deprecationNotice})}/>
            <ProcessNodeProviderDetailsContent provider={createProvider({deprecationNotice})}/>
        </>);

        expect(screen.queryByText('Veraltet')).not.toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('allows opening deprecation information from the editor badge with the keyboard', async () => {
        const onShowDeprecationDetails = vi.fn();
        const user = userEvent.setup();

        render(<ProcessNodeProviderBadges
            provider={createProvider({deprecationNotice: 'Bitte die Ersatzaktion verwenden.'})}
            onShowDeprecationDetails={onShowDeprecationDetails}
        />);

        const badge = screen.getByRole('button', {name: 'Veraltet – Hinweise anzeigen'});
        expect(badge).toHaveAttribute('aria-haspopup', 'dialog');
        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();

        await user.tab();
        expect(badge).toHaveFocus();
        await user.keyboard('{Enter}');
        expect(onShowDeprecationDetails).toHaveBeenCalledOnce();
    });

    it('shows only the plugin category and key without plugin.read', () => {
        const getPlugin = vi.spyOn(PluginsApiService.prototype, 'getPlugin');

        render(<ProcessNodeProviderDetailsContent provider={createProvider()}/>);

        expect(screen.getByText('Plugin')).toBeInTheDocument();
        expect(screen.getByText('de.aivot.test')).toBeInTheDocument();
        expect(screen.queryByText('Aivot Test-Plugin')).not.toBeInTheDocument();
        expect(getPlugin).not.toHaveBeenCalled();
    });

    it('marks the core plugin as standard scope', () => {
        render(<ProcessNodeProviderDetailsContent provider={createProvider({
            parentPluginKey: 'de.aivot.core',
        })}/>);

        expect(screen.getByText('Standardumfang')).toBeInTheDocument();
        expect(screen.getByText('de.aivot.core')).toBeInTheDocument();
    });

    it('loads authorized plugin metadata and opens the reusable plugin dialog', async () => {
        mocks.useHasSystemPermission.mockImplementation((permission: Permission) => (
            permission === Permission.PLUGIN_READ
        ));
        vi.spyOn(PluginsApiService.prototype, 'getPlugin').mockResolvedValue(plugin);
        const user = userEvent.setup();

        render(<ProcessNodeProviderDetailsContent provider={createProvider()}/>);

        expect(await screen.findByText('Aivot Test-Plugin')).toBeInTheDocument();
        expect(screen.getByText('Aivot Test GmbH')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {
            name: 'Plugin-Informationen zu Aivot Test-Plugin anzeigen',
        }));

        expect(screen.getByRole('dialog', {name: 'Plugin-Informationen'})).toBeInTheDocument();
        expect(screen.getByText('Pluginbeschreibung').tagName).toBe('STRONG');
        expect(screen.getByText('Version 2.3.4')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'https://example.com'})).toHaveAttribute(
            'rel',
            'noopener noreferrer',
        );
    });

    it('offers a retry when authorized plugin metadata cannot be loaded', async () => {
        mocks.useHasSystemPermission.mockReturnValue(true);
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        const getPlugin = vi.spyOn(PluginsApiService.prototype, 'getPlugin')
            .mockRejectedValueOnce(new Error('Request failed'))
            .mockResolvedValueOnce(plugin);
        const user = userEvent.setup();

        render(<ProcessNodeProviderDetailsContent provider={createProvider()}/>);

        await screen.findByText('Die Plugin-Informationen konnten nicht geladen werden.');
        await user.click(screen.getByRole('button', {name: 'Erneut versuchen'}));

        expect(await screen.findByText('Aivot Test-Plugin')).toBeInTheDocument();
        expect(getPlugin).toHaveBeenCalledTimes(2);
    });

    it('opens the TypeScript definition for one output', async () => {
        const user = userEvent.setup();

        render(<ProcessNodeProviderDetailsContent provider={createProvider({
            outputs: [{
                key: 'result',
                label: 'Ergebnis',
                description: 'Das berechnete Ergebnis.',
                typeDefinition: '{ successful: boolean; value: string }',
            }],
        })}/>);

        const outputSection = screen.getByRole('region', {name: 'Ergebnis'});
        expect(within(outputSection).getByText('Das berechnete Ergebnis.')).toBeInTheDocument();
        expect(within(outputSection).getByText('Schlüssel:')).toBeInTheDocument();
        expect(within(outputSection).getByRole('button', {name: 'Schlüssel result kopieren'})).toBeEnabled();

        await user.click(screen.getByRole('button', {
            name: 'Details zu Ergebnis anzeigen',
        }));

        const dialog = screen.getByRole('dialog', {name: 'Details zu den Ausgangsdaten'});
        expect(dialog).toHaveAccessibleDescription('Das berechnete Ergebnis.');
        const accessSection = within(dialog).getByRole('region', {name: 'Zugriff auf die Daten'});
        expect(within(accessSection).getByText(/mit dem Datenschlüssel des Prozesselements kombiniert/)).toBeInTheDocument();
        expect(within(accessSection).getByRole('button', {name: 'Schlüssel result kopieren'})).toBeEnabled();
        expect(within(dialog).getByRole('region', {name: 'Datentyp'})).toHaveTextContent('TypeScript');
        expect(screen.getByTestId('expandable-code-block')).toHaveTextContent(
            '{ successful: boolean; value: string }',
        );
    });

    it('renders a deprecation notice as Markdown', () => {
        const provider = createProvider({deprecationNotice: 'Bitte **Ersatzaktion** verwenden.'});
        render(<>
            <ProcessNodeProviderDetailsHeader provider={provider}/>
            <ProcessNodeProviderDetailsContent provider={provider}/>
        </>);

        expect(screen.getByText('Veraltet')).toBeInTheDocument();
        expect(screen.getByRole('alert')).toHaveTextContent('Bitte Ersatzaktion verwenden.');
        expect(screen.getByText('Ersatzaktion').tagName).toBe('STRONG');
    });
});

function createProvider(overrides: Partial<ProcessNodeProvider> = {}): ProcessNodeProvider {
    return {
        key: 'de.aivot.test.node',
        componentKey: 'node',
        componentType: 'ProcessNodeDefinition',
        componentVersion: '1.4.2',
        deprecationNotice: null,
        majorVersion: 1,
        type: ProcessNodeType.Action,
        executionTypes: [ProcessNodeExecutionType.Automatic],
        name: 'Test node',
        abstractDescription: 'Kurze Zusammenfassung.',
        description: 'Ausführlicher **Markdowninhalt** für Details.',
        documentationUrl: 'https://docs.example.com/process-nodes/test',
        parentPluginKey: 'de.aivot.test',
        ports: [],
        outputs: [],
        ...overrides,
    };
}

const plugin: PluginDTO = {
    key: 'de.aivot.test',
    name: 'Aivot Test-Plugin',
    description: 'Ausführliche **Pluginbeschreibung**.',
    documentationUrl: 'https://docs.example.com/plugins/test',
    buildDate: '2026-08-25T10:00:00Z',
    version: '2.3.4',
    vendorName: 'Aivot Test GmbH',
    vendorWebsite: 'https://example.com',
    changelog: '',
    deprecationNotice: null,
    components: [],
};
