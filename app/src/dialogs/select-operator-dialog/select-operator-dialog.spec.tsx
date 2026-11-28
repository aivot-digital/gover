import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {SelectOperatorDialog} from './select-operator-dialog';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';

const operator: NoCodeOperatorDetailsDTO = {
    identifier: 'equals',
    label: 'Ist gleich',
    packageName: 'core',
    tags: [],
    humanReadableTemplate: null,
    abstractDescription: 'Vergleicht zwei Werte.',
    description: '# Beschreibung\n\nVergleicht **zwei Werte**.\n\n## Beispiel\n\n`a = b`',
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [],
    }],
};
const textOperator = {
    ...operator,
    identifier: 'join',
    label: 'Text verketten',
    signatures: [{
        returnType: NoCodeDataType.String,
        parameters: [],
    }],
};

function setup(overrides: Partial<Parameters<typeof SelectOperatorDialog>[0]> = {}) {
    const props = {
        open: true,
        operators: [operator, textOperator],
        desiredReturnType: NoCodeDataType.Boolean,
        onSelect: vi.fn(),
        onClose: vi.fn(), ...overrides,
    };
    return {props, ...render(<SelectOperatorDialog {...props}/>)};
}

describe('operator selection', () => {
    it('starts another function at the top without changing search, list position or focus', async () => {
        const user = userEvent.setup();
        const otherOperator = {...operator, identifier: 'not-equals', label: 'Ist ungleich'};
        const {props, rerender} = setup({operators: [operator, otherOperator]});
        const search = screen.getByRole('searchbox', {name: 'Funktion suchen'});
        await user.type(search, 'Werte');
        const firstRow = await screen.findByRole('listitem', {name: 'Ist gleich'});
        const listBody = firstRow.parentElement!.parentElement!;
        const firstDetails = within(firstRow).getByRole('button', {name: 'Ist gleich: Details anzeigen'});
        const secondDetails = within(screen.getByRole('listitem', {name: 'Ist ungleich'}))
            .getByRole('button', {name: 'Ist ungleich: Details anzeigen'});
        listBody.scrollTop = 160;
        await user.click(firstDetails);
        const detailsBody = () => screen.getByRole('button', {name: 'Details schließen'})
            .parentElement!.previousElementSibling as HTMLElement;
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        detailsBody().scrollTop = 320;

        await user.click(firstDetails);

        expect(detailsBody().scrollTop).toBe(320);
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'true');

        rerender(<SelectOperatorDialog {...props} operators={props.operators.map(value => ({...value}))}/>);
        expect(detailsBody().scrollTop).toBe(320);

        await user.click(secondDetails);

        expect(screen.getByRole('region', {name: 'Details: Ist ungleich'})).toBeVisible();
        expect(detailsBody().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
        expect(secondDetails).toHaveFocus();
        expect(firstDetails).toHaveAttribute('aria-expanded', 'false');
        expect(search).toHaveValue('Werte');
        expect(listBody.scrollTop).toBe(160);
        detailsBody().scrollTop = 180;

        await user.click(firstDetails);

        expect(detailsBody().scrollTop).toBe(0);
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
        await user.click(screen.getByRole('button', {name: 'Details schließen'}));
        expect(firstDetails).toHaveFocus();
        await user.click(firstDetails);
        expect(detailsBody().scrollTop).toBe(0);
        expect(props.onSelect).not.toHaveBeenCalled();
    });

    it('returns focus to search if filtering removes the button that opened the details', async () => {
        const {props} = setup();
        fireEvent.click(screen.getByRole('button', {name: 'Ist gleich: Details anzeigen'}));
        const search = screen.getByRole('searchbox', {name: 'Funktion suchen'});
        fireEvent.change(search, {target: {value: 'does-not-exist'}});
        await screen.findByText('Keine Funktionen gefunden, die zu Ihrem Suchbegriff passen.');

        fireEvent.click(screen.getByRole('button', {name: 'Details schließen'}));

        expect(search).toHaveFocus();
        expect(search).toHaveValue('does-not-exist');
        expect(props.onSelect).not.toHaveBeenCalled();
    });

    it('only adds on explicit action and shows details with subordinate Markdown headings', async () => {
        const user = userEvent.setup();
        const {props} = setup();
        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
        expect(screen.getByRole('dialog', {name: 'Funktion auswählen'})).toBeVisible();
        await waitFor(() => expect(screen.getByRole('searchbox', {name: 'Funktion suchen'})).toHaveFocus());
        const row = screen.getByRole('listitem', {name: 'Ist gleich'});
        await user.click(within(row).getByText('Ist gleich'));
        expect(props.onSelect).not.toHaveBeenCalled();
        await user.click(within(row).getByRole('button', {name: 'Ist gleich: Details anzeigen'}));
        const detailsButton = within(row).getByRole('button', {name: 'Ist gleich: Details anzeigen'});
        expect(detailsButton).toHaveTextContent(/^Details$/);
        expect(detailsButton).toHaveAttribute('aria-expanded', 'true');
        await user.click(detailsButton);
        const details = screen.getByRole('region', {name: 'Details: Ist gleich'});
        expect(within(details).getByRole('heading', {
            name: 'Ist gleich',
            level: 3,
        })).toBeVisible();
        expect(within(details).getByRole('heading', {
            name: 'Beschreibung',
            level: 4,
        })).toBeVisible();
        expect(within(details).getByRole('heading', {
            name: 'Beispiel',
            level: 5,
        })).toBeVisible();
        expect(within(details).getByText('zwei Werte').tagName).toBe('STRONG');
        expect(within(details).getByText('a = b').tagName).toBe('CODE');
        await user.click(within(details).getByRole('button', {name: 'Details schließen'}));
        expect(screen.queryByRole('region')).not.toBeInTheDocument();
        expect(within(row).getByRole('button', {name: 'Ist gleich: Details anzeigen'})).toHaveFocus();
        expect(props.onSelect).not.toHaveBeenCalled();
        await user.click(within(row).getByRole('button', {name: 'Ist gleich: Hinzufügen'}));
        expect(props.onSelect).toHaveBeenCalledExactlyOnceWith(operator);
    });

    it('replaces from the details and keeps closing the dialog separate from selection', () => {
        const {props} = setup({mode: 'replace'});
        expect(screen.getByRole('button', {name: 'Ist gleich: Ersetzen'})).toBeVisible();
        fireEvent.click(screen.getByRole('button', {name: 'Ist gleich: Details anzeigen'}));
        const details = screen.getByRole('region', {name: 'Details: Ist gleich'});
        fireEvent.click(within(details).getByRole('button', {name: 'Ersetzen'}));
        expect(props.onSelect).toHaveBeenCalledExactlyOnceWith(operator);
        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});
        expect(props.onClose).toHaveBeenCalledOnce();
        expect(props.onSelect).toHaveBeenCalledOnce();
    });

    it('retains type filtering and searches by identifier without selecting', async () => {
        const {props} = setup();
        expect(screen.queryByRole('listitem', {name: 'Text verketten'})).not.toBeInTheDocument();
        fireEvent.change(screen.getByRole('searchbox'), {target: {value: 'does-not-exist'}});
        await screen.findByText('Keine Funktionen gefunden, die zu Ihrem Suchbegriff passen.');
        fireEvent.change(screen.getByRole('searchbox'), {target: {value: 'equals'}});
        expect(await screen.findByRole('listitem', {name: 'Ist gleich'})).toBeVisible();
        expect(props.onSelect).not.toHaveBeenCalled();
    });

    it('resets the opt-out filter and search after closing and retains the description during the exit transition', async () => {
        const {
            props,
            rerender,
        } = setup({operators: [textOperator]});
        fireEvent.click(screen.getByRole('button', {name: 'Auch unpassende Funktionen anzeigen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Text verketten: Details anzeigen'}));
        rerender(<SelectOperatorDialog {...props} open={false}
                                       operators={[]}/>);
        expect(screen.getByRole('region', {
            name: 'Details: Text verketten',
            hidden: true,
        })).toHaveTextContent('Beschreibung');
        await waitFor(() => expect(screen.queryByRole('dialog', {hidden: true})).not.toBeInTheDocument());
        rerender(<SelectOperatorDialog {...props}/>);
        expect(await screen.findByRole('button', {name: 'Auch unpassende Funktionen anzeigen'})).toBeVisible();
        expect(screen.queryByRole('listitem', {name: 'Text verketten'})).not.toBeInTheDocument();
    });
});
