import {fireEvent, render, screen} from '@testing-library/react';
import {DateTimeRangeFieldComponent} from './date-time-range-field-component';

jest.mock('../date-time-field/date-time-field-component', () => ({
    DateTimeFieldComponent: (props: { label: string; onChange: (value: string | null) => void }) => (
        <button
            type="button"
            onClick={() => props.onChange(null)}
        >
            {props.label}
        </button>
    ),
}));

describe('DateTimeRangeFieldComponent', () => {
    it('should merge rapid side changes against the current in-flight range', () => {
        const onChange = jest.fn();

        render(
            <DateTimeRangeFieldComponent
                label="Zeitraum"
                value={{
                    start: '2026-01-01T08:00:00.000Z',
                    end: '2026-01-02T09:00:00.000Z',
                }}
                onChange={onChange}
            />,
        );

        fireEvent.click(screen.getByRole('button', {name: 'Zeitraum (Von)'}));
        fireEvent.click(screen.getByRole('button', {name: 'Zeitraum (Bis)'}));

        expect(onChange).toHaveBeenNthCalledWith(1, {
            start: null,
            end: '2026-01-02T09:00:00.000Z',
        });
        expect(onChange).toHaveBeenNthCalledWith(2, null);
    });
});
