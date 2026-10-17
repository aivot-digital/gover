import {describe, expect, it, vi} from 'vitest';
import {fireEvent, render, screen} from '@testing-library/react';
import {DateRangeFieldComponent} from './date-range-field-component';

vi.mock('../date-field/date-field-component', () => ({
    DateFieldComponent: (props: {
        label: string;
        ariaDescribedBy?: string;
        onChange: (value: string | null) => void;
    }) => (
        <button
            type="button"
            aria-describedby={props.ariaDescribedBy}
            onClick={() => props.onChange(null)}
        >
            {props.label}
        </button>
    ),
}));

describe('DateRangeFieldComponent', () => {
    it('should merge rapid side changes against the current in-flight range', () => {
        const onChange = vi.fn();

        render(
            <DateRangeFieldComponent
                label="Zeitraum"
                value={{
                    start: '2026-01-01T00:00:00.000Z',
                    end: '2026-01-02T00:00:00.000Z',
                }}
                onChange={onChange}
                hint="Start und Ende des Zeitraums."
            />,
        );

        const group = screen.getByTitle('Zeitraum').closest('fieldset');
        const start = screen.getByText('Von');
        const end = screen.getByText('Bis');
        expect(group).toHaveAccessibleName('Zeitraum – optional');
        expect(group).toHaveAccessibleDescription('Start und Ende des Zeitraums.');
        expect(start).toHaveAccessibleDescription('Start und Ende des Zeitraums.');
        expect(end).toHaveAccessibleDescription('Start und Ende des Zeitraums.');

        fireEvent.click(start);
        fireEvent.click(end);

        expect(onChange).toHaveBeenNthCalledWith(1, {
            start: null,
            end: '2026-01-02T00:00:00.000Z',
        });
        expect(onChange).toHaveBeenNthCalledWith(2, null);
    });
});
