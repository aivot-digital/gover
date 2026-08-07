import {AdapterLuxon} from '@mui/x-date-pickers/AdapterLuxon';
import {DateTime} from 'luxon';

export const NONEXISTENT_LOCAL_DATETIME_REASON = 'nonexistent-local-datetime';

function rejectClockShift(
    original: DateTime,
    updated: DateTime,
    expected: {
        hour: number;
        minute: number;
        second: number;
        millisecond: number;
    },
): DateTime {
    if (!original.isValid || !updated.isValid) {
        return updated;
    }

    // Luxon normalizes nonexistent wall times into the next valid hour. MUI updates
    // picker fields incrementally, so verify that the requested clock fields survived.
    if (
        updated.hour !== expected.hour
        || updated.minute !== expected.minute
        || updated.second !== expected.second
        || updated.millisecond !== expected.millisecond
    ) {
        return DateTime.invalid(NONEXISTENT_LOCAL_DATETIME_REASON);
    }

    return updated;
}

export class GoverAdapterLuxon extends AdapterLuxon {
    setYear = (value: DateTime, year: number): DateTime => {
        return rejectClockShift(value, value.set({year}), value);
    };

    setMonth = (value: DateTime, month: number): DateTime => {
        return rejectClockShift(value, value.set({month: month + 1}), value);
    };

    setDate = (value: DateTime, day: number): DateTime => {
        return rejectClockShift(value, value.set({day}), value);
    };

    setHours = (value: DateTime, hour: number): DateTime => {
        return rejectClockShift(value, value.set({hour}), {
            hour,
            minute: value.minute,
            second: value.second,
            millisecond: value.millisecond,
        });
    };

    setMinutes = (value: DateTime, minute: number): DateTime => {
        return rejectClockShift(value, value.set({minute}), {
            hour: value.hour,
            minute,
            second: value.second,
            millisecond: value.millisecond,
        });
    };

    setSeconds = (value: DateTime, second: number): DateTime => {
        return rejectClockShift(value, value.set({second}), {
            hour: value.hour,
            minute: value.minute,
            second,
            millisecond: value.millisecond,
        });
    };

    setMilliseconds = (value: DateTime, millisecond: number): DateTime => {
        return rejectClockShift(value, value.set({millisecond}), {
            hour: value.hour,
            minute: value.minute,
            second: value.second,
            millisecond,
        });
    };
}
