import {DateTime} from 'luxon';
import {ConditionOperator} from '../data/condition-operator';
import {BaseEvaluator} from './base-evaluator';
import {
    dateValueToDateTime,
    getCurrentApplicationDate,
    isLocalDateIso,
    isYearIso,
    isYearMonthIso,
} from '../utils/temporal-utils';

const dayPattern = /^\d{2}\.\d{2}\.\d{4}$/;
const dayAnyMonthAnyYearPattern = /^\d{2}\.$/;
const monthPattern = /^\d{2}\.\d{4}$/;
const dayAndMonthAnyYearPattern = /^\d{2}\.\d{2}\.$/;
const comparisonAnchorYear = 2000;

enum Precision {
    Day,
    DayAnyMonthAnyYear,
    Month,
    DayAndMonthAnyYear,
    Year,
}

enum DateDiff {
    Less,
    Equal,
    Greater,
}

function currentDate(): DateTime<true> {
    return dateValueToDateTime(getCurrentApplicationDate(), 'day')!;
}

function validDate(date: DateTime): DateTime<true> | null {
    return date.isValid ? date as DateTime<true> : null;
}

function transformRuntimeValue(value: unknown): [DateTime<true>, Precision] | [null, null] {
    if (typeof value !== 'string') {
        return [null, null];
    }

    if (isLocalDateIso(value)) {
        return [dateValueToDateTime(value, 'day')!, Precision.Day];
    }

    if (isYearMonthIso(value)) {
        return [dateValueToDateTime(value, 'month')!, Precision.Month];
    }

    if (isYearIso(value)) {
        return [dateValueToDateTime(value, 'year')!, Precision.Year];
    }

    return [null, null];
}

function transformComparisonValue(value: unknown): [DateTime<true>, Precision] | [null, null] {
    const runtimeValue = transformRuntimeValue(value);
    if (runtimeValue[0] != null || typeof value !== 'string') {
        return runtimeValue;
    }

    if (dayPattern.test(value)) {
        const date = validDate(DateTime.fromFormat(value, 'dd.MM.yyyy', {zone: 'UTC'}));
        return date != null ? [date, Precision.Day] : [null, null];
    }

    if (dayAnyMonthAnyYearPattern.test(value)) {
        // January accepts day 31 and the leap year 2000 accepts February 29.
        // This keeps partial authored condition values independent of today's date.
        const date = validDate(DateTime.fromObject({
            year: comparisonAnchorYear,
            month: 1,
            day: Number.parseInt(value, 10),
        }, {zone: 'UTC'}));
        return date != null ? [date, Precision.DayAnyMonthAnyYear] : [null, null];
    }

    if (monthPattern.test(value)) {
        const date = validDate(DateTime.fromFormat(`01.${value}`, 'dd.MM.yyyy', {zone: 'UTC'}));
        return date != null ? [date, Precision.Month] : [null, null];
    }

    if (dayAndMonthAnyYearPattern.test(value)) {
        const [day, month] = value.split('.').map(Number);
        const date = validDate(DateTime.fromObject({
            year: comparisonAnchorYear,
            month,
            day,
        }, {zone: 'UTC'}));
        return date != null ? [date, Precision.DayAndMonthAnyYear] : [null, null];
    }

    return [null, null];
}

function comparePart(left: number, right: number): DateDiff {
    if (left === right) {
        return DateDiff.Equal;
    }
    return left < right ? DateDiff.Less : DateDiff.Greater;
}

function compareDate(left: DateTime, right: DateTime): [DateDiff, DateDiff, DateDiff] {
    return [
        comparePart(left.day, right.day),
        comparePart(left.month, right.month),
        comparePart(left.year, right.year),
    ];
}

