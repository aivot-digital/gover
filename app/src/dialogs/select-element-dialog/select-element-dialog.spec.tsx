import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {SelectElementDialog} from './select-element-dialog';
import {ElementType} from '../../data/element-type/element-type';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {AnyElement} from '../../models/elements/any-element';

vi.mock('../../components/view-dispatcher/view-dispatcher.component', () => ({
    ViewDispatcherComponent: ({element}: { element: AnyElement }) => <div>Vorschau für {element.id}</div>,
}));

const name = {
    ...generateElementWithDefaultValues(ElementType.Text),
    id: 'first-name',
    label: 'Vorname',
};
const amount = {
    ...generateElementWithDefaultValues(ElementType.Number),
    id: 'amount',
    label: 'Betrag',
};
const step = {
    ...generateElementWithDefaultValues(ElementType.Step),
    id: 'personal',
    title: 'Persönliche Angaben',
};
const options = [{
    element: name,
    parents: [step],
}, {
    element: amount,
    parents: [step],
}];

describe('element selection', () => {
    it('shows origin and ID, previews without applying, and uses an explicit reference action', () => {
        const onSelect = vi.fn();
        render(<SelectElementDialog open
                                    allElements={options}
                                    onSelect={onSelect}
                                    onClose={() => undefined}/>);
        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
        const row = screen.getByRole('listitem', {name: 'Vorname'});
        expect(within(row).getByText('Persönliche Angaben')).toBeVisible();
        expect(within(row).getByText('ID: first-name')).toBeVisible();
        fireEvent.click(within(row).getByText('Vorname'));
        expect(onSelect).not.toHaveBeenCalled();
        fireEvent.click(within(row).getByRole('button', {name: 'Vorname: Details anzeigen'}));
        const details = screen.getByRole('region', {name: 'Details: Vorname'});
        expect(within(details).getByText('Vorschau für first-name')).toBeVisible();
        expect(onSelect).not.toHaveBeenCalled();
        fireEvent.click(within(details).getByRole('button', {name: 'Referenz verwenden'}));
        expect(onSelect).toHaveBeenCalledExactlyOnceWith(name);
    });

    it('preserves type filtering and searches technical IDs and parent paths', async () => {
        render(<SelectElementDialog open
                                    allElements={options}
                                    desiredType={NoCodeDataType.String}
                                    onSelect={() => undefined}
                                    onClose={() => undefined}/>);
        expect(screen.queryByRole('listitem', {name: 'Betrag'})).not.toBeInTheDocument();
        for (const query of ['first-name', 'personal', 'Persönliche Angaben']) {
            fireEvent.change(screen.getByRole('searchbox'), {target: {value: 'does-not-exist'}});
            await screen.findByText('Keine Elemente gefunden, die zu Ihrem Suchbegriff passen.');
            fireEvent.change(screen.getByRole('searchbox'), {target: {value: query}});
            expect(await screen.findByRole('listitem', {name: 'Vorname'})).toBeVisible();
        }
    });

    it('labels the Low-Code lookup action and cancels without using an element', () => {
        const onSelect = vi.fn();
        const onClose = vi.fn();
        render(<SelectElementDialog open
                                    allElements={options}
                                    primaryActionLabel="Element-ID verwenden"
                                    onSelect={onSelect}
                                    onClose={onClose}/>);
        fireEvent.click(screen.getByRole('button', {name: 'Vorname: Details anzeigen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Vorname: Details anzeigen'}));
        expect(screen.getByRole('region', {name: 'Details: Vorname'})).toBeVisible();
        fireEvent.click(screen.getByRole('button', {name: 'Details schließen'}));
        expect(screen.queryByRole('region', {name: 'Details: Vorname'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Vorname: Details anzeigen'})).toHaveFocus();
        expect(onSelect).not.toHaveBeenCalled();
        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});
        expect(onClose).toHaveBeenCalledOnce();
        expect(onSelect).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Vorname: Element-ID verwenden'}));
        expect(onSelect).toHaveBeenCalledExactlyOnceWith(name);
    });
});
