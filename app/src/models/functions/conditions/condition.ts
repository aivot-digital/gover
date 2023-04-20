import {ConditionOperandReference} from "./condition-operand-reference";
import {ConditionOperandValue} from "./condition-operand-value";

export interface Condition {
    operator: number; // TODO: Enum
    operandA: ConditionOperandReference | ConditionOperandValue;
    operandB: ConditionOperandReference | ConditionOperandValue;
    conditionUnmetMessage: string;
}
