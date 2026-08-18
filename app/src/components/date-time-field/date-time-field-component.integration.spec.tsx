import {render} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {DateTimeFieldComponent} from './date-time-field-component';

describe('DateTimeFieldComponent with MUI', () => {
    it('should render an instant as application wall-clock sections', () => {
        const {container} = render(
            <DateTimeFieldComponent
                label="Termin"
                value="2026-07-29T07:00:00Z"
                onChange={vi.fn()}
            />,
        );

        const sections = Array
            .from(container.querySelectorAll('[contenteditable="true"]'))
            .map((section) => section.textContent);

        expect(sections).toEqual(['29', '07', '2026', '09', '00']);
    });
});
