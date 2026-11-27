import {useState} from 'react';
import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {DragAndDropProvider} from '../../providers/drag-and-drop-provider';
import {ReorderDialog} from './reorder-dialog';

describe('ReorderDialog', () => {
    it('moves dragged items from their current positions and saves the shown order', async () => {
        const user = userEvent.setup();
        const onReorder = vi.fn();
        render(<ReorderScene onReorder={onReorder}/>, {wrapper: DragAndDropProvider});
        await user.click(screen.getByRole('button', {name: 'Sortieren'}));

        drag('C', ['B', 'A']);
        expect(getShownOrder()).toEqual(['C', 'A', 'B']);
        // A later drag must start from the position the item was moved to.
        drag('A', ['C']);
        expect(getShownOrder()).toEqual(['A', 'C', 'B']);

        await user.click(screen.getByRole('button', {name: 'Reihenfolge speichern'}));
        expect(onReorder).toHaveBeenCalledWith(['A', 'C', 'B']);
    });

    it('discards an unsaved order when the dialog is reopened', async () => {
        const user = userEvent.setup();
        const onReorder = vi.fn();
        render(<ReorderScene onReorder={onReorder}/>, {wrapper: DragAndDropProvider});
        await user.click(screen.getByRole('button', {name: 'Sortieren'}));
        drag('C', ['A']);
        expect(getShownOrder()).toEqual(['C', 'A', 'B']);

        await user.click(screen.getByRole('button', {name: 'Abbrechen'}));
        await user.click(screen.getByRole('button', {name: 'Sortieren'}));

        expect(getShownOrder()).toEqual(['A', 'B', 'C']);
        await user.click(screen.getByRole('button', {name: 'Reihenfolge speichern'}));
        expect(onReorder).toHaveBeenCalledWith(['A', 'B', 'C']);
    });
});

function ReorderScene({onReorder}: {onReorder: (items: string[]) => void}) {
    const [open, setOpen] = useState(false);
    const [items] = useState(['A', 'B', 'C']);

    return <>
        <button onClick={() => setOpen(true)}>Sortieren</button>
        <ReorderDialog
            title="Testreihenfolge"
            open={open}
            transitionDuration={0}
            items={items}
            getLabel={item => ({primary: item})}
            onClose={() => setOpen(false)}
            onReorder={(reorderedItems) => {
                onReorder(reorderedItems);
                setOpen(false);
            }}
        />
    </>;
}

function getShownItems(): HTMLElement[] {
    return within(screen.getByRole('dialog', {name: 'Testreihenfolge'})).getAllByRole('listitem');
}

function getShownOrder(): Array<string | null> {
    return getShownItems().map(item => item.textContent);
}

function getShownItem(label: string): HTMLElement {
    const item = getShownItems().find(listItem => listItem.textContent === label);
    expect(item).toBeDefined();
    return item!;
}

// The dialog reorders on hover. jsdom reports empty layout boxes, so every hover passes
// the half-height threshold and moves the dragged item to the hovered position.
function drag(label: string, hoveredLabels: string[]) {
    const event = {dataTransfer: {types: [], setData: vi.fn()}};
    const source = getShownItem(label);
    fireEvent.dragStart(source, event);
    for (const hoveredLabel of hoveredLabels) {
        fireEvent.dragEnter(getShownItem(hoveredLabel), event);
    }
    fireEvent.dragEnd(source, event);
}
