import {act, render} from '@testing-library/react';
import {DateTime} from 'luxon';
import {describe, expect, it, vi} from 'vitest';
import {TimeFieldComponentModelMode} from '../../models/elements/form/input/time-field-element';
import {TimeFieldComponent} from './time-field-component';

const pickerState = vi.hoisted(() => ({
    props: null as any,
}));

vi.mock('@mui/x-date-pickers', () => ({
    LocalizationProvider: ({children}: { children: React.ReactNode }) => children,
    TimePicker: (props: any) => {
        pickerState.props = props;
        return <div data-testid="time-picker"/>;
    },
}));

describe('TimeFieldComponent', () => {
    it('should render a local time in a browser-independent picker zone', () => {
        render(
            <TimeFieldComponent
                label="Uhrzeit"
                value="09:30"
                onChange={vi.fn()}
            />,
        );

        expect(pickerState.props.timezone).toBe('UTC');
        expect(pickerState.props.value.toISO()).toBe('1970-01-01T09:30:00.000Z');
    });

    it.each([
        [TimeFieldComponentModelMode.Minute, '09:30'],
        [TimeFieldComponentModelMode.Second, '09:30:15'],
    ])('should write the canonical %s value', (mode, expectedValue) => {
        const onChange = vi.fn();

        render(
            <TimeFieldComponent
                label="Uhrzeit"
                mode={mode}
                onChange={onChange}
            />,
        );

        act(() => {
            pickerState.props.onOpen();
            const pickedTime = DateTime.fromISO('2026-07-29T09:30:15', {zone: 'America/Los_Angeles'});
            pickerState.props.onChange(pickedTime);
            pickerState.props.onAccept(pickedTime);
        });

        expect(onChange).toHaveBeenCalledWith(expectedValue);
    });
});
