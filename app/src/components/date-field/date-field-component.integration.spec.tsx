import {render, screen} from '@testing-library/react';
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
        expect(screen.getByRole('group', {name: 'Datum – optional'})).toBeInTheDocument();
    });

    it('associates external help and errors with the segmented picker control', () => {
        render(
            <DateFieldComponent
                label="Antragsdatum"
                mode={DateFieldComponentModelMode.Day}
                onChange={vi.fn()}
                error="Das Datum ist ungültig."
                required
            />,
        );

        const field = screen.getByRole('group', {name: 'Antragsdatum'});
        expect(field).toHaveAccessibleDescription('Das Datum ist ungültig.');
        expect(field).toHaveAttribute('aria-invalid', 'true');
        expect(field).toHaveAttribute('aria-required', 'true');
    });
});
