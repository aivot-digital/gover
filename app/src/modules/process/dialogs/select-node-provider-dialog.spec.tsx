import {useState} from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {
    ProcessNodeExecutionType,
    type ProcessNodeProvider,
    ProcessNodeType,
} from '../services/process-node-provider-api-service';
import {getSearchedNodeProviders, SelectNodeProviderDialog} from './select-node-provider-dialog';

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: () => false,
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

describe('getSearchedNodeProviders', () => {
    it('searches both the abstract and detailed description', () => {
        const provider = createProvider();

        expect(getSearchedNodeProviders([provider], 'Kurzfassung')).toEqual([provider]);
        expect(getSearchedNodeProviders([provider], 'Markdowninhalt')).toEqual([provider]);
    });

    it('keeps renamed variants together with the newer major version first', () => {
        const older = createProvider({name: 'Altbestand'});
        const newer = createProvider({name: 'Neuaufnahme', majorVersion: 2, componentVersion: '2.1.0'});
        const other = createProvider({name: 'Meldung', key: 'de.aivot.test.other'});
        const providers = [older, other, newer];

        expect(getSearchedNodeProviders(providers, '')).toEqual([other, newer, older]);
        expect(providers).toEqual([older, other, newer]);
    });
});

describe('SelectNodeProviderDialog', () => {
    it('starts other details at the top while preserving the list, search and focus', async () => {
        const user = userEvent.setup();
        const onSelect = vi.fn();
        render(<SelectNodeProviderDialog
            open
            nodeProviders={[
                createProvider({name: 'Erstes Element'}),
                createProvider({name: 'Zweites Element', key: 'de.aivot.test.second'}),
            ]}
            onClose={vi.fn()}
            onSelect={onSelect}
        />);
        const search = screen.getByRole('searchbox', {name: 'Prozesselement suchen'});
        fireEvent.change(search, {target: {value: 'Kurzfassung'}});
        const first = await screen.findByRole('button', {name: 'Erstes Element: Details anzeigen'});
        const second = screen.getByRole('button', {name: 'Zweites Element: Details anzeigen'});
        const list = first.closest('.MuiAccordion-root')!.parentElement as HTMLElement;
        list.scrollTop = 180;

        await user.click(first);
        const body = () => screen.getByRole('button', {name: 'Details schließen'})
            .parentElement!.previousElementSibling as HTMLElement;
        expect(first).toHaveAttribute('aria-expanded', 'true');
        expect(first).toHaveAttribute('aria-controls', screen.getByRole('region', {name: 'Details: Erstes Element'}).id);
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        body().scrollTop = 320;

        await user.click(second);

        expect(body().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'})).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByText('1.4.2')).not.toBeInTheDocument();
        expect(search).toHaveValue('Kurzfassung');
        expect(list.scrollTop).toBe(180);
        expect(second).toHaveFocus();
        expect(first).toHaveAttribute('aria-expanded', 'false');
        expect(first).not.toHaveAttribute('aria-controls');

        body().scrollTop = 120;
        await user.click(second);
        expect(body().scrollTop).toBe(120);
        await user.click(first);
        expect(body().scrollTop).toBe(0);
        await user.click(screen.getByRole('button', {name: 'Details schließen'}));
        expect(first).toHaveFocus();
        expect(screen.queryByRole('region', {name: 'Details: Erstes Element'})).not.toBeInTheDocument();
        await user.click(first);
        expect(body().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'})).toHaveAttribute('aria-expanded', 'false');
        expect(onSelect).not.toHaveBeenCalled();
    });

    it('resets details for another major version and preserves the reading position for metadata updates', async () => {
        const user = userEvent.setup();
        const older = createProvider({name: 'Vorherige Version'});
        const newer = createProvider({name: 'Aktuelle Version', majorVersion: 2, componentVersion: '2.1.0'});
        const props = {open: true, onClose: vi.fn(), onSelect: vi.fn()};
        const {rerender} = render(<SelectNodeProviderDialog {...props} nodeProviders={[older, newer]}/>);
        const body = () => screen.getByRole('button', {name: 'Details schließen'})
            .parentElement!.previousElementSibling as HTMLElement;

        await user.click(screen.getByRole('button', {name: 'Vorherige Version: Details anzeigen'}));
        body().scrollTop = 240;
        await user.click(screen.getByRole('button', {name: 'Aktuelle Version: Details anzeigen'}));
        expect(body().scrollTop).toBe(0);
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        expect(screen.getByText('2.1.0')).toBeVisible();
        body().scrollTop = 160;

        rerender(<SelectNodeProviderDialog {...props} nodeProviders={[older, {...newer, componentVersion: '2.1.1'}]}/>);

        expect(body().scrollTop).toBe(160);
        expect(screen.getByRole('button', {name: 'Technische Informationen'})).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByText('2.1.1')).toBeVisible();
        await user.click(screen.getByRole('button', {name: 'Vorherige Version: Details anzeigen'}));
        expect(body().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'})).toHaveAttribute('aria-expanded', 'false');
    });

    it('returns focus to search when the details trigger has been filtered out', async () => {
        const user = userEvent.setup();
        render(<SelectNodeProviderDialog open nodeProviders={[createProvider()]} onClose={vi.fn()} onSelect={vi.fn()}/>);
        await user.click(screen.getByRole('button', {name: 'Test node: Details anzeigen'}));
        const search = screen.getByRole('searchbox', {name: 'Prozesselement suchen'});
        await user.type(search, 'unauffindbar');
        await waitFor(() => expect(screen.queryByRole('button', {name: 'Test node: Details anzeigen'})).not.toBeInTheDocument());

        await user.click(screen.getByRole('button', {name: 'Details schließen'}));

        expect(search).toHaveFocus();
        expect(search).toHaveValue('unauffindbar');
    });

    it('returns focus to search when the opening button remains inside a collapsed group', async () => {
        const user = userEvent.setup();
        const onSelect = vi.fn();
        render(<SelectNodeProviderDialog open nodeProviders={[createProvider()]} onClose={vi.fn()} onSelect={onSelect}/>);
        const trigger = screen.getByRole('button', {name: 'Test node: Details anzeigen'});
        const group = trigger.closest('.MuiAccordion-root') as HTMLElement;
        const groupToggle = within(within(group).getByRole('heading')).getByRole('button');
        const search = screen.getByRole('searchbox', {name: 'Prozesselement suchen'});
        await user.click(trigger);
        await user.click(groupToggle);
        await waitFor(() => expect(trigger).not.toBeVisible());
        expect(trigger.isConnected).toBe(true);
        // JSDOM allows focusing hidden elements, unlike the browser.
        const focusTrigger = vi.spyOn(trigger, 'focus').mockImplementation(() => undefined);

        await user.click(screen.getByRole('button', {name: 'Details schließen'}));

        expect(focusTrigger).toHaveBeenCalledWith({preventScroll: true});
        expect(search).toHaveFocus();
        expect(groupToggle).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region', {name: 'Details: Test node'})).not.toBeInTheDocument();
        expect(onSelect).not.toHaveBeenCalled();
    });

    it('focuses search on every opening and restores focus after closing with Escape', async () => {
        const user = userEvent.setup();
        const onSelect = vi.fn();

        function Harness() {
            const [open, setOpen] = useState(false);

            return <>
                <button onClick={() => setOpen(true)}>Prozesselement hinzufügen</button>
                <SelectNodeProviderDialog
                    open={open}
                    nodeProviders={[createProvider()]}
                    onClose={() => setOpen(false)}
                    onSelect={onSelect}
                />
            </>;
        }

        render(<Harness/>);
        const opener = screen.getByRole('button', {name: 'Prozesselement hinzufügen'});
        await user.click(opener);

        const dialog = screen.getByRole('dialog', {name: 'Prozesselement hinzufügen'});
        const search = within(dialog).getByRole('searchbox', {name: 'Prozesselement suchen'});
        expect(search).toHaveFocus();
        await user.keyboard('Kurzfassung');
        expect(search).toHaveValue('Kurzfassung');
        expect(onSelect).not.toHaveBeenCalled();

        await user.tab({shift: true});
        expect(within(dialog).getByRole('tab', {name: 'Elemente'})).toHaveFocus();
        await user.tab();
        expect(search).toHaveFocus();

        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(opener).toHaveFocus();

        await user.click(opener);
        const reopenedSearch = screen.getByRole('searchbox', {name: 'Prozesselement suchen'});
        expect(reopenedSearch).toHaveFocus();
        expect(reopenedSearch).toHaveValue('');
    });

    it('shows a single active element without version badges and keeps its full version in the details', async () => {
        const user = userEvent.setup();
        render(<SelectNodeProviderDialog
            open
            nodeProviders={[createProvider()]}
            onClose={vi.fn()}
            onSelect={vi.fn()}
        />);

        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();
        expect(screen.queryByText('Veraltet')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Test node: Details anzeigen'}));

        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();
        expect(screen.queryByText('1.4.2')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        expect(screen.getByText('Version der Elementdefinition')).toBeInTheDocument();
        expect(screen.getByText('1.4.2')).toBeInTheDocument();
    });

    it('keeps major-version labels stable when the search leaves only one variant', async () => {
        const user = userEvent.setup();
        const older = createProvider({abstractDescription: 'Altbestand'});
        const newer = createProvider({majorVersion: 2, componentVersion: '2.1.0', abstractDescription: 'Neubau'});
        const onSelect = vi.fn();

        render(<SelectNodeProviderDialog
            open
            nodeProviders={[older, newer]}
            onClose={vi.fn()}
            onSelect={onSelect}
        />);

        expect(screen.getByText('Version 1')).toBeInTheDocument();
        expect(screen.getByText('Version 2')).toBeInTheDocument();
        expect(screen.queryByText('Veraltet')).not.toBeInTheDocument();

        await user.type(screen.getByRole('searchbox', {name: 'Prozesselement suchen'}), 'Altbestand');
        await waitFor(() => expect(screen.getAllByRole('button', {name: 'Test node: Details anzeigen'})).toHaveLength(1));

        expect(screen.getByText('Version 1')).toBeInTheDocument();
        expect(screen.queryByText('Version 2')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Test node: Details anzeigen'}));
        expect(screen.getAllByText('Version 1')).toHaveLength(2);
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        expect(screen.getByText('1.4.2')).toBeInTheDocument();

        await user.click(screen.getAllByRole('button', {name: 'Hinzufügen'}).at(-1)!);
        expect(onSelect).toHaveBeenCalledWith(older);
    });

    it('counts versions after the compatibility filter', () => {
        render(<SelectNodeProviderDialog
            open
            nodeProviders={[createProvider(), createProvider({majorVersion: 2, componentVersion: '2.1.0'})]}
            filter={(provider) => provider.majorVersion === 2}
            onClose={vi.fn()}
            onSelect={vi.fn()}
        />);

        expect(screen.getAllByRole('button', {name: 'Test node: Details anzeigen'})).toHaveLength(1);
        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();
    });

    it('does not treat equally named elements from different plugins as variants', () => {
        render(<SelectNodeProviderDialog
            open
            nodeProviders={[
                createProvider(),
                createProvider({key: 'de.aivot.other.node', parentPluginKey: 'de.aivot.other', majorVersion: 2}),
            ]}
            onClose={vi.fn()}
            onSelect={vi.fn()}
        />);

        expect(screen.getAllByRole('button', {name: 'Test node: Details anzeigen'})).toHaveLength(2);
        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();
    });

    it('marks deprecated elements in the list and details without preventing their selection', async () => {
        const user = userEvent.setup();
        const provider = createProvider({deprecationNotice: 'Bitte **Ersatzaktion** verwenden.'});
        const onSelect = vi.fn();

        render(<SelectNodeProviderDialog
            open
            nodeProviders={[provider]}
            onClose={vi.fn()}
            onSelect={onSelect}
        />);

        expect(screen.getByText('Veraltet')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test node: Hinzufügen'})).toBeEnabled();
        expect(screen.queryByText(/^Version \d/)).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Test node: Details anzeigen'}));
        expect(screen.getAllByText('Veraltet')).toHaveLength(2);
        expect(screen.getByRole('alert')).toHaveTextContent('Bitte Ersatzaktion verwenden.');
        expect(screen.getByText('Ersatzaktion').tagName).toBe('STRONG');

        await user.click(screen.getAllByRole('button', {name: 'Hinzufügen'}).at(-1)!);
        expect(onSelect).toHaveBeenCalledWith(provider);
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
        abstractDescription: 'Kurze Kurzfassung für Listen.',
        description: 'Ausführlicher **Markdowninhalt** für Details.',
        documentationUrl: null,
        parentPluginKey: 'de.aivot.test',
        outputs: [],
        ...overrides,
    };
}
