import {describe, expect, it, vi} from 'vitest';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {DurationFieldEditor} from './duration-field-editor';
import {DurationLargestUnit} from '../models/elements/form/input/duration-field-element';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';

describe('DurationFieldEditor', () => {
    it('patches only the largest unit and uses it for both bounds', async () => {
        const element = {...generateElementWithDefaultValues(ElementType.Duration), minDuration: 99000, maxDuration: 172800};
        const onPatch = vi.fn();
        const props = {element, onPatch, editable: true, hasSummaryLayoutParent: false, scope: 'application' as const};
        const {rerender} = render(<DurationFieldEditor {...props}/>);
        const user = userEvent.setup();
        await user.click(screen.getByRole('combobox', {name: 'Größte Einheit'}));
        await user.click(screen.getByRole('option', {name: 'Tage'}));
        expect(onPatch).toHaveBeenCalledExactlyOnceWith({largestUnit: DurationLargestUnit.Day});

        rerender(<DurationFieldEditor {...props} element={{...element, largestUnit: DurationLargestUnit.Day}}/>);
        const min = within(screen.getByRole('group', {name: 'Mindestdauer – optional'}));
        const max = within(screen.getByRole('group', {name: 'Höchstdauer – optional'}));
        expect(min.getByRole('textbox', {name: 'Tage'})).toHaveValue('1');
        expect(min.getByRole('textbox', {name: 'Stunden'})).toHaveValue('3');
        expect(max.getByRole('textbox', {name: 'Tage'})).toHaveValue('2');
        expect(screen.getByRole('combobox', {name: 'Genauigkeit'})).toHaveTextContent('Minuten');
    });
});
