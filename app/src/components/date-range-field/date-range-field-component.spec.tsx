import {fireEvent, render, screen} from '@testing-library/react';
import {DateRangeFieldComponent} from './date-range-field-component';

jest.mock('../date-field/date-field-component', () => ({
    DateFieldComponent: (props: { label: string; onChange: (value: string | null) => void }) => (
        <button
            type="button"
            onClick={() => props.onChange(null)}
        >
            {props.label}
        </button>
    ),
}));

describe('DateRangeFieldComponent', () => {
    it('should merge rapid side changes against the current in-flight range', () => {
        const onChange = jest.fn();

        render(
            <DateRangeFieldComponent
                label="Zeitraum"
                value={{
                    start: '2026-01-01T00:00:00.000Z',
                    end: '2026-01-02T00:00:00.000Z',
                }}
                onChange={onChange}
            />,
        );

        fireEvent.click(screen.getByRole('button', {name: 'Zeitraum (Von)'}));
        fireEvent.click(screen.getByRole('button', {name: 'Zeitraum (Bis)'}));

        expect(onChange).toHaveBeenNthCalledWith(1, {
            start: null,
            end: '2026-01-02T00:00:00.000Z',
        });
        expect(onChange).toHaveBeenNthCalledWith(2, null);
    });
});
