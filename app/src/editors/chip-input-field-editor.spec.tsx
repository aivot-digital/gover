import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ChipInputFieldEditor} from './chip-input-field-editor';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';
import {type BaseEditorProps} from './base-editor';
import {type ChipInputFieldElement} from '../models/elements/form/input/chip-input-field-element';

vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

function editorProps(): BaseEditorProps<ChipInputFieldElement> {
    return {
        element: generateElementWithDefaultValues(ElementType.ChipInput),
        editable: true, hasSummaryLayoutParent: false, scope: 'application', onPatch: vi.fn(),
    };
}

describe('ChipInputFieldEditor', () => {
    it('exposes and validates the minimum even for optional fields', () => {
        const props = editorProps();
        props.element = {...props.element, required: false, minItems: 3, maxItems: 2};
        render(<ChipInputFieldEditor {...props}/>);
        expect(screen.getByRole('textbox', {name: /^Minimale Anzahl/})).toHaveValue('3');
        expect(screen.getByText('Die Mindestanzahl darf nicht größer als die Maximalanzahl sein.')).toBeVisible();
        expect(screen.getByText('Die Maximalanzahl darf nicht kleiner als die Mindestanzahl sein.')).toBeVisible();
    });

    it('stores the maximum length for each entry', () => {
        const props = editorProps();
        render(<ChipInputFieldEditor {...props}/>);
        const input = screen.getByRole('textbox', {name: /^Maximale Zeichenanzahl je Eintrag/});
        fireEvent.change(input, {target: {value: '30'}});
        fireEvent.blur(input);
        expect(props.onPatch).toHaveBeenCalledWith({maxItemLength: 30});
    });

    it('defaults to newlines and can explicitly disable all separators or enable commas', () => {
        const props = editorProps();
        const {rerender} = render(<ChipInputFieldEditor {...props}/>);
        expect(screen.getByRole('checkbox', {name: 'Zeilenumbruch'})).toBeChecked();
        expect(screen.getByRole('checkbox', {name: 'Komma'})).not.toBeChecked();
        fireEvent.click(screen.getByRole('checkbox', {name: 'Zeilenumbruch'}));
        expect(props.onPatch).toHaveBeenLastCalledWith({pasteSeparators: []});
        rerender(<ChipInputFieldEditor {...props} element={{...props.element, pasteSeparators: []}}/>);
        expect(screen.getByRole('checkbox', {name: 'Zeilenumbruch'})).not.toBeChecked();
        fireEvent.click(screen.getByRole('checkbox', {name: 'Komma'}));
        expect(props.onPatch).toHaveBeenLastCalledWith({pasteSeparators: [',']});
    });

    it('disables the new configuration controls when editing is not allowed', () => {
        render(<ChipInputFieldEditor {...editorProps()} editable={false}/>);
        expect(screen.getByRole('textbox', {name: /^Maximale Zeichenanzahl je Eintrag/})).toBeDisabled();
        expect(screen.getByRole('checkbox', {name: 'Zeilenumbruch'})).toBeDisabled();
    });
});
