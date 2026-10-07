import React, {type PropsWithChildren, StrictMode, useState} from 'react';
import {act, fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Provider as StoreProvider} from 'react-redux';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {AppProvider} from '../../providers/app-provider';
import {useColorMode} from '../../providers/color-mode-context';
import {store} from '../../store.customer';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ReorderDialog} from '../../dialogs/reorder-dialog/reorder-dialog';
import {UiDefinitionInputFieldComponent} from '../ui-definition-input-field/ui-definition-input-field-component';
import {ElementTree} from './element-tree';

vi.mock('../../modules/process/pages/details/components/process-node-editor/process-node-editor-context', () => ({
    useOptionalProcessNodeEditorContext: () => null,
}));
vi.mock('./components/element-tree-editor', () => ({ElementTreeEditor: () => null}));
vi.mock('../code-editor/code-editor', () => ({CodeEditor: () => <div/>}));
vi.mock('allotment', () => ({
    Allotment: Object.assign(
        ({children}: PropsWithChildren) => <div>{children}</div>,
        {Pane: ({children}: PropsWithChildren) => <div>{children}</div>},
    ),
}));

afterEach(() => {
    vi.useRealTimers();
});

describe.each([false, true])('shared HTML5 backend (StrictMode: %s)', (strictMode) => {
    function Wrapper({children}: PropsWithChildren) {
        const app = <StoreProvider store={store}>
            <AppProvider>
                <MemoryRouter>{children}</MemoryRouter>
            </AppProvider>
        </StoreProvider>;
        return strictMode ? <StrictMode>{app}</StrictMode> : app;
    }

    it('keeps dragging after replacing a tree and repeatedly opening a sort dialog', async () => {
        const user = userEvent.setup();
        const {rerender} = render(<EditorScene treeId="first"/>, {wrapper: Wrapper});

        // The old provider's passive cleanup used to clear the manager already adopted by the new tree.
        rerender(<EditorScene treeId="second"/>);
        for (let attempt = 0; attempt < 2; attempt++) {
            await user.click(screen.getByRole('button', {name: 'Sortieren'}));
            const dialog = screen.getByRole('dialog', {name: 'Testreihenfolge'});
            expect(within(dialog).getAllByRole('listitem')).toHaveLength(2);
            await user.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));
        }

        await dragTreeFieldToEnd();
        expect(screen.getByRole('status', {name: 'Baumreihenfolge'})).toHaveTextContent('second-last,second-first');
        expect(screen.queryByText('Anzeigefehler')).not.toBeInTheDocument();
    });

    it('sorts in a portal while a tree remains mounted and survives a theme change', async () => {
        const user = userEvent.setup();
        render(<EditorScene treeId="tree"/>, {wrapper: Wrapper});
        await user.click(screen.getByRole('button', {name: 'Dunkel'}));
        await user.click(screen.getByRole('button', {name: 'Sortieren'}));

        const dialog = screen.getByRole('dialog', {name: 'Testreihenfolge'});
        const [first, second] = within(dialog).getAllByRole('listitem');
        // This dialog reorders on hover, so a single hover verifies its backend connection.
        const event = {dataTransfer: {types: [], setData: vi.fn()}};
        fireEvent.dragStart(first, event);
        fireEvent.dragEnter(second, event);
        expect(within(dialog).getAllByRole('listitem').map(item => item.textContent)).toEqual(['Beta', 'Alpha']);
        fireEvent.dragEnd(first, event);
        await user.click(within(dialog).getByRole('button', {name: 'Reihenfolge speichern'}));
        expect(screen.getByRole('status', {name: 'Parameterreihenfolge'})).toHaveTextContent('Beta,Alpha');

        await dragTreeFieldToEnd();
        expect(screen.getByRole('status', {name: 'Baumreihenfolge'})).toHaveTextContent('tree-last,tree-first');
    });

    it('supports removing all consumers and remounting the whole application', async () => {
        const firstApp = render(<EditorScene treeId="first"/>, {wrapper: Wrapper});
        firstApp.rerender(<EditorScene treeId={null}/>);
        firstApp.rerender(<EditorScene treeId="second"/>);
        await dragTreeFieldToEnd();
        expect(screen.getByRole('status', {name: 'Baumreihenfolge'})).toHaveTextContent('second-last,second-first');
        firstApp.unmount();

        render(<EditorScene treeId="third"/>, {wrapper: Wrapper});
        await dragTreeFieldToEnd();
        expect(screen.getByRole('status', {name: 'Baumreihenfolge'})).toHaveTextContent('third-last,third-first');
    });

    it('reopens the UI definition dialog with a real element tree after closing', async () => {
        vi.useFakeTimers();
        render(<UiDefinitionInputFieldComponent
            label="Testoberfläche"
            value={{...generateElementWithDefaultValues(ElementType.GroupLayout), children: []}}
            onChange={vi.fn()}
            displayContext={ElementDisplayContext.StaffFacing}
        />, {wrapper: Wrapper});

        for (let attempt = 0; attempt < 2; attempt++) {
            fireEvent.click(screen.getByRole('button', {name: 'Bearbeiten'}));
            const dialog = screen.getByRole('dialog');
            expect(within(dialog).getByRole('button', {name: 'Neues Element hinzufügen'})).toBeEnabled();
            expect(screen.queryByText('Anzeigefehler')).not.toBeInTheDocument();
            fireEvent.click(within(dialog).getByRole('button', {name: 'Abbrechen'}));
            // Complete both the exit transition and the existing delayed draft reset before reopening.
            await act(async () => vi.advanceTimersByTimeAsync(350));
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        }
    });
});

