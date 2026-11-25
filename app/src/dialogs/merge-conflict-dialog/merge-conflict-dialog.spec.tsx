import {type ComponentProps} from 'react';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {MergeConflictDialog} from './merge-conflict-dialog';
import {InputMode, InputVariableSource} from '../../models/input-mode';

const restoreTitle = 'Ungespeicherte Änderungen wiederherstellen?';
const mineLabel = 'Meine Änderung soll behalten werden';
const theirsLabel = 'Gespeicherter Stand soll übernommen werden';
const searchLabel = 'Abweichende Angaben suchen';

const groupedConflicts = [
    {
        id: 'street', label: 'Adresse: Platzhalter', propertyLabel: 'Platzhalter',
        group: {id: 'address', label: 'Adresse'}, mine: 'Straße eingeben', theirs: 'Neue Straße',
    },
    {
        id: 'required', label: 'Adresse: Pflichtfeld', propertyLabel: 'Pflichtfeld',
        group: {id: 'address', label: 'Adresse'}, mine: true, theirs: false,
    },
    {
        id: 'hint', label: 'Kontaktdaten: Hilfetext', propertyLabel: 'Hilfetext',
        group: {id: 'contact', label: 'Kontaktdaten'}, mine: 'Meine Hilfe', theirs: 'Gespeicherte Hilfe',
    },
];

function setup(overrides: Partial<ComponentProps<typeof MergeConflictDialog>> = {}) {
    const props = {
        open: true,
        mode: 'restore' as const,
        storedStateChanged: false,
        conflicts: [],
        onApply: vi.fn(),
        onDiscard: vi.fn(),
        onCancel: vi.fn(),
        ...overrides,
    };
    render(<MergeConflictDialog {...props}/>);
    return props;
}

