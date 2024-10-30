import {ConditionOperator} from "../data/condition-operator";
import {BaseEvaluator} from "./base-evaluator";

function resolveValueA(valueA: string[] | undefined) {
    return valueA != null ? valueA.length : 0;
}

function resolveValueB(valueB: any) {
    if (valueB == null) {
        return 0;
    } else if (Array.isArray(valueB)) {
        return valueB.length;
    } else if (typeof valueB === 'string') {
        return parseInt(valueB);
    } else if (typeof valueB === 'number') {
        return valueB;
    } else {
        return 0;
    }
}

export const ReplicatingContainerEvaluator: BaseEvaluator<string[]> = {
    [ConditionOperator.Empty]: (valueA, _) => {
        return valueA == null || valueA.length === 0;
    },
    [ConditionOperator.NotEmpty]: (valueA, _) => {
        return valueA != null && valueA.length > 0;
    },

    [ConditionOperator.ReplicatingListLengthEquals]: (valueA, valueB) => {
        return resolveValueA(valueA) === resolveValueB(valueB);
    },
    [ConditionOperator.ReplicatingListLengthNotEquals]: (valueA, valueB) => {
        return resolveValueA(valueA) !== resolveValueB(valueB);
    },

    [ConditionOperator.ReplicatingListLengthLessThan]: (valueA, valueB) => {
        return resolveValueA(valueA) < resolveValueB(valueB);
    },

    [ConditionOperator.ReplicatingListLengthLessThanOrEqual]: (valueA, valueB) => {
        return resolveValueA(valueA) <= resolveValueB(valueB);
    },

    [ConditionOperator.ReplicatingListLengthGreaterThan]: (valueA, valueB) => {
        return resolveValueA(valueA) > resolveValueB(valueB);
    },
    [ConditionOperator.ReplicatingListLengthGreaterThanOrEqual]: (valueA, valueB) => {
        return resolveValueA(valueA) >= resolveValueB(valueB);
    },
};
