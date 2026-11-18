import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {SelectFieldPresentation} from '../models/elements/form/input/select-field-presentation';
import {ElementType} from '../data/element-type/element-type';
import {
    type AuthoredElementValues,
    createDerivedRuntimeElementData,
    type DerivedRuntimeElementData,
    literalAuthoredValue,
} from '../models/element-data';
import {type AnyElement} from '../models/elements/any-element';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {flattenElements} from '../utils/flatten-elements';
import {ViewDispatcherComponent} from '../components/view-dispatcher/view-dispatcher.component';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../components/view-dispatcher/view-dispatcher.context';

vi.mock('../hooks/use-app-selector', () => ({useAppSelector: () => true}));
vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../components/element-tree-2/components/element-tree-inline-editor-context', () => ({
    useElementTreeInlineEditorContext: () => null,
}));

function groupSelect(id: string, label: string): AnyElement {
    return {
        ...generateElementWithDefaultValues(ElementType.Select),
        presentation: SelectFieldPresentation.Dropdown,
        id,
        label,
        options: [
            {value: 'group_a', label: 'Gruppe A'},
            {value: 'group_b', label: 'Gruppe B'},
        ],
    } as AnyElement;
}

function optionSelect(id: string, label: string, dependsOnSelectFieldId: string): AnyElement {
    return {
        ...generateElementWithDefaultValues(ElementType.Select),
        presentation: SelectFieldPresentation.Dropdown,
        id,
        label,
        dependsOnSelectFieldId,
        options: [
            {value: 'option_a', label: 'Option A', group: 'group_a'},
            {value: 'option_b', label: 'Option B', group: 'group_b'},
        ],
    } as AnyElement;
}

function rows(id: string, children: AnyElement[]): AnyElement {
    return {
        ...generateElementWithDefaultValues(ElementType.ReplicatingContainer),
        id,
        label: id,
        children,
    } as AnyElement;
}

function renderForm(rootDerivedData: DerivedRuntimeElementData = createDerivedRuntimeElementData(), presentation = SelectFieldPresentation.Dropdown) {
    const root = {
        ...generateElementWithDefaultValues(ElementType.GroupLayout),
        id: 'root',
        children: [
            groupSelect('parent', 'Formulargruppe'),
            rows('other_rows', [groupSelect('other_parent', 'Fremde Gruppe')]),
            rows('outer_rows', [
                groupSelect('row_parent', 'Zeilengruppe'),
                rows('inner_rows', [
                    optionSelect('row_child', 'Zeilenoption', 'row_parent'),
                    optionSelect('form_child', 'Formularoption', 'parent'),
                    optionSelect('foreign_child', 'Fremde Option', 'other_parent'),
                ]),
            ]),
        ],
    } as AnyElement;
    const authoredElementValues: AuthoredElementValues = {
        parent: literalAuthoredValue('group_b'),
        other_rows: literalAuthoredValue([{id: 'other-1', values: {other_parent: literalAuthoredValue('group_a')}}]),
        outer_rows: literalAuthoredValue([{
            id: 'outer-1',
            values: {
                row_parent: literalAuthoredValue('group_a'),
                inner_rows: literalAuthoredValue([{id: 'inner-1', values: {}}]),
            },
        }]),
    };

    for (const element of flattenElements(root)) {
        if (element.type === ElementType.Select) element.presentation = presentation;
    }

    render(
        <ViewDispatcherContextProvider value={{
            mode: ViewDispatcherMode.Viewer,
            rootElement: root,
            allElements: flattenElements(root),
            rootAuthoredElementValues: authoredElementValues,
            rootDerivedData,
        }}>
            <ViewDispatcherComponent
                element={root}
                authoredElementValues={authoredElementValues}
                derivedData={rootDerivedData}
                onAuthoredElementValuesChange={vi.fn()}
                onElementBlur={vi.fn()}
                onDerive={vi.fn()}
                onEvent={vi.fn()}
                onResetErrors={vi.fn()}
                isBusy={false}
                isDeriving={false}
                suppressErrors={false}
                derivationTriggerIdQueue={[]}
            />
        </ViewDispatcherContextProvider>,
    );
}

function openOptions(label: string) {
    fireEvent.mouseDown(screen.getByRole('combobox', {name: new RegExp(`^${label}`)}));
    return within(screen.getByRole('listbox'));
}

describe('SelectFieldView in nested rows', () => {
    it('uses the same dependent options in radio presentation', () => {
        renderForm(createDerivedRuntimeElementData(), SelectFieldPresentation.Radio);
        const row = within(screen.getByRole('group', {name: /^Zeilenoption/}));
        expect(row.getByRole('radio', {name: 'Option A'})).toBeInTheDocument();
        expect(row.queryByRole('radio', {name: 'Option B'})).not.toBeInTheDocument();
        const form = within(screen.getByRole('group', {name: /^Formularoption/}));
        expect(form.getByRole('radio', {name: 'Option B'})).toBeInTheDocument();
        expect(form.queryByRole('radio', {name: 'Option A'})).not.toBeInTheDocument();
    });

    it('offers the options of the group selected in the surrounding row', () => {
        renderForm();

        const options = openOptions('Zeilenoption');

        expect(options.getByRole('option', {name: 'Option A'})).toBeInTheDocument();
        expect(options.queryByRole('option', {name: 'Option B'})).not.toBeInTheDocument();
    });

    it('offers the options of the group selected on the form level', () => {
        renderForm();

        const options = openOptions('Formularoption');

        expect(options.getByRole('option', {name: 'Option B'})).toBeInTheDocument();
        expect(options.queryByRole('option', {name: 'Option A'})).not.toBeInTheDocument();
    });

    it('offers no grouped options for a select in the rows of another container', () => {
        renderForm();

        const options = openOptions('Fremde Option');

        expect(options.queryByRole('option', {name: /^Option/})).not.toBeInTheDocument();
        expect(options.getByRole('option', {
            name: 'Bitte wählen Sie zuerst im übergeordneten Auswahlfeld eine Option aus',
        })).toBeInTheDocument();
    });

    it('offers no grouped options while the referenced select is hidden', () => {
        renderForm(createDerivedRuntimeElementData({elementStates: {parent: {visible: false}}}));

        const options = openOptions('Formularoption');

        expect(options.queryByRole('option', {name: /^Option/})).not.toBeInTheDocument();
    });
});
