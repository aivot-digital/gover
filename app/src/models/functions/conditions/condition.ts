import {ConditionOperator} from "../../../data/condition-operator";

export interface Condition {
    operator?: ConditionOperator;
    reference: string;
    target?: string | null;
    value?: string | null;
    conditionUnmetMessage?: string | null;
}
