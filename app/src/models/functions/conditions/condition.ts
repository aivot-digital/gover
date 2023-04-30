import {ConditionOperandReference} from "./condition-operand-reference";
import {ConditionOperandValue} from "./condition-operand-value";
import {ConditionOperator} from "../../../data/condition-operator";

export interface Condition {
    operator?: ConditionOperator;
    operandA?: ConditionOperandReference | ConditionOperandValue;
    operandB?: ConditionOperandReference | ConditionOperandValue;
    conditionUnmetMessage?: string;
}
