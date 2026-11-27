import {act, render} from '@testing-library/react';
import {DateTime} from 'luxon';
import {describe, expect, it, vi} from 'vitest';
import {DateFieldComponentModelMode} from '../../models/elements/form/input/date-field-element';
import {DateFieldComponent} from './date-field-component';

const pickerState = vi.hoisted(() => ({
    props: null as any,
}));

vi.mock('@mui/x-date-pickers', () => ({
    LocalizationProvider: ({children}: { children: React.ReactNode }) => children,
    DatePicker: (props: any) => {
        pickerState.props = props;
        return <div data-testid="date-picker"/>;
    },
}));

describe('DateFieldComponent', () => {
    it('should render a local date in a browser-independent picker zone', () => {
        render(
            <DateFieldComponent
                label="Datum"
                mode={DateFieldComponentModelMode.Day}
                value="2026-07-29"
                onChange={vi.fn()}
            />,
        );

        expect(pickerState.props.timezone).toBe('UTC');
        expect(pickerState.props.value.toISO()).toBe('2026-07-29T00:00:00.000Z');
    });

    it.each([
        [DateFieldComponentModelMode.Day, '2026-07-29'],
        [DateFieldComponentModelMode.Month, '2026-07'],
        [DateFieldComponentModelMode.Year, '2026'],
    ])('should write the canonical %s value', (mode, expectedValue) => {
        const onChange = vi.fn();

        render(
            <DateFieldComponent
                label="Datum"
                mode={mode}
                onChange={onChange}
            />,
        );

        act(() => {
            pickerState.props.onOpen();
            const pickedDate = DateTime.fromISO('2026-07-29T23:30:00', {zone: 'Asia/Tokyo'});
            pickerState.props.onChange(pickedDate);
            pickerState.props.onAccept(pickedDate);
        });

        expect(onChange).toHaveBeenCalledWith(expectedValue);
    });

    it('should not commit a provisional picker value before it is accepted', () => {
        const onChange = vi.fn();

        render(
            <DateFieldComponent
                label="Datum"
                mode={DateFieldComponentModelMode.Day}
                onChange={onChange}
            />,
        );

        act(() => {
            pickerState.props.onOpen();
            pickerState.props.onChange(DateTime.fromISO('2026-07-29T00:00:00Z'));
        });

        expect(onChange).not.toHaveBeenCalled();
    });
});
