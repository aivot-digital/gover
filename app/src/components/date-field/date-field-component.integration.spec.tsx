import {render} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DateFieldComponent} from './date-field-component';

describe('DateFieldComponent with MUI', () => {
    it('should render a local date without shifting it', () => {
        const {container} = render(
            <DateFieldComponent
                label="Datum"
                mode={DateFieldComponentModelMode.Day}
                value="2026-07-29"
                onChange={vi.fn()}
            />,
        );

        const sections = Array
            .from(container.querySelectorAll('[contenteditable="true"]'))
            .map((section) => section.textContent);

        expect(sections).toEqual(['29', '07', '2026']);
    });
});