describe('MergeConflictDialog', () => {
    it('shows the complete destination for a moved field and the removal on the other side', () => {
        const destination = 'Abschnitt „Ansprechpersonen“ › Gruppe „Zuständige Stelle für die Bearbeitung“ › Gruppe „Kontakt“';
        setup({conflicts: [{
            id: 'move-or-delete', label: 'E-Mail', propertyLabel: 'Position im Formular',
            group: {id: 'email', label: 'Text „E-Mail“'}, mine: undefined, theirs: {id: 'email'},
            elementPlacement: {elementId: 'email', theirs: destination},
        }]});
        const group = within(screen.getByRole('group', {name: /Position im Formular/}));
        expect(group.getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription('Entfernt');
        expect(group.getByRole('radio', {name: theirsLabel})).toHaveAccessibleDescription(`Verschoben nach ${destination}`);
    });

    it('updates a dependent element decision in the open dialog and submits only the visible decisions', async () => {
        const props = setup({
            conflicts: [groupedConflicts[0]],
            dependentConflicts: (choices) => choices.street === 'theirs' ? [groupedConflicts[1]] : [],
        });
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        const primary = within(dialog).getByRole('group', {name: /Platzhalter/});
        const user = userEvent.setup();
        expect(within(dialog).queryByRole('group', {name: /Pflichtfeld/})).not.toBeInTheDocument();
        await user.click(within(primary).getByRole('radio', {name: theirsLabel}));
        await user.click(within(within(dialog).getByRole('group', {name: /Pflichtfeld/})).getByRole('radio', {name: theirsLabel}));
        await user.click(within(primary).getByRole('radio', {name: mineLabel}));
        expect(within(dialog).queryByRole('group', {name: /Pflichtfeld/})).not.toBeInTheDocument();
        await user.click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));
        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({street: 'mine'});
    });

    it('explains recovery and discarding before restoring changes without conflicts', async () => {
        const props = setup();
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        expect(within(dialog).getByText(/Aus Ihrer letzten Bearbeitung liegen ungespeicherte Änderungen vor/))
            .toBeInTheDocument();
        expect(within(dialog).getByText(/Speichern Sie Ihre Änderungen nach der Wiederherstellung/))
            .toHaveTextContent('Beim Verwerfen wird die zwischengespeicherte Fassung gelöscht.');
        expect(within(dialog).queryByText(/Zwischenzeitlich wurde ein neuerer Stand gespeichert/))
            .not.toBeInTheDocument();
        expect(within(dialog).getAllByRole('button').map((button) => button.textContent))
            .toEqual(['Änderungen wiederherstellen', 'Änderungen verwerfen']);

        await userEvent.setup().click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({});
        expect(props.onDiscard).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it('explains that recovery combines changes with a newer stored state when there are no conflicts', async () => {
        const props = setup({storedStateChanged: true});
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        expect(within(dialog).getByText(/Zwischenzeitlich wurde ein neuerer Stand gespeichert/))
            .toHaveTextContent('Ihre Änderungen können mit diesem Stand zusammengeführt werden.');
        expect(within(dialog).queryByRole('radio')).not.toBeInTheDocument();

        await userEvent.setup().click(within(dialog).getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({});
        expect(props.onDiscard).not.toHaveBeenCalled();
    });

    it.each(['restore', 'save'] as const)('passes the selected conflict values to the %s action', async (mode) => {
        const props = setup({
            mode,
            storedStateChanged: true,
            conflicts: [
                {
                    id: 'name', label: 'Name des Elementes', mine: 'Mein Name', theirs: 'Gespeicherter Name',
                    group: {id: 'element', label: 'Element'},
                },
                {
                    id: 'description', label: 'Beschreibung', mine: 'Meine Beschreibung', theirs: 'Gespeicherte Beschreibung',
                    group: {id: 'element', label: 'Element'},
                },
            ],
        });
        const dialog = screen.getByRole('dialog', {
            name: mode === 'restore' ? restoreTitle : 'Neuerer Stand gespeichert',
        });
        const name = within(dialog).getByRole('group', {name: 'Abweichende Einstellung: Name des Elementes'});
        const description = within(dialog).getByRole('group', {name: 'Abweichende Einstellung: Beschreibung'});
        expect(within(name).getByRole('radio', {name: mineLabel})).toBeChecked();
        expect(within(description).getByRole('radio', {name: mineLabel})).toBeChecked();
        expect(within(name).getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription('Mein Name');
        expect(within(name).getByRole('radio', {name: theirsLabel})).toHaveAccessibleDescription('Gespeicherter Name');
        if (mode === 'restore') {
            expect(within(dialog).getByText(/Einige Angaben wurden in beiden Fassungen unterschiedlich geändert/))
                .toHaveTextContent('Alle übrigen Änderungen werden zusammengeführt.');
        }

        const user = userEvent.setup();
        await user.click(within(name).getByRole('radio', {name: theirsLabel}));
        await user.click(within(dialog).getByRole('button', {
            name: mode === 'restore' ? 'Änderungen wiederherstellen' : 'Übernehmen und speichern',
        }));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({name: 'theirs', description: 'mine'});
        expect(props.onDiscard).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it('groups settings under their element and preserves choices while groups are collapsed', async () => {
        const props = setup({conflicts: groupedConflicts});
        const address = screen.getByRole('button', {name: /^Adresse/});
        const contact = screen.getByRole('button', {name: /^Kontaktdaten/});
        expect(address).toHaveAttribute('aria-expanded', 'true');
        expect(contact).toHaveAttribute('aria-expanded', 'false');
        expect(screen.getByRole('group', {name: 'Abweichende Einstellung: Platzhalter'})).toBeVisible();
        const user = userEvent.setup();

        await user.click(within(screen.getByRole('group', {name: 'Abweichende Einstellung: Platzhalter'}))
            .getByRole('radio', {name: theirsLabel}));
        await user.click(contact);
        expect(address).toHaveAttribute('aria-expanded', 'true');
        expect(contact).toHaveAttribute('aria-expanded', 'true');
        await user.click(address);
        await waitFor(() => expect(screen.queryByRole('group', {name: 'Abweichende Einstellung: Platzhalter'}))
            .not.toBeInTheDocument());
        await user.click(address);
        expect(within(screen.getByRole('group', {name: 'Abweichende Einstellung: Platzhalter'}))
            .getByRole('radio', {name: theirsLabel})).toBeChecked();
        await user.click(screen.getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({street: 'theirs', required: 'mine', hint: 'mine'});
    });

    it('uses conflict labels when no grouping or property metadata is available', async () => {
        setup({conflicts: [
            {id: 'first', label: 'Erste Angabe', mine: 'Mein Wert', theirs: 'Anderer Wert'},
            {id: 'second', label: 'Zweite Angabe', mine: 'Mein zweiter Wert', theirs: 'Anderer zweiter Wert'},
        ]});
        expect(screen.getByRole('button', {name: /^Erste Angabe/})).toHaveAttribute('aria-expanded', 'true');
        const second = screen.getByRole('button', {name: /^Zweite Angabe/});
        expect(second).toHaveAttribute('aria-expanded', 'false');
        await userEvent.setup().click(second);

        expect(screen.getByRole('group', {name: 'Abweichende Einstellung: Zweite Angabe'})).toBeVisible();
    });

    it('searches group names, settings and values while preserving expansion and selections', async () => {
        const props = setup({conflicts: groupedConflicts});
        const user = userEvent.setup();
        const search = screen.getByRole('searchbox', {name: searchLabel});
        const address = screen.getByRole('button', {name: /^Adresse/});
        await user.click(address);
        expect(address).toHaveAttribute('aria-expanded', 'false');

        for (const term of ['KONTAKTDATEN', 'Hilfetext', 'Gespeicherte Hilfe']) {
            await user.clear(search);
            await user.type(search, term);
            await user.tab();
            expect(await screen.findByRole('group', {name: 'Abweichende Einstellung: Hilfetext'})).toBeVisible();
            await waitFor(() => expect(screen.queryByRole('button', {name: /^Adresse/})).not.toBeInTheDocument());
        }
        await user.click(within(screen.getByRole('group', {name: 'Abweichende Einstellung: Hilfetext'}))
            .getByRole('radio', {name: theirsLabel}));
        await user.clear(search);

        expect(await screen.findByRole('button', {name: /^Adresse/})).toHaveAttribute('aria-expanded', 'false');
        expect(screen.getByRole('button', {name: /^Kontaktdaten/})).toHaveAttribute('aria-expanded', 'false');
        await user.click(screen.getByRole('button', {name: /^Kontaktdaten/}));
        expect(within(screen.getByRole('group', {name: 'Abweichende Einstellung: Hilfetext'}))
            .getByRole('radio', {name: theirsLabel})).toBeChecked();
        await user.click(screen.getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({street: 'mine', required: 'mine', hint: 'theirs'});
    });

    it.each([
        {bulkAction: 'Gespeicherten Stand für alle übernehmen', singleLabel: mineLabel, expected: {street: 'mine', required: 'theirs', hint: 'theirs'}},
        {bulkAction: 'Meine Änderungen für alle behalten', singleLabel: theirsLabel, expected: {street: 'theirs', required: 'mine', hint: 'mine'}},
    ])('applies "$bulkAction" to hidden conflicts and allows a later individual choice', async ({bulkAction, singleLabel, expected}) => {
        const props = setup({conflicts: groupedConflicts});
        const user = userEvent.setup();
        if (bulkAction === 'Meine Änderungen für alle behalten') {
            await user.click(screen.getByRole('button', {name: 'Gespeicherten Stand für alle übernehmen'}));
        }
        await user.type(screen.getByRole('searchbox', {name: searchLabel}), 'Platzhalter');
        await user.tab();
        const setting = await screen.findByRole('group', {name: 'Abweichende Einstellung: Platzhalter'});
        await waitFor(() => expect(screen.queryByRole('group', {name: 'Abweichende Einstellung: Pflichtfeld'}))
            .not.toBeInTheDocument());
        await user.click(screen.getByRole('button', {name: bulkAction}));
        await user.click(within(setting).getByRole('radio', {name: singleLabel}));
        await user.type(screen.getByRole('searchbox', {name: searchLabel}), ' ohne Treffer');
        await waitFor(() => expect(screen.queryByRole('radio')).not.toBeInTheDocument());
        await user.click(screen.getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith(expected);
        expect(props.onDiscard).not.toHaveBeenCalled();
    });

    it('omits bulk actions for a single conflict', () => {
        setup({conflicts: [groupedConflicts[0]]});

        expect(screen.queryByRole('button', {name: 'Meine Änderungen für alle behalten'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Gespeicherten Stand für alle übernehmen'})).not.toBeInTheDocument();
    });

    it.each([
        {value: undefined, expected: 'Entfernt'},
        {value: null, expected: 'Leer'},
        {value: '', expected: 'Leer'},
        {value: true, expected: 'Ja'},
        {value: false, expected: 'Nein'},
        {value: 0, expected: '0'},
        {value: {type: InputMode.Literal, value: 'Vorgegebener Text'}, expected: 'Vorgegebener Text'},
        {value: {type: InputMode.Literal, value: false}, expected: 'Nein'},
    ])('describes the authored value as "$expected"', ({value, expected}) => {
        setup({conflicts: [{id: 'value', label: 'Wert', mine: value, theirs: 'Gespeichert'}]});

        expect(screen.getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription(expected);
    });

    it('shows markup as text and lets people inspect and search the full value', async () => {
        const longValue = `${'Langer Inhalt '.repeat(40)}<script>alert("Sicher als Text")</script>`;
        const props = setup({conflicts: [{id: 'value', label: 'Beschreibung', mine: longValue, theirs: 'Gespeichert'}]});
        const user = userEvent.setup();
        const mine = screen.getByRole('radio', {name: mineLabel});
        expect(mine).not.toHaveAccessibleDescription(longValue);
        await user.click(screen.getByRole('button', {name: 'Vollständigen Wert anzeigen'}));

        expect(mine).toHaveAccessibleDescription(longValue);
        expect(screen.getByText(longValue)).toBeVisible();
        expect(screen.getByRole('dialog').querySelector('script')).toBeNull();
        await user.click(screen.getByRole('button', {name: 'Wert kürzen'}));
        expect(mine).not.toHaveAccessibleDescription(longValue);
        await user.type(screen.getByRole('searchbox', {name: searchLabel}), 'Sicher als Text');
        await user.tab();
        expect(await screen.findByRole('group', {name: 'Abweichende Einstellung: Beschreibung'})).toBeVisible();
        await user.click(screen.getByRole('button', {name: 'Änderungen wiederherstellen'}));

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({value: 'mine'});
    });

    it('describes dynamic input sources, scripts and expressions as inert text', () => {
        const group = {id: 'dynamic', label: 'Dynamische Angaben'};
        setup({conflicts: [
            {
                id: 'variable', label: 'Variable', group,
                mine: {type: InputMode.Variable, reference: {source: InputVariableSource.ElementData, nodeDataKey: 'person', path: 'name'}},
                theirs: {type: InputMode.Literal, value: 'Gespeicherter Name'},
            },
            {
                id: 'script', label: 'Skript', group,
                mine: {type: InputMode.LowCode, code: 'return "<b>Name</b>";'},
                theirs: {type: InputMode.Literal, value: 'Gespeicherter Text'},
            },
            {
                id: 'expression', label: 'Ausdruck', group,
                mine: {type: InputMode.NoCode, operand: {type: 'NoCodeStaticValue', value: '<b>Vorlage</b>'}},
                theirs: {type: InputMode.Literal, value: 'Gespeicherte Vorlage'},
            },
        ]});
        const variable = screen.getByRole('group', {name: 'Abweichende Einstellung: Variable'});
        expect(within(variable).getByRole('radio', {name: mineLabel}))
            .toHaveAccessibleDescription(/Variable: _\.person\.name/);
        expect(within(variable).getByRole('radio', {name: theirsLabel}))
            .toHaveAccessibleDescription('Gespeicherter Name');
        const script = screen.getByRole('group', {name: 'Abweichende Einstellung: Skript'});
        expect(within(script).getByRole('radio', {name: mineLabel}))
            .toHaveAccessibleDescription(/Low-Code-Skript:[\s\S]*return "<b>Name<\/b>";/);
        expect(within(script).getByText(/return "<b>Name<\/b>";/)).toBeVisible();
        const expression = screen.getByRole('group', {name: 'Abweichende Einstellung: Ausdruck'});
        expect(within(expression).getByRole('radio', {name: mineLabel}))
            .toHaveAccessibleDescription(/No-Code-Ausdruck:[\s\S]*"type": "NoCodeStaticValue"[\s\S]*"value": "<b>Vorlage<\/b>"/);
        expect(within(expression).getByText(/"value": "<b>Vorlage<\/b>"/)).toBeVisible();
        expect(screen.getByRole('dialog').querySelector('b')).toBeNull();
    });

    it.each([
        {
            label: 'Variable',
            reference: (nodeDataKey: string, path: string) => ({
                type: InputMode.Variable, reference: {source: InputVariableSource.ElementData, nodeDataKey, path},
            }),
        },
        {
            label: 'No-Code-Verweis',
            reference: (nodeDataKey: string, path: string) => ({type: 'NoCodeNodeDataReference', nodeDataKey, path}),
        },
    ])('shows both complete $label paths even when they exceed the normal preview length', ({label, reference}) => {
        const key = 'verantwortlichePerson'.repeat(5);
        setup({conflicts: [{id: 'selection', label: 'Auswahl', mine: reference(key, '[0].email'), theirs: reference(key, '[0].name')}]});
        const mine = `${label}: _.${key}[0].email`;
        const theirs = `${label}: _.${key}[0].name`;

        expect(screen.getByText(mine)).toBeVisible();
        expect(screen.getByText(theirs)).toBeVisible();
        expect(screen.getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription(mine);
        expect(screen.getByRole('radio', {name: theirsLabel})).toHaveAccessibleDescription(theirs);
    });

    it('distinguishes identical displayed paths by their data layer and Markdown flag', () => {
        const group = {id: 'dynamic', label: 'Dynamische Angaben'};
        const metadata = {type: InputMode.Variable, reference: {source: InputVariableSource.ElementMetadata, nodeDataKey: 'pruefen', path: 'finished'}};
        const protectedData = {type: InputMode.Variable, reference: {source: InputVariableSource.ProtectedProcessData, path: 'taskMetadata.pruefen.finished'}};
        const markdown = {...metadata, reference: {...metadata.reference, markdown: true}};
        setup({conflicts: [
            {id: 'source', label: 'Quelle', group, mine: metadata, theirs: protectedData},
            {id: 'format', label: 'Format', group, mine: markdown, theirs: metadata},
        ]});
        const path = 'Variable: $$.taskMetadata.pruefen.finished';
        const source = within(screen.getByRole('group', {name: /Einstellung: Quelle/}));
        expect(source.getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription(`${path}\nElementmetadatenebene`);
        expect(source.getByRole('radio', {name: theirsLabel})).toHaveAccessibleDescription(`${path}\nGeschützte Vorgangsdatenebene`);
        const format = within(screen.getByRole('group', {name: /Einstellung: Format/}));
        expect(format.getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription(`${path}\nMarkdown-Quelle: Ja`);
        expect(format.getByRole('radio', {name: theirsLabel})).toHaveAccessibleDescription(`${path}\nMarkdown-Quelle: Nein`);
    });

    it.each([
        {value: {type: 'NoCodeNodeDataReference', path: 'email'}, expected: 'No-Code-Verweis (unvollständig)\nDatenschlüssel: fehlt\nPfad: email'},
        {
            value: {type: InputMode.Variable, reference: {source: InputVariableSource.ElementData, nodeDataKey: 'pruefen', path: null}},
            expected: 'Variable (unvollständig)\nDatenschlüssel: pruefen\nPfad: fehlt',
        },
        {
            value: {type: InputMode.Variable, reference: {source: 'Unknown', path: 'name'}},
            expected: 'Variable (unvollständig)\nUnbekannte Datenebene (Unknown)\nPfad: name',
        },
        {value: {type: InputMode.Variable, reference: null}, expected: 'Keine Variable referenziert'},
        {
            value: {type: InputMode.Literal, value: {type: InputMode.Variable, reference: {source: InputVariableSource.ProcessData, path: 'name'}}},
            expected: 'Variable: $.name',
        },
        {
            value: {type: InputMode.Literal, value: {path: 'email', nodeDataKey: 'pruefen'}},
            expected: /\{\s*"path": "email",\s*"nodeDataKey": "pruefen"\s*\}/,
        },
    ])('safely describes an incomplete reference or literal as "$expected"', ({value, expected}) => {
        setup({conflicts: [{id: 'selection', label: 'Auswahl', mine: value, theirs: null}]});
        expect(screen.getByRole('radio', {name: mineLabel})).toHaveAccessibleDescription(expected);
    });

    it('changes choices with the keyboard and expands a value without changing the chosen side', async () => {
        const longValue = 'Vollständig lesbarer Inhalt '.repeat(8).trim();
        const props = setup({conflicts: [{id: 'value', label: 'Beschreibung', mine: longValue, theirs: 'Gespeichert'}]});
        const user = userEvent.setup();
        const mine = screen.getByRole('radio', {name: mineLabel});
        const theirs = screen.getByRole('radio', {name: theirsLabel});
        for (let attempts = 0; document.activeElement !== mine && attempts < 12; attempts += 1) {
            await user.tab();
        }
        expect(mine).toHaveFocus();
        await user.keyboard('{ArrowRight}');
        expect(theirs).toHaveFocus();
        expect(theirs).toBeChecked();
        await user.tab({shift: true});
        expect(screen.getByRole('button', {name: 'Vollständigen Wert anzeigen'})).toHaveFocus();
        await user.keyboard('{Enter}');

        expect(mine).toHaveAccessibleDescription(longValue);
        expect(theirs).toBeChecked();
        await user.tab();
        await user.tab();
        expect(screen.getByRole('button', {name: 'Änderungen wiederherstellen'})).toHaveFocus();
        await user.keyboard('{Enter}');

        expect(props.onApply).toHaveBeenCalledExactlyOnceWith({value: 'theirs'});
    });

    it.each(['restore', 'save'] as const)('discards changes only after the explicit %s discard action', async (mode) => {
        const props = setup({mode});

        await userEvent.setup().click(screen.getByRole('button', {name: 'Änderungen verwerfen'}));

        expect(props.onDiscard).toHaveBeenCalledOnce();
        expect(props.onApply).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it.each(['Escape', 'background'] as const)('keeps the recovery decision pending on %s', async (trigger) => {
        const props = setup();
        const dialog = screen.getByRole('dialog', {name: restoreTitle});
        expect(within(dialog).queryByRole('button', {name: 'Schließen'})).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('button', {name: 'Abbrechen'})).not.toBeInTheDocument();
        const user = userEvent.setup();

        if (trigger === 'Escape') {
            await user.keyboard('{Escape}');
        } else {
            await user.click(dialog.parentElement!);
        }

        expect(screen.getByRole('dialog', {name: restoreTitle})).toBe(dialog);
        expect(props.onApply).not.toHaveBeenCalled();
        expect(props.onDiscard).not.toHaveBeenCalled();
        expect(props.onCancel).not.toHaveBeenCalled();
    });

    it.each(['Abbrechen', 'Schließen', 'Escape', 'background'] as const)(
        'cancels a save conflict through %s without applying or discarding changes',
        async (trigger) => {
            const props = setup({mode: 'save', storedStateChanged: true});
            const dialog = screen.getByRole('dialog', {name: 'Neuerer Stand gespeichert'});
            const apply = within(dialog).getByRole('button', {name: 'Übernehmen und speichern'});
            expect(within(apply.parentElement!).getAllByRole('button').map((button) => button.textContent))
                .toEqual(['Übernehmen und speichern', 'Änderungen verwerfen', 'Abbrechen']);
            const user = userEvent.setup();

            if (trigger === 'Escape') {
                await user.keyboard('{Escape}');
            } else if (trigger === 'background') {
                await user.click(dialog.parentElement!);
            } else {
                await user.click(within(dialog).getByRole('button', {name: trigger}));
            }

            expect(props.onCancel).toHaveBeenCalledOnce();
            expect(props.onApply).not.toHaveBeenCalled();
            expect(props.onDiscard).not.toHaveBeenCalled();
        },
    );
});
