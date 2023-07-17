import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";
import {addDays, addMonths, addYears, isAfter, isBefore, isSameDay, isValid, parse, parseISO, subDays, subMonths, subYears} from "date-fns";

const dayRegex = /^\d\d\.\d\d\.\d\d\d\d$/;
const dayAnyMonthAnyYearRegex = /^\d\d\.$/;
const monthRegex = /^\d\d\.\d\d\d\d$/;
const monthAnyYearRegex = /^\d\d\.\d\d\.$/;
const yearRegex = /^\d\d\d\d$/;

const germanDateFormat = 'dd.MM.yyyy';

enum Precision {
    day,
    dayAnyMonthAnyYear,
    month,
    dayAndMonthAnyYear,
    year,
    iso,
}

function transformValue(val: any): [Date, Precision] | [null, null] {
    if (typeof val !== 'string') {
        return [null, null];
    }

    const today = new Date();

    let date: Date | null = null;
    let precision: Precision | null = null;
    try {
        if (val.match(dayRegex)) {
            date = parse(val, germanDateFormat, new Date());
            precision = Precision.day;
        } else if (val.match(dayAnyMonthAnyYearRegex)) {
            date = parse(val + today.getMonth() + '.' + today.getFullYear(), germanDateFormat, new Date());
            precision = Precision.dayAnyMonthAnyYear;
        } else if (val.match(monthRegex)) {
            date = parse('01.' + val, germanDateFormat, new Date());
            precision = Precision.month;
        } else if (val.match(monthAnyYearRegex)) {
            date = parse(val + today.getFullYear(), germanDateFormat, new Date());
            precision = Precision.dayAndMonthAnyYear;
        } else if (val.match(yearRegex)) {
            date = parse('01.01.' + val, germanDateFormat, new Date());
            precision = Precision.year;
        } else {
            date = parseISO(val);
            precision = Precision.iso;
        }
    } catch (_) {
        return [null, null];
    }

    return isValid(date) ? [date, precision] : [null, null];
}

enum DateDiff {
    Less,
    Equal,
    Greater,
}

function compareDate(d1: Date, d2: Date): [DateDiff, DateDiff, DateDiff] {
    return [
        d1.getDate() === d2.getDate() ? DateDiff.Equal : (d1.getDate() < d2.getDate() ? DateDiff.Less : DateDiff.Greater),
        d1.getMonth() === d2.getMonth() ? DateDiff.Equal : (d1.getMonth() < d2.getMonth() ? DateDiff.Less : DateDiff.Greater),
        d1.getFullYear() === d2.getFullYear() ? DateDiff.Equal : (d1.getFullYear() < d2.getFullYear() ? DateDiff.Less : DateDiff.Greater),
    ];
}

