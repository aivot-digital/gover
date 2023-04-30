export interface ConditionOperandValue {
    value: string;
}

export function isConditionOperandValue(obj: any): obj is ConditionOperandValue {
    return obj != null && 'value' in obj;
}
