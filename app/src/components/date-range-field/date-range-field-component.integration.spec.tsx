import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DateRangeFieldComponent} from './date-range-field-component';

describe('DateRangeFieldComponent with MUI', () => {
    it('uses one range label and describes both segmented picker controls with the shared error', () => {
        render(
            <DateRangeFieldComponent
                label="Gültigkeitszeitraum"
                value={{start: '2026-08-01', end: '2026-08-31'}}
                onChange={vi.fn()}
                error="Das Enddatum muss nach dem Startdatum liegen."
                mode={DateFieldComponentModelMode.Day}
            />,
        );

        const rangeLabel = screen.getByTitle('Gültigkeitszeitraum');
        const startLabel = screen.getByTitle('Von');
        const endLabel = screen.getByTitle('Bis');
        const range = rangeLabel.closest('fieldset');
        const start = document.querySelector(`[role="group"][aria-labelledby="${startLabel.id}"]`);
        const end = document.querySelector(`[role="group"][aria-labelledby="${endLabel.id}"]`);

        expect(range).toHaveAccessibleName('Gültigkeitszeitraum – optional');
        expect(start).toHaveAccessibleName('Von');
        expect(end).toHaveAccessibleName('Bis');
        expect(range).toHaveAccessibleDescription('Das Enddatum muss nach dem Startdatum liegen.');
        expect(start).toHaveAccessibleDescription('Das Enddatum muss nach dem Startdatum liegen.');
        expect(end).toHaveAccessibleDescription('Das Enddatum muss nach dem Startdatum liegen.');
        expect(start).toHaveAttribute('aria-invalid', 'true');
        expect(end).toHaveAttribute('aria-invalid', 'true');
        expect(document.querySelectorAll('[role="alert"]')).toHaveLength(1);
    });
});
