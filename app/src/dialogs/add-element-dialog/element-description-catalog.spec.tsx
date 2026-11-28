import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {getElementNameForType} from '../../data/element-type/element-names';
import {getElementDescriptionForType, getElementGroupForType} from './element-dialog-metadata';
import {ElementInfoTab} from './tabs/element-info-tab';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

const sectionTitles = [
    'Wofür verwenden Sie dieses Element?',
    'Welche Einstellungen sind wichtig?',
    'Wie sieht das Element aus?',
    'Was ist zu beachten?',
    'Technische Informationen',
];

const supportedElements = Object.values(ElementType)
    .filter((type): type is ElementType => typeof type === 'number'
        && getElementGroupForType(type) != null
        && type !== ElementType.SubmittedStep)
    .map(type => ({type, name: getElementNameForType(type)}));

function renderDescription(type: ElementType) {
    return render(<ElementInfoTab type={type} onAddElement={vi.fn()}
                                  primaryActionLabel="Hinzufügen" primaryActionIcon={null} onClose={vi.fn()}/>);
}

describe('supported element description catalog', () => {
    it.each(supportedElements)('renders the complete description and previews for $name ($type)', ({type, name}) => {
        renderDescription(type);

        const summary = getElementDescriptionForType(type);
        expect(summary.trim()).not.toBe('');
        expect(summary).not.toBe('Dieses Element steht für den aktuellen Bereich zur Verfügung.');
        expect(screen.getAllByText(summary, {exact: true}).length).toBeGreaterThan(0);
        expect(screen.getByRole('heading', {name, level: 3})).toBeVisible();
        expect(screen.getByRole('heading', {name: sectionTitles[0], level: 4})).toBeVisible();

        const sectionHeadings = screen.getAllByRole('heading', {level: 4})
            .filter(heading => sectionTitles.includes(heading.textContent ?? ''));
        const renderedTitles = sectionHeadings.map(heading => heading.textContent ?? '');
        expect(new Set(renderedTitles).size).toBe(renderedTitles.length);
        expect(renderedTitles).toEqual(sectionTitles.filter(title => renderedTitles.includes(title)));

        for (const heading of sectionHeadings) {
            if (heading.textContent === 'Technische Informationen') continue;
            const content = heading.nextElementSibling ?? heading.parentElement?.nextElementSibling;
            expect(content).not.toBeNull();
            expect(content).not.toBeEmptyDOMElement();
        }

        const technicalButton = screen.queryByRole('button', {name: 'Technische Informationen'});
        if (technicalButton != null) {
            expect(technicalButton).toHaveAttribute('aria-expanded', 'false');
            expect(technicalButton.getAttribute('aria-controls')).toBeTruthy();
        }
        expect(screen.queryByRole('region', {name: 'Technische Informationen'})).not.toBeInTheDocument();
    });

    it.each([
        {type: ElementType.Date, name: 'Datum', dataType: 'Date'},
        {type: ElementType.Table, name: 'Tabelle', dataType: 'List'},
        {type: ElementType.ProcessDataKeyInput, name: 'Datenschlüssel', dataType: 'String'},
    ])('makes technical information accessible from the keyboard for $name', async ({type, dataType}) => {
        const user = userEvent.setup();
        renderDescription(type);

        const technicalButton = screen.getByRole('button', {name: 'Technische Informationen'});
        technicalButton.focus();
        await user.keyboard('{Enter}');

        expect(technicalButton).toHaveAttribute('aria-expanded', 'true');
        const technicalSection = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(technicalSection).toBeVisible();
        expect(technicalSection.id).toBe(technicalButton.getAttribute('aria-controls'));
        expect(within(technicalSection).getByText(dataType, {exact: true}).tagName).toBe('CODE');
    });
});
