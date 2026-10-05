import React, {StrictMode, useState} from 'react';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {MemoryRouter} from 'react-router-dom';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {type AnyElement} from '../../models/elements/any-element';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type StepperLayoutElement} from '../../models/elements/form/layout/stepper-layout-element';
import {type AnyFormElement} from '../../models/elements/form/any-form-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementTree} from './element-tree';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../providers/confirm-provider', () => ({useConfirm: () => vi.fn()}));
vi.mock('../../modules/process/pages/details/components/process-node-editor/process-node-editor-context', () => ({
    useOptionalProcessNodeEditorContext: () => null,
}));
vi.mock('./components/element-tree-editor', () => ({ElementTreeEditor: () => null}));

beforeEach(() => {
    window.history.replaceState(null, '', '/');
});

describe('ElementTree section expansion', () => {
    it('keeps existing sections collapsed when loading a form', () => {
        render(<TestTree value={createForm([
            createSection('existing'),
            createIntroduction(),
        ])}/>, {wrapper: MemoryRouter});

        expect(screen.getAllByRole('button', {name: 'Ausklappen'})).toHaveLength(2);
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it.each(['Standardabschnitt', 'Allgemeine Informationen'])(
        'opens a newly added %s from the tree dialog',
        async (sectionLabel) => {
            const user = userEvent.setup();
            render(<EditableTree initialValue={createForm([createSection('existing')])}/>, {wrapper: MemoryRouter});

            await user.click(screen.getByRole('button', {name: 'Neuen Abschnitt hinzufügen'}));
            const dialog = screen.getByRole('dialog', {name: 'Formularabschnitt hinzufügen'});
            const optionRow = within(dialog).getByText(sectionLabel).closest('div')!.parentElement!.parentElement!;
            await user.click(within(optionRow).getByRole('button', {name: 'Hinzufügen'}));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

            expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Neues Element hinzufügen'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        },
    );

    it('opens multiple sections supplied through external form changes, including empty sections', () => {
        const initial = createForm([createSection('existing')]);
        const {rerender} = render(<TestTree value={initial}/>, {wrapper: MemoryRouter});

        rerender(<TestTree value={createForm([
            ...initial.children ?? [],
            createSection('new'),
            createIntroduction(),
            generateElementWithDefaultValues(ElementType.SummaryStep),
            generateElementWithDefaultValues(ElementType.SubmitStep),
        ])}/>);

        expect(screen.getAllByRole('button', {name: 'Einklappen'})).toHaveLength(2);
        expect(screen.getAllByRole('button', {name: 'Neues Element hinzufügen'})).toHaveLength(2);
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
    });

    it('opens new sections under StrictMode', () => {
        const {rerender} = render(<StrictMode><TestTree value={createForm()}/></StrictMode>, {wrapper: MemoryRouter});

        rerender(<StrictMode><TestTree value={createForm([createSection('new')])}/></StrictMode>);

        expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Neues Element hinzufügen'})).toBeInTheDocument();
    });

    it('keeps nested groups collapsed inside a newly added section', () => {
        const initial = createForm();
        const {rerender} = render(<TestTree value={initial}/>, {wrapper: MemoryRouter});

        rerender(<TestTree value={createForm([
            createSection('new', [createGroup('nested', [createField('field', 'Gruppendaten')])]),
        ])}/>);

        expect(screen.getByText('nested')).toBeInTheDocument();
        expect(screen.queryByText('Gruppendaten')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
    });

    it('does not reopen a manually collapsed new section on subsequent edits', async () => {
        const user = userEvent.setup();
        const {rerender} = render(<TestTree value={createForm()}/>, {wrapper: MemoryRouter});
        const section = createSection('new');
        rerender(<TestTree value={createForm([section])}/>);
        await user.click(screen.getByRole('button', {name: 'Einklappen'}));

        rerender(<TestTree value={createForm([{...section, title: 'Umbenannter Abschnitt'}])}/>);

        expect(screen.getByText('Umbenannter Abschnitt')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('preserves section expansion when reordering existing sections', async () => {
        const user = userEvent.setup();
        const first = createSection('first', [createField('firstField', 'Erster Inhalt')]);
        const second = createSection('second', [createField('secondField', 'Zweiter Inhalt')]);
        const {rerender} = render(<TestTree value={createForm([first, second])}/>, {wrapper: MemoryRouter});
        await user.click(screen.getAllByRole('button', {name: 'Ausklappen'})[0]);

        rerender(<TestTree value={createForm([second, first])}/>);

        expect(screen.getByText('Erster Inhalt')).toBeInTheDocument();
        expect(screen.queryByText('Zweiter Inhalt')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Einklappen'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
    });

    it('does not treat a restored section as newly added after undo and redo', async () => {
        const user = userEvent.setup();
        const emptyForm = createForm();
        const addedSection = createSection('new');
        const {rerender} = render(<TestTree value={emptyForm}/>, {wrapper: MemoryRouter});
        rerender(<TestTree value={createForm([addedSection])}/>);
        await user.click(screen.getByRole('button', {name: 'Einklappen'}));

        rerender(<TestTree value={emptyForm}/>);
        rerender(<TestTree value={createForm([addedSection])}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('keeps the sections of a different form collapsed when switching roots', () => {
        const {rerender} = render(<TestTree value={createForm()}/>, {wrapper: MemoryRouter});

        rerender(<TestTree value={{...createForm([createSection('otherSection')]), id: 'otherForm'}}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('does not automatically expand sections received in read-only mode', () => {
        const {rerender} = render(<TestTree value={createForm()} editable={false}/>, {wrapper: MemoryRouter});
        const updated = createForm([createSection('new')]);
        rerender(<TestTree value={updated} editable={false}/>);
        rerender(<TestTree value={updated}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
    });

    it('does not apply form section defaults to other UI definition trees', () => {
        const initial: StepperLayoutElement = {
            ...createGroup('stepper', []),
            type: ElementType.StepperLayout,
            children: [],
        };
        const {rerender} = render(<TestTree value={initial}/>, {wrapper: MemoryRouter});

        rerender(<TestTree value={{...initial, children: [createSection('new')]}}/>);

        expect(screen.getByRole('button', {name: 'Ausklappen'})).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Neues Element hinzufügen'})).toHaveLength(1);
    });

    it.each(['Elemente einklappen', 'Elemente ausklappen'])(
        'keeps the new section open and its groups collapsed after a previous "%s" command',
        async (command) => {
            const user = userEvent.setup();
            const initial = createForm([
                createSection('existing', [createGroup('existingGroup', [createField('existingField', 'Bestehende Gruppendaten')])]),
            ]);
            const {rerender} = render(<TestTree value={initial}/>, {wrapper: MemoryRouter});
            await user.click(screen.getByRole('button', {name: command}));
            if (command === 'Elemente ausklappen') {
                expect(screen.getByText('Bestehende Gruppendaten')).toBeInTheDocument();
            }

            rerender(<TestTree value={createForm([
                ...initial.children ?? [],
                createSection('new', [createGroup('newGroup', [createField('newField', 'Neue Gruppendaten')])]),
            ])}/>);

            expect(screen.getByText('newGroup')).toBeInTheDocument();
            expect(screen.queryByText('Neue Gruppendaten')).not.toBeInTheDocument();
            const existingOpen = command === 'Elemente ausklappen';
            expect(screen.getAllByRole('button', {name: 'Einklappen'})).toHaveLength(existingOpen ? 3 : 1);
            expect(screen.getAllByRole('button', {name: 'Ausklappen'})).toHaveLength(existingOpen ? 1 : 2);

            await user.click(screen.getByRole('button', {name: 'Elemente ausklappen'}));
            expect(screen.getByText('Neue Gruppendaten')).toBeInTheDocument();
            await user.click(screen.getByRole('button', {name: 'Elemente einklappen'}));
            expect(screen.queryByRole('button', {name: 'Neues Element hinzufügen'})).not.toBeInTheDocument();
        },
    );
});

function TestTree({
    value,
    editable = true,
    onChange = vi.fn(),
}: {
    value: AnyElement;
    editable?: boolean;
    onChange?: (value: AnyElement) => void;
}) {
    return <ElementTree
        value={value}
        onChange={onChange}
        editable={editable}
        displayContext={ElementDisplayContext.CustomerFacing}
        allowElementIdEditing={false}
    />;
}

function EditableTree({initialValue}: {initialValue: FormLayoutElement}) {
    const [value, setValue] = useState(initialValue);

    return <TestTree value={value} onChange={(updated) => setValue(updated as FormLayoutElement)}/>;
}

function createForm(children: AnyElement[] = []): FormLayoutElement {
    return {...generateElementWithDefaultValues(ElementType.FormLayout) as unknown as FormLayoutElement, id: 'form', children};
}

function createSection(id: string, children: AnyFormElement[] = []): StepElement {
    return {...generateElementWithDefaultValues(ElementType.Step), id, title: id, children};
}

function createIntroduction(): AnyElement {
    return {...generateElementWithDefaultValues(ElementType.IntroductionStep), id: 'introduction'};
}

function createGroup(id: string, children: AnyFormElement[]): GroupLayout {
    return {...generateElementWithDefaultValues(ElementType.GroupLayout), id, name: id, children};
}

function createField(id: string, label: string): AnyFormElement {
    return {...generateElementWithDefaultValues(ElementType.Text), id, label};
}