function EditorScene({treeId}: {treeId: string | null}) {
    const {setPreference} = useColorMode();
    return <>
        <button onClick={() => setPreference('dark')}>Dunkel</button>
        {treeId != null && <EditableTree key={treeId} id={treeId}/>}
        <ReorderControls/>
    </>;
}

function EditableTree({id}: {id: string}) {
    const [value, setValue] = useState<GroupLayout>(() => ({
        ...generateElementWithDefaultValues(ElementType.GroupLayout),
        id,
        children: [
            {...generateElementWithDefaultValues(ElementType.Text), id: `${id}-first`, label: 'Erstes Feld'},
            {...generateElementWithDefaultValues(ElementType.Text), id: `${id}-last`, label: 'Letztes Feld'},
        ],
    }));

    return <>
        <ElementTree
            value={value}
            onChange={setValue}
            editable
            displayContext={ElementDisplayContext.StaffFacing}
            allowElementIdEditing={false}
        />
        <output aria-label="Baumreihenfolge">{value.children?.map(child => child.id).join(',')}</output>
    </>;
}

function ReorderControls() {
    const [open, setOpen] = useState(false);
    const [items, setItems] = useState(['Alpha', 'Beta']);

    return <>
        <button onClick={() => setOpen(true)}>Sortieren</button>
        <output aria-label="Parameterreihenfolge">{items.join(',')}</output>
        <ReorderDialog
            title="Testreihenfolge"
            open={open}
            transitionDuration={0}
            items={items}
            getLabel={item => ({primary: item})}
            onClose={() => setOpen(false)}
            onReorder={updated => {
                setItems(updated);
                setOpen(false);
            }}
        />
    </>;
}

async function dragTreeFieldToEnd() {
    const source = screen.getByText('Erstes Feld').closest('[draggable="true"]');
    const target = screen.getByRole('button', {name: 'Neues Element hinzufügen'}).previousElementSibling;
    expect(source).not.toBeNull();
    expect(target).not.toBeNull();
    await dragAndDrop(source!, target!);
}

async function dragAndDrop(source: Element, target: Element) {
    const event = {dataTransfer: {types: [], setData: vi.fn()}, clientX: 0, clientY: 1};
    fireEvent.dragStart(source, event);
    fireEvent.dragEnter(target, event);
    fireEvent.dragOver(target, event);
    fireEvent.drop(target, event);
    fireEvent.dragEnd(source, event);
    await act(async () => {});
}
