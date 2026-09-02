import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {
    GenericDetailsPageProvider,
} from '../../../../../components/generic-details-page/generic-details-page-context';
import {ConfirmProvider} from '../../../../../providers/confirm-provider';
import {
    PluginComponentType,
    type PluginDTO,
} from '../../../../../services/plugins-api-service';
import {ExtensionsList, type ExtensionsDetailsPageItem} from './extensions';

describe('ExtensionsList', () => {
    it('shows plain-text abstracts and opens Markdown details for every component version', async () => {
        const user = userEvent.setup();
        renderExtensionsList();

        expect(screen.getByText('Kurze **Plain-Text** Zusammenfassung.')).toBeInTheDocument();
        expect(screen.queryByText('Plain-Text', {selector: 'strong'})).not.toBeInTheDocument();
        expect(screen.queryByText('Markdownbeschreibung')).not.toBeInTheDocument();
        expect(screen.getByText('Dokumentation öffnen').closest('a')).toHaveAttribute(
            'href',
            'https://docs.example.com/plugins/test',
        );

        const detailButtons = screen
            .getAllByText('Details')
            .filter((element): element is HTMLButtonElement => element instanceof HTMLButtonElement);
        expect(detailButtons).toHaveLength(2);

        await user.click(detailButtons[0]);

        let dialog = (await screen.findByText('Komponente: Example component')).closest('[role="dialog"]');
        expect(dialog).not.toBeNull();
        if (!(dialog instanceof HTMLElement)) {
            throw new Error('Component details dialog was not rendered.');
        }
        expect(within(dialog).getByText('Kurze **Plain-Text** Zusammenfassung.')).toBeInTheDocument();
        expect(within(dialog).getByText('Markdownbeschreibung').tagName).toBe('STRONG');
        expect(within(dialog).getByText('2.0.0')).toBeInTheDocument();
        expect(within(dialog).getByText('Dokumentation öffnen').closest('a')).toHaveAttribute(
            'href',
            'https://docs.example.com/components/example/v2',
        );

        await user.click(within(dialog).getByText('Schließen'));
        await waitFor(() => {
            expect(document.querySelector('[role="dialog"]')).not.toBeInTheDocument();
        });

        await user.click(detailButtons[1]);

        dialog = (await screen.findByText('Komponente: Example component')).closest('[role="dialog"]');
        expect(dialog).not.toBeNull();
        if (!(dialog instanceof HTMLElement)) {
            throw new Error('Legacy component details dialog was not rendered.');
        }
        expect(within(dialog).getByText('Legacy-Markdown').tagName).toBe('STRONG');
        expect(within(dialog).getByText('1.0.0')).toBeInTheDocument();
        expect(within(dialog).queryByText('Dokumentation öffnen')).not.toBeInTheDocument();
    });
});

function renderExtensionsList() {
    const contextValue = {
        item: {
            plugins: [plugin],
            loadingFailed: false,
        } satisfies ExtensionsDetailsPageItem,
        setItem: vi.fn(),
        setAdditionalData: vi.fn(),
        isBusy: false,
        setIsBusy: vi.fn(),
        refresh: vi.fn(),
        isEditable: false,
    };

    return render(
        <GenericDetailsPageProvider value={contextValue}>
            <ConfirmProvider>
                <ExtensionsList/>
            </ConfirmProvider>
        </GenericDetailsPageProvider>,
    );
}

const plugin: PluginDTO = {
    key: 'de.aivot.test',
    name: 'Test extension',
    description: 'Beschreibung der Erweiterung.',
    documentationUrl: 'https://docs.example.com/plugins/test',
    buildDate: '2026-08-24T10:00:00Z',
    version: '1.0.0',
    vendorName: 'Aivot',
    vendorWebsite: '',
    changelog: 'Initial release',
    deprecationNotice: null,
    components: [[
        {
            parentPluginKey: 'de.aivot.test',
            componentKey: 'example',
            key: 'de.aivot.test.example',
            componentVersion: '2.0.0',
            majorVersion: 2,
            name: 'Example component',
            componentType: PluginComponentType.ProcessNodeDefinition,
            abstractDescription: 'Kurze **Plain-Text** Zusammenfassung.',
            description: 'Ausführliche **Markdownbeschreibung** für die aktuelle Version.',
            documentationUrl: 'https://docs.example.com/components/example/v2',
            deprecationNotice: null,
        },
        {
            parentPluginKey: 'de.aivot.test',
            componentKey: 'example',
            key: 'de.aivot.test.example',
            componentVersion: '1.0.0',
            majorVersion: 1,
            name: 'Example component',
            componentType: PluginComponentType.ProcessNodeDefinition,
            abstractDescription: 'Kurze Zusammenfassung der älteren Version.',
            description: 'Ausführliche **Legacy-Markdown** Beschreibung.',
            documentationUrl: null,
            deprecationNotice: null,
        },
    ]],
};
