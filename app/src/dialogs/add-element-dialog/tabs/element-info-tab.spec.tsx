import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementInfoTab} from './element-info-tab';
import {ElementType} from '../../../data/element-type/element-type';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

describe('ElementInfoTab description pilots', () => {
    const sectionTitles = [
        'Wofür verwenden Sie dieses Element?',
        'Welche Einstellungen sind wichtig?',
        'Wie sieht das Element aus?',
        'Was ist zu beachten?',
        'Technische Informationen',
    ];

    it.each([
        [ElementType.Text, 'Texteingabe'],
        [ElementType.Select, 'Einzelauswahl'],
        [ElementType.Number, 'Zahl'],
        [ElementType.PhoneNumber, 'Telefonnummer'],
        [ElementType.Email, 'E-Mail-Adresse'],
    ] as const)('shows ordered sections below the element title for %s', (type, title) => {
        render(<ElementInfoTab type={type} onAddElement={vi.fn()}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);

        expect(screen.getByRole('heading', {name: title, level: 3})).toBeVisible();
        expect(screen.getAllByRole('heading', {level: 4}).map((heading) => heading.textContent))
            .toEqual(sectionTitles);
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();
    });

    it('keeps text examples and explains text values in a keyboard-accessible technical section', async () => {
        const user = userEvent.setup();
        render(<ElementInfoTab type={ElementType.Text} onAddElement={vi.fn()}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);

        expect(screen.getAllByRole('textbox')).toHaveLength(2);
        expect(screen.getByRole('textbox', {name: /^Aktenzeichen/})).toHaveValue('00123');
        expect(screen.getByRole('textbox', {name: /^Aktenzeichen/})).toHaveAttribute('maxlength', '20');
        expect(screen.getByRole('textbox', {name: /^Aktenzeichen/}))
            .toHaveAccessibleDescription(/Höchstens 20 Zeichen\./);
        expect(screen.getByRole('textbox', {name: /^Beschreibung des Anliegens/}).tagName).toBe('TEXTAREA');
        expect(screen.getByText(/^„Mehrzeilige Texteingabe“/).tagName).toBe('STRONG');
        expect(screen.queryByText('String', {exact: true})).not.toBeInTheDocument();

        const technicalButton = screen.getByRole('button', {name: 'Technische Informationen'});
        technicalButton.focus();
        await user.keyboard('{Enter}');

        expect(technicalButton).toHaveAttribute('aria-expanded', 'true');
        const technicalSection = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(technicalSection).toBeVisible();
        expect(technicalSection.id).toBe(technicalButton.getAttribute('aria-controls'));
        expect(technicalSection).toHaveTextContent('Textwert');
        expect(within(technicalSection).getByText('String', {exact: true}).tagName).toBe('CODE');
        expect(within(technicalSection).getAllByText('00123', {exact: true})
            .every((value) => value.tagName === 'CODE')).toBe(true);

        await user.keyboard(' ');
        expect(technicalButton).toHaveAttribute('aria-expanded', 'false');
        await waitFor(() => expect(screen.queryByText('String', {exact: true})).not.toBeInTheDocument());
    });

    it('keeps the single-choice variants and distinguishes labels from stored text values', async () => {
        const user = userEvent.setup();
        render(<ElementInfoTab type={ElementType.Select} onAddElement={vi.fn()}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);

        expect(screen.getByRole('radio', {name: 'E-Mail'})).toBeChecked();
        expect(screen.getByRole('radio', {name: 'Post'})).not.toBeChecked();
        expect(screen.getAllByRole('combobox')).toHaveLength(2);
        expect(screen.getByRole('combobox', {name: /^Veranstaltungsart/})).toBeVisible();

        const searchableChoice = screen.getByRole('combobox', {name: /^Durchsuchbare Einzelauswahl/});
        await user.click(searchableChoice);
        await user.type(searchableChoice, 'Gewerbe');
        expect(screen.getByRole('option', {name: 'Gewerbeanmeldung'})).toBeVisible();
        expect(screen.queryByRole('option', {name: 'Baugenehmigung'})).not.toBeInTheDocument();
        await user.keyboard('{Escape}');

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        const technicalSection = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(technicalSection).toHaveTextContent('Textwert');
        expect(technicalSection).toHaveTextContent('E-Mail');
        expect(within(technicalSection).getByText('String', {exact: true}).tagName).toBe('CODE');
        expect(within(technicalSection).getAllByText('email', {exact: true})
            .every((value) => value.tagName === 'CODE')).toBe(true);
    });

    it('shows numeric examples with separate units and explains the numeric value', async () => {
        const user = userEvent.setup();
        render(<ElementInfoTab type={ElementType.Number} onAddElement={vi.fn()}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);

        expect(screen.getAllByRole('textbox')).toHaveLength(2);
        expect(screen.getByRole('textbox', {name: /^Gewicht/})).toHaveValue('42,31');
        expect(screen.getByText('kg', {exact: true, selector: 'p'})).toBeVisible();
        expect(screen.getByRole('textbox', {name: /^Anzahl der Teilnehmenden/})).toHaveValue('19');
        expect(screen.getByText('Personen', {exact: true})).toBeVisible();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        const technicalSection = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(technicalSection).toBeVisible();
        expect(within(technicalSection).getByText('Number', {exact: true}).tagName).toBe('CODE');
        expect(within(technicalSection).getAllByText('42.31', {exact: true})
            .every((value) => value.tagName === 'CODE')).toBe(true);
    });

    it('shows phone examples with country selection and explains the stored international text value', async () => {
        const user = userEvent.setup();
        render(<ElementInfoTab type={ElementType.PhoneNumber} onAddElement={vi.fn()}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);

        expect(screen.getAllByRole('textbox')).toHaveLength(2);
        expect(screen.getByRole('textbox', {name: /^Telefonnummer für Rückfragen/})).toHaveValue('30123456');
        expect(screen.getByRole('textbox', {name: /^Telefonnummer in Österreich/})).toHaveValue('6641234567');
        expect(screen.getByRole('button', {name: /Germany, Open flags menu/})).toBeVisible();
        expect(screen.getByRole('button', {name: /Austria, Open flags menu/})).toBeVisible();
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        const technicalSection = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(technicalSection).toHaveTextContent('Textwert');
        expect(within(technicalSection).getByText('String', {exact: true}).tagName).toBe('CODE');
        expect(within(technicalSection).getAllByText('+4930123456', {exact: true})
            .every((value) => value.tagName === 'CODE')).toBe(true);
    });

    it('shows email examples including a local domain and explains normalized text values', async () => {
        const user = userEvent.setup();
        render(<ElementInfoTab type={ElementType.Email} onAddElement={vi.fn()}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);

        expect(screen.getAllByRole('textbox')).toHaveLength(2);
        const notificationAddress = screen.getByRole('textbox', {name: /^E-Mail-Adresse für Benachrichtigungen/});
        expect(notificationAddress).toHaveValue('vorname.nachname@example.org');
        expect(notificationAddress).toHaveAttribute('inputmode', 'email');
        const internalAddress = screen.getByRole('textbox', {name: /^Interne E-Mail-Adresse/});
        expect(internalAddress).toHaveValue('poststelle@verwaltung');
        expect(internalAddress).toHaveAttribute('inputmode', 'email');
        await user.click(internalAddress);
        await user.tab();
        expect(internalAddress).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        const technicalSection = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(technicalSection).toHaveTextContent('Textwert');
        expect(within(technicalSection).getByText('String', {exact: true}).tagName).toBe('CODE');
        expect(within(technicalSection).getByText('kontakt@büro.de', {exact: true}).tagName).toBe('CODE');
        expect(within(technicalSection).getByText('kontakt@xn--bro-hoa.de', {exact: true}).tagName).toBe('CODE');
    });

    it.each([ElementType.Text, ElementType.Select, ElementType.Number, ElementType.PhoneNumber, ElementType.Email])('preserves adding and closing for %s', async (type) => {
        const user = userEvent.setup();
        const onAddElement = vi.fn();
        const onClose = vi.fn();
        render(<ElementInfoTab type={type} onAddElement={onAddElement}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={onClose}/>);

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        expect(onAddElement).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', {name: 'Hinzufügen'}));
        expect(onAddElement).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({type}));
        expect(onClose).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: 'Details schließen'}));
        expect(onClose).toHaveBeenCalledOnce();
    });

    it('starts technical details collapsed when switching to another element', async () => {
        const user = userEvent.setup();
        const props = {onAddElement: vi.fn(), primaryActionLabel: 'Hinzufügen', primaryActionIcon: null, onClose: vi.fn()};
        const {rerender} = render(<ElementInfoTab type={ElementType.Text} {...props}/>);
        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));
        expect(screen.getByRole('region', {name: 'Technische Informationen'})).toBeVisible();

        rerender(<ElementInfoTab type={ElementType.Select} {...props}/>);

        expect(screen.getByRole('heading', {name: 'Einzelauswahl', level: 3})).toBeVisible();
        expect(screen.getByRole('button', {name: 'Technische Informationen'}))
            .toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();
    });

    it('starts each element description at the top when switching elements and returning', () => {
        const props = {onAddElement: vi.fn(), primaryActionLabel: 'Hinzufügen', primaryActionIcon: null, onClose: vi.fn()};
        const {rerender} = render(<ElementInfoTab type={ElementType.Text} {...props}/>);
        const detailsBody = () => screen.getByRole('button', {name: 'Details schließen'})
            .parentElement!.previousElementSibling as HTMLElement;

        detailsBody().scrollTop = 320;
        expect(detailsBody().scrollTop).toBe(320);

        rerender(<ElementInfoTab type={ElementType.Select} {...props}/>);
        expect(screen.getByRole('heading', {name: 'Einzelauswahl', level: 3})).toBeVisible();
        expect(detailsBody().scrollTop).toBe(0);

        detailsBody().scrollTop = 180;
        rerender(<ElementInfoTab type={ElementType.Text} {...props}/>);
        expect(screen.getByRole('heading', {name: 'Texteingabe', level: 3})).toBeVisible();
        expect(detailsBody().scrollTop).toBe(0);
    });

    it('preserves the reading position when the same element description receives updated actions', () => {
        const props = {onAddElement: vi.fn(), primaryActionLabel: 'Hinzufügen', primaryActionIcon: null, onClose: vi.fn()};
        const {rerender} = render(<ElementInfoTab type={ElementType.Text} {...props}/>);
        const detailsBody = () => screen.getByRole('button', {name: 'Details schließen'})
            .parentElement!.previousElementSibling as HTMLElement;

        detailsBody().scrollTop = 240;
        rerender(<ElementInfoTab type={ElementType.Text} {...props} primaryActionLabel="Übernehmen"/>);

        expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeVisible();
        expect(detailsBody().scrollTop).toBe(240);
    });
});