function compareByPrecision(
    valueA: unknown,
    valueB: unknown,
    relation: DateDiff,
    includeEqual: boolean,
): boolean {
    const [dateA] = transformRuntimeValue(valueA);
    const [dateB, precisionB] = transformComparisonValue(valueB);

    if (dateA == null || dateB == null || precisionB == null) {
        return false;
    }

    const [dayDiff, monthDiff, yearDiff] = compareDate(dateA, dateB);
    const matches = (diff: DateDiff) => diff === relation || (includeEqual && diff === DateDiff.Equal);

    switch (precisionB) {
        case Precision.Day:
            if (yearDiff !== DateDiff.Equal) {
                return matches(yearDiff);
            }
            if (monthDiff !== DateDiff.Equal) {
                return matches(monthDiff);
            }
            return matches(dayDiff);
        case Precision.DayAnyMonthAnyYear:
            return matches(dayDiff);
        case Precision.Month:
            if (yearDiff !== DateDiff.Equal) {
                return matches(yearDiff);
            }
            return matches(monthDiff);
        case Precision.DayAndMonthAnyYear:
            if (monthDiff !== DateDiff.Equal) {
                return matches(monthDiff);
            }
            return matches(dayDiff);
        case Precision.Year:
            return matches(yearDiff);
    }
}

function equalsByPrecision(valueA: unknown, valueB: unknown): boolean | null {
    const [dateA] = transformRuntimeValue(valueA);
    const [dateB, precisionB] = transformComparisonValue(valueB);

    if (dateA == null || dateB == null || precisionB == null) {
        return null;
    }

    const [dayDiff, monthDiff, yearDiff] = compareDate(dateA, dateB);

    switch (precisionB) {
        case Precision.Day:
            return dayDiff === DateDiff.Equal
                && monthDiff === DateDiff.Equal
                && yearDiff === DateDiff.Equal;
        case Precision.DayAnyMonthAnyYear:
            return dayDiff === DateDiff.Equal;
        case Precision.Month:
            return monthDiff === DateDiff.Equal && yearDiff === DateDiff.Equal;
        case Precision.DayAndMonthAnyYear:
            return dayDiff === DateDiff.Equal && monthDiff === DateDiff.Equal;
        case Precision.Year:
            return yearDiff === DateDiff.Equal;
    }
}

function parseAmount(value: unknown): number | null {
    if (typeof value !== 'string' && typeof value !== 'number') {
        return null;
    }

    const amountString = String(value);
    if (!/^-?\d+$/.test(amountString)) {
        return null;
    }

    const amount = Number(amountString);
    return Number.isInteger(amount)
        && amount >= -2_147_483_648
        && amount <= 2_147_483_647
        ? amount
        : null;
}

function compareRelative(
    value: unknown,
    amountValue: unknown,
    unit: 'years' | 'months' | 'days',
    direction: 'past' | 'future',
): boolean {
    const [date] = transformRuntimeValue(value);
    const amount = parseAmount(amountValue);

    if (date == null || amount == null) {
        return false;
    }

    const target = direction === 'past'
        ? currentDate().minus({[unit]: amount})
        : currentDate().plus({[unit]: amount});

    return direction === 'past'
        ? date.toMillis() <= target.toMillis()
        : date.toMillis() >= target.toMillis();
}

export const DateEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) =>
        equalsByPrecision(valueA, valueB) ?? false,
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        const equals = equalsByPrecision(valueA, valueB);
        return equals != null ? !equals : false;
    },
    [ConditionOperator.LessThan]: (valueA, valueB) =>
        compareByPrecision(valueA, valueB, DateDiff.Less, false),
    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) =>
        compareByPrecision(valueA, valueB, DateDiff.Less, true),
    [ConditionOperator.GreaterThan]: (valueA, valueB) =>
        compareByPrecision(valueA, valueB, DateDiff.Greater, false),
    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) =>
        compareByPrecision(valueA, valueB, DateDiff.Greater, true),
    [ConditionOperator.Empty]: (valueA) => valueA == null,
    [ConditionOperator.NotEmpty]: (valueA) => valueA != null,
    [ConditionOperator.YearsInPast]: (valueA, valueB) =>
        compareRelative(valueA, valueB, 'years', 'past'),
    [ConditionOperator.MonthsInPast]: (valueA, valueB) =>
        compareRelative(valueA, valueB, 'months', 'past'),
    [ConditionOperator.DaysInPast]: (valueA, valueB) =>
        compareRelative(valueA, valueB, 'days', 'past'),
    [ConditionOperator.YearsInFuture]: (valueA, valueB) =>
        compareRelative(valueA, valueB, 'years', 'future'),
    [ConditionOperator.MonthsInFuture]: (valueA, valueB) =>
        compareRelative(valueA, valueB, 'months', 'future'),
    [ConditionOperator.DaysInFuture]: (valueA, valueB) =>
        compareRelative(valueA, valueB, 'days', 'future'),
};
