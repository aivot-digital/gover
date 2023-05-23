import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";
import {isValid, parse, parseISO} from "date-fns";

const dayRegex = /^\d\d\.\d\d\.\d\d\d\d$/;
const dayThisMonthRegex = /^\d\d\.$/;
const monthRegex = /^\d\d\.\d\d\d\d$/;
const monthThisYearRegex = /^\d\d\.\d\d\.$/;
const yearRegex = /^\d\d\d\d$/;

const germanDateFormat = 'dd.MM.yyyy';

enum Precision {
    day,
    month,
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
        }

        else if (val.match(dayThisMonthRegex)) {
            date = parse(val + today.getMonth() + '.' + today.getFullYear(), germanDateFormat, new Date());
            precision = Precision.day;
        }

        else if (val.match(monthRegex)) {
            date = parse('01.' + val, germanDateFormat, new Date());
            precision = Precision.month;
        }

        else if (val.match(monthThisYearRegex)) {
            date = parse(val + today.getFullYear(), germanDateFormat, new Date());
            precision = Precision.month;
        }

        else if (val.match(yearRegex)) {
            date = parse('01.01.' + val, germanDateFormat, new Date());
            precision = Precision.year;
        }

        else {
            date = parseISO(val);
            precision = Precision.iso;
        }
    } catch (_) {
        return [null, null];
    }

    return isValid(date) ? [date, precision] : [null, null];
}

export const DateEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null){
            return false;
        }

        switch (pValB) {
            case Precision.day:
                return tValA.getDay() == tValB.getDay() && tValA.getMonth() === tValB.getMonth() && tValA.getFullYear() === tValB.getFullYear();
            case Precision.month:
                return tValA.getMonth() === tValB.getMonth() && tValA.getFullYear() === tValB.getFullYear();
            case Precision.year:
                return tValA.getFullYear() === tValB.getFullYear();
            case Precision.iso:
                switch (pValA) {
                    case Precision.day:
                    case Precision.iso:
                        return tValA.getDay() == tValB.getDay() && tValA.getMonth() === tValB.getMonth() && tValA.getFullYear() === tValB.getFullYear();
                    case Precision.month:
                        return tValA.getMonth() === tValB.getMonth() && tValA.getFullYear() === tValB.getFullYear();
                    case Precision.year:
                        return tValA.getFullYear() === tValB.getFullYear();
                }
        }
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        const [tValA, pValA] = transformValue(valueA);
        const [tValB, pValB] = transformValue(valueB);

        if (tValA == null || tValB == null){
            return false;
        }

        switch (pValB) {
            case Precision.day:
                return tValA.getDay() != tValB.getDay() || tValA.getMonth() !== tValB.getMonth() || tValA.getFullYear() !== tValB.getFullYear();
            case Precision.month:
                return tValA.getMonth() !== tValB.getMonth() || tValA.getFullYear() !== tValB.getFullYear();
            case Precision.year:
                return tValA.getFullYear() !== tValB.getFullYear();
            case Precision.iso:
                switch (pValA) {
                    case Precision.day:
                    case Precision.iso:
                        return tValA.getDay() != tValB.getDay() || tValA.getMonth() !== tValB.getMonth() || tValA.getFullYear() !== tValB.getFullYear();
                    case Precision.month:
                        return tValA.getMonth() !== tValB.getMonth() || tValA.getFullYear() !== tValB.getFullYear();
                    case Precision.year:
                        return tValA.getFullYear() !== tValB.getFullYear();
                }
        }
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null;
    },
};
