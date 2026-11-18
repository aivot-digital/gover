import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ChipInputFieldView} from './chip-input-field-view';
import {type BaseViewProps} from './base-view';
import {type ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';

vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

function viewProps(): BaseViewProps<ChipInputFieldElement, string[]> {
    return {
        element: {...generateElementWithDefaultValues(ElementType.ChipInput), label: 'Tags'},
        isBusy: false, isDeriving: false, errors: [], setValue: vi.fn(), onBlur: vi.fn(),
        authoredElementValues: {}, onAuthoredElementValuesChange: vi.fn(),
        derivedData: createDerivedRuntimeElementData(), onDerive: vi.fn(), onEvent: vi.fn(),
        onResetErrors: vi.fn(), suppressErrors: false, derivationTriggerIdQueue: [],
    };
}

describe('ChipInputFieldView', () => {
    it('applies configured separators and length validation with an empty backend error list', () => {
        const props = viewProps();
        props.element = {...props.element, maxItemLength: 3, pasteSeparators: [';']};
        const {rerender} = render(<ChipInputFieldView {...props}/>);
        fireEvent.paste(screen.getByRole('combobox'), {clipboardData: {getData: () => 'ABCD;EF'}});
        expect(props.setValue).toHaveBeenCalledWith(['ABCD', 'EF']);
        rerender(<ChipInputFieldView {...props} value={['ABCD', 'EF']}/>);
        expect(screen.getByRole('alert')).toHaveTextContent('Jeder Eintrag darf höchstens 3 Zeichen enthalten.');
    });

    it('blocks pending input while a derived field is busy', () => {
        const props = viewProps();
        props.element = {...props.element, visibility: {
            type: 'Javascript', requirements: null, conditionSet: null, noCode: null,
            referencedIds: [], javascriptCode: {code: 'return true;'},
        }};
        const {rerender} = render(<ChipInputFieldView {...props}/>);
        const input = screen.getByRole('combobox');
        fireEvent.change(input, {target: {value: 'Entwurf'}});
        rerender(<ChipInputFieldView {...props} isDeriving/>);
        expect(input).toHaveAttribute('aria-busy', 'true');
        fireEvent.keyDown(input, {key: 'Enter'});
        fireEvent.blur(input);
        expect(props.setValue).not.toHaveBeenCalled();
    });
});
