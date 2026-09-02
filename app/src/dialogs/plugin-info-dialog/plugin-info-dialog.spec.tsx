import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {type PluginDTO} from '../../services/plugins-api-service';
import {PluginInfoDialog} from './plugin-info-dialog';

const mocks = vi.hoisted(() => ({
    useHasSystemPermission: vi.fn(),
}));

vi.mock('../../modules/permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: mocks.useHasSystemPermission,
}));

describe('PluginInfoDialog', () => {
    beforeEach(() => {
        mocks.useHasSystemPermission.mockReset();
        mocks.useHasSystemPermission.mockReturnValue(true);
    });

    it('renders plugin metadata, links and a Markdown deprecation notice', () => {
        render(<PluginInfoDialog open plugin={plugin} onClose={vi.fn()}/>);

        expect(screen.getByRole('dialog', {name: 'Plugin-Informationen'})).toBeInTheDocument();
        expect(screen.getByText('Test-Plugin')).toBeInTheDocument();
        expect(screen.getByText('Version 2.3.4')).toBeInTheDocument();
        expect(screen.getByText('Veraltet')).toBeInTheDocument();
        expect(screen.getByText('Pluginbeschreibung').tagName).toBe('STRONG');
        expect(screen.getByText('Ersatz-Plugin').tagName).toBe('STRONG');
        expect(screen.getByRole('link', {name: 'https://vendor.example.com'})).toHaveAttribute(
            'target',
            '_blank',
        );
        expect(screen.getByRole('link', {name: 'Dokumentation öffnen'})).toHaveAttribute(
            'href',
            'https://docs.example.com/plugin',
        );
    });

    it('does not expose plugin data without plugin.read', () => {
        mocks.useHasSystemPermission.mockReturnValue(false);

        render(<PluginInfoDialog open plugin={plugin} onClose={vi.fn()}/>);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(screen.queryByText('Test-Plugin')).not.toBeInTheDocument();
    });
});

const plugin: PluginDTO = {
    key: 'com.example.plugin',
    name: 'Test-Plugin',
    description: 'Ausführliche **Pluginbeschreibung**.',
    documentationUrl: 'https://docs.example.com/plugin',
    buildDate: '2026-08-25T10:00:00Z',
    version: '2.3.4',
    vendorName: 'Example GmbH',
    vendorWebsite: 'https://vendor.example.com',
    changelog: '',
    deprecationNotice: 'Bitte das **Ersatz-Plugin** verwenden.',
    components: [],
};