describe('ElementInfoTab multiple text input', () => {
    it('describes the field and provides working examples for suggestions, counts and pasting', () => {
        const onAddElement = vi.fn();
        render(<ElementInfoTab type={ElementType.ChipInput} onAddElement={onAddElement}
                               primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);
        expect(screen.getByText('Mehrfach-Texteingabe', {exact: true})).toBeVisible();
        expect(screen.getByText('Erfasst mehrere kurze Texte in einem Feld, jeweils als eigenen Eintrag.')).toBeVisible();
        const topics = screen.getByRole('combobox', {name: 'Themen'});
        expect(topics).toHaveAccessibleDescription(/2 von 5 Einträgen verwendet/);
        fireEvent.change(topics, {target: {value: 'Bild'}});
        fireEvent.click(screen.getByRole('option', {name: 'Bildung'}));
        expect(topics).toHaveAccessibleDescription(/3 von 5 Einträgen verwendet/);

        const references = screen.getByRole('combobox', {name: /^Aktenzeichen/});
        fireEvent.paste(references, {clipboardData: {getData: () => 'AZ-2026-001\nAZ-2026-002'}});
        expect(screen.getByText('AZ-2026-001')).toBeVisible();
        expect(screen.getByText('AZ-2026-002')).toBeVisible();

        fireEvent.click(screen.getByRole('button', {name: 'Hinzufügen'}));
        expect(onAddElement).toHaveBeenCalledWith(expect.objectContaining({type: ElementType.ChipInput, label: 'Mehrfach-Texteingabe'}));
    });
});
