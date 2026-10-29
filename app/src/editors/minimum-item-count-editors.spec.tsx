import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {MultiCheckboxFieldComponentEditor} from '../components/multi-checkbox-field/multi-checkbox-field.component.editor';
import {TableFieldComponentEditor} from '../components/table-field/table-field.component.editor';
import {ReplicatingContainerEditor} from '../components/replicating-container/replicating-container.editor';
import {ProcessInstanceAttachmentSetSelectFieldEditor} from './process-instance-attachment-set-select-field-editor';
import {DomainUserSelectFieldEditor} from './domain-user-select-field-editor';
import {AssignmentContextFieldEditor} from './assignment-context-field-editor';
import {ElementType} from '../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';

vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

const common = {editable: true, hasSummaryLayoutParent: false, scope: 'application' as const};
type EditorProps = typeof common & {onPatch: (patch: object) => void};
const cases = [
    {
        name: 'multiple choice', key: 'minimumRequiredOptions', cleared: undefined,
        render: (props: EditorProps, required: boolean) => <MultiCheckboxFieldComponentEditor {...props}
            element={{...generateElementWithDefaultValues(ElementType.MultiCheckbox), required}} />,
    },
    {
        name: 'table', key: 'minimumRequiredRows', cleared: 0,
        render: (props: EditorProps, required: boolean) => <TableFieldComponentEditor {...props}
            element={{...generateElementWithDefaultValues(ElementType.Table), required}} />,
    },
    {
        name: 'repeated records', key: 'minimumRequiredSets', cleared: undefined,
        render: (props: EditorProps, required: boolean) => <ReplicatingContainerEditor {...props}
            element={{...generateElementWithDefaultValues(ElementType.ReplicatingContainer), minimumRequiredSets: undefined, required}} />,
    },
    {
        name: 'attachment sets', key: 'minItems', cleared: 0,
        render: (props: EditorProps, required: boolean) => <ProcessInstanceAttachmentSetSelectFieldEditor {...props}
            element={{...generateElementWithDefaultValues(ElementType.ProcessInstanceAttachmentSetSelect), required}} />,
    },
];

describe.each(cases)('minimum count editor: $name', testCase => {
    it.each([false, true])('changes the minimum without changing required=%s', required => {
        const onPatch = vi.fn();
        render(testCase.render({...common, onPatch}, required));
        const input = screen.getByRole('textbox', {name: /^(Mindestanzahl|Minimale Anzahl)/});
        expect(input).toHaveValue('');
        expect(input).toHaveAccessibleDescription(/Optionale Felder dürfen leer bleiben/);
        fireEvent.change(input, {target: {value: '3'}});
        fireEvent.blur(input);
        expect(onPatch).toHaveBeenLastCalledWith({[testCase.key]: 3});
        fireEvent.change(input, {target: {value: '0'}});
        fireEvent.blur(input);
        expect(onPatch).toHaveBeenLastCalledWith({[testCase.key]: testCase.cleared});
    });

    it('disables the minimum when editing is not allowed', () => {
        render(testCase.render({...common, onPatch: vi.fn(), editable: false}, false));
        expect(screen.getByRole('textbox', {name: /^(Mindestanzahl|Minimale Anzahl)/})).toBeDisabled();
    });
});

it.each([ElementType.DomainAndUserSelect, ElementType.AssignmentContext] as const)('allows an unlimited maximum with an optional minimum in editor %s', type => {
    const props = {...common, onPatch: vi.fn()};
    function editor(maxItems: number) {
        const limits = {required: false, minItems: 3, maxItems};
        return type === ElementType.DomainAndUserSelect
            ? <DomainUserSelectFieldEditor {...props} element={{...generateElementWithDefaultValues(type), ...limits}} />
            : <AssignmentContextFieldEditor {...props} element={{...generateElementWithDefaultValues(type), ...limits}} />;
    }
    const {rerender} = render(editor(0));
    expect(screen.getByRole('textbox', {name: /^Minimale Anzahl/})).not.toHaveAttribute('aria-invalid', 'true');
    rerender(editor(2));
    expect(screen.getByRole('textbox', {name: /^Minimale Anzahl/})).toHaveAccessibleDescription(/Die Mindestanzahl darf nicht größer/);
});