export const DateEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null) {
            return false;
        }

        const [
            dayEq,
            monthEq,
            yearEq,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Equal);

        switch (pValB) {
            case Precision.day:
            case Precision.iso:
                return dayEq && monthEq && yearEq;
            case Precision.dayAnyMonthAnyYear:
                return dayEq;
            case Precision.month:
                return monthEq && yearEq;
            case Precision.dayAndMonthAnyYear:
                return dayEq && monthEq;
            case Precision.year:
                return yearEq;
        }
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null) {
            return false;
        }

        const [
            dayNeq,
            monthNeq,
            yearNeq,
        ] = compareDate(tValA, tValB).map(d => d !== DateDiff.Equal);

        switch (pValB) {
            case Precision.day:
            case Precision.iso:
                return dayNeq || monthNeq || yearNeq;
            case Precision.dayAnyMonthAnyYear:
                return dayNeq;
            case Precision.month:
                return monthNeq || yearNeq;
            case Precision.dayAndMonthAnyYear:
                return dayNeq || monthNeq;
            case Precision.year:
                return yearNeq;
        }
    },

    [ConditionOperator.LessThan]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null) {
            return false;
        }

        const [
            _,
            monthEqual,
            yearEqual,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Equal);

        const [
            dayLess,
            monthLess,
            yearLess,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Less);

        switch (pValB) {
            case Precision.day:
            case Precision.iso:
                return (
                    yearLess ||
                    (yearEqual && monthLess) ||
                    (yearEqual && monthEqual && dayLess)
                );
            case Precision.dayAnyMonthAnyYear:
                return dayLess;
            case Precision.month:
                return (
                    yearLess ||
                    (yearEqual && monthLess)
                );
            case Precision.dayAndMonthAnyYear:
                return monthLess || (monthEqual && dayLess);
            case Precision.year:
                return yearLess;
        }
    },

    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null) {
            return false;
        }

        const [
            dayEqual,
            monthEqual,
            yearEqual,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Equal);

        const [
            dayLess,
            monthLess,
            yearLess,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Less);

        switch (pValB) {
            case Precision.day:
            case Precision.iso:
                return (
                    yearLess ||
                    (yearEqual && monthLess) ||
                    (yearEqual && monthEqual && (dayLess || dayEqual))
                );
            case Precision.dayAnyMonthAnyYear:
                return dayLess || dayEqual;
            case Precision.month:
                return (
                    yearLess ||
                    (yearEqual && (monthLess || monthEqual))
                );
            case Precision.dayAndMonthAnyYear:
                return monthLess || (monthEqual && (dayLess || dayEqual));
            case Precision.year:
                return yearLess || yearEqual;
        }
    },

    [ConditionOperator.GreaterThan]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null) {
            return false;
        }

        const [
            _,
            monthEqual,
            yearEqual,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Equal);

        const [
            dayGreater,
            monthGreater,
            yearGreater,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Greater);

        switch (pValB) {
            case Precision.day:
            case Precision.iso:
                return (
                    yearGreater ||
                    (yearEqual && monthGreater) ||
                    (yearEqual && monthEqual && dayGreater)
                );
            case Precision.dayAnyMonthAnyYear:
                return dayGreater;
            case Precision.month:
                return (
                    yearGreater ||
                    (yearEqual && monthGreater)
                );
            case Precision.dayAndMonthAnyYear:
                return monthGreater || (monthEqual && dayGreater);
            case Precision.year:
                return yearGreater;
        }
    },

    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null) {
            return false;
        }

        const [
            dayEqual,
            monthEqual,
            yearEqual,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Equal);

        const [
            dayGreater,
            monthGreater,
            yearGreater,
        ] = compareDate(tValA, tValB).map(d => d === DateDiff.Greater);

        switch (pValB) {
            case Precision.day:
            case Precision.iso:
                return (
                    yearGreater ||
                    (yearEqual && monthGreater) ||
                    (yearEqual && monthEqual && (dayGreater || dayEqual))
                );
            case Precision.dayAnyMonthAnyYear:
                return dayGreater || dayEqual;
            case Precision.month:
                return (
                    yearGreater ||
                    (yearEqual && (monthGreater || monthEqual))
                );
            case Precision.dayAndMonthAnyYear:
                return monthGreater || (monthEqual && (dayGreater || dayEqual));
            case Precision.year:
                return yearGreater || yearEqual;
        }
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null;
    },

    [ConditionOperator.YearsInPast]: (valueA, valueB) => {
        if (valueA == null || valueB == null) {
            return false;
        }

        const years = parseInt(valueB);
        if (isNaN(years)) {
            return false;
        }

        const [valueADate, _] = transformValue(valueA);
        if (valueADate == null) {
            return false;
        }

        const target = subYears(new Date(), years);
        return isBefore(valueADate, target) || isSameDay(valueADate, target);
    },

    [ConditionOperator.MonthsInPast]: (valueA, valueB) => {
        if (valueA == null || valueB == null) {
            return false;
        }

        const months = parseInt(valueB);
        if (isNaN(months)) {
            return false;
        }

        const [valueADate, _] = transformValue(valueA);
        if (valueADate == null) {
            return false;
        }

        const target = subMonths(new Date(), months);
        return isBefore(valueADate, target) || isSameDay(valueADate, target);
    },

    [ConditionOperator.DaysInPast]: (valueA, valueB) => {
        if (valueA == null || valueB == null) {
            return false;
        }

        const days = parseInt(valueB);
        if (isNaN(days)) {
            return false;
        }

        const [valueADate, _] = transformValue(valueA);
        if (valueADate == null) {
            return false;
        }

        const target = subDays(new Date(), days);
        return isBefore(valueADate, target) || isSameDay(valueADate, target);
    },

    [ConditionOperator.YearsInFuture]: (valueA, valueB) => {
        if (valueA == null || valueB == null) {
            return false;
        }

        const years = parseInt(valueB);
        if (isNaN(years)) {
            return false;
        }

        const [valueADate, _] = transformValue(valueA);
        if (valueADate == null) {
            return false;
        }

        const target = addYears(new Date(), years);
        return isAfter(valueADate, target) || isSameDay(valueADate, target);
    },

    [ConditionOperator.MonthsInFuture]: (valueA, valueB) => {
        if (valueA == null || valueB == null) {
            return false;
        }

        const months = parseInt(valueB);
        if (isNaN(months)) {
            return false;
        }

        const [valueADate, _] = transformValue(valueA);
        if (valueADate == null) {
            return false;
        }

        const target = addMonths(new Date(), months);
        return isAfter(valueADate, target) || isSameDay(valueADate, target);
    },

    [ConditionOperator.DaysInFuture]: (valueA, valueB) => {
        if (valueA == null || valueB == null) {
            return false;
        }

        const days = parseInt(valueB);
        if (isNaN(days)) {
            return false;
        }

        const [valueADate, _] = transformValue(valueA);
        if (valueADate == null) {
            return false;
        }

        const target = addDays(new Date(), days);
        return isAfter(valueADate, target) || isSameDay(valueADate, target);
    },
};
