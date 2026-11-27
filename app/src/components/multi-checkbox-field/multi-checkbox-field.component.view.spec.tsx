import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {MultiCheckboxFieldComponentView} from './multi-checkbox-field.component.view';
import {MultiChoicePresentation} from '../../models/elements/form/input/multi-choice-presentation';
import {ElementType} from '../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {createDerivedRuntimeElementData} from '../../models/element-data';

describe.each([MultiChoicePresentation.Combobox, MultiChoicePresentation.Checkboxes])('multiple choice hints (%s)', presentation => {
    const base = {
        element: {...generateElementWithDefaultValues(ElementType.MultiCheckbox), label: 'Bereiche', presentation,
            minimumRequiredOptions: 2, required: false, hint: 'Wählen Sie passende Bereiche.',
            options: [{label: 'Bildung', value: 'a'}, {label: 'Wohnen', value: 'b'}]},
        isBusy: false, isDeriving: false, setValue: vi.fn(), onBlur: vi.fn(),
        authoredElementValues: {}, onAuthoredElementValuesChange: vi.fn(),
        derivedData: createDerivedRuntimeElementData(), onDerive: vi.fn(), onEvent: vi.fn(),
        onResetErrors: vi.fn(), suppressErrors: false, derivationTriggerIdQueue: [],
    };
    const control = () => screen.getByRole(presentation === MultiChoicePresentation.Combobox ? 'combobox' : 'group', {name: /^Bereiche/});

    it('announces the optional minimum before input and updates the remaining count', () => {
        const {rerender} = render(<MultiCheckboxFieldComponentView {...base} value={null}/>);
        expect(control()).toHaveAccessibleDescription('Wählen Sie passende Bereiche. Bei Auswahl mindestens 2 Optionen');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        rerender(<MultiCheckboxFieldComponentView {...base} value={['a']}/>);
        expect(control()).toHaveAccessibleDescription('Wählen Sie passende Bereiche. Noch mindestens 1 Option');
        rerender(<MultiCheckboxFieldComponentView {...base} value={['a', 'b']}/>);
        expect(control()).toHaveAccessibleDescription('Wählen Sie passende Bereiche.');
    });

    it('uses required wording and retains the indicator alongside validation errors', () => {
        const props = {...base, element: {...base.element, required: true}};
        const {rerender} = render(<MultiCheckboxFieldComponentView {...props}/>);
        expect(control()).toHaveAccessibleDescription('Wählen Sie passende Bereiche. Mindestens 2 Optionen');
        rerender(<MultiCheckboxFieldComponentView {...props} value={['a']} errors={['Zu wenige Optionen.']}/>);
        expect(control()).toHaveAccessibleDescription('Zu wenige Optionen. Noch mindestens 1 Option');
        expect(control()).toHaveAttribute('aria-invalid', 'true');
    });

    it('shows the maximum counter, keeps the minimum first and reports existing excess values', () => {
        const props = {...base, element: {...base.element, maximumOptions: 2}};
        const {rerender} = render(<MultiCheckboxFieldComponentView {...props}/>);
        expect(control()).toHaveAccessibleDescription(/Bei Auswahl mindestens 2 Optionen/);
        rerender(<MultiCheckboxFieldComponentView {...props} value={['a', 'b']}/>);
        expect(control()).toHaveAccessibleDescription('Wählen Sie passende Bereiche. 2/2 Optionen ausgewählt');
        rerender(<MultiCheckboxFieldComponentView {...props} value={['a', 'b', 'c']}/>);
        expect(control()).toHaveAttribute('aria-invalid', 'true');
        expect(control()).toHaveAccessibleDescription(/Maximal 2 Optionen erlaubt.*3\/2 Optionen ausgewählt/);
    });

    it.each([null, 0, -1])('omits inactive minimum limits (%s)', minimumRequiredOptions => {
        render(<MultiCheckboxFieldComponentView {...base} element={{...base.element, minimumRequiredOptions}}/>);
        expect(control()).toHaveAccessibleDescription('Wählen Sie passende Bereiche.');
    });
});
