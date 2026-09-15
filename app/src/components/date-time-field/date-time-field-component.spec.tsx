import {act, render} from '@testing-library/react';
import {DateTime} from 'luxon';
import {describe, expect, it, vi} from 'vitest';
import {DateTimeFieldComponent} from './date-time-field-component';

const pickerState = vi.hoisted(() => ({
    props: null as any,
}));

vi.mock('@mui/x-date-pickers', () => ({
    LocalizationProvider: ({children}: { children: React.ReactNode }) => children,
    DateTimePicker: (props: any) => {
        pickerState.props = props;
        return <div data-testid="date-time-picker"/>;
    },
}));

describe('DateTimeFieldComponent', () => {
    it('should render an instant in the application timezone', () => {
        render(
            <DateTimeFieldComponent
                label="Termin"
                value="2026-07-29T07:00:00Z"
                onChange={vi.fn()}
            />,
        );

        expect(pickerState.props.timezone).toBe('Europe/Berlin');
        expect(pickerState.props.value.toISO()).toBe('2026-07-29T09:00:00.000+02:00');
    });

    it('should commit picker wall-clock values with the application timezone offset', () => {
        const onChange = vi.fn();

        render(
            <DateTimeFieldComponent
                label="Termin"
                onChange={onChange}
            />,
        );

        act(() => {
            pickerState.props.onOpen();
            const pickedDateTime = DateTime.fromISO('2026-07-29T09:00:00', {zone: 'Asia/Tokyo'});
            pickerState.props.onChange(pickedDateTime);
            pickerState.props.onAccept(pickedDateTime);
        });

        expect(onChange).toHaveBeenCalledWith('2026-07-29T09:00:00+02:00');
    });

    it('should select the earlier application offset for an overlapping wall-clock value', () => {
        const onChange = vi.fn();

        render(
            <DateTimeFieldComponent
                label="Termin"
                onChange={onChange}
            />,
        );

        act(() => {
            pickerState.props.onOpen();
            const pickedDateTime = DateTime
                .fromISO('2026-10-25T02:30:00', {zone: 'Europe/Berlin'})
                .setZone('Europe/Berlin', {keepLocalTime: true});
            pickerState.props.onChange(pickedDateTime);
            pickerState.props.onAccept(pickedDateTime);
        });

        expect(onChange).toHaveBeenCalledWith('2026-10-25T02:30:00+02:00');
    });
});
