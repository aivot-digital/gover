import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from "../utils/string-utils";
import {isValid, parseISO} from "date-fns";

const timeRegex = /^\d\d:\d\d$/;

function transformValue(val: any): number | null {
    if (val == null) {
        return null;
    }
    if (typeof val !== 'string') {
        return null;
    }

    let h: number | null = null;
    let m: number | null = null;

    const timeIso = parseISO(val);
    if (isValid(timeIso)) {
        h = timeIso.getHours();
        m = timeIso.getMinutes();
    } else {
        if (!val.match(timeRegex)) {
            return null;
        }
        const [hStr, mStr] = val.split(':');

        h = parseInt(hStr);
        if (isNaN(h)) {
            return null;
        }

        m = parseInt(mStr);
        if (isNaN(m)) {
            return null;
        }
    }

    return (h % 24) * 60 + (m % 60);
}

export const TimeEvaluator: BaseEvaluator<string> = {
    [ConditionOperator.Equals]: (valueA, valueB) => {
        return transformValue(valueA) === transformValue(valueB);
    },
    [ConditionOperator.NotEquals]: (valueA, valueB) => {
        return transformValue(valueA) !== transformValue(valueB);
    },

    [ConditionOperator.LessThan]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA < tValB;
    },
    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA <= tValB;
    },

    [ConditionOperator.GreaterThan]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA > tValB;
    },
    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) => {
        const tValA = transformValue(valueA);
        const tValB = transformValue(valueB);
        return tValA != null && tValB != null && tValA >= tValB;
    },

    [ConditionOperator.Empty]: (valueA, _) => {
        return isStringNullOrEmpty(valueA);
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return isStringNotNullOrEmpty(valueA);
    },
};
